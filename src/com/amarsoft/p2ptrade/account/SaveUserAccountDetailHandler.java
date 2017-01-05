package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 保存用户详细信息
 * 输入参数：
 * 		UserID		用户ID
 * 		RealName    真实姓名
 * 		CertID		身份证号
 * 		Sexual		性别
 * 		BornDate	出生日期
 * 		Education	最高学历
 * 		Marriage	婚姻状况
 * 		City		所在省市
 * 		IndustryType行业类型
 * 		Position	职业
 * 		Income		月收入
 *     	LiveState   居住状况
 *     	EmployeeType 雇佣类型
 *     	InComeLevel 收入水平
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 * @author dxu
 *
 */
public class SaveUserAccountDetailHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
//		String realName = (String) request.get("RealName");
//		String certID = (String) request.get("CertID");
//		String sexual = (String) request.get("Sexual");
//		String bornDate = (String) request.get("BornDate");
		String education = (String) request.get("Education");
		String marriage = (String) request.get("Marriage");
		String city = (String) request.get("City");
//		String industryType = (String) request.get("IndustryType");
//		String position = (String) request.get("Position");
		String inComeLevel = (String) request.get("InComeLevel");
		String liveState = (String) request.get("LiveState");
		String employeeType = (String) request.get("EmployeeType");
		
		
		
		JSONObject result = new JSONObject();
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = userAcctManager.createQuery("select * from o where UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAccount = query.getSingleResult(true);
			if(userAccount == null){
				throw new HandlerException("saveuseracctdetail.usernotexist");
			}
			
			boolean createFlag = false;
			BizObjectManager userAcctDetailManager =jbo.getManager("jbo.trade.account_detail");
			query = userAcctDetailManager.createQuery("select * from o where UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAcctDetail = query.getSingleResult(true);
			BizObject userAcctDetailOld = null;
			if(userAcctDetail == null){
				createFlag = true;
				userAcctDetail = userAcctDetailManager.newObject();
				userAcctDetail.setAttributeValue("USERID", userID);
				userAcctDetail.setAttributeValue("INPUTTIME", StringFunction.getTodayNow());
			}else{
				userAcctDetailOld = (BizObject) userAcctDetail.clone();
			}
//			userAcctDetail.setAttributeValue("REALNAME", realName);
//			userAcctDetail.setAttributeValue("CERTID", certID);
//			userAcctDetail.setAttributeValue("SEXUAL", sexual);
//			userAcctDetail.setAttributeValue("BORNDATE", bornDate);
			userAcctDetail.setAttributeValue("EDUCATION", education);
			userAcctDetail.setAttributeValue("MARRIAGE", marriage);
			userAcctDetail.setAttributeValue("CITY", city);
//			userAcctDetail.setAttributeValue("INDUSTRIALTYPE", industryType);
//			userAcctDetail.setAttributeValue("POSITION", position);
			userAcctDetail.setAttributeValue("INCOMELEVEL", inComeLevel);
			userAcctDetail.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
			userAcctDetail.setAttributeValue("LIVESTATE", liveState);
			userAcctDetail.setAttributeValue("EMPLOYEETYPE", employeeType);
			userAcctDetailManager.saveObject(userAcctDetail);
			
			if(!createFlag && userAcctDetailOld != null){
//				logAudit(jbo,userAcctDetailOld,"REALNAME", realName);
//				logAudit(jbo,userAcctDetailOld,"CERTID", certID);
//				logAudit(jbo,userAcctDetailOld,"SEXUAL", sexual);
//				logAudit(jbo,userAcctDetailOld,"BORNDATE", bornDate);
				logAudit(jbo,userAcctDetailOld,"EDUCATION", education);
				logAudit(jbo,userAcctDetailOld,"MARRIAGE", marriage);
				logAudit(jbo,userAcctDetailOld,"CITY", city);
//				logAudit(jbo,userAcctDetailOld,"INDUSTRIALTYPE", industryType);
//				logAudit(jbo,userAcctDetailOld,"POSITION", position);
				logAudit(jbo,userAcctDetailOld,"LIVESTATE", liveState);
				logAudit(jbo,userAcctDetailOld,"EMPLOYEETYPE", employeeType);
				logAudit(jbo,userAcctDetailOld,"INCOMELEVEL", inComeLevel);
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("saveuseracctdetail.error");
		}
		result.put("SuccessFlag", "S");
		
		return result;
	}
	
	private void logAudit(JBOFactory jbo,BizObject userAcctDetail,String fieldName,String fieldValue) throws Exception{
		BizObjectManager userAcctAuditManager =jbo.getManager("jbo.trade.account_audit");
		String oldValue = userAcctDetail.getAttribute(fieldName).getString();
		if(oldValue == null)oldValue = "";
		if(fieldValue == null)fieldValue = "";
		if(!oldValue.equals(fieldValue)){
			BizObject userAcctAudit = userAcctAuditManager.newObject();
			userAcctAudit.setAttributeValue("USERID", userAcctDetail.getAttribute("UserID").getString());
			userAcctAudit.setAttributeValue("CHANGETYPE", fieldName);
			userAcctAudit.setAttributeValue("OLDVALUE1", oldValue);
			userAcctAudit.setAttributeValue("NEWVALUE1", fieldValue);
			userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
			userAcctAuditManager.saveObject(userAcctAudit);
		}
	}

}
