package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 用户注册交易
 * 输入参数：
 * 		userid:	用户id
 * 		old_password:   初始密码
 * 		new_password:	新密码	
 *      confirm_password  确认密码
 *      question		安全问题
 *      answer			问题答案
 * 输出参数：
 * 		SuccessFlag:是否注册成功	S/F
 *
 */
public class ModifyPasswordHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String userid = (String) request.get("userid");
		String old_password = (String) request.get("old_password");
		String new_password = (String) request.get("new_password");
		String confirm_password = (String) request.get("confirm_password");
		//String question = (String) request.get("question");
		//String answer = (String) request.get("answer");
		//原始密码不能为空
		if(old_password == null || old_password.length() == 0){
			throw new HandlerException("modify.emptyoldpassword");
		}
		//新密码不能为空
		if(new_password == null || new_password.length() == 0){
			throw new HandlerException("modify.emptynewpassword");
		}
		//确认密码不能为空
		if(confirm_password == null || confirm_password.length() == 0){
			throw new HandlerException("modify.emptyconfirmpassword");
		}
	/*	//安全问题不能为空
		if(question == null || question.length() == 0){
			throw new HandlerException("modify.emptyquestion");
		}
		//问题答案不能为空
		if(answer == null || answer.length() == 0){
			throw new HandlerException("modify.emptyanswer");
		}*/
		JSONObject result = new JSONObject();
		
		try{
			//校验原始密码
			old_password = MessageDigest.getDigestAsUpperHexString("MD5", old_password);
			BizObject boz = JBOFactory.getBizObjectManager("jbo.trade.user_account")
					.createQuery("select UserID from o where password=:password and userid=:userid")
					.setParameter("password", old_password.toUpperCase())
					.setParameter("userid", userid)
					.getSingleResult(false);
			if(boz==null){
				throw new HandlerException("old_password.exist.error");
			}
	/*		//校验安全问题
			BizObject boz1 = JBOFactory.getBizObjectManager("jbo.trade.user_account")
					.createQuery("select UserID from o where securityquestion=:securityquestion and userid=:userid")
					.setParameter("securityquestion", question)
					.setParameter("userid", userid)
					.getSingleResult(false);
			if(boz1==null){
				throw new HandlerException("question.exist.error");
			}
			//校验问题答案
			BizObject boz2 = JBOFactory.getBizObjectManager("jbo.trade.user_account")
					.createQuery("select UserID from o where securityanswer=:securityanswer and userid=:userid")
					.setParameter("securityanswer", answer)
					.setParameter("userid", userid)
					.getSingleResult(false);
			if(boz2==null){
				throw new HandlerException("answer.exist.error");
			}*/
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
			
			BizObjectQuery query = m.createQuery("select UserID from o where UserID=:UserID ");
			query.setParameter("UserID", userid);
    
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				o.setAttributeValue("PASSWORD", MessageDigest.getDigestAsUpperHexString("MD5", new_password));
				System.out.println("--------------"+new_password);
				m.saveObject(o);
			
			}
			
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("register.error");
		}	
		result.put("SuccessFlag", "S");
		return result;
	}
}
