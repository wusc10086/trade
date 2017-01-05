package com.amarsoft.p2ptrade.front;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 产品列表
 * **/
public class InvestProHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return proList(request);
	}
	
	public JSONObject proList(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		JSONArray arry = new JSONArray();
		try {
			//申请贷款
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager loanManger = jbo.getManager("jbo.trade.CORE_BUSINESS_TYPE");
			BizObjectQuery q = loanManger.createQuery("select o.typeno,o.typename,abd.LoanRate,abd.MAXLPERIOD,abd.MINLPERIOD,abd.DBMINLIMIT from o,jbo.trade.ACCT_BUSINESS_DEFINE abd where o.typeno=abd.typeno and o.isinuse='1' and o.isshow='1' and o.RELBUSINESSTYPE is not null");
			List<BizObject> list = q.getResultList(false);
			for(BizObject o : list){
				JSONObject obj = new JSONObject();
				obj.put("typeno", o.getAttribute("typeno")==null?"":o.getAttribute("typeno").toString());
				obj.put("typename", o.getAttribute("typename")==null?"":o.getAttribute("typename").toString());
				obj.put("LoanRate", o.getAttribute("LoanRate")==null?"":o.getAttribute("LoanRate").toString());
				obj.put("MAXLPERIOD", o.getAttribute("MAXLPERIOD")==null?"":o.getAttribute("MAXLPERIOD").toString());
				obj.put("MINLPERIOD", o.getAttribute("MINLPERIOD")==null?"":o.getAttribute("MINLPERIOD").toString());
				obj.put("DBMINLIMIT", o.getAttribute("DBMINLIMIT")==null?"":o.getAttribute("DBMINLIMIT").toString());
				arry.add(obj);
			}
			result.put("proList", arry);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return result;
	}
}
