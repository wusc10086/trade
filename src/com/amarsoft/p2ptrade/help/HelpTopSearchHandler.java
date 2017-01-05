package com.amarsoft.p2ptrade.help;
/**
 * 帮助列表
 * 输入参数：
 * 		CatalogNo 分类号
 * 		Title		搜索标题
 * 		CurPgae		当前页码
 * 		PageSize	每页条数
 * 输出参数：
 * 		count:	总记录数
 * 		array:	数据数组
 * 				title  标题
 * 				serialno 编号
 */
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

public class HelpTopSearchHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		JSONArray datas = new JSONArray();
		String sql = "select querytitle,querycount from o order by querycount desc";
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.ti_help_query");
			BizObjectQuery query = manager.createQuery(sql);
			int iRowCount = query.getTotalCount();
			result.put("count",iRowCount);
			result.put("array", datas);
			query.setFirstResult(0);
			query.setMaxResults(5);
			List<BizObject> queryResult = query.getResultList(false);
			if(query!=null){
				for(BizObject obj : queryResult){
					JSONObject objx = new JSONObject();
					objx.put("querytitle", obj.getAttribute("querytitle").getString());
					objx.put("querycount", obj.getAttribute("querycount").getInt());
					datas.add(objx);
				}
			}
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		
	}

}
