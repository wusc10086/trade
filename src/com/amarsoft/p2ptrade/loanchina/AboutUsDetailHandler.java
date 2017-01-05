package com.amarsoft.p2ptrade.loanchina;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class AboutUsDetailHandler extends JSONHandler{
	private String classify;
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectGuideDetail(request);
	}

	private JSONObject selectGuideDetail(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_help");
		    
			if(request.containsKey("classify")){
				if (request.get("classify")!=null) {
					this.classify = request.get("classify").toString();
					
					BizObjectQuery query = m.createQuery("select ih.title,ih.content1,ih.classify from jbo.trade.inf_help ih where ih.status='Y' and ih.classify='"+classify+"'");
					
					List<BizObject> list = query.getResultList(false);
					if (list != null) {
						JSONArray array = new JSONArray();
						for (int i = 0; i < list.size(); i++) {
							BizObject o = list.get(i);
							JSONObject obj = new JSONObject();
							
							//TODO:判断内容的长度，超过4000则再取content2，与content1进行拼接
							
							obj.put("content1",o.getAttribute("content1").toString()==null ? "" : o.getAttribute("content1").toString());
							obj.put("title",o.getAttribute("title").toString()==null ? "" : o.getAttribute("title").toString());
							
							array.add(obj);
						}
						result.put("array", array);
					}
		            
				    
				}else{
					throw new HandlerException("default.database.error");
				}
			}
			
		    return result;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
     
	}

}
