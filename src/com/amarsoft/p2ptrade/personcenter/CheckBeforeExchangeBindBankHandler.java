package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 用户更换银行卡前验证
 * 输入参数： 
 * 		UserID:用户编号
 * 输出参数： 成功标志 
 * 
 */
public class CheckBeforeExchangeBindBankHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkBeforeExchangeBank(request);
	}

	/**
	 * 用户更换银行卡前验证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject checkBeforeExchangeBank(JSONObject request)
			throws HandlerException {

		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}

		try {
			String sUserID = request.get("UserID").toString();// 用户编号
			JBOFactory jbo = JBOFactory.getFactory();
			
			BizObjectManager accountManager = jbo.getManager(
					"jbo.trade.user_account");
			BizObject accountBo = accountManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			double usableBalance = 0;// 账户可用余额
			double frozenBalance = 0;// 账户冻结金额
			// 获取用户余额信息
			if (accountBo != null) {
				usableBalance = Double.parseDouble(accountBo.getAttribute(
						"USABLEBALANCE").toString() == null ?"0":accountBo.getAttribute(
								"USABLEBALANCE").toString());
				frozenBalance = Double.parseDouble(accountBo.getAttribute(
						"FROZENBALANCE").toString() == null ?"0":accountBo.getAttribute(
								"FROZENBALANCE").toString());
			} else {
				throw new HandlerException("quaryaccountamount.nodata.error");
			}
			//账户余额校验
			if(usableBalance != 0 || frozenBalance!=0){
				throw new HandlerException("counthavebalance.error");
			}
			
			//投资和还款校验
//			BizObjectManager loanManager = jbo.getManager("jbo.trade.acct_loan");
//			BizObjectQuery loanQuery = loanManager
//					.createQuery("select count(1) as v.cnt from o ,jbo.trade.ti_contract_info tci ,jbo.trade.user_contract tua  "
//							+ "where tci.contractid = tua.contractid and serialno = tci.loanno and loanstatus in ('0','1') "
//							+ "and finishdate is null  and tua.userid =:userid ");
			
			BizObjectManager loanManager = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery loanQuery = loanManager
					.createQuery("select count(1) as v.cnt from o ,jbo.trade.ti_contract_info tci ,jbo.trade.user_contract tua  "
							+ "where tci.contractid = tua.contractid and o.serialno = tci.loanno and o.loanstatus not  in ('10','20','30','80','91','92') "
							+ "  and tua.userid =:userid ");
			loanQuery.setParameter("userid", sUserID);
			
			

			BizObject loanManagerBo = loanQuery.getSingleResult(false);
			double count;
			if (loanManagerBo != null) {
				count = Double.parseDouble(loanManagerBo.getAttribute("cnt").toString());
			} else {
				count = 0.0;
			}
			if(count > 0){
				throw new HandlerException("haveunclearbus.error");
			}
			return null;
		}
		catch (HandlerException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("checkbeforeexchangebindcark.error");
		}
	}
}
