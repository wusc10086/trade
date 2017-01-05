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
 * 发起充值交易 
 * 输入参数： 
 * 			UserID:账户编号 
 * 			SerialNo:流水号
 * 			Amount:充值金额 
 * 输出参数： ChargeFlag：充值是否成功 (true-->成功 false-->失败)
 * 			Amount：充值金额
 * 
 */
public class ChargeApplyHandler extends JSONHandler {
	public static final String TRANS_CODE_CHARGE_DELAY = "1012";
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
		if (request.get("SerialNo") == null || "".equals(request.get("SerialNo")))
			throw new HandlerException("param.emptyserialno.error");
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
		String sSerialNo = request.get("SerialNo").toString();
		
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		boolean savaRecordFlag = false;
		try {
			tx = jbo.createTransaction();

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

			// 获取用户充值银行卡信息
			String sAccountBelong = sSerialNo;//getAccountBelong(jbo, sSerialNo);

			String sTranType = null;
			String sStatus = null;
//			String sCurrentTime = tool.getsCurrentTime();// 获取当前时间，格式HH:mm:ss
//			JSONObject timeArray = GeneralTools.getChargeDividTime(jbo, "0010");
//			String startTime = timeArray.get("StartTime").toString();
//			String endTime = timeArray.get("EndTime").toString();
//			
//			
//			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//			Date d1 = sdf.parse(sCurrentTime);
//			Date d2 = sdf.parse(startTime);
//			Date d3 = sdf.parse(endTime);
//			long diff1 = d1.getTime() - d2.getTime();
//			long diff2 = d1.getTime() - d3.getTime();
//			if (!(diff1 < 0 && diff2 > 0)) {// 23：55 ~ 01:05
//				throw new HandlerException("out.bankchargetime.error");
//			}

			// 拆分充值金额
/*********开始************/
//			double limitAmount = GeneralTools.getLimitAmount(jbo,
//					sAccountBelong, "ATTRIBUTE7");// 获取渠道限额
//			recordBizList = new ArrayList<BizObject>();
//			managerList = new ArrayList<BizObjectManager>();
//			
//			savaRecordFlag = getAmountList(jbo, tx, sUserID,
//					sAccountBelong, sSerialNo, sTranType, sStatus, amount,
//					limitAmount);
//			tx.commit();
//
//			for (int i = 0; i < recordBizList.size(); i++) {
//				if (diff1 < 0 && diff2 > 0) {// 01:00 ~ 23：00 ，实时接口充值
//					BizObjectManager recordManager = managerList.get(i);
//					BizObject recordBo = recordBizList.get(i);
//					String transSerialNo = recordBo.getAttribute("SERIALNO").toString();
//					double transAmount = recordBo.getAttribute("AMOUNT").getDouble();
//					double handlCharge = recordBo.getAttribute("HANDLCHARGE").getDouble();
//					double usableBalance = 0;// 账户可用余额
//					double frozenBalance = 0;// 账户冻结金额
//					double userBalance = 0;// 账户余额
//					double reChargeAmount = 0;// 充值成功后的用户账户余额
//					double reUsableBalance = 0;// 充值成功后的账户可用余额
//					
//					// 账户信息处理对象
//					BizObjectManager accountManager = jbo.getManager(
//							"jbo.trade.user_account", tx);
//					BizObject accountBo = accountManager
//							.createQuery("userid=:userid")
//							.setParameter("userid", sUserID)
//							.getSingleResult(true);
//					// 获取用户余额信息
//					if (accountBo != null) {
//						accountBo.setAttributeValue("LOCKFLAG", "1");
//						accountManager.saveObject(accountBo);
//
//						usableBalance = Double.parseDouble(accountBo
//								.getAttribute("USABLEBALANCE").toString());
//						frozenBalance = Double.parseDouble(accountBo
//								.getAttribute("FROZENBALANCE").toString());
//
//						userBalance = usableBalance + frozenBalance;// 账户余额
//						reUsableBalance = usableBalance + transAmount
//								- handlCharge;// 充值成功后账户可用余额
//						reChargeAmount = userBalance + transAmount
//								- handlCharge;// 充值成功后的用户账户余额
//					} else {
//						throw new HandlerException("quaryaccountamount.nodata.error");
//					}
//					JSONObject obj = new JSONObject();
//					for (int j = 0; j < recordBo.getAttributeNumber(); j++) {
//						obj.put(recordBo.getAttribute(j).getName()
//								.toUpperCase(), recordBo.getAttribute(j)
//								.getValue());
//					}
//					String channel = String.valueOf(obj.get("TRANSCHANNEL"));
//					String transCode = String.valueOf(obj.get("TRANSTYPE"));
//					RealTimeTradeTranscation rttt = TranscationFactory
//							.getRealTimeTradeTranscation(channel, transCode);
//					rttt.init(obj);
//					rttt.execute();
//					String sLogId = rttt.getLogId();
//					String sTransUrl = null;
//					if (rttt.getTemplet().isSuccess()) {//成功
//						if(rttt.getReponseMessage().getSingleMessage("INFO")!=null){
//							com.amarsoft.message.Message infoMessage = rttt.getReponseMessage().getSingleMessage("INFO");
//							if(infoMessage.getField("URL")!=null)
//								sTransUrl = infoMessage.getField("URL").getStringValue();
//						}
//						recordBo.setAttributeValue("STATUS", "01");// 已提交，待处理
//						recordBo.setAttributeValue("TRANSLOGID", sLogId);// 
//						recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
//						recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
//						recordManager.saveObject(recordBo);
//						result.put("TransURL", sTransUrl);
//						result.put("ChargeFlag", true);
//						result.put("TransSerialNo", transSerialNo);
//						result.put("LogId", sLogId);
//						result.put("isSUCCESS", true);
//					} else {// 失败
//						recordBo.setAttributeValue("STATUS", "04");// 已失效
//						recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
//						recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
//						recordManager.saveObject(recordBo);
//						result.put("ChargeFlag", false);
//						result.put("isSUCCESS", false);
//					}
//				} 
//				tx.commit();
//			}
/*********结束************/
			//不做金额拆分
			/******开始********/
			BizObjectManager recordManager = jbo.getManager("jbo.trade.transaction_record", tx);
			BizObject recordBo = saveTransRecord(jbo, recordManager,sUserID, amount,
					sAccountBelong, sSerialNo,sTranType, sStatus);
			String transSerialNo = recordBo.getAttribute("SERIALNO").toString();
			double transAmount = recordBo.getAttribute("AMOUNT").getDouble();
			double handlCharge = recordBo.getAttribute("HANDLCHARGE").getDouble();
			double usableBalance = 0;// 账户可用余额
			double frozenBalance = 0;// 账户冻结金额
			double userBalance = 0;// 账户余额
			double reChargeAmount = 0;// 充值成功后的用户账户余额
			double reUsableBalance = 0;// 充值成功后的账户可用余额
			
			// 账户信息处理对象
			BizObjectManager accountManager = jbo.getManager(
					"jbo.trade.user_account", tx);
			BizObject accountBo = accountManager
					.createQuery("userid=:userid")
					.setParameter("userid", sUserID)
					.getSingleResult(true);
			// 获取用户余额信息
			if (accountBo != null) {
//				accountBo.setAttributeValue("LOCKFLAG", "1");
//				accountManager.saveObject(accountBo);

				/*usableBalance = Double.parseDouble(accountBo
						.getAttribute("USABLEBALANCE").toString());
				frozenBalance = Double.parseDouble(accountBo
						.getAttribute("FROZENBALANCE").toString());
				*/
				//获取客户名下所有的余额 modify by xjqin 20150120
				HashMap<String,Double> balances = ObjectBalanceUtils.ObjectBalanceUtils(sUserID, ObjectConstants.OBJECT_TYPE_001);
				if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_001))
					usableBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_001); //查询可用余额
				
				if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_002))
					frozenBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_002); //查询冻结余额
				
				//modify end
				
				
				userBalance = usableBalance + frozenBalance;// 账户余额
				reUsableBalance = usableBalance + transAmount
						- handlCharge;// 充值成功后账户可用余额
				reChargeAmount = userBalance + transAmount
						- handlCharge;// 充值成功后的用户账户余额
			} else {
				throw new HandlerException("quaryaccountamount.nodata.error");
			}
			JSONObject obj = new JSONObject();
			for (int j = 0; j < recordBo.getAttributeNumber(); j++) {
				obj.put(recordBo.getAttribute(j).getName()
						.toUpperCase(), recordBo.getAttribute(j)
						.getValue());
			}
			/**赋值银行号*/
			obj.remove("RELAACCOUNT");
			obj.put("BANKNO", sSerialNo);
			String channel = String.valueOf(obj.get("TRANSCHANNEL"));
			String transCode = String.valueOf(obj.get("TRANSTYPE"));
			RealTimeTradeTranscation rttt = TranscationFactory
					.getRealTimeTradeTranscation(channel, transCode);
			rttt.init(obj);
			rttt.execute();
			String sLogId = rttt.getLogId();
			String sTransUrl = null;
			if (rttt.getTemplet().isSuccess()) {//成功
				sTransUrl = (String)InterfaceHelper.getFieldValue(rttt.getReponseMessage(), "URL","");
//				if(rttt.getReponseMessage().getSingleMessage("INFO")!=null){
//					com.amarsoft.message.Message infoMessage = rttt.getReponseMessage().getSingleMessage("INFO");
//					if(infoMessage.getField("URL")!=null)
//						sTransUrl = infoMessage.getField("URL").getStringValue();
//				}
				recordBo.setAttributeValue("STATUS", "01");// 已提交，待处理
				recordBo.setAttributeValue("TRANSLOGID", sLogId);// 
				recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
				recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
				recordManager.saveObject(recordBo);
				result.put("TransURL", sTransUrl);
				result.put("ChargeFlag", true);
				result.put("TransSerialNo", transSerialNo);
				result.put("LogId", sLogId);
				result.put("isSUCCESS", true);
			} else {// 失败
				recordBo.setAttributeValue("STATUS", "04");// 已失效
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
	private String getAccountBelong(JBOFactory jbo, String sSerialNo)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = m
					.createQuery("serialno = :serialno");
			query.setParameter("serialno", sSerialNo);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sAccountBelong = o.getAttribute("ACCOUNTBELONG").toString();
				return sAccountBelong;
			} else {
				throw new HandlerException("queryaccountinfo.nodata.error");
			}
		} catch (HandlerException e) {
			throw e;
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

			String sTransChannel = GeneralTools.getTransChannel(jbo,
					sAccountBelong, "ATTRIBUTE1");


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
