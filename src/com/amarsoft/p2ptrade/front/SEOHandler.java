package com.amarsoft.p2ptrade.front;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;


/**
 * SEO 根据页面编号查询SEO详情
 * */
public class SEOHandler extends JSONHandler{	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();

		String pageno = "";
		String pageurl = "";
		if(request.containsKey("pageno"))
			pageno = request.get("pageno").toString();
		if(request.containsKey("pageurl"))
			pageurl = request.get("pageurl").toString();
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_seo");

            BizObjectQuery query = m.createQuery("isinuse='1' and pageurl like :pageurl");
            query.setParameter("pageurl", pageurl);//.setParameter("pageno",pageno)
          
			BizObject o = query.getSingleResult(false);
			StringBuffer sb = new StringBuffer();
			if(o!=null){				
				String serialno = o.getAttribute("serialno")==null ? "" : o.getAttribute("serialno").toString();
				String title = o.getAttribute("title")==null ? "" : o.getAttribute("title").toString();
				String keywords = o.getAttribute("keywords")==null ? "" : o.getAttribute("keywords").toString();
				String description = o.getAttribute("description")==null ? "" : o.getAttribute("description").toString();
				String meta = o.getAttribute("meta")==null ? "" : o.getAttribute("meta").toString();
				String meta1 = o.getAttribute("meta1")==null ? "" : o.getAttribute("meta1").toString();
				String meta2 = o.getAttribute("meta2")==null ? "" : o.getAttribute("meta2").toString();
				String iscache = o.getAttribute("iscache")==null ? "" : o.getAttribute("iscache").toString();
				String inputtime = o.getAttribute("inputtime")==null ? "" : o.getAttribute("inputtime").toString();
				
				title = "<title>"+title+"</title>\r\n";
				keywords = "<meta name=\"Keywords\" content=\""+keywords+"\"/>\r\n";
				description = "<meta name=\"Description\" content=\""+description+"\"/>\r\n";
				sb.append(title);
				sb.append(keywords);
				sb.append(description);
				sb.append(meta+"\r\n");
				sb.append(meta1+"\r\n");
				sb.append(meta2+"\r\n");				
				
			}
			result.put("SEO", sb.toString());
			return result;			
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
}
