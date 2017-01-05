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
 * 修改用户名
 * 输入参数：
 * 		userid:	
 * 		username:	
 * 输出参数：
 * 		SuccessFlag:是否成功	S/F
 *
 */
public class SaveUserNameHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String userid = (String) request.get("userid");
		String username = (String) request.get("username");
		//新密码不能为空
		if(userid == null || userid.length() == 0){
			throw new HandlerException("modify.emptyuserid");
		}
		//确认密码不能为空
		if(username == null || username.length() == 0){
			throw new HandlerException("modify.emptyusername");
		}

		JSONObject result = new JSONObject();
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
			
			BizObjectQuery query = m.createQuery("select username from o where username=:username ");
			query.setParameter("username", username);
    
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				result.put("SuccessFlag", "F");
			}else{
				BizObjectQuery query1 = m.createQuery("select userid,username from o where userid=:userid ");
				query1.setParameter("userid", userid);
	    
				BizObject oo = query1.getSingleResult(true);
				if(oo!=null){
					oo.setAttributeValue("username", username);
					m.saveObject(oo);
					result.put("SuccessFlag", "S");
				}else{
					result.put("SuccessFlag", "F");
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("modify.error");
		}		
		return result;
	}
}