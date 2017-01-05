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
 * 判断用户当前卡是否已经存在
 * 输入参数： UserID:账户编号 
 * 			AccountNo:银行卡号 
 * 			AccountName:账户名 
 * 			AccountBelong:开户行
 * 输出参数： 
		true: 未有
		false：已有
 * 
 */
public class CheckNeedBindBankHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkBeforeBindBank(request);
	}

	/**
	 * 用户更换银行卡前验证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject checkBeforeBindBank(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("AccountNo") == null
				|| "".equals(request.get("AccountNo")))
			throw new HandlerException("accountno.error");
		if (request.get("AccountName") == null
				|| "".equals(request.get("AccountName")))
			throw new HandlerException("accountname.error");
		if (request.get("AccountBelong") == null
				|| "".equals(request.get("AccountBelong")))
			throw new HandlerException("accountbelong.error");
		try {
			String sUserID = request.get("UserID").toString();
			String sAccountNo = request.get("AccountNo").toString();
			String sAccountName = request.get("AccountName").toString();
			String sAccountBelong = request.get("AccountBelong").toString();

			JBOFactory jbo = JBOFactory.getFactory();

			BizObjectManager accountManager = jbo
					.getManager("jbo.trade.account_info");
			BizObjectQuery query = accountManager
					.createQuery("select serialno from o where userid=:userid and accountno=:accountno and accountname=:accountname and accountbelong=:accountbelong and status='2'");
					query.setParameter("userid", sUserID).setParameter("accountno", sAccountNo).setParameter("accountname", sAccountName).setParameter("accountbelong", sAccountBelong);
			BizObject accountBo = query.getSingleResult(false);
			String sSerialNo = null;
			String againFlag = "false";
			if (accountBo != null) {
				/*
				sSerialNo = accountBo.getAttribute("serialno").getString();
				BizObjectManager recordManager = jbo
						.getManager("jbo.trade.transaction_record");
				BizObject recordBo = recordManager.createQuery("userid=:userid and relaaccount=:relaaccount and transtype like :transtype and status in ('03','10')")
						.setParameter("userid", sUserID).setParameter("relaaccount", sSerialNo).setParameter("transtype", "107%").getSingleResult(false);
				
				if(recordBo != null){
				*/
					againFlag = "false";
//					throw new HandlerException("bindcard.again.error");
				//}
			} else {
				againFlag = "true";
			}
			
			JSONObject result = new JSONObject();
			result.put("againFlag", againFlag);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("checkneedbindbank.error");
		}
	}
}
