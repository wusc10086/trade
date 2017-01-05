/**
 * 
 */
package com.amarsoft.p2ptrade.transaction;

import java.sql.SQLException;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.web.service.imp.transclass.JsptoTrans;
import com.amarsoft.web.service.imp.transclass.transformobject.OPayInfoRes;
import com.amarsoft.web.service.imp.util.TransTools;

/**
 * @ 还款试算接口 2014-5-16 输入 Paytype 还款类型：1一般还款,3提前还款 合同号 输出
 * 本金、利息、罚息、平台服务费、担保费、平台管理费、担保管理费、提前还款违约金、账户余额
 */
public class PaymentInfoConsultHandler extends TradeHandler {
	static {
		Parser.registerFunction("to_date");
		Parser.registerFunction("min");

	}

	/*
	 * 发起请求
	 * 
	 * REPaymentConsult 提前还款 PaymentConsult 手动还款 交易方式 Payment 一般还款 REPayment
	 * 提前还款 PaymentConsult 一般还款试算 REPaymentConsult 提前还款试算
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected Object requestObject(JSONObject request, JBOFactory jbo) throws HandlerException {

		if (request.get("PayType") == null || request.get("loanNo") == null || null == request.get("userId")) {
			throw new HandlerException("request.invalid");
		}
		// else if(!request.get("userId").equals(getUseId(request, jbo))){
		// throw new HandlerException("request.invalid");
		// }
		String paytypeString = (String) request.get("PayType");
		if ("3".equals(paytypeString)) {
			request.put("Method", "REPaymentConsult");
		} else if ("1".equals(paytypeString)) {
			request.put("Method", "PaymentConsult");
		}
		validateInfo(request, jbo);
		return request;
	}

	/**
	 * 调用接口保存数据，并判断是否需要充值
	 * 
	 * @param request
	 * @param jbo
	 * @throws HandlerException 
	 */
	@SuppressWarnings("unchecked")
	private void validateInfo(JSONObject request, JBOFactory jbo) throws HandlerException {
		JSONObject result = new JSONObject();
		JsptoTrans jt = new JsptoTrans();
		try {
			jt.setConn(ARE.getDBConnection("als"));// 链接
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		String loanNo = "TEST20140916";
		int seqId=3;
//		String loanNo = request.get("loanNo").toString();
//		int seqId = getCurrentSeq(request, jbo);// 测试初始化值，正常为0
		String serialNo = loanNo + seqId;
		jt.setMethod(request.get("Method").toString());// 交易方式
		jt.setObjectNo(loanNo);// 贷款账号 借据号
		jt.setAmt("1000");// 还款金额 试算时随便输入
		jt.setObjectType("");
		jt.setSerialNo(serialNo);
		OPayInfoRes opir = (OPayInfoRes) jt.runPayment();
		String stype = opir.getReturnType();
		//检测是否有错误
		if (null!=stype&&!"0000".equals(stype)) {
			System.out.println(TransTools.getErrorMss(stype));
		} else {
			String actualPayCorpusAmt = opir.getActualPayCorpusAmt()==null?"0":opir.getActualPayCorpusAmt().toString();// 本金
			String actualPayFineAmt = opir.getActualPayFineAmt()==null?"0":opir.getActualPayFineAmt().toString();// 罚息
			String actualPayInteAmt = opir.getActualPayInteAmt()==null?"0":opir.getActualPayInteAmt().toString();// 利息
			String plantMange = opir.getPlantmange()==null?"0":opir.getPlantmange().toString();// 平台管理费
			String plantfee = opir.getPlantfee()==null?"0":opir.getPlantfee().toString();// 平台服务费
			String managefee = opir.getManagefee()==null?"0":opir.getManagefee().toString();// 担保费
			String insureManagementFee = opir.getInsuremanagement_fee()==null?"0":opir.getInsuremanagement_fee().toString();// 担保管理费
			String penalValue = opir.getPenal_value()==null?"0":opir.getPenal_value().toString();// 违约金
			String payAmt = opir.getPayamt()==null?"0":opir.getPayamt().toString();// 总金额
			
			double total = Double.parseDouble(actualPayCorpusAmt) + Double.parseDouble(actualPayFineAmt) + Double.parseDouble(actualPayInteAmt)
					+ Double.parseDouble(managefee) + Double.parseDouble(plantfee) + Double.parseDouble(plantMange) + Double.parseDouble(insureManagementFee)
					+ Double.parseDouble(penalValue);
			double accountBalance = 0;
			try {
				accountBalance = getAccountBalance(request, jbo);
			} catch (HandlerException e) {
				e.printStackTrace();
			}
			if (total > accountBalance) {
				result.put("needCharge", "t");
			} else {
				result.put("needCharge", "f");
			}
			result.put("actualPayCorpusAmt", actualPayCorpusAmt);// 本金
			result.put("actualPayInteAmt", actualPayInteAmt);// 利息
			result.put("actualPayFineAmt", actualPayFineAmt);// 罚息
			result.put("plantfee", plantfee);// 平台服务费
			result.put("plantMange", plantMange);// 平台管理费
			result.put("managefee", managefee);// 担保费
			result.put("insureManagementFee", insureManagementFee);// 担保管理费
			result.put("penalValue", penalValue);// 违约金
			result.put("payAmt", payAmt);// 总金额
			result.put("total", total);// 须还总金额
			result.put("accountBalance", accountBalance);// 账户总金额
			request.put("paymentInfo", result);
		}
	}

	/**
	 * 获得用户id
	 * 
	 * @param request
	 * @param jbo
	 * @return
	 * @throws HandlerException
	 */
	private String getUseId(JSONObject request, JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		String customerId = "";
		String loanNo = (String) request.get("loanNo");
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query = m.createQuery("select CUSTOMERID from o where SERIALNO=:SERIALNO");
			query.setParameter("SERIALNO", loanNo);
			BizObject o = query.getSingleResult(false);
			customerId = o.getAttribute("CUSTOMERID").getValue() == null ? "" : o.getAttribute("CUSTOMERID").getString();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return customerId;
	}

	/**
	 * 获得账户余额
	 * 
	 * @param request
	 * @param jbo
	 * @throws HandlerException
	 */
	private double getAccountBalance(JSONObject request, JBOFactory jbo) throws HandlerException {
		BizObjectManager m;
		double balance = 0;
		String userId = request.get("userId").toString();
		try {
			m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m.createQuery(" USERID=:USERID");
			query.setParameter("USERID", userId);
			BizObject o = query.getSingleResult(false);
			// 账户余额
			balance = o.getAttribute("USABLEBALANCE").getString() == null ? 0 : o.getAttribute("USABLEBALANCE").getDouble();
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return balance;
	}

	/**
	 * 获取当前期次
	 * 
	 * @param request
	 * @param jbo
	 * @return
	 * @throws HandlerException
	 */
	private int getCurrentSeq(JSONObject request, JBOFactory jbo) throws HandlerException {
		BizObjectManager m;
		String loanNo = request.get("loanNo").toString();// 借据号
		int currentSeq = 0;// 当前期次
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select o.seqid,o.papdate from o where o.OBJECTNO=:loanNo and to_date(o.paydate,'yyyy/MM/dd')=(select min(to_date(o.paydate,'yyyy/MM/dd')) from o where to_date(o.paydate,'yyyy/MM/dd')>sysdate)";
		try {
			m = jbo.getManager(table);
			BizObjectQuery query = m.createQuery(sql);
			query.setParameter("loanNo", loanNo);
			BizObject o = query.getSingleResult(false);
			// 账户余额
			currentSeq = o.getAttribute("USABLEBALANCE").getString() == null ? 0 : o.getAttribute("USABLEBALANCE").getInt();
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return currentSeq;

	}

	/*
	 * 返回信息
	 */
	@Override
	protected Object responseObject(JSONObject request, JSONObject response, String logid, String transserialno, JBOFactory jbo) throws HandlerException {

		return response;
	}

}
