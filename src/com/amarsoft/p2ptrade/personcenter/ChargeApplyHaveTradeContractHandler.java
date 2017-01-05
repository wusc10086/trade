package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.DataConvert;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.common.pdf.Html2PdfHandler;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.CodeManager;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.p2ptrade.util.RunTradeService;

/**
 * 发起充值交易 
 * 输入参数： 
 * 			UserID:账户编号 
 * 			AccountNo:银行卡号 
 * 			AccountName:账户名 
 * 			AccountBelong:开户行
 * 			Amount:充值金额 
 * 输出参数： ChargeFlag：充值是否成功 (true-->成功 false-->失败)
 * 			Amount：充值金额
 * 
 */
public class ChargeApplyHaveTradeContractHandler extends JSONHandler {
	ArrayList<BizObject> recordBizList = null;
	ArrayList<BizObjectManager> managerList = null;
	
	JSONObject contractInfo = null;

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
		if (request.get("AccountNo") == null
				|| "".equals(request.get("AccountNo")))
			throw new HandlerException("accountno.error");
		if (request.get("AccountName") == null
				|| "".equals(request.get("AccountName")))
			throw new HandlerException("accountname.error");
		if (request.get("AccountBelong") == null
				|| "".equals(request.get("AccountBelong")))
			throw new HandlerException("accountbelong.error");
		if (request.get("Amount") == null || "".equals(request.get("Amount")))
			throw new HandlerException("chargeapply.amount.error");
		double amount = 0d;
		try {
			amount = Double.parseDouble(request.get("Amount").toString());
		} catch (NumberFormatException e2) {
			throw new HandlerException("chargeapply.amount.error");
		}
		if (amount <= 0) {
			throw new HandlerException("chargeapply.amount.error");
		}

