package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;


import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 借款信息查询
 * 输入参数：
 * 		UserId:用户 名
 * 输出参数： 
 * 借款列表：
 *      PROJECTNAME|SERIALNO项目名称|申请号project
 *      申请时间PUTOUTDATE loan
 *      结束时间MATURITYDATE loan
 *      BUSINESSSUM借款本金(元) loan 
 *      还款总额(元)PAYCORPUSAMT+PAYINTEAMT+PAYFINEAMT+PAYFEEAMT1+PAYFEEAMT2 payment
 *      loan.serialno=payment.objectno
 *      acct_payment_schedule.objectno=ti_contract_info.loanno
 *      ti_contract_info.contractid=project.contractid
 *      
 * 项目基本信息：
 *      PROJECTNAME|SERIALNO项目名称|申请号
 *      还款期PUTOUTDATE-MATURITYDATE
 *      总期数max(MATURITYDATE)
 *      BUSINESSSUM借款本金(元) loan
 *      实际还款总额(元) 实
 *      LOANSTATUS状态 loan
 * 还款明细： 
 *      ActualPayCorpusAmt已还本金
 *      ActualPayInteAmt已还利息
 *      ActualFineAmt已付逾期罚息
 *      ActualPayFeeAmt1已付担保费
 *      已付提前还款违约金
 *      已付担保管理费
 *      已付追偿款
 * 还款计划表：
 *      SeqId期数
 *      PAYDATE还款截止日 project
 *      PayCorpusAmt+PayInteAmt+PayFineAmt+PayFeeAmt1应还总额(元)
 *      PayCorpusAmt应还本金(元)
 *      PayInteAmt应还利息(元)
 *      PayFineAmt应付逾期罚息(元)
 *      PayFeeAmt1应付担保费(元)
 *      Status状态
 * （非必输参数）
 * 		PageSize：每页的条数;
 *		CurPage：当前页;
 */
public class LoanListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanList(request);
	}
	
	/**
	 * 借款信息查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanList(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		if(request.containsKey("PageSize"))
			this.pageSize = Integer.parseInt(request.get("PageSize").toString());
		if(request.containsKey("CurPage"))
			this.curPage = Integer.parseInt(request.get("CurPage").toString());
		String sUserID = request.get("UserID").toString();
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m0 =jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query0 = m0.createQuery(
					"select ps.objectno,pi.projectname,pi.serialno,li.putoutdate,li.maturitydate," +
					" li.businesssum,sum(ps.paycorpusamt+ps.payinteamt+ps.payfineamt+" +
					" ps.payfeeamt1) as v.sum1,li.loanstatus " +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					" jbo.trade.project_info pi,jbo.trade.acct_loan li," +
					" jbo.trade.acct_payment_schedule ps " +
					" where uc.userid=:userid and uc.relativetype='001' " +
					" and uc.contractid=pi.contractid " +
					" and uc.contractid=ci.contractid and ci.loanno=li.serialno " +
					" and ps.objectno=ci.loanno " +
					" group by ps.objectno,pi.projectname,pi.serialno," +
					" li.putoutdate,li.maturitydate,li.businesssum,li.loanstatus"
					);
			query0.setParameter("userid",sUserID);
			
			int firstRow = curPage * pageSize;
			if(firstRow < 0){
				firstRow = 0;
			}
			int maxRow = pageSize;
			if(maxRow <= 0){
				maxRow = 10;
			}
			query0.setFirstResult(firstRow);
			if(request.containsKey("PageSize")){
				query0.setMaxResults(maxRow);
			}
			
			int totalAcount = query0.getTotalCount();
			int temp = totalAcount % pageSize;
			int pageCount = totalAcount / pageSize;
			if(temp != 0){
				pageCount += 1;
			}
			
			List<BizObject> list0 = query0.getResultList(false);
			if(list0 != null){
				JSONArray array = new JSONArray();
				for (int i = 0; i < list0.size(); i++) {
					BizObject o = list0.get(i);
					JSONObject obj = new JSONObject();
					double sPaySum = Double.parseDouble(
							o.getAttribute("SUM1").toString()==null?
									"0":o.getAttribute("SUM1").toString());
					obj.put("ObjectNo", o.getAttribute("OBJECTNO").getValue()==null?
							"":o.getAttribute("OBJECTNO").getString());//借据号
					obj.put("ProjectName", o.getAttribute("PROJECTNAME").getValue()==null?
							"":o.getAttribute("PROJECTNAME").getString());//项目名称|申请号
					obj.put("SerialNo", o.getAttribute("SERIALNO").getValue()==null?
							"":o.getAttribute("SERIALNO").getString());//项目名称|申请号
					obj.put("PutOutDate", o.getAttribute("PUTOUTDATE").getValue()==null?
							"":o.getAttribute("PUTOUTDATE").getString());//申请时间
					obj.put("MaturityDate", o.getAttribute("MATURITYDATE").getValue()==null?
							"":o.getAttribute("MATURITYDATE").getString());//结束时间
					obj.put("BusinessSum", Double.parseDouble(
							o.getAttribute("BUSINESSSUM").toString()==null?
									"0":o.getAttribute("BUSINESSSUM").toString()));//借款本金(元) 
					obj.put("PaySum", GeneralTools.numberFormat(sPaySum, 0, 2));//还款总额(元)
					obj.put("LoanStatus", o.getAttribute("LOANSTATUS").getValue()==null?
							"":o.getAttribute("LOANSTATUS").getString());//状态
					array.add(obj);
				}
				result.put("RootType", "020");
				result.put("TotalAcount", String.valueOf(totalAcount));// 该条件下从数量
				result.put("PageCount", String.valueOf(pageCount));
				result.put("array", array);
			}
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryloanlist.error");
		}
	}
	 
}
