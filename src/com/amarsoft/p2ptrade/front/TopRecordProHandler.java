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
import com.amarsoft.p2ptrade.invest.P2pString;
/**
 * 推荐的项目列表
 * **/
public class TopRecordProHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return proList(request);
	}
	
	public JSONObject proList(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		JSONArray arry = new JSONArray();
		try {

			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager loanManger = jbo.getManager("jbo.trade.project_info_listview");
			BizObjectQuery q = loanManger.createQuery("select o.* from o,jbo.trade.business_contract bc where o.contractid=bc.SERIALNO order by status asc,o.toprecordtime desc");
			List<BizObject> list = q.getResultList(false);
			for(BizObject o : list){
				JSONObject obj = new JSONObject();
				obj.put("SERIALNO",o.getAttribute("SERIALNO").toString()==null ? "" : o.getAttribute("SERIALNO").toString());
				obj.put("PROJECTNAME",o.getAttribute("PROJECTNAME").toString()==null ? "" : o.getAttribute("PROJECTNAME").toString());
				obj.put("LOANAMOUNT",o.getAttribute("LOANAMOUNT").toString()==null ? "0" : o.getAttribute("LOANAMOUNT").toString());
				obj.put("LOANTERM",o.getAttribute("LOANTERM").toString()==null ? "0" : o.getAttribute("LOANTERM").toString());
				obj.put("LOANRATE",o.getAttribute("LOANRATE").toString()==null ? "0" : o.getAttribute("LOANRATE").toString());
				obj.put("CONTRACTID",o.getAttribute("CONTRACTID").toString()==null ? "" : o.getAttribute("CONTRACTID").toString());
				obj.put("BEGINDATE",o.getAttribute("BEGINDATE").toString()==null ? "" : o.getAttribute("BEGINDATE").toString());
				obj.put("ENDDATE",o.getAttribute("ENDDATE").toString()==null ? "" : o.getAttribute("ENDDATE").toString());
				obj.put("ENDTIME",o.getAttribute("ENDTIME").toString()==null ? "" : o.getAttribute("ENDTIME").toString());
				obj.put("BEGINAMOUNT",o.getAttribute("BEGINAMOUNT").toString()==null ? "" : o.getAttribute("BEGINAMOUNT").toString());
				obj.put("PAYMENTMETHOD", o.getAttribute("PAYMENTMETHOD").toString()==null?"":o.getAttribute("PAYMENTMETHOD").toString());
				obj.put("paymentmethodN", o.getAttribute("PAYMENTMETHODN").toString()==null?"":o.getAttribute("PAYMENTMETHODN").toString());
				obj.put("remainamount", o.getAttribute("remainamount").toString()==null?"0":o.getAttribute("remainamount").toString());
				obj.put("BetweenTime", P2pString.getBetweenTime(obj.get("ENDDATE").toString()+" "+obj.get("ENDTIME").toString()));
				obj.put("STATUS", o.getAttribute("STATUS").toString()==null ? "" : o.getAttribute("STATUS").toString());
				obj.put("guaranteeflag", o.getAttribute("guaranteeflag").toString()==null ? "0" : o.getAttribute("guaranteeflag").toString());
				obj.put("GRANANTORNAME", o.getAttribute("GRANANTORNAME").toString()==null?"":o.getAttribute("GRANANTORNAME").toString());
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
