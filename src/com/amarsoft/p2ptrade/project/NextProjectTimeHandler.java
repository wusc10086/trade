package com.amarsoft.p2ptrade.project;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class NextProjectTimeHandler extends JSONHandler{

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		try{
			String sTodayNow = StringFunction.getTodayNow();
			int iDot = sTodayNow.indexOf(" ");
			String sDate = sTodayNow.substring(0, iDot);
			String sTime = sTodayNow.substring(iDot+1);
			
			BizObjectManager m = JBOFactory.getBizObjectManager("jbo.trade.project_info");
			String sql = "select begindate,begintime from o where status in ('1','101')"
					+ "and begindate >:begindate1 or (begindate=:begindate2 and begintime>:begintime)"
					+ "order by begindate asc,begintime asc";
			BizObject obj = m.createQuery(sql).setParameter("begindate1", sDate).setParameter("begindate2", sDate).setParameter("begintime", sTime).setFirstResult(0).setMaxResults(1).getSingleResult(false);
			if(obj==null){
				return "";
			}
			else
				return obj.getAttribute("begindate") + " " + obj.getAttribute("begintime");
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		
	}

}
