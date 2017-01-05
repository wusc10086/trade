package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 验证安全问题
 * 输入参数：
 * 		UserID				用户ID
 * 		SecurityQuestion   	安全问题
 * 		SecurityAnswer		安全问题答案
 * 输出参数：
 * 		SuccessFlag:		成功标识	S/F
 * @author dxu
 *
 */
public class ValidSecurityQuestionHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
		String securityQuestion = (String) request.get("SecurityQuestion");
		String securityAnswer = (String) request.get("SecurityAnswer");
		
		if(userID == null || userID.length() == 0){
			throw new HandlerException("common.emptyuserid");
		}
		if(securityQuestion == null || securityQuestion.length() == 0){
			throw new HandlerException("securityquestion.emptyquestion");
		}
		if(securityAnswer == null || securityAnswer.length() == 0){
			throw new HandlerException("securityquestion.emptyanswer");
		}
		
		JSONObject result = new JSONObject();
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = userAcctManager.createQuery("UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAccount = query.getSingleResult(false);
			if(userAccount == null){
				throw new HandlerException("common.usernotexist");
			}else{
				String question1 = userAccount.getAttribute("SECURITYQUESTION").getString();
				String answer1 = userAccount.getAttribute("SECURITYANSWER").getString();
				
				String question2 = userAccount.getAttribute("SECURITYQUESTION2").getString();
				String answer2 = userAccount.getAttribute("SECURITYANSWER2").getString();
				
				String question3 = userAccount.getAttribute("SECURITYQUESTION3").getString();
				String answer3 = userAccount.getAttribute("SECURITYANSWER3").getString();
				
				if(question1 == null || question1.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				if(answer1 == null || answer1.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				if(question2 == null || question2.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				if(answer2 == null || answer2.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				if(question3 == null || question3.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				if(answer3 == null || answer3.length() == 0){
					throw new HandlerException("securityquestion.securityquestionnotset");
				}
				
				if(question1.equals(securityQuestion) && answer1.equals(securityAnswer)){
					result.put("SuccessFlag", "S");
				}else if(question2.equals(securityQuestion) && answer2.equals(securityAnswer)){
					result.put("SuccessFlag", "S");
				}else if(question3.equals(securityQuestion) && answer3.equals(securityAnswer)){
					result.put("SuccessFlag", "S");
				}else{
					result.put("SuccessFlag", "F");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("securityquestion.error");
		}
		return result;
	}

}
