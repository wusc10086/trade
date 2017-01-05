package com.amarsoft.p2ptrade.front;

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



public class NewsHandler extends JSONHandler{
	private int pageSize = 10 ;
	private int curPage = 0 ;
	
	//注册sql中用到的函数
	static{
		Parser.registerFunction("getitemname");
		Parser.registerFunction("getAppProcess");
		Parser.registerFunction("nvl");
		Parser.registerFunction("getRandAppTitle");
		Parser.registerFunction("getRandAppNo");
		Parser.registerFunction("getTermMonthStr");
	}
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		String classify = "";
		//获取pageSize每页的条数、curPage当前所在页
		if(request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if(request.containsKey("pageNo"))
			this.curPage = Integer.parseInt(request.get("pageNo").toString());
		if(request.containsKey("classify"))
			classify = request.get("classify").toString();
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_news");
			//获取新闻的所有记录
            BizObjectQuery query = m.createQuery("classify=:classify and isshow='Y' order by InputTime desc,SerialNo desc");
            query.setParameter("classify",classify);
            //分页
            int totalAcount = query.getTotalCount();
    		int pageCount = (totalAcount + pageSize - 1) / pageSize;
    		if (curPage > pageCount)
    			curPage = pageCount;
    		if (curPage < 1)
    			curPage = 1;
    		query.setFirstResult((curPage - 1) * pageSize);
    		query.setMaxResults(pageSize);
    		
    		//将申请贷款中获取的记录放到json对象中
            List<BizObject> list = query.getResultList(false);
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					obj.put("newsid",o.getAttribute("serialno")==null ? "" : o.getAttribute("serialno").toString());
					obj.put("title",o.getAttribute("title")==null ? "" : o.getAttribute("title").toString());
					obj.put("author",o.getAttribute("author")==null ? "" : o.getAttribute("author").toString());
					obj.put("source",o.getAttribute("source")==null ? "" : o.getAttribute("source").toString());
					obj.put("describe",o.getAttribute("describe")==null ? "" : o.getAttribute("describe").toString());
					obj.put("inputtime",o.getAttribute("inputtime")==null ? "" : o.getAttribute("inputtime").toString());
					
					array.add(obj);
				}
				result.put("RootType", "020");// 返回形式为列表
				result.put("array", array);
				result.put("TotalAcount", String.valueOf(totalAcount));
				result.put("curPage", String.valueOf(curPage));
				result.put("pagesize", String.valueOf(pageSize));
				return result;
 			}else{
				throw new HandlerException("default.database.error");
			}
			
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
}
