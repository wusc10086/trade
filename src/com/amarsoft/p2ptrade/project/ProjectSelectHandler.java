package com.amarsoft.p2ptrade.project;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.awe.util.json.JSONValue;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.sun.xml.rpc.processor.model.Request;
import com.sun.xml.rpc.processor.modeler.j2ee.xml.iconType;


/*
 * @queryprojectlist   投资列表查询
 * 输入：  
 * 
 * CurPage   0开始
 * PageSize
 * 
 * Sum  0 1 5
 * Term 0 1 
 * Sort  排序字段
 * Dir  方向
 * 输出：
 * ProjectName
 * LoanTerm
 * LoanRate
 * BeginTime
 * LoanAmount
 * Status
 * PaymentMethod
 */

public class ProjectSelectHandler extends JSONHandler{
	private int pageSize ;
	private int curPage ;
	private String  sSumCount ;
	private String Sum ;  //0 1 5
	private String Term ; //0 1 z
	private String Sort ;  //排序字段
	private String Dir ; //方向
	private int pageAcount = 0; //总页数
	
	private String sDate = "";
	private String sTime = "";
	/*
	public static String sWhereCondition = " status in ( '1','2') and (BEGINDATE<:date or (BEGINDATE=:date and begintime<=:time))"
			+ " and (ENDDATE is null or ENDDATE='' or ENDDATE>:date or (ENDDATE=:date and endtime>=:time))"
			+ " and (INVALIDDATE is null or INVALIDDATE='' or INVALIDDATE>:date or (INVALIDDATE=:date and INVALIDDATE>=:time))";
	*/
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectProject(request);
	}

	private JSONObject selectProject(JSONObject request)throws HandlerException {
		JSONObject result = new JSONObject();
		if(request.containsKey("Sort")){
			this.Sort =  request.get("Sort").toString();
		}else {
			this.Sort =  "";
		}
		if(request.containsKey("Dir")){
			this.Dir = request.get("Dir").toString();
		}else {
			this.Dir = "";
		}
		//时间处理
 		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info_listview");//项目信息
			
			String sQuerySql = getQuerySql(request);
			BizObjectQuery query = m.createQuery(sQuerySql);
			
			
			if (request.containsKey("PageSize")&&request.containsKey("CurPage")) {
			if (!"".equals(request.get("PageSize").toString())||request.get("PageSize")!=null) {
				pageSize = Integer.parseInt(request.get("PageSize").toString());
				if (!"".equals(request.get("CurPage").toString())||request.get("CurPage")!=null) {
					curPage =  Integer.parseInt(request.get("CurPage").toString());
					//分页
					int firstRow = curPage * pageSize;
					if(firstRow < 0){
						firstRow = 0;
					}
					int maxRow = pageSize;
					if(maxRow <= 0){
						maxRow = 10;
					}
					System.out.println("firstRow="+firstRow);
					query.setFirstResult(firstRow);
					query.setMaxResults(maxRow);
					//总数
					int totalAcount = query.getTotalCount();
					//总页数
					if (totalAcount%pageSize==0) {
						pageAcount = totalAcount/pageSize;
					}else {
						pageAcount = totalAcount/pageSize;
						pageAcount +=1;
					}
					sSumCount = getSumCount();
					result.put("TotalAcount", String.valueOf(totalAcount));
					result.put("SumCount", sSumCount);
					result.put("PageCount", String.valueOf(pageAcount));
				}
			}
			}
			
			//查询常见问题
			JSONArray helpdatas = new JSONArray();
			BizObjectManager manager2 = JBOFactory.getBizObjectManager("jbo.trade.ti_help_info");
			List<BizObject> list2 = manager2.createQuery("select title,serialno,catalogno from o where catalogno=:catalogno order by sortno asc").setParameter("catalogno", "invest").getResultList(false);
			for(BizObject obj : list2){
				JSONObject objx = new JSONObject();
				objx.put("title", obj.getAttribute("title").getString());
				objx.put("serialno", obj.getAttribute("serialno").getString());
				objx.put("catalogno", obj.getAttribute("catalogno").getString());
				helpdatas.add(objx);
			}
			result.put("helpdatas", helpdatas);
			
			
			List<BizObject> list = query.getResultList(false);
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					obj.put("Serialno",o.getAttribute("SERIALNO").toString()==null ? "" : o.getAttribute("SERIALNO").toString());
					obj.put("ProjectName",o.getAttribute("PROJECTNAME").toString()==null ? "" : o.getAttribute("PROJECTNAME").toString());
					obj.put("LoanTerm",o.getAttribute("LOANTERM").toString()==null ? "" : o.getAttribute("LOANTERM").toString());
					obj.put("LoanRate",o.getAttribute("LOANRATE").toString()==null ? "" : o.getAttribute("LOANRATE").toString());
					obj.put("BeginDate",o.getAttribute("BEGINDATE").toString()==null ? "" : o.getAttribute("BEGINDATE").toString());
					obj.put("BeginTime",o.getAttribute("BEGINTIME").toString()==null ? "" : o.getAttribute("BEGINTIME").toString());
					obj.put("LoanAmount",o.getAttribute("LOANAMOUNT").toString()==null ? "" : o.getAttribute("LOANAMOUNT").toString());
					obj.put("Status",o.getAttribute("STATUS").toString()==null ? "" : o.getAttribute("STATUS").toString());
					obj.put("PaymentMethod",o.getAttribute("PAYMENTMETHOD").toString()==null ? "" : o.getAttribute("PAYMENTMETHOD").toString());
					obj.put("TopRecord",o.getAttribute("TopRecordTime").toString()==null ? "" : o.getAttribute("TopRecordTime").toString());
					array.add(obj);
				}
				result.put("RootType", "020");// 返回形式为列表
				result.put("array", array);
				//result.put("count", query.getTotalCount());
				return result;
 			}else{
				throw new HandlerException("default.database.error");
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
/**
	 *  @return
     *  @throws HandlerException 
     *  
	 */
	private String getSumCount() throws HandlerException {
		// TODO Auto-generated method stub
		String Sumcount = "";
		String sQuerySql = " select count(1) as v.cnt from  o where 1=1";

		JBOFactory jbo = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.project_info_listview");
			BizObjectQuery query0 = m.createQuery(sQuerySql+"  and   LOANAMOUNT between 0 and 9999  ");
			query0.setParameter("date",sDate);
			query0.setParameter("time",sTime);
			BizObject bizObject = query0.getSingleResult(false);
			int cnt0 = bizObject.getAttribute("cnt").getInt();
			Sumcount=Sumcount+cnt0+",";
			
			BizObjectQuery query1 = m.createQuery(sQuerySql+"  and LOANAMOUNT between 10000 and 49999 ");
			query1.setParameter("date",sDate);
			query1.setParameter("time",sTime);
			BizObject bizObject1 = query1.getSingleResult(false);
			int cnt1 = bizObject1.getAttribute("cnt").getInt();
			Sumcount=Sumcount+cnt1+",";
			
			BizObjectQuery query2 = m.createQuery(sQuerySql+"  and LOANAMOUNT > 49999 ");
			query2.setParameter("date",sDate);
			query2.setParameter("time",sTime);
		    BizObject bizObject2 = query2.getSingleResult(false);
			int cnt2 = bizObject2.getAttribute("cnt").getInt();
			Sumcount=Sumcount+cnt2;
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return  Sumcount ;
	}

	//Sql
	private String getQuerySql(JSONObject requese){
		String sqlString = "";
		String sqlString1 = "";
		String sqlString2 = "";
		//String sQuerySql = " select * from  o where (INVALIDDATE > :INVALIDDATE and BEGINDATE >=:BEGINDATE) or  INVALIDDATE is null ";
		String sQuerySql = " select * from  o where 1=1";
		
		// where 补充
			if(requese.get("Sum") != null && !"".equals(requese.get("Sum").toString())){
				Sum = requese.get("Sum").toString();
				if ("0".equals(Sum)) {
					sqlString = " and LOANAMOUNT between 0 and 9999";
				}else if ("1".equals(Sum)) {
					sqlString = " and LOANAMOUNT between 10000 and 49999";
				}else if ("5".equals(Sum)) {
					sqlString = " and LOANAMOUNT > 49999 ";
				}
			}
			if (requese.get("Term") != null&& !"".equals(requese.get("Term").toString())) {
				Term = requese.get("Term").toString();
				if ("0".equals(Term)) {
					sqlString1 = " and LOANTERM between 1 and 12";
				} else if ("1".equals(Term)) {
					sqlString1 = " and LOANTERM > 12";
				}
			}
		
		// 排序
        String Sort1 =" order by status asc, TopRecordTime desc,  begindate asc, begintime asc ";
        if("".equals(Sort)){
        	sqlString2 = Sort1;
        }
        else  if (!"".equals(Sort)&&!"".equals(Dir)) {
        	sqlString2 =" order by status asc,"+Sort+"  "+Dir;
		}
		sQuerySql = sQuerySql+"  "+sqlString+"  "+sqlString1+"  "+sqlString2;
		//System.out.println(" sQuerySql =    " + sQuerySql);
		return sQuerySql;
	}
	
}
