package com.amarsoft.p2ptrade.account;
/**
 * 官网找回手机号后触发的界面
 * 输入参数：	
 * 			UserID			用户名
 * 			CertID			身份证号码
 * 			Answer			答案
 * 			AnswerName		答案字段名
 * 			OTPCode			短信验证码
 * 输出参数：
 */
import java.util.Properties;
import java.util.Random;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.ConfirmMobileChangeHandler;
import com.amarsoft.p2ptrade.account.ConfirmPasswordRetrieveHandler;
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
import com.amarsoft.p2ptrade.project.InvestUserCheckHandler;

public class QueryUserCertIDrHandler extends JSONHandler {
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		if(request.containsKey("UserID")==false || request.get("UserID")==null)
			throw new HandlerException("common.emptyuserid");

		try{
			//校验身份证及安全问题
			BizObject obj = JBOFactory.getBizObjectManager("jbo.trade.user_authentication")
				.createQuery("select DOCID from o where  userid=:userid")
				.setParameter("userid", request.get("UserID").toString())
				.getSingleResult(false);
			if(obj == null){
				throw new HandlerException("common.usernotauth");
			}
			JSONObject result = new JSONObject();
			result.put("certid", obj.getAttribute("DOCID").getString());
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}

}
