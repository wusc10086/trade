package com.amarsoft.p2ptrade.areacode;

import java.util.List;
import java.util.Properties;


import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 投资记录查询
 * 输入参数：
 * 输出参数： 
 *  ItemNo
 *  ItemName
 */
public class CityHandler extends JSONHandler {
	 
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getInvestmentList(request);
	}
 
	@SuppressWarnings("unchecked")
	private JSONObject getInvestmentList(JSONObject request)throws HandlerException {
		 
		try{
			JBOFactory jbo = JBOFactory.getFactory();
		 
			BizObjectManager m =jbo.getManager("jbo.trade.code_library"); 
			BizObjectQuery query = m.createQuery(
					"select itemno,itemname from o where codeno='AreaCode' and length(itemno)=2");
			 
			List<BizObject> list = query.getResultList(false);
			JSONObject result = new JSONObject();
			JSONArray array = new JSONArray();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					obj.put("ItemNo", o.getAttribute("ITEMNO").getValue()==null?
							"":o.getAttribute("ITEMNO").getString());//ItemNo 
					obj.put("ItemName", o.getAttribute("ITEMNAME").getValue()==null?
							"":o.getAttribute("ITEMNAME").getString());//ItemName
					array.add(obj);
				}
			}
			
			result.put("RootType", "020");
			result.put("array", array);
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("citylist.error");
		}
	}
}
