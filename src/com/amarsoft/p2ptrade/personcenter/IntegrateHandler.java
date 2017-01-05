package com.amarsoft.p2ptrade.personcenter;

/*
 * 整合的Handler，将一个页面中调用的多个Handler合并在这一个Handler中执行
 * 
 * @author by cyliu at 2014.8.25
 * */

import java.util.Properties;

import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class IntegrateHandler extends JSONHandler {

	// 用于接受表示调用哪些具体Handler的参数
	private int runType = 0;

	String rootTypeStat = "";
	String rootTypeList = "";

	// 存放返回的结果
	JSONObject result = new JSONObject();

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		runType = Integer.valueOf((String) request.get("RunType"));
		switch (runType) {
		// 借款管理-我的网络P2P借款
		case 1:
			return getLoanInfo(request, arg1);
		// 投资管理-我的投资
		case 2:
			return getInvestInfo(request, arg1);
		default:
			return "";
		}
	}

	private Object getLoanInfo(JSONObject request, Properties arg)
			throws HandlerException {
		LoanInfoSumHandler loanInfoSumHandler = new LoanInfoSumHandler();
		LoanListHandler listHandler = new LoanListHandler();

		// 借款统计
		JSONObject sResponseLoanStat = (JSONObject) loanInfoSumHandler
				.createResponse(request, arg);
		rootTypeStat = (String) sResponseLoanStat.get("RootType");
		if (rootTypeStat.equals("010")) {
			result.put("responseLoanStat", sResponseLoanStat); // 借款统计信息
		}

		// 借款列表信息查询
		JSONObject sResponseLoanList = (JSONObject) listHandler.createResponse(
				request, arg);
		rootTypeList = (String) sResponseLoanList.get("RootType");
		if (rootTypeList.equals("020")) {
			result.put("responseLoanList", sResponseLoanList); // 借款列表信息
		}

		return result;
	}

	private Object getInvestInfo(JSONObject request, Properties arg)
			throws HandlerException {

		InvestmentStatHandler investmentStatHandler = new InvestmentStatHandler();
		InvestmentListHandler investmentListHandler = new InvestmentListHandler();

		// 投资统计
		JSONObject sResponseInvestStat = (JSONObject) investmentStatHandler
				.createResponse(request, arg);
		rootTypeStat = (String) sResponseInvestStat.get("RootType");
		if (rootTypeStat.equals("010")) {
			result.put("responseInvestStat", sResponseInvestStat);// 投资统计信息
		}

		// 投资列表信息
		JSONObject sResponseInvestList = (JSONObject) investmentListHandler
				.createResponse(request, arg);
		rootTypeList = (String) sResponseInvestList.get("RootType");
		if (rootTypeList.equals("030")) {
			result.put("responseInvestList", sResponseInvestList); // 投资列表信息
		}

		return result;
	}
}
