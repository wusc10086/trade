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
import com.taiping.common.bean.SysOperateLogBean;

/*
 * @loanchinainvest 贷款招标列表
 * 输入：
 * pageSize
 * curPage
 * 
 * 输出：
 * applicationid
 * title
 * businesssum
 * termmonth
 * cycletype
 * city
 * wishbidcount
 * endtime 
 * appprocess 
 * RootType 
 * array  
 * TotalAcount 
 * curPage 
 * pagesize
 */

public class InvestTenderListHandler extends JSONHandler{
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
		//获取pageSize每页的条数、curPage当前所在页
		if(request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if(request.containsKey("pageNo"))
			this.curPage = Integer.parseInt(request.get("pageNo").toString());
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.loanchina.application");
			//获取申请贷款的所有记录
            BizObjectQuery query = m.createQuery(
            		" SELECT"+
            		" a.applicationid,"+
            		" nvl(a.title,getRandAppTitle(a.applicationid,getRandAppNo(a.applicationid))) as v.title,"+
                    " a.businesssum,getTermMonthStr(a.termmonth) as v.termMonth,getitemname('CycleFlag',a.cycleflag) as v.cycletype,"+
            		" getitemname('DistrictCode',a.REGIONCITY) as v.city,a.wishbidcount,a.endtime,getAppProcess(a.applicationid) as v.appprocess"+
        //            " (select count(*) from bid_record bid where bid.applicationid =app.applicationid  and bid.checkstatus <>'020') as scount"+
        //    		" count(*) as v.scount"+
            		" from jbo.loanchina.application a where a.estatus='030' and a.loantype in('car','single','company','house','startup')");
            System.out.println("query--------------"+query);
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
					obj.put("applicationid",o.getAttribute("applicationid").toString()==null ? "" : o.getAttribute("applicationid").toString());
					obj.put("title",o.getAttribute("title").toString()==null ? "" : o.getAttribute("title").toString());
					obj.put("businesssum",o.getAttribute("businesssum").toString()==null ? "" : o.getAttribute("businesssum").toString());
					obj.put("termmonth",o.getAttribute("termMonth").toString()==null ? "" : o.getAttribute("termMonth").toString());
					obj.put("cycletype",o.getAttribute("cycletype").toString()==null ? "" : o.getAttribute("cycletype").toString());
					obj.put("city",o.getAttribute("city").toString()==null ? "" : o.getAttribute("city").toString());
					obj.put("wishbidcount",o.getAttribute("wishbidcount").toString()==null ? "" : o.getAttribute("wishbidcount").toString());
					obj.put("endtime",o.getAttribute("endtime").toString()==null ? "" : o.getAttribute("endtime").toString());
					obj.put("appprocess",o.getAttribute("appprocess").toString()==null ? "" : o.getAttribute("appprocess").toString());
			//		obj.put("scount",o.getAttribute("scount").toString()==null ? "" : o.getAttribute("scount").toString());
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
