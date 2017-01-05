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
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.web.service.imp.transclass.JsptoTrans;
import com.amarsoft.web.service.imp.transclass.transformobject.OPayInfoRes;
import com.amarsoft.web.service.imp.util.TransTools;

/**
 * @ 发起还款 2014-5-16 输入 Paytype 还款类型：1一般还款,3提前还款 合同号 输出
 * 本金、利息、罚息、平台服务费、担保费、平台管理费、担保管理费、提前还款违约金、账户余额
 */
public class PaymentHandler extends TradeHandler {
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
		/**
		 * 以下为测试时被注释，正式使用时应该取消
		 */
		// else if(!request.get("userId").equals(getUseId(request, jbo))){
		// throw new HandlerException("request.invalid");
		// }
		
		if ("1".equals(request.get("PayType").toString())) {
			request.put("Method", "Payment");
		}else if ("3".equals(request.get("PayType").toString())) {
			request.put("Method", "REPayment");
		}
		validateInfo(request, jbo);
		
		return request;
	}

	/**
	 * 调用接口保存数据，并判断是否需要充值
	 * 
	 * @param request
	 * @param jbo
	 */
	@SuppressWarnings("unchecked")
	private void validateInfo(JSONObject request, JBOFactory jbo) {
		
		
		String sPayAmt=request.get("payAmt")==null?"0":request.get("payAmt").toString();
		JsptoTrans jt = new JsptoTrans();
		try {
			jt.setConn(ARE.getDBConnection("als"));// 链接
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		String loanNo = "TEST20140916";
		int seqId = 3;// 测试初始化值，正常为0
//		try {
//			seqId = getCurrentSeq(request, jbo);
//		} catch (HandlerException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		String serialNo = loanNo + seqId;
		jt.setMethod(request.get("Method").toString());// 交易方式
		jt.setObjectNo(loanNo);// 贷款账号 借据号
		jt.setAmt(sPayAmt);// 还款金额 试算时随便输入
		jt.setObjectType("");
		jt.setSerialNo(serialNo);
	
			OPayInfoRes opir = (OPayInfoRes) jt.runPayment();
			String stype = opir.getReturnType();
		
		//检测是否有错误
		if (null!=stype&&!"0000".equals(stype)) {
			
			System.out.println("发起还款出错=======================》"+TransTools.getErrorMss(stype));
		} else {
			System.out.println("还款成功！=================》");
			request.put("flag", "success");
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

	/*
	 * 返回信息
	 */
	@Override
	protected Object responseObject(JSONObject request, JSONObject response, String logid, String transserialno, JBOFactory jbo) throws HandlerException {

		return response;
	}

}
