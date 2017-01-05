package com.amarsoft.p2ptrade.usercenterchange;
/**
 * 官网找回手机号后触发的界面
 * 输入参数：	
 * 			UserID			用户名
 * 输出参数：
 * 			question		安全问题
 * 			answerName	安全答案字段名
 * 			type			变更类型
 */
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
import com.amarsoft.p2ptrade.project.InvestUserCheckHandler;

public class QuestionGetterHandler extends JSONHandler {
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		
		//获取会员信息
		QueryUserAccountInfoHandler qd = new QueryUserAccountInfoHandler();
		JSONObject rq = (JSONObject)qd.createResponse(request, arg1);
		//检查账户是否异常
		//MobileChangeFlag		官网手机变更标识
		if(InvestUserCheckHandler.IGNORE_MOBILE_CHECK || (rq.containsKey("MobileChangeFlag") && "1".equalsIgnoreCase(rq.get("MobileChangeFlag").toString()))){
			result.put("type", "MobileChange");
		}
		//RetrievePasswordFlag	官网找回密码标识
		else if(InvestUserCheckHandler.IGNORE_PASSWORD_CHECK ||(rq.containsKey("RetrievePasswordFlag") && "1".equalsIgnoreCase(rq.get("RetrievePasswordFlag").toString()))){
			result.put("type", "RetrievePassword");
		}
		else{
			result.put("type", "NOCHANGE");
		}
		//返回安全问题
		SecureRandom random = new SecureRandom();
		int index = random.nextInt(3)+1;
		createSafeQuestion(index,request,result);
		return result;
	}
	
	private void createSafeQuestion(int index,JSONObject request,JSONObject result)throws HandlerException{
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_account");
			String sColName = "SECURITYQUESTION";
			String sAnswerName = "SECURITYANSWER";
			if(index>1){
				sColName = sColName + index;
				sAnswerName = sAnswerName + index;
			}
			result.put("answerName", sAnswerName);
			BizObject obj = manager.createQuery("select " + sColName + " from o where o.userid=:userid").setParameter("userid", request.get("UserID").toString()).getSingleResult(false);
			if(obj==null)
				throw new HandlerException("check.security.error");
			else{
				result.put("question", obj.getAttribute(sColName).getString());
			}
		}
		catch(JBOException je){
			throw new HandlerException("default.database.error");
		}
		
	}

}
