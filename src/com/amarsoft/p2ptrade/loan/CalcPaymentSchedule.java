package com.amarsoft.p2ptrade.loan;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.amarsoft.app.accounting.entity.BusinessObject;
import com.amarsoft.app.accounting.entity.BusinessObjectPK;
import com.amarsoft.app.accounting.util.ACCOUNT_CONSTANTS;
import com.amarsoft.app.accounting.util.DateTools;
import com.amarsoft.app.accounting.util.LoanTools;
import com.amarsoft.app.accounting.util.NumberTools;
import com.amarsoft.are.util.DataConvert;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 计算贷款还款计划，取消原有的处理方式
 * */
public class CalcPaymentSchedule extends JSONHandler{

	@Override
	public Object createResponse(JSONObject json, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		// 获取贷款金额
		String amount = json.get("amount").toString();
		// 获取贷款期限
		String sLoanTerm = json.get("year").toString();
		// 获取还款方式
		String paymethod = json.get("ways").toString();
		// 获取贷款利率
		String businessRate = json.get("businessRate").toString();
		// 获取月管理费
		String monthfee = json.get("monthfee").toString();
		// 获取一次性收费
		String lonelyfee = json.get("lonelyfee").toString();

		NumberFormat formatter = new DecimalFormat("#0.00");
		//初始化信息
		try {
			if(amount==null||"".equals(amount)) amount = "0";
			if(sLoanTerm==null||"".equals(sLoanTerm)) amount = "0";
			if(businessRate==null||"".equals(businessRate)) businessRate = "0.000001";
			double businessSum = Double.parseDouble(amount);
			double loanRate = Double.parseDouble(businessRate);
			int loanTerm = Integer.parseInt(sLoanTerm);
			
			String beginDate = StringFunction.getToday("/");
			String endDate = DateTools.getRelativeDate(beginDate, ACCOUNT_CONSTANTS.TERM_UNIT_MONTH, loanTerm);
			
			BusinessObject loan = new BusinessObject();
			
			getRepaySchedule(loan,
					businessSum,
					beginDate,
					endDate,
					loanRate,
					paymethod
			);
			//计算还款计划
			ArrayList<BusinessObject> rps = LoanTools.getPaymentSchedules(loan, endDate);
			//利息合计
			double dInteSum = 0.0;
			
			JSONArray calclist = new JSONArray();
			for(int i = 0 ; i < rps.size() ; i++)
			{
				
				BusinessObject ps = rps.get(i);
				HashMap<String,Object> rpAttribute = ps.getAllAttributes();
				
				double dCorpus =  Double.parseDouble(String.valueOf(rpAttribute.get("PAYCORPUSAMT")));
				double dInte =  Double.parseDouble(String.valueOf(rpAttribute.get("PAYINTEAMT"))); 
				double dBalance =  Double.parseDouble(String.valueOf(rpAttribute.get("CORPUSBALANCE"))); 
				String seqId =  String.valueOf(rpAttribute.get("SEQID")); 
				
				dInteSum = NumberTools.round(dInteSum+dInte, 2);
				
				JSONObject calc = new JSONObject();
				calc.put("order", seqId);// 期次
				calc.put("MonthInterest", DataConvert.toMoney(dInte));// 每月利息
				calc.put("MonthSum", DataConvert.toMoney(dCorpus));// 每月本金
				calc.put("MonthPay", DataConvert.toMoney(NumberTools.round(dInte+dCorpus, 2)));// 每月还款总额
				calc.put("overplus", DataConvert.toMoney(dBalance));// 剩余本金
				calclist.add(calc);
			}
			
			
			String repay_total_string = formatter.format(NumberTools.round(dInteSum+businessSum, 2));
			result.put("total_loan", DataConvert.toMoney(businessSum));
			result.put("total_month", loanTerm);
			result.put("repay_month", loanTerm);
			result.put("repay", DataConvert.toMoney(dInteSum));
			result.put("repay_total", DataConvert.toMoney(NumberTools.round(dInteSum+businessSum, 2)));
			
			result.put("repay_total_string", repay_total_string);

			result.put("tot_int", 0);
			result.put("manageefee", 0.0);
			result.put("onepayfee", 0.0);
			result.put("repay_first", 0.0);
			result.put("month_intrest", 0.0);
			result.put("currway", 0.0);
			result.put("currcal", 0.0);
			
			result.put("calclist", calclist);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	/**
	 * 计算还款计划
	 * */
	private static final void getRepaySchedule(BusinessObject loan,
			double businessSum,
			String beginDate,
			String endDate,
			double loanRate,
			String payMethod
			) throws Exception
	{
		loan.setId(new BusinessObjectPK("Loan"));
		//设置金额
		loan.setAttribute("NormalBalance", businessSum);
		//设置期限
		loan.setAttribute("PUTOUTDATE", beginDate);
		loan.setAttribute("MATURITYDATE", endDate);
		
		//设置利率
		loan.setAttribute("LOANRATE", loanRate);
		loan.setAttribute("LOANRATEMODE", "2");
		loan.setAttribute("LOANRATECODE", "01");
		loan.setAttribute("BASEDAYS", 360);
		
		//TODO 
		if("RPT000040".equals(payMethod)||"RPT000050".equals(payMethod))
			loan.setAttribute("INTEBEARINGTYPE", "0");
		else if("RPT000010".equals(payMethod)||"RPT000020".equals(payMethod))
			loan.setAttribute("INTEBEARINGTYPE", "2");
		
		loan.setAttribute("Lastintedate", loan.getAttributeValue("PUTOUTDATE"));
		loan.setAttribute("PMTMaturity", loan.getAttributeValue("MATURITYDATE"));
		//loan.setAttribute("DEFAULTPAYDATE", "20");
		
//		loan.setAttribute("LOANRATEFLOATTYPE", "2");
		//设置还款方式
		loan.setAttribute("RepaymentMethod",  payMethod);
		loan.setAttribute("ISPAYCURRENTMONTH", ACCOUNT_CONSTANTS.NO);
		loan.setAttribute("REPAYMENTCYCLE", "1");
		
		loan.setAttribute("INOFFFLAG", "1");
		
		
	}
	
}
