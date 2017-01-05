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
 * 提现手续费计算交易 
 * 输入参数： 
 * 		Amount:提现金额 
 * 		AccountBelong：开户行 
 * 输出参数：
 * 		Amount:实际到帐金额  
 * 		HandlCharge：手续费
 */
public class CalcTransFeeHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getCalcTransFee(request);

	}

	@SuppressWarnings("unchecked")
	private JSONObject getCalcTransFee(JSONObject request) throws HandlerException {
		if (request.get("Amount") == null || "".equals(request.get("Amount"))) {
			throw new HandlerException("withdraw.amount.error");
		}
		if (request.get("AccountBelong") == null
				|| "".equals(request.get("AccountBelong"))) {
			throw new HandlerException("accountbelong.error");
		} 

		double amount = Double.parseDouble(request.get("Amount").toString());// 提现金额
		
		JBOFactory jbo = JBOFactory.getFactory();
		
		double handlCharge = GeneralTools.getCalTransFee(jbo,"0010",amount);//手续费
		
		String sAccountBelong = request.get("AccountBelong").toString();// 开户银行

		try {
			JSONObject obj = new JSONObject();
			obj.put("RootType", "010");
			obj.put("Amount", String.valueOf(GeneralTools.numberFormat(amount - handlCharge)));
			obj.put("HandlCharge", String.valueOf(GeneralTools.numberFormat(handlCharge)));
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("calctransfee.error");
		}
	}
}