		String sUserID = request.get("UserID").toString();
		String sAccountNo = request.get("AccountNo").toString();
		String sAccountName = request.get("AccountName").toString();
		String sAccountBelong = request.get("AccountBelong").toString();

		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		boolean savaRecordFlag = false;
		try {
			tx = jbo.createTransaction();

			// 获取当前时间
			TimeTool tool = new TimeTool();

			// 当日限额校验
			double chargeAllAmount = getChargeCountByDate(jbo, sUserID,
					tool.getsCurrentDate());
			if (chargeAllAmount + amount >= 1000000) {
				throw new HandlerException("charge.limitallamount.error");
			}

			// 获取用户账户信息
			JSONObject infoObj = getAccountInfo(jbo, sUserID, sAccountNo,
					sAccountBelong, sAccountName);

			String sTranType = null;
			String sStatus = null;
			String sCurrentTime = tool.getsCurrentTime();// 获取当前时间，格式HH:mm:ss
			JSONObject timeArray = GeneralTools.getChargeDividTime(jbo, "0010");
			String startTime = timeArray.get("StartTime").toString();
			String endTime = timeArray.get("EndTime").toString();
			
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			Date d1 = sdf.parse(sCurrentTime);
			Date d2 = sdf.parse(startTime);
			Date d3 = sdf.parse(endTime);
			long diff1 = d1.getTime() - d2.getTime();
			long diff2 = d1.getTime() - d3.getTime();
			if (!(diff1 < 0 && diff2 > 0)) {// 23：00 ~ 01:00
				sTranType = "1011";// 虚拟账户充值（延时）
				sStatus = "01";// 申请已提交，待处理
			} else {// 未超过23：00
				sTranType = "1010";// 虚拟账户充值（实时）
				sStatus = "00";// 申请待提交
			}

			// 拆分充值金额
			double limitAmount = GeneralTools.getLimitAmount(jbo,
					sAccountBelong, "ATTRIBUTE7");// 获取渠道限额

			recordBizList = new ArrayList<BizObject>();
			managerList = new ArrayList<BizObjectManager>();
			
			contractInfo = new JSONObject();
			contractInfo.put("AccountBelong", infoObj.get("ACCOUNTBELONGNAME").toString());
			contractInfo.put("AccountNo", sAccountNo);
			contractInfo.put("AccountName", sAccountName);
			getAccountDetailInfo(jbo, sUserID);
			getUserName(jbo, sUserID);
			
			savaRecordFlag = getAmountList(jbo, tx, sUserID,
					sAccountBelong, infoObj, sTranType, sStatus, amount,
					limitAmount);
			tx.commit();

			for (int i = 0; i < recordBizList.size(); i++) {
				if (diff1 < 0 && diff2 > 0) {// 01:00 ~ 23：00 ，实时接口充值
					BizObjectManager recordManager = managerList.get(i);
					BizObject recordBo = recordBizList.get(i);
					
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
						accountBo.setAttributeValue("LOCKFLAG", "1");
						accountManager.saveObject(accountBo);

						usableBalance = Double.parseDouble(accountBo
								.getAttribute("USABLEBALANCE").toString());
						frozenBalance = Double.parseDouble(accountBo
								.getAttribute("FROZENBALANCE").toString());

						userBalance = usableBalance + frozenBalance;// 账户余额
						reUsableBalance = usableBalance + transAmount
								- handlCharge;// 充值成功后账户可用余额
						reChargeAmount = userBalance + transAmount
								- handlCharge;// 充值成功后的用户账户余额
						
					} else {
						throw new HandlerException("quaryaccountamount.nodata.error");
					}

					String sTempletID = null;
					HashMap<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("Amount", DataConvert.toMoney(transAmount));

					// 调用P2P_Trade服务
					String sMethod = "runrealtimetranshavec";
					String sRequestFormat = "json";

					if(recordBo != null){
						recordBo.setAttributeValue("TRANSDATE",StringFunction.getToday());
						recordBo.setAttributeValue("TRANSTIME",StringFunction.getNow());
					}
					
					JSONObject obj = new JSONObject();
					obj.put("ContractInfo", contractInfo);
					for (int j = 0; j < recordBo.getAttributeNumber(); j++) {
						obj.put(recordBo.getAttribute(j).getName()
								.toUpperCase(), recordBo.getAttribute(j)
								.getValue());
					}
					String sRequestStr = GeneralTools.createJsonString(obj);
					JSONObject responsePram = RunTradeService.runTranProcess(
							sMethod, sRequestFormat, sRequestStr);
					String sTransFlag = responsePram.get("TransFlag") == null ? "":responsePram.get("TransFlag").toString();
					String sLogId = responsePram.get("LogId") == null ? "":responsePram.get("LogId").toString();
					String sContractSerialNo = responsePram.get("ContractSerialNo") == null ? "":responsePram.get("ContractSerialNo").toString();
					
					BizObjectManager contractManager = null;
					BizObject contractBo = null;
					if(!sContractSerialNo.equals("")){
						contractManager = jbo.getManager("jbo.trade.contract_s_record",tx);
						contractBo = getContractSignRecord(contractManager,sContractSerialNo,sUserID);
					}
					
					if ("SUCCESS".equals(sTransFlag)) {
						if(responsePram.containsKey("PendingFlag")){//等待通联返回结果
							recordBo.setAttributeValue("STATUS", "03");// 待返回结果
							recordBo.setAttributeValue("TRANSLOGID", sLogId);// 交易日志编号
							recordBo.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());// 添加更新时间
						}else{//充值成功
							// 更改交易记录
							recordBo.setAttributeValue("BALANCE", reChargeAmount);// 余额
							recordBo.setAttributeValue("ACTUALAMOUNT", transAmount - handlCharge);// 实际到帐金额
							recordBo.setAttributeValue("STATUS", "10");// 状态（成功）
							recordBo.setAttributeValue("TRANSLOGID", sLogId);// 交易日志编号
							recordBo.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());// 添加更新时间

							if(contractBo != null){
								contractBo.setAttributeValue("STATUS", "3");//已生效，待盖章
							}
							
							
							// 更改账户可用余额
							accountBo.setAttributeValue("USABLEBALANCE",
									reUsableBalance);
							sTempletID = "P2P_CZCG";
						}
					} else {// 失败
						ARE.getLog().info(sTransFlag);

						// 更改交易记录
						recordBo.setAttributeValue("BALANCE", userBalance);// 余额
						recordBo.setAttributeValue("STATUS", "04");// 状态（失败）
						recordBo.setAttributeValue("TRANSLOGID", sLogId);// 交易日志编号
						recordBo.setAttributeValue("UPDATETIME",
								new TimeTool().getsCurrentMoment());// 添加更新时间

						if(contractBo != null){
							contractBo.setAttributeValue("STATUS", "4");//合同失效
						}
						
						
						sTempletID = "P2P_CZSB";
						parameters.put("Date", tool.getsChDate());
					}
					recordManager.saveObject(recordBo);// 更新记录
					if(contractManager != null && contractBo != null){
						contractManager.saveObject(contractBo);
					}
					
