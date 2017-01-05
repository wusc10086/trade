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
 * 设置安全问题
 * 输入参数：
 * 		UserID				用户ID
 * 		SecurityQuestion	安全问题1
 * 		SecurityAnswer		安全问题答案1
 * 		SecurityQuestion2   安全问题2
 * 		SecurityAnswer2		安全问题答案2
 * 		SecurityQuestion3   安全问题3
 * 		SecurityAnswer3		安全问题答案3
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 * @author dxu
 *
 */
public class SetSecurityQuestionHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
		String securityQuestion1 = (String) request.get("SecurityQuestion");
		String securityAnswer1 = (String) request.get("SecurityAnswer");
		String securityQuestion2 = (String) request.get("SecurityQuestion2");
		String securityAnswer2 = (String) request.get("SecurityAnswer2");
		String securityQuestion3 = (String) request.get("SecurityQuestion3");
		String securityAnswer3 = (String) request.get("SecurityAnswer3");
		
		if(userID == null || userID.length() == 0){
			throw new HandlerException("common.emptyuserid");
		}
		if(securityQuestion1 == null || securityQuestion1.length() == 0){
			throw new HandlerException("securityquestion.emptyquestion");
		}
		if(securityAnswer1 == null || securityAnswer1.length() == 0){
			throw new HandlerException("securityquestion.emptyanswer");
		}
		if(securityQuestion2 == null || securityQuestion2.length() == 0){
			throw new HandlerException("securityquestion.emptyquestion");
		}
		if(securityAnswer2 == null || securityAnswer2.length() == 0){
			throw new HandlerException("securityquestion.emptyanswer");
		}
		if(securityQuestion3 == null || securityQuestion3.length() == 0){
			throw new HandlerException("securityquestion.emptyquestion");
		}
		if(securityAnswer3 == null || securityAnswer3.length() == 0){
			throw new HandlerException("securityquestion.emptyanswer");
		}
		
		JSONObject result = new JSONObject();
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = userAcctManager.createQuery("UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAccount = query.getSingleResult(true);
			if(userAccount == null){
				throw new HandlerException("common.usernotexist");
			}else{
				userAccount.setAttributeValue("SECURITYQUESTION", securityQuestion1);
				userAccount.setAttributeValue("SECURITYANSWER", securityAnswer1);
				
				userAccount.setAttributeValue("SECURITYQUESTION2", securityQuestion2);
				userAccount.setAttributeValue("SECURITYANSWER2", securityAnswer2);
				
				userAccount.setAttributeValue("SECURITYQUESTION3", securityQuestion3);
				userAccount.setAttributeValue("SECURITYANSWER3", securityAnswer3);
				userAcctManager.saveObject(userAccount);
				
				BizObjectManager userAcctAuditManager =jbo.getManager("jbo.trade.account_audit");
				
				BizObject userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYQUESTION");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYQUESTION").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityQuestion1);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
				userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYANSWER");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYANSWER").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityAnswer1);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
				userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYQUESTION2");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYQUESTION").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityQuestion2);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
				userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYANSWER2");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYANSWER").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityAnswer2);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
				userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYQUESTION3");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYQUESTION").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityQuestion3);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
				userAcctAudit = userAcctAuditManager.newObject();
				userAcctAudit.setAttributeValue("USERID", userID);
				userAcctAudit.setAttributeValue("CHANGETYPE", "SECURITYANSWER3");
				userAcctAudit.setAttributeValue("OLDVALUE1", userAccount.getAttribute("SECURITYANSWER").getString());
				userAcctAudit.setAttributeValue("NEWVALUE1", securityAnswer3);
				userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
				userAcctAuditManager.saveObject(userAcctAudit);
				
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("securityquestion.error");
		}
		result.put("SuccessFlag", "S");
		return result;
	}

}
