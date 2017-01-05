package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 用户设置交易密码
 * 输入参数：
 * 		userid:	用户id
 * 		new_transpwd:	新密码	
 *      confirm_transpwd  确认密码
 *      question1		安全问题
 *      answer1			问题答案
 * 输出参数：
 * 		SuccessFlag:是否注册成功	S/F
 *
 */
public class SaveTranspwdAndClearHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String userid = (String) request.get("userid");
		String new_transpwd = (String) request.get("new_transpwd");
		String confirm_transpwd = (String) request.get("confirm_transpwd");
		String question = (String) request.get("question");
		String answer = (String) request.get("answer");
		//新密码不能为空
		if(new_transpwd == null || new_transpwd.length() == 0){
			throw new HandlerException("modify.emptynewtranspwd");
		}
		//确认密码不能为空
		if(confirm_transpwd == null || confirm_transpwd.length() == 0){
			throw new HandlerException("modify.emptyconfirmtranspwd");
		}
//		//安全问题不能为空
//		if(question == null || question1.length() == 0){
//			throw new HandlerException("modify.emptyquestion1");
//		}
//		//问题答案不能为空
//		if(answer == null || answer1.length() == 0){
//			throw new HandlerException("modify.emptyanswer1");
//		}
		
		JSONObject result = new JSONObject();
		JBOTransaction tx =null;
		
		try{
			tx = JBOFactory.createJBOTransaction();
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account",tx);
			
			BizObjectQuery query = m.createQuery("select UserID,TRANSPWD from o where UserID=:UserID ");
			query.setParameter("UserID", userid);
    
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				o.setAttributeValue("TRANSPWD", new_transpwd);
				m.saveObject(o);
				BizObjectManager m2 =jbo.getManager("jbo.trade.account_validlog",tx);
				m2.createQuery("delete from o where userid=:userid and validdate=:validdate and validtype='T' ")
					.setParameter("userid", userid)
					.setParameter("validdate", StringFunction.getToday())
					.executeUpdate();
				tx.commit();
				result.put("SuccessFlag", "S");
			}else
				result.put("SuccessFlag", "F");
			
		}catch(Exception e){
			try{
				tx.rollback();
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			
			e.printStackTrace();
			throw new HandlerException("修改交易密码出错");
		}	
		return result;
	}
}
