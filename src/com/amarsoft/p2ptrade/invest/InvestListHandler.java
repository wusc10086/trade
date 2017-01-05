package com.amarsoft.p2ptrade.invest;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

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
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
import com.taiping.common.bean.SysOperateLogBean;
/**
 * 投资项目列表查询
 * 输入：
 * 	curPage
 * 	pageSize
 *  sort  排序字段
 *  dir	  排序方向
 * 
 * 输出：
 * 	SERIALNO     项目编号
 * 	PROJECTNAME  项目名称
 * 	LOANAMOUNT   投资金额
 * 	LOANTERM     期限
 * 	LOANRATE     利率
 * 	CONTRACTID   合同编号
 * 	COUNT        投资人数
 * 	INVESTSUM    已投资金额
 * 
 * @author xhcheng
 */
public class InvestListHandler extends JSONHandler{
	private int pageSize = 10 ;
	private int curPage = 0 ;
	private String loanterm = "";
	private String loanrate = "";
	private String loanamount = "";
	private String pace = "";
	
	private String sort ="status";
	private String dir ="asc";
	private String conditionKeys[]={"loanterm_"};//来自首页的查询条件的键集合
	//注册sql中用到的函数
	static{
		Parser.registerFunction("getitemname");
		Parser.registerFunction("getAppProcess");
		Parser.registerFunction("nvl");
		Parser.registerFunction("sum");
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
		
		String userID = (String) request.get("UserID");
		String userVip = "0";
		if(userID == null || userID.length() == 0){
			
		}else{
			QueryUserAccountInfoHandler qu = new QueryUserAccountInfoHandler();
			JSONObject json = (JSONObject)qu.createResponse(request, null);
			userVip = (String)json.get("isvip");
		}
		
		//获取pageSize每页的条数、curPage当前所在页
		if(request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if(request.containsKey("pageNo"))
			this.curPage = Integer.parseInt(request.get("pageNo").toString());
		
		if(request.containsKey("sort"))
			this.sort = request.get("sort").toString();
		
		if(request.containsKey("dir"))
			this.dir = request.get("dir").toString();
		
		if(request.containsKey("loanterm"))
			loanterm = request.get("loanterm").toString();
		if(request.containsKey("loanrate"))
			loanrate = request.get("loanrate").toString();
		if(request.containsKey("loanamount"))
			loanamount = request.get("loanamount").toString();
		if(request.containsKey("pace"))
			pace = request.get("pace").toString();
		String st = "";
		if(request.containsKey("pace"))
			st = request.get("st").toString();
		String sc = "";
		
		if("1".equals(loanterm)){
			sc += " and loanterm < '12'";
		}else if("2".equals(loanterm)){
			sc += " and loanterm >= '12'";
		}
		
		if("1".equals(st)){
			sc += " and status in ('1')";
		}
		else if("2".equals(st)){
			sc += " and status in ('104')";
		}
		else if("3".equals(st)){
			sc += " and status in ('105')";
		}
		else if("4".equals(st)){
			sc += " and status in ('106')";
		}else {
			sc += " and status in ('1','104','105','106')";
		}
		
		StringBuffer sb = new StringBuffer("select o.* from o,jbo.trade.business_contract bc where o.contractid=bc.SERIALNO ");
		
		//查询条件
		sb.append(sc);
		//来自首页时添加sql语句的查询
		sb=addConditions(sb, setMap(true,request));

		sb.append(" order by "+sort+" "+dir+",begindate,begintime");
		
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info_listview");
			//获取申请贷款的所有记录
            BizObjectQuery query = m.createQuery(sb.toString());
            
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
					if(o!=null){
						JSONObject obj = new JSONObject();
						String proVip = o.getAttribute("isvip").toString()==null?"":o.getAttribute("isvip").toString();
						String endtime = o.getAttribute("endtime").toString()==null?"":o.getAttribute("endtime").toString();
						String enddate = o.getAttribute("enddate").toString()==null?"":o.getAttribute("enddate").toString();
						String begintime = o.getAttribute("begintime").toString()==null?"":o.getAttribute("begintime").toString();
						String begindate = o.getAttribute("begindate").toString()==null?"":o.getAttribute("begindate").toString();
						
						String status = o.getAttribute("STATUS").toString()==null ? "1" : o.getAttribute("STATUS").toString();
						
						String loanamount = o.getAttribute("LOANAMOUNT").toString()==null ? "0" : o.getAttribute("LOANAMOUNT").toString();
						String remainamount = o.getAttribute("remainamount").toString()==null?"0":o.getAttribute("remainamount").toString();
						String reciprocaltime = o.getAttribute("reciprocaltime").toString()==null?"0":o.getAttribute("reciprocaltime").toString();
						
						obj.put("endtime", endtime);
						obj.put("enddate", enddate);
						obj.put("begintime", begintime);
						obj.put("begindate", begindate);
						
						obj.put("LOANAMOUNT", loanamount);
						obj.put("remainamount", remainamount);
						obj.put("reciprocaltime", reciprocaltime);
						
						obj.put("STATUS", status);
						String end = enddate+" "+endtime;
						String begin = begindate+" "+begintime;		
						
						obj.put("begind", begin);
						String begin1 = P2pString.addDateFormat(begin, 1, -Integer.parseInt(reciprocaltime),"yyyy/MM/dd HH:mm:ss");
						//到期未满标的项目 设置显示预约已满
						if(getBetwon(end)<0 && "1".equals(status)){
							obj.put("end","1");
						}
						//未开始的项目 设置显示预约中

						if(getBetwon(begin)>0 && getBetwon(begin1)<0 && "1".equals(status)){
							obj.put("begin","1");							
						}
						
						//统计项目进度
						double percent = 0;
						double amount = Double.parseDouble(loanamount);
						double remain = Double.parseDouble(remainamount);

						percent = (amount-remain)/amount-0.00001<0?0:(amount-remain)/amount-0.00001;


						obj.put("INVESTSUM", amount-remain);
						obj.put("percent", percent);
						
						obj.put("isvip", o.getAttribute("isvip").toString()==null?"0":o.getAttribute("isvip").toString());
						obj.put("SERIALNO",o.getAttribute("SERIALNO").toString()==null ? "" : o.getAttribute("SERIALNO").toString());
						obj.put("PROJECTNAME",o.getAttribute("PROJECTNAME").toString()==null ? "" : o.getAttribute("PROJECTNAME").toString());
						obj.put("LOANTERM",o.getAttribute("LOANTERM").toString()==null ? "" : o.getAttribute("LOANTERM").toString());
						obj.put("LOANRATE",o.getAttribute("LOANRATE").toString()==null ? "" : o.getAttribute("LOANRATE").toString());
						obj.put("CONTRACTID",o.getAttribute("CONTRACTID").toString()==null ? "" : o.getAttribute("CONTRACTID").toString());
						obj.put("BEGINAMOUNT",o.getAttribute("BEGINAMOUNT").toString()==null ? "" : o.getAttribute("BEGINAMOUNT").toString());
						obj.put("PAYMENTMETHOD", o.getAttribute("PAYMENTMETHOD").toString()==null?"":o.getAttribute("PAYMENTMETHOD").toString());
						obj.put("paymentmethodN", o.getAttribute("PAYMENTMETHODN").toString()==null?"":o.getAttribute("PAYMENTMETHODN").toString());
						obj.put("remainamount", o.getAttribute("remainamount").toString()==null?"0":o.getAttribute("remainamount").toString());
						//obj.put("BetweenTime", P2pString.getBetweenTime(obj.get("ENDDATE").toString()+" "+obj.get("ENDTIME").toString()));
						obj.put("guaranteeflag", o.getAttribute("guaranteeflag").toString()==null ? "0" : o.getAttribute("guaranteeflag").toString());
						obj.put("GRANANTORNAME", o.getAttribute("GRANANTORNAME").toString()==null?"":o.getAttribute("GRANANTORNAME").toString());
						obj.put("proVip", proVip);
						String PROJECTID = o.getAttribute("SERIALNO").toString();
						BizObjectManager m1 = jbo.getManager("jbo.trade.user_contract");
						BizObjectQuery query1 = m1.createQuery("SELECT * FROM O WHERE PROJECTID = :PROJECTID");
						query1.setParameter("PROJECTID",PROJECTID);
						int count = query1.getTotalCount();
						obj.put("COUNT", count);
						query1 = m1.createQuery("SELECT SUM(O.INVESTSUM) AS V.SUM FROM O WHERE PROJECTID = :PROJECTID and status <>'2'");
						query1.setParameter("PROJECTID",PROJECTID);
						BizObject object = query1.getSingleResult(false);
						obj.put("INVESTSUM", object.getAttribute("SUM").toString()==null?"0":object.getAttribute("SUM").toString());
						if("1".equals(proVip) && "0".equals(userVip)){
							totalAcount--;
						}else
							array.add(obj);
					}
					
				}
				result.put("RootType", "020");// 返回形式为列表
				result.put("array", array);
				result.put("TotalAcount", String.valueOf(totalAcount));
				result.put("pageNo", String.valueOf(curPage));
				result.put("pageSize", String.valueOf(pageSize));
				return result;
 			}else{
				throw new HandlerException("default.database.error");
			}
			
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	
	//时间差
	private int getBetwon(String str){
   
		java.text.DateFormat df=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		java.util.Calendar c1=java.util.Calendar.getInstance();   
		java.util.Calendar c2=java.util.Calendar.getInstance();   
		str = str.replaceAll("/", "-");
		try {
			c1.setTime(df.parse(str));   
			c2.setTime(new Date());   
		}catch(java.text.ParseException e){   
			System.err.println("格式不正确");   
		}
		
		int result=c1.compareTo(c2);
		return result;
	}
	/*
	 * sql语句添加查询条件
	 */
	private StringBuffer addConditions(StringBuffer sql,Map<String, String> newConditions){
	    Set<String> keySet = newConditions.keySet();
	    Iterator it=keySet.iterator();
	    while(it.hasNext()){
	      String  key=it.next().toString();
	        sql.append(" and "+key+"="+newConditions.get(key));
	    }
	    return sql;
	}
	/*
	 * 设置查询条件的键值对
	 */
	private Map<String, String> setMap(boolean needSetConditions,JSONObject request){
	    Map<String, String> conditionsMap=new TreeMap<String, String>();
	    if(needSetConditions){
	        for (String key : conditionKeys) {
	            if(null!= request.get(key)){
	                conditionsMap.put(key.substring(0, key.length()-1), request.get(key).toString());
	            }
            }
	    }
	    return conditionsMap;
	}
}
