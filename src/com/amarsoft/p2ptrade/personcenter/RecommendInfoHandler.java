package com.amarsoft.p2ptrade.personcenter;

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

public class RecommendInfoHandler extends JSONHandler{
	private int pageSize = 1 ;
	private int pageNo = 0 ;//当前页
	@Override
	public Object createResponse(JSONObject request, Properties params)
			throws HandlerException {
		
		return selectUserApplyInfo(request);
	}

	public JSONObject selectUserApplyInfo(JSONObject request){
		
		JSONObject result=new JSONObject();
		String userId=(String)request.get("userId");
		JBOFactory jbo=JBOFactory.getFactory();
		try {
			BizObjectManager mg = jbo.getManager("jbo.trade.loan_apply");
			BizObjectQuery query = mg.createQuery("userid=:userid");
			query.setParameter("userid", userId);
			//分页
			int recordTotal = query.getTotalCount();
			JSONObject pageInfo = setPage(request, recordTotal);
			query.setFirstResult((pageNo - 1) * pageSize);
			query.setMaxResults(pageSize);
			List<BizObject>  applyInfoList = query.getResultList(false);
			JSONArray array=new JSONArray();
			for (int i = 0; i < applyInfoList.size(); i++) {
				BizObject o = applyInfoList.get(i);
				JSONObject obj=new JSONObject();
				//存入贷款编号，贷款标题，贷款额度，投标产品，投标日期，投标状态，操作
				obj.put("applyno",o.getAttribute("applyno").toString()==null ? "" : o.getAttribute("applyno").toString());//编号，用于详情页查询
				obj.put("projectname",o.getAttribute("projectname").toString()==null ? "" : o.getAttribute("projectname").toString());//贷款标题
				obj.put("businesssum",o.getAttribute("businesssum").toString()==null ? "" : o.getAttribute("businesssum").toString());//贷款额度
				obj.put("applytime",o.getAttribute("applytime").toString()==null ? "" : o.getAttribute("applytime").toString());//申请时间
				obj.put("productid",o.getAttribute("productid").toString()==null ? "" : o.getAttribute("productid").toString());//投标产品编号
				obj.put("applystatus",o.getAttribute("applystatus").toString()==null ? "" : o.getAttribute("applystatus").toString());//根据状态显示
				obj.put("userId",o.getAttribute("userid").toString()==null ? "" : o.getAttribute("userid").toString());//根据状态显示
				
				array.add(obj);
			}
			result.put("RootType", "020");// 返回形式为列表
			result.put("array", array);
			result.put("pageInfo", pageInfo);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}
	//设置分页信息
	private JSONObject setPage(JSONObject request,int recordCount){
		JSONObject pageInfo =new JSONObject();
		if(request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if(request.containsKey("pageNo"))
			this.pageNo = Integer.parseInt(request.get("pageNo").toString());	
		System.out.println("pageSize-->>"+pageSize);
		int pageCount = (recordCount + pageSize - 1) / pageSize;
		if (pageNo > pageCount)
			pageNo = pageCount;
		if (pageNo < 1)
			pageNo = 1;
		pageInfo.put("recordCount", String.valueOf(recordCount));
		pageInfo.put("pageNo", String.valueOf(pageNo));
		pageInfo.put("pageSize", String.valueOf(pageSize));
		return pageInfo;
	}
}
