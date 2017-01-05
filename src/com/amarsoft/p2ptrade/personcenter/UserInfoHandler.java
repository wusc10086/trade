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
 * 获取用户信息交易
 * 输入参数： UserID:账户编号 
 * 输出参数： 
 * 			UsableBalance:账户可用余额 
 * 			FrozenBalance:账户冻结金额
 * 			AccountNo:账户号 
 * 			AccountName:账户名 
 * 			AccountBelong:开户银行 
 * 			LimitAmount:提现限额
 * 			TimeLimitAmount:单笔限额
 * 			DayLimitAmount:单日限额 
 * 
 */
public class UserInfoHandler extends JSONHandler {
	JSONObject result = null;
	private String sAccountBelong = null;
	

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}
		String sUserID = request.get("UserID").toString();
		result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		getAccountBalance(jbo,sUserID);
		getAccountInfo(jbo,sUserID);
		double dayLimitAmount = GeneralTools.getLimitAmount(jbo, sAccountBelong, "ATTRIBUTE8");//获取渠道限额
		if(sAccountBelong.equals("0102")){
			double timeLimitAmount = GeneralTools.getLimitAmount(jbo, sAccountBelong, "ATTRIBUTE7");//获取渠道限额
			result.put("TimeLimitAmount", String.valueOf(GeneralTools.numberFormat(timeLimitAmount)));//单笔限额（单位万元）
			result.put("DayLimitAmount", String.valueOf(GeneralTools.numberFormat(dayLimitAmount)));//当日限额（单位万元）
		}else{
			result.put("TimeLimitAmount", "500000.00");//单笔限额（单位元）
			result.put("DayLimitAmount", String.valueOf(GeneralTools.numberFormat(dayLimitAmount)));//当日限额（单位万元）
		}
		
		return result;
	}

	
	/**
	 * 账户余额查询
	 * @param jbo   JBOFactory
	 * @param sUserID   用户编号
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private void getAccountBalance(JBOFactory jbo,String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");

			BizObjectQuery query = m
					.createQuery("select usablebalance,frozenbalance from o where userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				double usableBalance = Double.parseDouble(o.getAttribute("USABLEBALANCE").toString() == null?"0":o.getAttribute("USABLEBALANCE").toString());
				double frozenBalance = Double.parseDouble(o.getAttribute("FROZENBALANCE").toString() == null?"0":o.getAttribute("FROZENBALANCE").toString());
				result.put("UsableBalance", GeneralTools.numberFormat(usableBalance, 0, 2));
				result.put("FrozenBalance", GeneralTools.numberFormat(frozenBalance, 0, 2));
			} else {
				
				throw new HandlerException("common.user_account.usernotexist");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getuseablebalance.error");
		}
	}
	
	/**
	 * 账户信息查询
	 * @param jbo   JBOFactory
	 * @param sUserID   用户编号
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private void getAccountInfo(JBOFactory jbo,String sUserID)
			throws HandlerException {

		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");

			BizObjectQuery query = m
					.createQuery("userid=:userid and status = '2'");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);//应该为getResultList(false)
			if (o != null) {
				String sSerialNo = o.getAttribute("SERIALNO").getString();
				String sAccountNo = o.getAttribute("ACCOUNTNO").getString();
				String sAccountEndNo = sAccountNo.substring(sAccountNo.length() - 4, sAccountNo.length());
				
				result.put("SerialNo", sSerialNo);
				result.put("AccountNo", sAccountNo);
				result.put("AccountEndNo", sAccountEndNo);
				result.put("AccountName", o.getAttribute("ACCOUNTNAME").getString());
				sAccountBelong =  o.getAttribute("ACCOUNTBELONG").getString();
				result.put("AccountBelong", o.getAttribute("ACCOUNTBELONG").getString());
				result.put("LimitAmount", String.valueOf(GeneralTools.numberFormat(o.getAttribute("LIMITAMOUNT").getDouble())));
			} else {
				throw new HandlerException("common.account_info.usernotexist");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryaccountinfo.error");
		}
	}
	
}
