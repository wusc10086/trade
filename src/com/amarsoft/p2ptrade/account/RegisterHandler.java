package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.invest.P2pString;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 用户注册交易
 * 输入参数：
 * 		UserName:	用户名
 * 		Mobile:		手机号
 * 		Password:	密码	
 * 输出参数：
 * 		SuccessFlag:是否注册成功	S/F
 * 		UserID:		用户ID
 *
 */
public class RegisterHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String username = (String) request.get("username");
		String mobile = (String) request.get("phone");
		String password = (String) request.get("password");

		
		JSONObject result = null;
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
			
			BizObjectQuery query = m.createQuery("select username from o where username=:username");
			query.setParameter("username", username);
    
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				throw new HandlerException("register.alreadyregistered");
			}
			
			query = m.createQuery("select username from o where PhoneTel=:PhoneTel");
			query.setParameter("PhoneTel", mobile);
			
			o = query.getSingleResult(false);
			if(o!=null){
				throw new HandlerException("register.mobileduplicate");
			}
			result = registerUser(request);
		}catch(HandlerException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("register.error");
		}		
		return result;
	}
	
	private JSONObject registerUser(JSONObject request) throws Exception{
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = jbo.createTransaction();
		try {
			String username = (String) request.get("username");
			String mobile = (String) request.get("phone");
			String password = (String) request.get("password");
			String usercode = (String) request.get("usercode");
			String email = (String) request.get("email");
			//用户名不能为空
			if(username == null || username.length() == 0){
				throw new HandlerException("用户不能为空！");
			}
			if(username.length()<4||username.length()>20){
				throw new HandlerException("用户的长度不符合！");
			}
			//密码不能为空
			if(password == null || password.length() == 0){
				throw new HandlerException("密码不能为空！");
			}
			if(password.length()<6||password.length()>40){
				throw new HandlerException("密码的长度不符合！");
			}
			//手机号不能为空
			if(mobile == null || mobile.length() == 0){
				throw new HandlerException("手机号不能为空！");
			}
			if(mobile.length() != 11){
				throw new HandlerException("手机号长度不符合！");
			}
			if(usercode==null) usercode = "";
			
			BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account",tx);
			BizObjectQuery query = userAcctManager.createQuery("select * from o where UserName=:UserName");
			query.setParameter("UserName", username);
			BizObject userAccount = query.getSingleResult(true);
			String sUserid = "";
			if(userAccount == null){
				
				String invitecode = P2pString.getInviteCode();//邀请码
				userAccount = userAcctManager.newObject();
				userAccount.setAttributeValue("USERNAME", username);
				userAccount.setAttributeValue("PASSWORD", password);
				userAccount.setAttributeValue("PHONETEL", mobile);
				userAccount.setAttributeValue("usercode", usercode);
				userAccount.setAttributeValue("email", email);
				userAccount.setAttributeValue("invitecode", invitecode.toLowerCase());
				userAccount.setAttributeValue("PHONEAUTHFLAG", "2");
				userAccount.setAttributeValue("INPUTTIME", StringFunction.getTodayNow());
				userAccount.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctManager.saveObject(userAccount);
				
				sUserid = userAccount.getAttribute("USERID").toString();
				
				
				//用户激活操作以及用户手机绑定
				java.util.HashMap<String,Object> recordMap = new java.util.HashMap<String,Object>();
				recordMap.put("USERID", sUserid);
				recordMap.put("PHONENO", mobile);
				recordMap.put("USERACCOUNTTYPE", "001"); //默认为个人客户
				//暂时写死
				RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation("3010", "1000");
				rttt.init(recordMap);
				rttt.execute();
				//如果不成功，则抛出异常
				if(!rttt.getTemplet().isSuccess())
					throw new HandlerException("register.error");
				
				
				//查询邀请者
//				BizObjectQuery q = userAcctManager.createQuery(" select userid from o where ( userid=:usercode or username=:usercode or phonetel=:usercode or invitecode=:usercode) and userauthflag='2'");
//					q.setParameter("usercode", usercode);
//				BizObject o = q.getSingleResult(false);
//				if(o!=null){
//					String sUserID = o.getAttribute("userid").toString();
//					//插入返利
//					setTransfer(sUserID,tx);
//				}		
				
				//用户详情表中插入信息
				BizObjectManager detailManager =jbo.getManager("jbo.trade.account_detail",tx);
				BizObjectQuery qq = detailManager.createQuery("select * from o where UserID=:UserID");
				qq.setParameter("UserID", sUserid);
				BizObject userDetail = qq.getSingleResult(true);
				if(userDetail==null){
					userDetail = detailManager.newObject();
					userDetail.setAttributeValue("UserID", sUserid);
					userDetail.setAttributeValue("INPUTTIME", StringFunction.getTodayNow());
					userDetail.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
					detailManager.saveObject(userDetail);
				}
				result.put("userid", sUserid);
				result.put("username", username);
				result.put("mobile", mobile);
				result.put("SuccessFlag", "S");
				tx.commit();
			}else
				result.put("SuccessFlag", "F");
		} catch (Exception e) {
			if(tx!=null)
				tx.rollback();
			e.printStackTrace();
			throw new HandlerException(e.getMessage());
		}
		return result;
	}
	
	/**
	 * 插入返利信息
	 * */
	private void setTransfer(String sUserID,JBOTransaction tx){
		JBOFactory jbo = JBOFactory.getFactory();
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.restore_rule");
			//从签到返利规则查询当前可返利金额
			BizObjectQuery ruleq = m.createQuery(" select restoresum from o where rule.rulecode='invite_reg'");

			BizObject ruleo = ruleq.getSingleResult(false);
			double restoresum = 0;
			if(ruleo!=null){
				restoresum = ruleo.getAttribute("restoresum").getDouble();
			}
			
			if(restoresum>0){
				//插入交易记录
				BizObjectManager recordm =jbo.getManager("jbo.trade.transaction_record",tx);
				
				//插入交易记录
				BizObject recordo1 = recordm.newObject();
				recordo1.setAttributeValue("USERID", ARE.getProperty("HouBankSerialNo"));
				recordo1.setAttributeValue("relaaccount", "");
				recordo1.setAttributeValue("DIRECTION", "P");
				recordo1.setAttributeValue("AMOUNT", restoresum);
				recordo1.setAttributeValue("BALANCE", 0);
				recordo1.setAttributeValue("TRANSTYPE", "2030");
				recordo1.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo1.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo1.setAttributeValue("STATUS", "01");
				recordo1.setAttributeValue("USERACCOUNTTYPE", "003");
				recordm.saveObject(recordo1);
				
				BizObject recordo = recordm.newObject();
				recordo.setAttributeValue("USERID", sUserID);
				recordo.setAttributeValue("relaaccount", "");
				recordo.setAttributeValue("DIRECTION", "R");
				recordo.setAttributeValue("AMOUNT", restoresum);
				recordo.setAttributeValue("BALANCE", 0);
				recordo.setAttributeValue("TRANSTYPE", "2030");
				recordo.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo.setAttributeValue("STATUS", "01");
				recordo.setAttributeValue("USERACCOUNTTYPE", "001");
				recordm.saveObject(recordo);
				
				//插入打款交易
				BizObjectManager mm =jbo.getManager("jbo.trade.acct_transfer",tx);
				
				//平台营销账户
				BizObject transfer1 = mm.newObject();
				transfer1.setAttributeValue("objectno", "");
				transfer1.setAttributeValue("objecttype", "030");
				transfer1.setAttributeValue("seqid", "1");
				transfer1.setAttributeValue("userid", ARE.getProperty("HouBankSerialNo"));
				transfer1.setAttributeValue("direction", "R");
				transfer1.setAttributeValue("amount", restoresum);
				transfer1.setAttributeValue("status", "01");
				transfer1.setAttributeValue("inputdate", StringFunction.getToday());
				transfer1.setAttributeValue("inputtime", StringFunction.getNow());
				transfer1.setAttributeValue("transserialno", recordo1.getAttribute("serialno"));
				transfer1.setAttributeValue("remark", "代收邀请注册收益");
				transfer1.setAttributeValue("transcode", "1001");
				transfer1.setAttributeValue("useraccounttype", "003");
				mm.saveObject(transfer1);
				
				//邀请人
				BizObject transfer = mm.newObject();						
				transfer.setAttributeValue("objectno", "");
				transfer.setAttributeValue("objecttype", "030");
				transfer.setAttributeValue("seqid", "2");
				transfer.setAttributeValue("userid", sUserID);
				transfer.setAttributeValue("direction", "P");
				transfer.setAttributeValue("amount", restoresum);
				transfer.setAttributeValue("status", "01");
				transfer.setAttributeValue("inputdate", StringFunction.getToday());
				transfer.setAttributeValue("inputtime", StringFunction.getNow());
				transfer.setAttributeValue("transserialno", recordo.getAttribute("SERIALNO"));
				transfer.setAttributeValue("remark", "代付邀请注册收益");
				transfer.setAttributeValue("transcode", "2001");
				transfer.setAttributeValue("useraccounttype", "001");
				mm.saveObject(transfer);
			}
		}catch(Exception e){}
	}
}
