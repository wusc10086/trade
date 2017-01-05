package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/**
 * 提现限额设置 
 * 输入参数： 
 * 		UserID:账户编号 
 * 		AccountNo:卡号 
 * 		AccountName户名 
 * 		AccountBelong开户行 
 * 		LimitAmount:限额。
 * 输出参数： 
 * 		成功标识、错误代码
 */
public class SetRegBankCardWithdrawLimitHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return SetBankCardWithdrawLimit(request);
	}

	/**
	 * 查询绑定银行卡
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject SetBankCardWithdrawLimit(JSONObject request)
			throws HandlerException {
		//参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}
		if (request.get("AccountNo") == null
				|| "".equals(request.get("AccountNo")))
			throw new HandlerException("accountno.error");
		if (request.get("AccountName") == null
				|| "".equals(request.get("AccountName")))
			throw new HandlerException("accountname.error");
		if (request.get("AccountBelong") == null
				|| "".equals(request.get("AccountBelong")))
			throw new HandlerException("accountbelong.error");
		if (request.get("LimitAmount") == null || "".equals(request.get("LimitAmount")))
			throw new HandlerException("limitamount.error");
		
		String sUserID = request.get("UserID").toString();
		String sAccountNo = request.get("AccountNo").toString();
		String sAccountName = request.get("AccountName").toString();
		String sAccountBelong = request.get("AccountBelong").toString();
		double limitAmount = 0d;
		try {
			limitAmount = Double.parseDouble(request.get("LimitAmount").toString());
		} catch (NumberFormatException e2) {
			throw new HandlerException("limitamount.error");
		}
		if(limitAmount <= 0){
			throw new HandlerException("limitamount.error");
		}

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");

			BizObjectQuery query = m.createQuery("userid=:userid and accountno=:accountno and accountname=:accountname and accountbelong=:accountbelong and status='2'");
			query.setParameter("userid", sUserID).setParameter("accountno", sAccountNo).setParameter("accountname", sAccountName).setParameter("accountbelong", sAccountBelong);

			JSONObject obj = new JSONObject();
			BizObject o = query.getSingleResult(true);
			if (o != null) {
				o.setAttributeValue("LIMITAMOUNT", limitAmount);
				m.saveObject(o);
			} else {
				throw new HandlerException("account.notexist.error");
			}
			return obj;
		}catch (HandlerException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("setregbankcardwithdrawlimit.error");
		}
	}
}
