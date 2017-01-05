package com.amarsoft.p2ptrade.demo;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class Demo extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfoSum(request);
	}
	
	/**
	 * µã»÷ÉêÇë½è¿î
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfoSum(JSONObject request)throws HandlerException {
			
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m =jbo.getManager("jbo.trade.ti_business_apply");
            
			//BizObjectQuery query = m.createQuery(" userid=:userid and status in ('1','2','3') ");
			BizObject o = m.newObject();
			o.setAttributeValue("userid", "mgao1");
			m.saveObject(o);
			result.put("returnCode", "ok");
			
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryapplicationlist.error");
		}
	}
}

