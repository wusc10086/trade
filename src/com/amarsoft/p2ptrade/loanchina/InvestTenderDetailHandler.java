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
 * @loanchinainvestDetail 贷款招标详情
 * 输入：
 * applicationId
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
 *  RootType
 *  loantyp 
 *  managedate 
 *  paycyc 
 *  wishrate 
 *  wuid 
 *  monthincoming 
 *  paymodality 
 *  occupation
 * 
 */

public class InvestTenderDetailHandler extends JSONHandler{
	private String applicationId;
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
		
		return selectInvestDetail(request);
	}

	private JSONObject selectInvestDetail(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		//获取请求中传来的申请ID
		if(request.containsKey("applicationId"))
			this.applicationId = request.get("applicationId").toString();
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.loanchina.application");
			//根据申请ID获取申请贷款的记录和个人用户的信息
            BizObjectQuery query = m.createQuery(
            		" SELECT"+
            		" a.applicationid,getitemname('NewLoanType',a.loantype) as v.loantype,"+
            		" a.managedate,getitemname('PayCyc',a.paycyc) as v.paycyc,a.wishrate,"+
            		" nvl(a.title,getRandAppTitle(a.applicationid,getRandAppNo(a.applicationid))) as v.title,"+
                    " a.businesssum,getTermMonthStr(a.termmonth) as v.termMonth,getitemname('CycleFlag',a.cycleflag) as v.cycletype,"+
            		" getitemname('DistrictCode',a.REGIONCITY) as v.city,a.wishbidcount,a.endtime,getAppProcess(a.applicationid) as v.appprocess,"+
                    " ua.wuid,getitemname('QMonth_Incoming',ua.monthincoming) as v.monthincoming,getitemname('PayModality',ua.paymodality) as v.paymodality,getitemname('NewOccupation',ua.occupation) as v.occupation"+
            		" from jbo.loanchina.application a,jbo.loanchina.user_account ua where ua.wuid=a.customerid and a.applicationid='"+applicationId+"'");
          
            BizObject o = query.getSingleResult(false);
            if(o==null){
				 throw new HandlerException("common.projectnotexist");
            }
            
            result.put("RootType", "030");// 返回形式为列表
            //将贷款申请和个人用户的属性值放到json对象中去
            result.put("applicationId",o.getAttribute("applicationid").toString()==null ? "" : o.getAttribute("applicationid").toString());
            result.put("title",o.getAttribute("title").toString()==null ? "" : o.getAttribute("title").toString());
            result.put("businesssum",o.getAttribute("businesssum").toString()==null ? "" : o.getAttribute("businesssum").toString());
            result.put("termmonth",o.getAttribute("termMonth").toString()==null ? "" : o.getAttribute("termMonth").toString());
            result.put("cycletype",o.getAttribute("cycletype").toString()==null ? "" : o.getAttribute("cycletype").toString());
            result.put("city",o.getAttribute("city").toString()==null ? "" : o.getAttribute("city").toString());
            result.put("wishbidcount",o.getAttribute("wishbidcount").toString()==null ? "" : o.getAttribute("wishbidcount").toString());
            result.put("endtime",o.getAttribute("endtime").toString()==null ? "" : o.getAttribute("endtime").toString());
            result.put("appprocess",o.getAttribute("appprocess").toString()==null ? "" : o.getAttribute("appprocess").toString());
            result.put("loantype",o.getAttribute("loantype").toString()==null ? "" : o.getAttribute("loantype").toString());
            result.put("managedate",o.getAttribute("managedate").toString()==null ? "" : o.getAttribute("managedate").toString());
            result.put("paycyc",o.getAttribute("paycyc").toString()==null ? "" : o.getAttribute("paycyc").toString());
            result.put("wishrate",o.getAttribute("wishrate").toString()==null ? "" : o.getAttribute("wishrate").toString());
            result.put("wuid",o.getAttribute("wuid").toString()==null ? "" : o.getAttribute("wuid").toString());
            result.put("monthincoming",o.getAttribute("monthincoming").toString()==null ? "" : o.getAttribute("monthincoming").toString());
            result.put("paymodality",o.getAttribute("paymodality").toString()==null ? "" : o.getAttribute("paymodality").toString());
            result.put("occupation",o.getAttribute("occupation").toString()==null ? "" : o.getAttribute("occupation").toString());
           
			return result;
			
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	

}
