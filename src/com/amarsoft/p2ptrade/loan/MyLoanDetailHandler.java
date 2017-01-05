package com.amarsoft.p2ptrade.loan;

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
/**
 * 获得还款页面信息
 */
public class MyLoanDetailHandler extends JSONHandler {
static{
	Parser.registerFunction("add_months");
	Parser.registerFunction("to_date");
	Parser.registerFunction("nvl");
}
	@Override
	public Object createResponse(JSONObject request, Properties params) throws HandlerException {
		if(isCurrentUser(request)){
			return getMyRepayingInfo(request);
		}
		//如果编号与当前用户无关，则返回空
		return new JSONObject();
	}

	/**
	 * 判断查询编号是否与当前用户关联
	 * @param request
	 * @return
	 */
	private boolean isCurrentUser(JSONObject request) {
		boolean flag=false;
		String userId=(null==request.get("userId")?"":request.get("userId").toString());
		String loanNo=(null==request.get("loanNo")?"":request.get("loanNo").toString());
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			String table="jbo.trade.acct_loan";
			String sql = "SELECT CUSTOMERID FROM O WHERE SERIALNO=:loanNo";
			m = f.getManager(table);
			BizObjectQuery q = m.createQuery(sql);
			BizObject userInBaseB = q.setParameter("loanNo", loanNo).getSingleResult(false);
			if(userInBaseB==null){return false;}
			else if(!userId.equals(userInBaseB.getAttribute("CUSTOMERID").toString())){
				return false;
			}else{
				flag=true;
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return flag;
	}

	@SuppressWarnings("unchecked")
	private JSONObject getMyRepayingInfo(JSONObject request) {
		JSONObject result = new JSONObject();// 我的贷款信息的结果
		JSONObject loanInfo = new JSONObject();// 贷款信息
		JSONObject repayTotalInfo = new JSONObject();// 总还款详情
		JSONArray repayPerTermArray = new JSONArray();// 每期还款详情
		String loanNo = (String) request.get("loanNo");
		float payCorpusamtTotal = 0;// 实际总还本金
		float payingTeamtTotal = 0;// 实际总还利息
		float fineamtTotal = 0;// 实际总罚息
		int curSeqId = 0;// 当前还款期数
		try {
			BizObject projectInfoResult = getProjectInfoResult(loanNo);// 项目信息sql结果
			
			List<BizObject> payDetailResult = getPayDetailResultList(loanNo);// 还款明细sql结果
			for (BizObject bizO : payDetailResult) {
				JSONObject repayPerTerm = new JSONObject();
				float actualPayCorpusamt = Float.parseFloat(bizO.getAttribute("ACTUALPAYCORPUSAMT").toString());
				float actualPayinTeamt = Float.parseFloat(bizO.getAttribute("ACTUALPAYINTEAMT").toString());
				float actualFineamt = Float.parseFloat(bizO.getAttribute("ACTUALFINEAMT").toString());// 实际罚息
				float payCorpusamt = Float.parseFloat(bizO.getAttribute("PAYCORPUSAMT").toString());
				float payinTeamt = Float.parseFloat(bizO.getAttribute("PAYINTEAMT").toString());
				float payFineamt = Float.parseFloat(bizO.getAttribute("PAYFINEAMT").toString());// 应付罚息
				payCorpusamtTotal += actualPayCorpusamt;
				payingTeamtTotal += actualPayinTeamt;
				fineamtTotal += actualFineamt;
				
				boolean isPay = (actualPayCorpusamt + actualPayinTeamt + actualFineamt == payCorpusamt + payinTeamt + payFineamt);
//				curSeqId = bizO.getAttribute("SEQID") == null ? 0: bizO.getAttribute("SEQID").getInt();
				curSeqId++;
				// 向分期还款详情中填充数据
				repayPerTerm.put("seqId", bizO.getAttribute("SEQID") == null ? 0 : bizO.getAttribute("SEQID").getInt());// 期数
				repayPerTerm.put("payDate", bizO.getAttribute("PAYDATE") == null ? "" : bizO.getAttribute("PAYDATE").toString());// 还款截止日期
				repayPerTerm.put("payTotal", payCorpusamt + payinTeamt + payFineamt);// 应还总额：本金+利息
				repayPerTerm.put("payCorpusamt", bizO.getAttribute("PAYCORPUSAMT") == null ? "" : bizO.getAttribute("PAYCORPUSAMT").toString());// 应还本金
				repayPerTerm.put("payinTeamt", bizO.getAttribute("PAYINTEAMT") == null ? "" : bizO.getAttribute("PAYINTEAMT").toString());// 应还利息
				repayPerTerm.put("payFineamt", bizO.getAttribute("PAYFINEAMT") == null ? "" : bizO.getAttribute("PAYFINEAMT").toString());// 应付逾期罚息
				if (isPay) {// 判断还款状态
					repayPerTerm.put("status", "已付清");// 状态
				} else {
					repayPerTerm.put("status", "未付清");// 状态
				}
				repayPerTermArray.add(repayPerTerm);
			}
			// 向总还款详情中填充数据
			repayTotalInfo.put("payCorpusamtTotal", payCorpusamtTotal);// 总本金
			repayTotalInfo.put("payingTeamtTotal", payingTeamtTotal);// 总利息
			repayTotalInfo.put("fineamtTotal", fineamtTotal);// 总罚息
			repayTotalInfo.put("paymentTotal", payCorpusamtTotal + payingTeamtTotal + fineamtTotal);// 总还款
			// 向项目详情中填充数据
			if(null!=projectInfoResult){
			loanInfo.put("serialNo", projectInfoResult.getAttribute("SERIALNO") == null ? "" : projectInfoResult.getAttribute("SERIALNO").toString());// 项目编号
			loanInfo.put("projectName", projectInfoResult.getAttribute("PROJECTNAME") == null ? "" : projectInfoResult.getAttribute("PROJECTNAME").toString());// 项目名称
			loanInfo.put("rateDate", projectInfoResult.getAttribute("RATEDATE") == null ? "" : projectInfoResult.getAttribute("RATEDATE").toString());// 起始日期，起息日
			//add_months(to_date(o.applytime,'yyyy/mm/dd'）结束日期
			loanInfo.put("endDate", projectInfoResult.getAttribute("ENDDATE") == null ? "" : projectInfoResult.getAttribute("ENDDATE").toString());// 结束日期
			int loanTerm=projectInfoResult.getAttribute("LOANTERM") == null ? 0 : projectInfoResult.getAttribute("LOANTERM").getInt();
			loanInfo.put("loanTerm", loanTerm);// 投资期限
			loanInfo.put("remianSeq", loanTerm-curSeqId);// 当前期限
			loanInfo.put("loanMount", projectInfoResult.getAttribute("LOANAMOUNT") == null ? "" : projectInfoResult.getAttribute("LOANAMOUNT").toString());// 借款本金
			loanInfo.put("capitalRemain", Float.parseFloat(projectInfoResult.getAttribute("LOANAMOUNT").toString()) - payCorpusamtTotal);// 剩余本金
			loanInfo.put("status", projectInfoResult.getAttribute("STATUS") == null ? "" : projectInfoResult.getAttribute("STATUS").toString());// 状态
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		result.put("loanInfo", loanInfo);
		result.put("repayTotalInfo", repayTotalInfo);
		result.put("repayPerTermArray", repayPerTermArray);
		return result;
	}

/*
 * 获取项目表的结果集
 */
	private BizObject getProjectInfoResult(String param) {
		BizObject result = null;
		JBOFactory factory = JBOFactory.getFactory();
		BizObjectManager manager;
		try {
			manager = factory.getManager("jbo.trade.project_info");
			String sql="select  o.SERIALNO,o.PROJECTNAME,o.RATEDATE,o.LOANTERM,o.LOANAMOUNT,o.STATUS,add_months(to_date(nvl(o.RATEDATE,'1990/01/01 00:00:00'),'yyyy/mm/dd hh24:mi:ss'),nvl(o.LOANTERM,0)) AS V.ENDDATE from o,jbo.trade.business_contract ci where o.CONTRACTID = ci.serialno and ci.RELATIVESERIALNO=:loanNo";
			BizObjectQuery query = manager.createQuery(sql);
			query.setParameter("loanNo", param);
			result = query.getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}
	/*
	 * 还款计划和还款明细级联表
	 */
	@SuppressWarnings("unchecked")
	private List<BizObject> getPayDetailResultList(String param) {
		List<BizObject> result = null;
		JBOFactory factory = JBOFactory.getFactory();
		BizObjectManager manager;
		try {
			manager = factory.getManager("jbo.trade.acct_back_detail");
			BizObjectQuery query = manager
					.createQuery("select  o.ACTUALPAYCORPUSAMT,o.ACTUALPAYINTEAMT,o.ACTUALFINEAMT,ps.PAYCORPUSAMT,ps.PAYINTEAMT,ps.PAYFINEAMT,ps.SEQID,ps.PAYDATE from o , jbo.trade.acct_payment_schedule ps where o.PSSERIALNO=ps.SERIALNO and o.LOANSERIALNO=:loanSerialNo");
			query.setParameter("loanSerialNo", param);
			result=query.getResultList(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
