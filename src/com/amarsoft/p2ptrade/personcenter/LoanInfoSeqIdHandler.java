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
 * 		ProjectSerialNo:申请号
 * 输出参数： 
 *      FINISHDATE实际还款日
 *      ACTUALPAYCORPUSAMT+ACTUALPAYINTEAMT+ACTUALFINEAMT实还总额(元)
 *      ACTUALPAYCORPUSAMT本金(元)
 *      ACTUALPAYINTEAMT利息(元)
 *      ACTUALFINEAMT逾期罚息(元)
 *      ACTUALPAYFEEAMT1担保费(元)
 * （非必输参数）
 * 		PageSize：每页的条数;
 *		CurPage：当前页;
 */
public class LoanInfoSeqIdHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfoSeqId(request);
	}
	
	/**
	 * 借款信息查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfoSeqId(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		
		if(request.get("SeqId")==null || "".equals(request.get("SeqId"))){
			throw new HandlerException("seqid.error");
		}
		
		if(request.get("ObjectNo")==null || "".equals(request.get("ObjectNo"))){
			throw new HandlerException("objectno.error");
		}
		if(request.containsKey("PageSize"))
			this.pageSize = Integer.parseInt(request.get("PageSize").toString());
		if(request.containsKey("CurPage"))
			this.curPage = Integer.parseInt(request.get("CurPage").toString());
		
		String sUserID = request.get("UserID").toString();
		String sObjectNo = request.get("ObjectNo").toString();
		String sSeqId = request.get("SeqId").toString();
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m =jbo.getManager("jbo.trade.user_contract");
            
//			BizObjectQuery query = m.createQuery(
//					"select ps.seqid,ps.objectno,ps.paydate,ps.actualpaycorpusamt," +
//					"ps.actualpayinteamt,ps.actualfineamt,ps.actualpayfeeamt1 " +
//					"from jbo.trade.acct_payment_schedule ps " +
//					"where ps.objectno=:objectno and ps.seqid=:seqid");
            
            BizObjectQuery query = m.createQuery(
					"select ps.seqid,ps.objectno,ps.paydate,ps.actualpaycorpusamt," +
					" ps.actualpayinteamt,ps.actualfineamt,ps.actualpayfeeamt1 " +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					" jbo.trade.acct_payment_schedule ps " +
					" where uc.userid=:userid and uc.relativetype='001' " +
					" and ps.objectno=:objectno and ps.seqid=:seqid" +
					" and uc.contractid=ci.contractid and ci.loanno=ps.objectno ");
			
            query.setParameter("userid",sUserID);
			query.setParameter("objectno",sObjectNo);
			query.setParameter("seqid",sSeqId);
			int firstRow = curPage * pageSize;
			if(firstRow < 0){
				firstRow = 0;
			}
			int maxRow = pageSize;
			if(maxRow <= 0){
				maxRow = 10;
			}
			query.setFirstResult(firstRow);
			query.setMaxResults(maxRow);
			
			int totalAcount = query.getTotalCount();
			int temp = totalAcount % pageSize;
			int pageCount = totalAcount / pageSize;
			if(temp != 0){
				pageCount += 1;
			}
			 
			List<BizObject> list = query.getResultList(false);
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					double sActualPayCorpusAmt = Double.parseDouble(
							o.getAttribute("ACTUALPAYCORPUSAMT").toString()==null?
									"0":o.getAttribute("ACTUALPAYCORPUSAMT").toString());
					double sActualPayInteAmt = Double.parseDouble(
							o.getAttribute("ACTUALPAYINTEAMT").toString()==null?
									"0":o.getAttribute("ACTUALPAYINTEAMT").toString());
					double sActualFineAmt = Double.parseDouble(
							o.getAttribute("ACTUALFINEAMT").toString()==null?
									"0":o.getAttribute("ACTUALFINEAMT").toString());
					double sActualPayFeeAmt1 = Double.parseDouble(
							o.getAttribute("ACTUALPAYFEEAMT1").toString()==null?
									"0":o.getAttribute("ACTUALPAYFEEAMT1").toString());
					double sActualPaySum = sActualPayCorpusAmt + sActualPayInteAmt + sActualFineAmt + sActualPayFeeAmt1;
					obj.put("PayDate", o.getAttribute("PAYDATE").getValue()==null?
							"":o.getAttribute("PAYDATE").getString());//还款截止日 
					obj.put("ActualSum",GeneralTools.numberFormat(sActualPaySum, 0, 2));//应还总额(元)
					obj.put("ActualPayCorpusAmt", sActualPayCorpusAmt);//应还本金(元)
					obj.put("ActualPayInteAmt", sActualPayInteAmt);//应还利息(元)
					obj.put("ActualFineAmt", sActualFineAmt);//应付逾期罚息(元)
					obj.put("ActualPayFeeAmt1", sActualPayFeeAmt1);//应付担保费(元)
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
			throw new HandlerException("queryloaninfoseqid.error");
		}
	}
}
