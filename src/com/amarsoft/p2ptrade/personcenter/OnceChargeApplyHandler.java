package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import com.amarsoft.account.common.ObjectBalanceUtils;
import com.amarsoft.account.common.ObjectConstants;
import com.amarsoft.are.ARE;
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
import com.amarsoft.p2ptrade.util.RunTradeService;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 快捷支付处理
 * 输入参数： 
 * 			UserID:账户编号 
 * 			Amount:充值金额 
 * 输出参数： ChargeFlag：充值是否成功 (true-->成功 false-->失败)
 * 			Amount：充值金额
 * 
 */
public class OnceChargeApplyHandler extends JSONHandler {
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
		if (request.get("Amount") == null || "".equals(request.get("Amount")))
			throw new HandlerException("param.emptyamount.error");
		double amount = 0d;
		try {
			amount = Double.parseDouble(request.get("Amount").toString());
		} catch (NumberFormatException e2) {
			throw new HandlerException("param.formatamount.error");
		}
		if (amount <= 0) {
			throw new HandlerException("param.limitamount.error");
		}

		String sUserID = request.get("UserID").toString();
		
		
		
		
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		boolean savaRecordFlag = false;
		try {
			tx = jbo.createTransaction();
			
			BizObject accountBiz = this.getAccountInfo(jbo, sUserID);
			//获取账户信息
			String sSerialNo = accountBiz.getAttribute("SERIALNO").toString();
			String sAccountBelong = accountBiz.getAttribute("ACCOUNTBELONG").toString();
			
			// 获取当前时间
			TimeTool tool = new TimeTool();

//			// 当日限额校验
//			double chargeAllAmount = getChargeCountByDate(jbo, sUserID,
//					tool.getsCurrentDate());
//			if (chargeAllAmount + amount >= 1000000) {
//				result.put("isSUCCESS", false);
//				result.put("ChargeFlag",false);
//				result.put("ChargeResultFlag", "limitallamount.notenough");
//				//throw new HandlerException("charge.limitallamount.error");
//				return result;
//			}


			String sTranType = null;
			String sStatus = null;
			//不做金额拆分
			/******开始********/
			BizObjectManager recordManager = jbo.getManager("jbo.trade.transaction_record", tx);
			BizObject recordBo = saveTransRecord(jbo, recordManager,sUserID, amount,
					sAccountBelong, sSerialNo,sTranType, sStatus);
			String transSerialNo = recordBo.getAttribute("SERIALNO").toString();
			
			JSONObject obj = new JSONObject();
			for (int j = 0; j < recordBo.getAttributeNumber(); j++) {
				obj.put(recordBo.getAttribute(j).getName()
						.toUpperCase(), recordBo.getAttribute(j)
						.getValue());
			}
			/**赋值银行号*/
			String channel = String.valueOf(obj.get("TRANSCHANNEL"));
			String transCode = String.valueOf(obj.get("TRANSTYPE"));
			RealTimeTradeTranscation rttt = TranscationFactory
					.getRealTimeTradeTranscation(channel, transCode);
			rttt.init(obj);
			rttt.execute();
			String sLogId = rttt.getLogId();
			String sTransUrl = null;
			if (rttt.getTemplet().isSuccess()) {//成功
				String status = "10";
				String is_verified = "false" ; //是否需要推进，默认为否
				//返回ticket,需要推进
				String ticket = (String)InterfaceHelper.getFieldValue(rttt.getReponseMessage(), "ticket",""); 
				if(InterfaceHelper.isNotNull(ticket))
				{
					is_verified = "true";
					//TODO 
					status = "01"; //推进的状态待定，应该单独给一个推进的状态，且批量中设置相应的15分钟失效的机制
				}
				result.put("is_verified", is_verified);
				
				recordBo.setAttributeValue("STATUS", "01");// 已提交，待处理
				recordBo.setAttributeValue("TRANSLOGID", sLogId);// 
				recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
				recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
				recordManager.saveObject(recordBo);
				result.put("ChargeFlag", true);
				result.put("TransSerialNo", transSerialNo);
				result.put("LogId", sLogId);
				result.put("isSUCCESS", true);
			} else {// 失败
				//如果正在处理，则需要后续查询，设置标志为03
				if(rttt.isProcessed())
				{
					recordBo.setAttributeValue("STATUS", "03");// 正在处理中
				}
				else
				{
					recordBo.setAttributeValue("STATUS", "04");// 已失效
				}
				recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
				recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
				recordManager.saveObject(recordBo);
				result.put("isSUCCESS", false);
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

	
	/**
	 * 查询账户信息
	 * @param jbo
	 *            JBOFactory
	 * @param sSerialNo
	 *             流水号
	 * @return
	 * @throws HandlerException
	 */
	private BizObject getAccountInfo(JBOFactory jbo, String sUserId)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = m
					.createQuery(" userId=:userId and status=:status");
			query.setParameter("userId", sUserId);
			query.setParameter("status", "2");
			BizObject o = query.getSingleResult(false);
			if(o==null) throw new HandlerException("queryaccountinfo.error");
			return o;
		} catch (Exception e) {
			throw new HandlerException("queryaccountinfo.error");
		}
	}

