package com.amarsoft.p2ptrade.personcenter;


import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2p.interfaces.utils.InterfaceHelper;
import com.amarsoft.p2ptrade.account.AddRestore;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 发起绑卡交易 输入参数： UserID:账户编号 AccountNo:银行卡号 AccountName:账户名 AccountBelong:开户行
 * BelongArea:开户地区 输出参数： 成功标识 BindFlag：是否绑卡申请成功 true-->成功 false-->失败
 * 
 * 
 */
public class BindingCardCodeHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return runBindingCardApply(request);
	}

	/**
	 * 发起绑卡
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject runBindingCardApply(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("SerialNo") == null
				|| "".equals(request.get("SerialNo")))
			throw new HandlerException("SerialNo.error");
		
		if (request.get("transSerialNo") == null
				|| "".equals(request.get("transSerialNo")))
			throw new HandlerException("transSerialNo.error");
		
		if (request.get("PhoneCode") == null
				|| "".equals(request.get("PhoneCode")))
			throw new HandlerException("PhoneCode.error");
		
		String sUserID = request.get("UserID").toString();
		String sSerialNo = request.get("SerialNo").toString();
		String sPhoneCode = (String)request.get("PhoneCode");
		String stransSerialNo = request.get("transSerialNo").toString();
		
		JSONObject result = new JSONObject();

		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		BizObjectManager m = null;
		try {
			
			tx = jbo.createTransaction();
			m = jbo.getManager("jbo.trade.account_info");
			BizObject o = m.createQuery(" serialno=:serialno and USERID=:USERID")
					.setParameter("serialno", sSerialNo)
					.setParameter("USERID", sUserID)
					.getSingleResult(true);
			if(o!=null){
				//发送绑卡交易
				
				BizObjectManager recordManager = jbo.getManager(
				"jbo.trade.transaction_record");
				
				BizObject bo = recordManager.createQuery(" serialno=:serialno ").setParameter("serialno",stransSerialNo).getSingleResult(false);
				
				JSONObject obj = new JSONObject();
				for (int i = 0; i < bo.getAttributeNumber(); i++) {
					obj.put(bo.getAttribute(i).getName()
							.toUpperCase(), bo.getAttribute(i)
							.getValue());
				}
				
				//暂时写死 实名认证信息
				String channel = String.valueOf(obj.get("TRANSCHANNEL"));
				String transCode = "1073";//String.valueOf(obj.get("TRANSTYPE"));
				obj.put("TRANSTYPE", transCode);
				obj.put("VALIDCODE", sPhoneCode);
				
				RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation(channel, transCode);
				rttt.init(obj);
				rttt.execute();
				
				if(rttt.getTemplet().isSuccess())
				{
					o.setAttributeValue("status", "2");
					m.saveObject(o);
					//返利
					AddRestore.setRestore(sUserID,"invite_reg",tx);
					tx.commit();
					result.put("BindFlag", "1");
				}
				else
				{
					//rttt.getTemplet().getFeedBackCode()
					ARE.getLog().info("bind card error：" + rttt.getTemplet().getFeedBackMsg() + "["+rttt.getTemplet().getFeedBackCode()+"]");
					result.put("BindFlag", "0");
					result.put("serialno", sSerialNo);
					result.put("transSerialNo", stransSerialNo);
				}
			}else{
				result.put("BindFlag", "2");								
			}
			
			return result;
		} catch (Exception e) {
			if(tx!=null)
				try {
					tx.rollback();
				} catch (JBOException e1) {
					e1.printStackTrace();
				}
			e.printStackTrace();
			throw new HandlerException("bindingcardapply.error");
		}
	}

	/**
	 * 改变原有已绑定银行卡的状态，原状态 -->失效
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param tx
	 *            JBOTransaction
	 * @param sUserID
	 *            用户ID
	 * @throws HandlerException
	 */
	private void changeOriginalCardStatus(JBOFactory jbo, JBOTransaction tx,
			String sUserID) throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.account_info", tx);
			BizObjectQuery query = m
					.createQuery("userid=:userid order by inputtime desc");
			query.setParameter("userid", sUserID);

			List<BizObject> list = query.getResultList(true);
			if (list != null) {
				for (Iterator<BizObject> it = list.iterator(); it.hasNext();) {
					BizObject o = it.next();
					o.setAttributeValue("STATUS", "5");
					o.setAttributeValue("UPDATETIME",
							new TimeTool().getsCurrentMoment());
					m.saveObject(o);
				}
			}
		} catch (Exception e) {
			throw new HandlerException("changeoriginalcardstatus.error");
		}
	}

	/**
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param recordManager
	 *            BizObjectManager
	 * @param recordBo
	 *            BizObject
	 * @param sUserID
	 *            用户编号
	 * @param sSerialNo
	 *            帐号流水
	 * @param sAccountBelong
	 *            开户银行
	 * @param amount
	 *            交易金额
	 * @param sInputTime
	 *            插入时间
	 * @param sStatus
	 *            交易状态
	 * @param sTransType
	 *            交易类型
	 * @param items
	 *            开户银行码值键值对
	 * @return
	 * @throws HandlerExceptionsaveTransRecord
	 */
	private String saveTransRecord(JBOFactory jbo,
			BizObjectManager recordManager, BizObject recordBo, String sUserID,
			String sSerialNo, String sAccountBelong, double amount,
			String sInputTime, String sStatus, String sTransType,
			JSONObject items) throws HandlerException {
		try {
			String sAccountBelongName = items.containsKey(sAccountBelong) ? items
					.get(sAccountBelong).toString() : sAccountBelong;
			recordBo.setAttributeValue("USERID", sUserID);// 用户编号
			recordBo.setAttributeValue("DIRECTION", "P");// 发生方向(付)
			recordBo.setAttributeValue("AMOUNT", amount);// 交易金额
			recordBo.setAttributeValue("HANDLCHARGE", 0);// 手续费
			recordBo.setAttributeValue("TRANSTYPE", sTransType);// 交易类型(绑卡打款)
			recordBo.setAttributeValue("STATUS", sStatus);// 交易状态
			recordBo.setAttributeValue("INPUTTIME", sInputTime);// 创建时间
			recordBo.setAttributeValue("RELAACCOUNT", sSerialNo);// 关联账户流水号
			recordBo.setAttributeValue("RELAACCOUNTTYPE", "001");// 交易关联账户类型
			String sTransChannel  = null;
			
			sTransChannel = GeneralTools.getTransChannel(jbo,
					sAccountBelong, "ATTRIBUTE2");
			
			/*if(!("0302".equals(sAccountBelong))){
				sTransChannel = GeneralTools.getTransChannel(jbo,
						sAccountBelong, "ATTRIBUTE2");
			}else{
				sTransChannel = GeneralTools.getTransChannel(jbo,
						sAccountBelong, "ATTRIBUTE6");
			}
			*/
			
			recordBo.setAttributeValue("TRANSCHANNEL", sTransChannel);// 交易渠道
			recordBo.setAttributeValue("REMARK", "" + "银行卡绑卡|"
					+ sAccountBelongName);// 备注

			recordManager.saveObject(recordBo);
			return recordBo.getAttribute("SERIALNO").toString();

		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("savetransrecord.error");
		}
	}

	/**
	 * 获取手机号码
	 * 
	 * @param accountBo
	 * @throws HandlerException
	 */
	private String getPhoneTel(JBOFactory jbo, String sUserID)
			throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("PHONETEL").getString();
			} else {
				return "";
			}
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}


/**
 * 获取用户真实姓名
 * 
 * @param accountBo
 * @throws HandlerException
 */
private String getRealName(JBOFactory jbo, String sUserID)
		throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.account_detail");
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("REALNAME").getString();
			} else {
				return "";
			}
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}

/**
 * 回滚实名认证
 * 
 * @param accountBo
 * @throws HandlerException
 */
private void RollbackRealName(JBOFactory jbo, String sUserID)
		throws HandlerException {
		BizObjectManager ma;
		BizObjectManager mb;
		BizObjectManager mc;
		try {
			
			//清除实名和身份证
			ma = jbo.getManager("jbo.trade.account_detail");
			BizObject o1=ma.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			o1.setAttributeValue("realname", null);
			o1.setAttributeValue("CERTID", null);
			ma.saveObject(o1);
			
			//修改实名认证标志
			mb = jbo.getManager("jbo.trade.user_account");
			BizObject o2=mb.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			o2.setAttributeValue("USERAUTHFLAG", null);
			mb.saveObject(o2);
			
			//删除认证记录
			mc = jbo.getManager("jbo.trade.user_authentication");
			BizObject o3=mc.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			mc.deleteObject(o3);
			
		} catch (Exception e) {
			
		}
	}
}
