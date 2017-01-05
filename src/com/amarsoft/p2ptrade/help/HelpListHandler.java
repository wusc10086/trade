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

public class HelpListHandler extends JSONHandler {
	
	private int iCurPgae = 0;
	private int iPageSize = 20;

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		JSONArray datas = new JSONArray();
		
		if(request.containsKey("CurPage")){
			iCurPgae =((Long)request.get("CurPage")).intValue();
		}
		if(request.containsKey("PageSize")){
			iPageSize =((Long)request.get("PageSize")).intValue();
		}
		
		String[] queryTitles = null;
		String sql = "select title,serialno,catalogno from o where 1=1";
		if(request.containsKey("CatalogNo"))
			sql += " and catalogno=:CatalogNo";
		if(request.containsKey("Title")){
			queryTitles = request.get("Title").toString().split(" ");
			if(queryTitles.length>0){
				sql += " and (";
				for(int i=0;i<queryTitles.length;i++){
					if(i==0)
						sql += "Title like :T" + i;
					else
						sql += " or Title like :T" + i;
				}
				sql += ")";
			}
		}
		sql += " order by sortno asc";
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.ti_help_info");
			BizObjectManager manager2 = JBOFactory.getBizObjectManager("jbo.trade.ti_help_query");
			BizObjectQuery query = manager.createQuery(sql);
			if(request.containsKey("CatalogNo"))
				query.setParameter("CatalogNo", request.get("CatalogNo").toString());
			if(request.containsKey("Title")){
				if(queryTitles != null && queryTitles.length>0){
					for(int i=0;i<queryTitles.length;i++){
						query.setParameter("T" + i, "%" + queryTitles[i] + "%");
						//插入到ti_help_info
						BizObject objt = manager2.createQuery("select querycount from o where querytitle=:querytitle").setParameter("querytitle", queryTitles[i]).getSingleResult(true);
						if(objt==null){
							objt= manager2.newObject();
							objt.setAttributeValue("querycount", 1);
							objt.setAttributeValue("querytitle", queryTitles[i]);
							manager2.saveObject(objt);
						}
						else
							manager2.createQuery("update o set querycount=querycount+1 where querytitle=:querytitle").setParameter("querytitle", queryTitles[i]).executeUpdate();
							//objt.setAttributeValue("querycount",objt.getAttribute("querycount").getInt()+1);
						
					}
					
					
				}
			}
			int iRowCount = query.getTotalCount();
			result.put("count",iRowCount);
			result.put("array", datas);
			//执行查询并获得结果
			query.setFirstResult(iCurPgae * iPageSize);
			query.setMaxResults(iPageSize);
			List<BizObject> queryResult = query.getResultList(false);
			if(query!=null){
				for(BizObject obj : queryResult){
					JSONObject objx = new JSONObject();
					objx.put("title", obj.getAttribute("title").getString());
					objx.put("serialno", obj.getAttribute("serialno").getString());
					objx.put("catalogno", obj.getAttribute("catalogno").getString());
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