	/**
	 * 保存交易记录
	 * @param recordManager
	 *            BizObjectManager
	 * @param recordBo
	 *            BizObject
	 * @param sUserID
	 *            用户编号
	 * @param mount
	 *            充值金额
	 * @param infoObj
	 *            账户信息集合
	 * @param sTranType
	 *            交易类型
	 * @param sStatus
	 *            交易状态
	 * @param tool
	 *            时间工具类
	 * @return
	 * @throws HandlerException
	 */
	private BizObject saveTransRecord(JBOFactory jbo,
			BizObjectManager recordManager, String sUserID,
			double amount, String sAccountBelong,
			String sSerialNo, String sTranType, String sStatus)
			throws HandlerException {
		try {
			BizObjectQuery q = recordManager.createQuery("RELAACCOUNT=:RELAACCOUNT").setParameter("RELAACCOUNT", sSerialNo);
			BizObject recordBo = q.getSingleResult(true);
			
			recordBo = recordManager.newObject();
			recordBo.setAttributeValue("USERID", sUserID);// 用户编号
			recordBo.setAttributeValue("DIRECTION", "R");// 发生方向(收入)
			recordBo.setAttributeValue("AMOUNT", amount);// 交易金额

			double handlCharge = GeneralTools.getCalTransFee(jbo, "0020",
					amount);// 计算手续费

			recordBo.setAttributeValue("HANDLCHARGE", handlCharge);// 手续费
			
			recordBo.setAttributeValue("TRANSTYPE", TRANS_CODE_CHARGE_DELAY);// 交易类型（充值,延迟充值）
			recordBo.setAttributeValue("INPUTTIME",
					new TimeTool().getsCurrentMoment());// 创建时间
			recordBo.setAttributeValue("STATUS", "00");// 交易状态
			recordBo.setAttributeValue("RELAACCOUNT", sSerialNo);// 关联账户流水号
			recordBo.setAttributeValue("RELAACCOUNTTYPE", "001");// 交易关联账户类型

			//TODO 要找个地方好好配置
			String sTransChannel = "3010";
//			String sTransChannel = GeneralTools.getTransChannel(jbo,
//					sAccountBelong, "ATTRIBUTE1");


			recordBo.setAttributeValue("TRANSCHANNEL", sTransChannel);// 交易渠道
			recordBo.setAttributeValue("REMARK",
					"充值");// + "|" + infoObj.get("ACCOUNTBELONGNAME").toString());// 备注
			recordManager.saveObject(recordBo);

			return recordBo;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("savetransrecord.error");
		}
	}

	/**
	 * 获取手机号码
	 * 
	 * @param accountBo
	 * @throws HandlerException
	 */
	private String getPhoneTel(BizObject accountBo) throws HandlerException {
		try {
			return accountBo.getAttribute("PHONETEL").toString();
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}

	/**
	 * 获取当日已进行提现交易的次数
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @param transdate
	 *            当前日期
	 * @return 已进行充值交易的总金额
	 * @throws HandlerException
	 */
	private double getChargeCountByDate(JBOFactory jbo, String sUserID,
			String transdate) throws HandlerException {
		double amount = 0;
		try {
			BizObjectManager manager;
			manager = jbo.getManager("jbo.trade.transaction_record");
			BizObjectQuery query = manager
					.createQuery("select sum(o.AMOUNT) as v.amount from o where userid=:userid and transdate=:transdate and transtype in (:transtype1,:transtype2,:transtype3) and status<>'04'");
			query.setParameter("userid", sUserID)
					.setParameter("transdate", transdate)
					.setParameter("transtype1", "1010")
					.setParameter("transtype2", "1011")
					.setParameter("transtype3", "1012");
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				amount = Double
						.parseDouble(o.getAttribute("amount").toString() == null ? "0.0"
								: o.getAttribute("amount").toString());
			} else {
				amount = 0;
			}
			return amount;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("getchargeallamount.error");
		}
	}

	/**
	 * 根据限额去拆分，如发生金额>限额,则依次往下拆分，拆分结果存放至ArrayList中返回
	 * 
	 * @return ArrayList<Double>
	 * @throws HandlerException
	 * @throws JBOException 
	 * */
	private final boolean getAmountList(JBOFactory jbo,JBOTransaction tx
			, String sUserID,
			String sAccountBelong, String sSerialNo,
			String sTranType, String sStatus, double amount,
			double limitAmount) throws HandlerException, JBOException {
		// 如果限额等于0 ，则默认为无上限
		if (amount <= limitAmount || limitAmount == 0) {
			BizObjectManager recordManager = jbo.getManager(
					"jbo.trade.transaction_record", tx);
			BizObject recordBo = saveTransRecord(jbo, recordManager,
					sUserID, amount, sAccountBelong, sSerialNo,
					sTranType, sStatus);
			managerList.add(recordManager);
			recordBizList.add(recordBo);
		} else {
			double dTemp = amount;
			while (dTemp > limitAmount) {
				dTemp -= limitAmount;
				BizObjectManager recordManager = jbo.getManager(
						"jbo.trade.transaction_record", tx);
				BizObject recordBo = saveTransRecord(jbo, recordManager,
						sUserID, limitAmount, sAccountBelong,
						sSerialNo, sTranType, sStatus);
				managerList.add(recordManager);
				recordBizList.add(recordBo);
			}

			if (dTemp > 0) {
				BizObjectManager recordManager = jbo.getManager(
						"jbo.trade.transaction_record", tx);
				BizObject recordBo = saveTransRecord(jbo, recordManager,
						sUserID, dTemp, sAccountBelong, sSerialNo,
						sTranType, sStatus);
				managerList.add(recordManager);
				recordBizList.add(recordBo);
			}
		}
		return true;
	}
}