					accountBo.setAttributeValue("LOCKFLAG", "2");
					accountManager.saveObject(accountBo);
					String sPhoneTel = getPhoneTel(accountBo);
//					if(sTempletID != null && sPhoneTel != null && !"".equals(sPhoneTel)){
//						// 发送短信提醒
//						boolean sSendResult = GeneralTools.sendSMS(sTempletID,
//								sPhoneTel, parameters);
//					}
				} 
				tx.commit();
			}
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
		result.put("ChargeFlag", savaRecordFlag?true:false);
		result.put("Amount", DataConvert.toMoney(amount));
		return result;
	}

	/**
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @param sAccountNo
	 *            账户号
	 * @param sAccountBelong
	 *            开户银行
	 * @param sAccountName
	 *            账户名
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject getAccountInfo(JBOFactory jbo, String sUserID,
			String sAccountNo, String sAccountBelong, String sAccountName)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = m
					.createQuery("userid=:userid and accountno=:accountno "
							+ "and accountbelong=:accountbelong  and accountname=:accountname and status='2'");
			query.setParameter("userid", sUserID)
					.setParameter("accountno", sAccountNo)
					.setParameter("accountbelong", sAccountBelong)
					.setParameter("accountname", sAccountName);

			BizObject o = query.getSingleResult(false);
			
			if (o != null) {
				String sSerialNo = o.getAttribute("SERIALNO").toString();
				JSONObject items = GeneralTools.getItemName(jbo, "BankNo");
				String sAccountBelongName = items.containsKey(sAccountBelong) ? items
						.get(sAccountBelong).toString() : sAccountBelong;
				JSONObject obj = new JSONObject();
				obj.put("SERIALNO", sSerialNo);
				obj.put("ACCOUNTBELONGNAME", sAccountBelongName);
				return obj;
			} else {
				throw new HandlerException("queryaccountinfo.nodata.error");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			throw new HandlerException("queryaccountinfo.error");
		}
	}

	private void getAccountDetailInfo(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_detail");
			BizObjectQuery query = m
					.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sRealName = o.getAttribute("REALNAME").getString();
				String sCertType = CodeManager.getCodeManager().getItemName("CertType", o.getAttribute("certtype").getString());
				String sCertID = o.getAttribute("CERTID").getString();
				
				contractInfo.put("realname", sRealName);
				contractInfo.put("certid", sCertID);
				contractInfo.put("certtype", sCertType);
			} else {
				throw new HandlerException("queryaccountinfo.nodata.error");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			throw new HandlerException("queryaccountinfo.error");
		}
	}
	
	private void getUserName(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m
					.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sUserName = o.getAttribute("USERNAME").getString();
				contractInfo.put("username", sUserName);
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
	 * 
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
			JSONObject infoObj, String sTranType, String sStatus)
			throws HandlerException {
		try {
			BizObject recordBo = recordManager.newObject();
			recordBo.setAttributeValue("USERID", sUserID);// 用户编号
			recordBo.setAttributeValue("DIRECTION", "R");// 发生方向(收入)
			recordBo.setAttributeValue("AMOUNT", amount);// 交易金额

			double handlCharge = GeneralTools.getCalTransFee(jbo, "0020",
					amount);// 计算手续费

			recordBo.setAttributeValue("HANDLCHARGE", handlCharge);// 手续费
			recordBo.setAttributeValue("TRANSTYPE", sTranType);// 交易类型（充值,延迟充值）
			recordBo.setAttributeValue("INPUTTIME",
					new TimeTool().getsCurrentMoment());// 创建时间
			recordBo.setAttributeValue("STATUS", sStatus);// 交易状态
			recordBo.setAttributeValue("RELAACCOUNT", infoObj.get("SERIALNO")
					.toString());// 关联账户流水号
			recordBo.setAttributeValue("RELAACCOUNTTYPE", "001");// 交易关联账户类型

			String sTransChannel = GeneralTools.getTransChannel(jbo,
					sAccountBelong, "ATTRIBUTE1");


			recordBo.setAttributeValue("TRANSCHANNEL", sTransChannel);// 交易渠道
			recordBo.setAttributeValue("REMARK",
					"充值" + "|" + infoObj.get("ACCOUNTBELONGNAME").toString());// 备注
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
			String sAccountBelong, JSONObject infoObj,
			String sTranType, String sStatus, double amount,
			double limitAmount) throws HandlerException, JBOException {
		// 如果限额等于0 ，则默认为无上限
		if (amount <= limitAmount || limitAmount == 0) {
			BizObjectManager recordManager = jbo.getManager(
					"jbo.trade.transaction_record", tx);
			BizObject recordBo = saveTransRecord(jbo, recordManager,
					sUserID, amount, sAccountBelong, infoObj,
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
						infoObj, sTranType, sStatus);
				managerList.add(recordManager);
				recordBizList.add(recordBo);
				

			}

			if (dTemp > 0) {
				BizObjectManager recordManager = jbo.getManager(
						"jbo.trade.transaction_record", tx);
				BizObject recordBo = saveTransRecord(jbo, recordManager,
						sUserID, dTemp, sAccountBelong, infoObj,
						sTranType, sStatus);
				managerList.add(recordManager);
				recordBizList.add(recordBo);

			}
		}
		return true;
	}
	
	private BizObject getContractSignRecord(BizObjectManager contractManager, String sContractSerialNo, String sUserID)
			throws HandlerException {
		try {
			BizObjectQuery query = contractManager.createQuery("serialno=:serialno and contracttype=:contracttype and signuserid=:signuserid");
			query.setParameter("serialno", sContractSerialNo).setParameter("contracttype", "001").setParameter("signuserid", sUserID);

			BizObject o = query.getSingleResult(true);
			if (o != null) {
				return o;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new HandlerException("queryaccountinfo.error");
		}
	}
}
