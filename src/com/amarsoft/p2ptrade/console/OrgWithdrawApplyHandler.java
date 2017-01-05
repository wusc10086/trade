package com.amarsoft.p2ptrade.console;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;

/**
 * 机构提现申请交易 
 * 输入参数： 
 * 			OrgID:			机构编号 
 * 			AccountType:	账户类型（Code:OrgAccountType）
 * 			AccountNo:		银行卡号 
 * 			AccountName:	账户名 
 * 			AccountBelong:	开户行
 * 			Amount:			提现金额 
 * 输出参数： 
 * 			SuccessFlag：	成功标识（S/F）
 * @author dxu
 */
public class OrgWithdrawApplyHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String orgID = request.get("OrgID").toString();// 用户编号
		String accountNo = request.get("AccountNo").toString();
		String accountName = request.get("AccountName").toString();
		String accountBelong = request.get("AccountBelong").toString();
		String accountType = request.get("AccountType").toString();
		
		// 参数校验
		if (orgID == null || orgID.length() == 0)
			throw new HandlerException("common.emptyorgid");
		if (accountNo == null || accountNo.length() == 0)
			throw new HandlerException("accountno.error");
		if (accountName == null || accountName.length() == 0)
			throw new HandlerException("accountname.error");
		if (accountBelong == null || accountBelong.length() == 0)
			throw new HandlerException("accountbelong.error");
		if (request.get("Amount") == null || "".equals(request.get("Amount")))
			throw new HandlerException("withdraw.amount.error");
		
		double amount = 0d;
		try {
			amount = Double.parseDouble((String) request.get("Amount"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new HandlerException("withdraw.amount.error");
		}
		
		if (amount <= 0) {
			throw new HandlerException("withdraw.amount.error");
		}
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		try {
			tx = jbo.createTransaction();
			
			BizObjectManager m = jbo.getManager("jbo.trade.org_account_info");
			BizObjectQuery query = m.createQuery("OrgID=:OrgID and AccountNo=:AccountNo "
				+ "and AccountBelong=:AccountBelong  and AccountName=:AccountName and AccountType=:AccountType and Status='1'");
			query.setParameter("OrgID", orgID).setParameter("accountno",accountNo).
				setParameter("AccountBelong",accountBelong).setParameter("AccountName",accountName).setParameter("AccountType",accountType);
			
			BizObject account = null;
			List list = query.getResultList(false);
			if(list != null && list.size() > 1){
				throw new HandlerException("withdraw.multipleaccount");
			}else if (list != null && list.size() == 1) {
				account = (BizObject) list.get(0);
			} else {
				throw new HandlerException("queryaccountinfo.nodata.error");
			}

			// 获取当前时间
			TimeTool tool = new TimeTool();
			String sTransDate = tool.getsCurrentDate();
			String sInputTime = tool.getsCurrentMoment();

			double usablebalance = Double.parseDouble(account.getAttribute("USABLEBALANCE").getString());// 获取账户可用余额

			if (usablebalance < amount) {// 可用余额小于提现金额
				throw new HandlerException("usablebalance.notenough");
			}

			// 冻结提现金额
			account.setAttributeValue("LOCKFLAG", "1");
			m.saveObject(account);
			
			double usableBalanceOld = Double.parseDouble(account.getAttribute("USABLEBALANCE").toString());
			double frozenBalanceOld = Double.parseDouble(account.getAttribute("FROZENBALANCE").toString());

			double usableBalance = usableBalanceOld - amount;
			double frozenBalance = frozenBalanceOld + amount;
			account.setAttributeValue("USABLEBALANCE", usableBalance);
			account.setAttributeValue("FROZENBALANCE", frozenBalance);

			account.setAttributeValue("LOCKFLAG", "2");
			m.saveObject(account);

			// 添加交易记录
			m = jbo.getManager("jbo.trade.transaction_record", tx);
			BizObject o = m.newObject();

			o.setAttributeValue("ORGID", orgID);// 用户编号
			o.setAttributeValue("DIRECTION", "P");// 发生方向(支出)
			o.setAttributeValue("AMOUNT", amount);// 交易金额
			o.setAttributeValue("HANDLCHARGE", 0);// 手续费
			o.setAttributeValue("TRANSTYPE", "1025");// 交易类型（提现）
			o.setAttributeValue("INPUTTIME", sTransDate+" "+sInputTime);// 创建时间
			o.setAttributeValue("STATUS", "01");// 交易状态
			o.setAttributeValue("RELAACCOUNT", account.getAttribute("SERIALNO").getString());// 关联账户流水号
			o.setAttributeValue("RELAACCOUNTTYPE", "002");// 交易关联账户类型
			o.setAttributeValue("TRANSCHANNEL", "1010");// 交易渠道
			JSONObject items = GeneralTools.getItemName(jbo, "BankNo");
			String accountBelongName = items.containsKey(accountBelong) ? items.get(accountBelong).toString() : accountBelong;
			o.setAttributeValue("REMARK","提现" + "|" + accountBelongName);// 备注
			m.saveObject(o);

			tx.commit();
			
			result.put("SuccessFlag", "S");
			return result;
		} catch (HandlerException e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			throw e;
		} catch (Exception e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			throw new HandlerException("withdraw.error");
		}
	}
	
	/**
	 * 冻结提现金额
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param tx
	 *            JBOTransaction
	 * @param sUserId
	 *            用户编号
	 * @param amount
	 *            冻结金额
	 * @throws HandlerException
	 */
	private void makeAmountFrozen(JBOFactory jbo, JBOTransaction tx,
			String sUserId, double amount) throws HandlerException {
		double UsableBalance = 0;// 用户可用余额
		double FrozenBalance = 0;// 用户冻结金额
		double reUsableBalance = 0;// 调整后余额
		double reFrozenBalance = 0;// 调整后冻结金额
		BizObjectManager account;
		try {
			account = jbo.getManager("jbo.trade.user_account", tx);

			BizObject oo = account.createQuery("userid=:userid")
					.setParameter("userid", sUserId).getSingleResult(true);
			if (oo != null) {
				UsableBalance = Double.parseDouble(oo.getAttribute(
						"USABLEBALANCE").toString());
				FrozenBalance = Double.parseDouble(oo.getAttribute(
						"FROZENBALANCE").toString());

				oo.setAttributeValue("LOCKFLAG", "2");
				account.saveObject(oo);

				reUsableBalance = UsableBalance - amount;
				reFrozenBalance = FrozenBalance + amount;
			} else {
				throw new HandlerException("quaryaccountamount.nodata.error");
			}
			oo.setAttributeValue("USABLEBALANCE", reUsableBalance);
			oo.setAttributeValue("FROZENBALANCE", reFrozenBalance);

			oo.setAttributeValue("LOCKFLAG", "1");
			account.saveObject(oo);
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			throw new HandlerException("makeamountfrozen.error");
		}
	}
	
}
