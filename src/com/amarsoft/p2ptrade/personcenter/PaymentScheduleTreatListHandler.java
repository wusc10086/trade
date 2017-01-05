package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;




import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.lang.StringX;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 收款明细查询
 * 输入参数：
 * 		UserId:用户 名
 * 输出参数： 
 * 待收款明细列表：
 *      ContractId:合同号
 *      FinishDate:收款时间ps
 *      PROJECTNAME|SERIALNO:项目名称∣编号pi
 *      ActualPayCorpusAmt+ActualPayInteAmt+ActualFineAmt收款额（元）ps
 *      ActualPayCorpusAmt:本金ps
 *      ActualPayInteAmt:利息ps
 *      ActualFineAmt:罚息ps
 *（非必输参数）
 * 		PageSize：每页的条数;
 *		CurPage：当前页;
 */
public class PaymentScheduleTreatListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	private String sStartDate;
	private String sEndDate;
	private String sQuerySql;
	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getPaymentScheduleTreatList(request);
	}
	
	/**
	 * 收款明细查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getPaymentScheduleTreatList(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		
		if(request.containsKey("StartDate"))
			this.sStartDate =  request.get("StartDate").toString();
		if(request.containsKey("EndDate"))
			this.sEndDate =  request.get("EndDate").toString();
		if(request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if(request.containsKey("pageNo"))
			this.curPage = Integer.parseInt(request.get("pageNo").toString());
		String sUserID = request.get("UserID").toString();
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.project_info");
			
			sQuerySql= " uc.userid=:userid and " +
					   " uc.relativetype='002' and " +
					   " uc.contractid=pi.contractid and " +
					   " uc.SUBCONTRACTNO=id.SUBCONTRACTNO and " +
			           " id.ActualPayCorpusAmt+id.ActualPayInteAmt+id.ActualFineAmt+id.ActualExpiationSum>0" ;
			if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
				sQuerySql = sQuerySql + " and id.actualpaydate between :startdate and :enddate";
			} 
			BizObjectQuery query = m.createQuery(
					"select uc.contractid,id.actualpaydate,pi.PROJECTNAME,pi.SERIALNO," +
					" id.ActualPayCorpusAmt,id.ActualPayInteAmt,id.ActualFineAmt,id.ActualExpiationSum " +
					" from jbo.trade.user_contract uc," +
					" jbo.trade.project_info pi,jbo.trade.income_detail id " +
					" where " + sQuerySql +
					" order by id.actualpaydate " 
					);
			query.setParameter("userid", sUserID);
			
			if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
				query.setParameter("startdate", sStartDate);
				query.setParameter("enddate", sEndDate);
			} 
			
			 //分页
            int totalAcount = query.getTotalCount();
    		int pageCount = (totalAcount + pageSize - 1) / pageSize;
    		if (curPage > pageCount)
    			curPage = pageCount;
    		if (curPage < 1)
    			curPage = 1;
    		query.setFirstResult((curPage - 1) * pageSize);
    		query.setMaxResults(pageSize);
			
			List<BizObject> list = query.getResultList(false);
			JSONObject result = new JSONObject();
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					String sPojectName =  o.getAttribute("PROJECTNAME").toString()==null?
							"":o.getAttribute("PROJECTNAME").toString();
					String sSerialNo1 =  o.getAttribute("SERIALNO").toString()==null?
							"":o.getAttribute("SERIALNO").toString();
					String sProjectNameSerialNo = sPojectName + sSerialNo1;
					double sActualPayCorpusAmt = Double.parseDouble(StringX.isEmpty(o.getAttribute("ACTUALPAYCORPUSAMT"))?
							"0":o.getAttribute("ACTUALPAYCORPUSAMT").getString());
					double sActualPayInteAmt = Double.parseDouble(StringX.isEmpty(o.getAttribute("ACTUALPAYINTEAMT"))?
							"0":o.getAttribute("ACTUALPAYINTEAMT").getString());
					double sActualFineAmt = Double.parseDouble(StringX.isEmpty(o.getAttribute("ACTUALFINEAMT"))?
							"0":o.getAttribute("ACTUALFINEAMT").getString());
					double sActualExpiationSum = Double.parseDouble(StringX.isEmpty(o.getAttribute("ACTUALEXPIATIONSUM"))?
							"0":o.getAttribute("ACTUALEXPIATIONSUM").getString());
					double sPaySum = sActualPayCorpusAmt + sActualPayInteAmt + sActualFineAmt + sActualExpiationSum;
					
					JSONObject obj = new JSONObject();
					obj.put("ContractId", o.getAttribute("CONTRACTID").getValue()==null?
							"":o.getAttribute("CONTRACTID").getString());//合同号
					obj.put("ProjectNameSerialNo", sProjectNameSerialNo);//项目名称∣编号
					obj.put("FinishDate", o.getAttribute("ACTUALPAYDATE").getValue()==null?
							"":o.getAttribute("ACTUALPAYDATE").getString());//到账日期
					obj.put("PaySum", GeneralTools.numberFormat(sPaySum, 0, 2));//收款额（元）
					obj.put("ActualPayCorpusAmt", GeneralTools.numberFormat(sActualPayCorpusAmt, 0, 2));//本金
					obj.put("ActualPayInteAmt", GeneralTools.numberFormat(sActualPayInteAmt, 0, 2));//利息
					obj.put("ActualFineAmt", GeneralTools.numberFormat(sActualFineAmt, 0, 2));//利息
					array.add(obj);
				}
				result.put("RootType", "030");
				result.put("TotalAcount", String.valueOf(totalAcount));
				result.put("curPage", String.valueOf(curPage));
				result.put("pagesize", String.valueOf(pageSize));
				result.put("array", array);
			}
			
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("paymentscheduletreatlist.error");
		}
	}
}
