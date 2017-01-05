package com.amarsoft.p2ptrade.account;


import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 绑定手机
 * 输入参数：
 * 		UserID		用户ID
 * 		phonetel   ：手机号
 * 		phoneauthflag：手机是否认证，‘2’为已认证
 * 
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 * 		FailCode:	失败原因
 * 		FailDesc:	失败原因说明		
 * @author yxpan 2014/9/24
 *
 */                     
public class  ModifyPhoneHandler extends JSONHandler {

	@SuppressWarnings("unchecked")
	@Override
	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
		//原手机号码
		//String phonetel1= (String) request.get("phonetel1");
		//新手机号码
		String phonetel = (String) request.get("phonetel");
       //判断用户ID是否为空
		if(userID == null || userID.length() == 0){
			throw new HandlerException("modifyuseraccount.emptyuserid");
		}
		//判断新手机手机号是否为空
		if(phonetel == null || phonetel.length() == 0){
			throw new HandlerException("modifyphonetel.empty");
		}
		JBOFactory jbo = JBOFactory.getFactory();
		JSONObject result = new JSONObject();
		//从数据库查询原来的手机号码
		String oldphonetel=getOldPhone(jbo,phonetel);
		if(!phonetel.equals(oldphonetel)){
			try{
				
				BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
				BizObjectQuery query = userAcctManager.createQuery("select * from o where UserID=:UserID");
				query.setParameter("UserID", userID);
				BizObject userAccount = query.getSingleResult(true);
				if(userAccount != null){
					//调用实时交易,发送用户认证交易
					java.util.HashMap<String,Object> recordMap = new java.util.HashMap<String,Object>();
					recordMap.put("USERID", userID);
					recordMap.put("PHONENO", phonetel);
					//暂时写死
					RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation("3010", "1001");
					rttt.init(recordMap);
					rttt.execute();
					
					if(rttt.getTemplet().isSuccess())
					{
						//修改基本信息
						userAccount.setAttributeValue("phonetel", phonetel);
						//默认将手机认证标识设置为2
						userAccount.setAttributeValue("phoneauthflag","2");
					    userAcctManager.saveObject(userAccount);
					}
					else
						throw new HandlerException("modifyuseraccount.error");
					
				}else{
					throw new HandlerException("modifyuseraccount.usernotexist");
				}
			}catch(JBOException e){
				e.printStackTrace();
				throw new HandlerException("modifyuseraccount.error");
			}catch(HandlerException e){
				e.printStackTrace();
				throw e;
			}catch(Exception e){
				e.printStackTrace();
				throw new HandlerException("modifyuseraccount.error");
			}
		
		}else{
			throw new HandlerException("modifyuseraccount.phonetelexist");
	
		}
		 result.put("flag","success");
		 return result;
	}
	//判断数据库手机号码
	@SuppressWarnings("unused")
	private String getOldPhone(JBOFactory jbo, String phonetel) throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m.createQuery("phonetel=:phonetel and phoneauthflag='2'");
			query.setParameter("phonetel", phonetel);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("phonetel").getString();
			} else {
				return "";
			}
		} catch (Exception e) {
			throw new HandlerException("quaryphonetel.error");
			//return "";
		}
	}			
}
