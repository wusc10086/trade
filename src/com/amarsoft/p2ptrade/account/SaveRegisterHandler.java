package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class SaveRegisterHandler extends JSONHandler{
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		Parser.registerFunction("sum");
		return saveUser(request);
	}
	
	private JSONObject  saveUser(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.myloading");
			m.createQuery("select sum(userid) from o ");
			BizObject newobject = m.newObject();
			newobject.setAttributeValue("username", (String)request.get("username"));
			newobject.setAttributeValue("userpassword",  (String)request.get("userpassword"));
			newobject.setAttributeValue("phonenum", (String)request.get("phonenum"));
			m.saveObject(newobject);
			result.put("Flag", "ture");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			ARE.getLog().error(e.toString());
			throw new HandlerException("riskreviews.error");
		}
	}
}
