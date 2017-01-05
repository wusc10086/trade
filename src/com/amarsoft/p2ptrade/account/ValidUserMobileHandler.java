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
 * 验证用户名手机号是否匹配
 * 输入参数：
 * 		UserName:		用户名
 * 		Mobile:			手机号
 * 输出参数：
 * 		ValidResult:	校验结果	S/F
 *
 */
public class ValidUserMobileHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		String userName = (String) request.get("UserName");
		String mobile = (String) request.get("Mobile");
		
		if(userName == null || userName.length() == 0){
			throw new HandlerException("common.emptyusername");
		}
		if(mobile == null || mobile.length() == 0){
			throw new HandlerException("common.emptymobile");
		}
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager manager = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = manager.createQuery("UserName=:UserName and PhoneTel=:Mobile");
			query.setParameter("UserName", userName);
			query.setParameter("Mobile", mobile);
			BizObject user = query.getSingleResult(false);
			if(user == null){
				result.put("ValidResult", "F");
			}else{
				result.put("ValidResult", "S");
			}						
			return result;
			
		} catch(Exception e){
			throw new HandlerException("usermobilevalid.error");
		}
	}

}
