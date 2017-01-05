package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/*
 * 查询用户基本信息
 * 
 */

public class UserAccountInfoByMobileHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return userAccount(request);
	}
    
	private JSONObject userAccount(JSONObject request)throws HandlerException {
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		String sMobile = request.get("Mobile").toString();
		try {
			BizObjectManager	m = jbo.getManager("jbo.trade.account_detail");
			BizObject o = m.createQuery("select o.* , a.PHONETEL , a.EMAIL ,a.USERNAME  from o,jbo.trade.user_account a where o.userid = a.userid and  a.PHONETEL=:PHONETEL").setParameter("PHONETEL",sMobile).getSingleResult(false);

			if(o!=null){				
				result.put("USERNAME",o.getAttribute("USERNAME").toString());
				result.put("REALNAME", o.getAttribute("REALNAME").toString());
				result.put("CERTID", o.getAttribute("CERTID").toString());
				result.put("SEXUAL", o.getAttribute("SEXUAL").toString());
				result.put("BORNDATE", o.getAttribute("BORNDATE").toString());
				result.put("EDUCATION", o.getAttribute("EDUCATION").toString());
				result.put("MARRIAGE", o.getAttribute("MARRIAGE").toString());
				result.put("CITY", o.getAttribute("CITY").toString());
				result.put("INDUSTRIALTYPE", o.getAttribute("INDUSTRIALTYPE").toString());
				result.put("POSITION", o.getAttribute("POSITION").toString());
				result.put("INCOME", o.getAttribute("INCOME").toString());
				result.put("PHONETEL", o.getAttribute("PHONETEL").toString());
				result.put("EMAIL", o.getAttribute("EMAIL").toString());			
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("query.user.error");
		}
		return result;
	}
}