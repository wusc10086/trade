package com.amarsoft.p2ptrade.personcenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.amarsoft.account.common.ObjectBalanceUtils;
import com.amarsoft.account.common.ObjectConstants;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.DataConvert;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2p.interfaces.utils.InterfaceHelper;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

import freemarker.log.Logger;

/**
 * 快捷支付推进
 * 输入参数： 
 * 			UserID:账户编号 
 * 			transSerialNo:充值流水号
 * 			code:流水号
 * 输出参数：
 */
public class OnceChargeVerifiedHandler extends JSONHandler {
	public static final String TRANS_CODE_CHARGE_DELAY = "1013";
	ArrayList<BizObject> recordBizList = null;
	ArrayList<BizObjectManager> managerList = null;

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return runChargeApply(request);
	}

	/**
	 * 发起充值
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject runChargeApply(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("transSerialNo") == null || "".equals(request.get("transSerialNo")))
			throw new HandlerException("param.transSerialNo.error");
		
		if (request.get("code") == null || "".equals(request.get("code")))
			throw new HandlerException("param.code.error");
		
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		boolean savaRecordFlag = false;
		try {
			tx = jbo.createTransaction();
			
			
			// 获取当前时间
			TimeTool tool = new TimeTool();

			String sTranType = null;
			String sStatus = null;
			//不做金额拆分
			/******开始********/
			BizObjectManager recordManager = jbo.getManager("jbo.trade.transaction_record", tx);
			BizObject recordBo = 
				recordManager.createQuery(" serialno=:serialno and userid=:userid and status=:status")
				.setParameter("serialno",String.valueOf(request.get("transSerialNo")))
				.setParameter("userid",String.valueOf(request.get("UserID")))
				.setParameter("status","01").getSingleResult(true)
			;
			
			if(recordBo==null)
				throw new HandlerException("chargeapply.error");
			
			//开始发送绑卡推进
			JSONObject obj = new JSONObject();
			for (int j = 0; j < recordBo.getAttributeNumber(); j++) {
				obj.put(recordBo.getAttribute(j).getName()
						.toUpperCase(), recordBo.getAttribute(j)
						.getValue());
			}
			
			obj.put("VALIDCODE", request.get("code"));
			
			String channel = String.valueOf(obj.get("TRANSCHANNEL"));
			String transCode = "1016";
			RealTimeTradeTranscation rttt = TranscationFactory
					.getRealTimeTradeTranscation(channel, transCode);
			rttt.init(obj);
			rttt.execute();
			//System.out.println(rttt.getReponseMessage().getField("response_message").getStringValue());
			if("ADVANCE_FAILED".equalsIgnoreCase(rttt.getReponseMessage().getField("response_code").getStringValue())){
				throw new HandlerException("sina.ADVANCE_FAILED");
			}
			String sLogId = rttt.getLogId();
			String sTransUrl = null;
			
			HashMap<String,Double> balances = ObjectBalanceUtils.ObjectBalanceUtils(String.valueOf(request.get("UserID")), ObjectConstants.OBJECT_TYPE_001);
			double balance = 0.0;
			if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_001)) balance += balances.get(ObjectConstants.ACCOUNT_TYPE_001);
			if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_002)) balance += balances.get(ObjectConstants.ACCOUNT_TYPE_002);
			
			boolean success = false;
			double amount = Double.parseDouble(String.valueOf(obj.get("AMOUNT")));
			if (rttt.getTemplet().isSuccess()) {
				recordBo.setAttributeValue("updatetime", StringFunction.getToday("/")+" "+StringFunction.getNow());
				recordBo.setAttributeValue("ACTUALAMOUNT", Double.parseDouble(String.valueOf(obj.get("AMOUNT"))));
				recordBo.setAttributeValue("STATUS", "10");
				recordBo.setAttributeValue("BALANCE", balance);
				
				result.put("amount",amount);
				success = true;
			}
			else
			{
				recordBo.setAttributeValue("updatetime", StringFunction.getToday("/")+" "+StringFunction.getNow());
				recordBo.setAttributeValue("ACTUALAMOUNT",0.0);
				recordBo.setAttributeValue("STATUS", "04");
				recordBo.setAttributeValue("BALANCE", balance);
				
				result.put("amount",0.0);
				success = false;
			}
			result.put("isSUCCESS",success);
			
			recordManager.saveObject(recordBo);
			//发送短信
			BizObjectManager userManager = jbo.getManager("jbo.trade.user_account",tx);
			BizObjectQuery query = userManager.createQuery("UserID=:UserID");
			query.setParameter("UserID",String.valueOf(request.get("UserID")));
			BizObject user = query.getSingleResult(false);
			String mobile = user.getAttribute("PHONETEL").toString();
			
			if(mobile != null && !"".equals(mobile)){
				// 发送短信提醒
				HashMap<String, Object> parameters = new HashMap<String, Object>();
				parameters.put("Amount", DataConvert.toMoney(amount));
				parameters.put("Date", tool.getsChDate());
				
				boolean sSendResult = GeneralTools.sendSMS(success?"P2P_CZCG":"P2P_CZSB",mobile, parameters);
				if (sSendResult) {
					result.put("SendSMSFlag", true);
				} else {
					result.put("SendSMSFlag", false);
				}	
			}
			tx.commit();
			/*******结束*******/
		} catch (HandlerException e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			if (!savaRecordFlag) {
				throw e;
			}
		} catch (Exception e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			if (!savaRecordFlag) {
				throw new HandlerException("chargeapply.error");
			}
		}
		return result;
	}
}
