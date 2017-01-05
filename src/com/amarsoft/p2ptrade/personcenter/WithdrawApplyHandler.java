package com.amarsoft.p2ptrade.personcenter;

import java.util.HashMap;
import java.util.Properties;

import com.amarsoft.account.AccountFactory;
import com.amarsoft.account.common.ObjectBalanceUtils;
import com.amarsoft.account.common.ObjectConstants;
import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2p.interfaces.utils.InterfaceConstant;
import com.amarsoft.p2p.interfaces.utils.InterfaceHelper;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 发起提现申请交易 
 * 输入参数： 
 * 			UserID:账户编号
 * 			SerialNo:流水号 
 * 			Amount:提现金额 
 * 输出参数： 
 * 			成功标识
 * 
 */
public class WithdrawApplyHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return withdrawApply(request);
	}

	/**
	 * 发起提现申请
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject withdrawApply(JSONObject request)
			throws HandlerException {
		JSONObject result = new JSONObject();
		//boolean WithDrawFlag = false;
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("SerialNo") == null || "".equals(request.get("SerialNo")))
			throw new HandlerException("param.emptyserialno.error");
//		if (request.get("AccountNo") == null
//				|| "".equals(request.get("AccountNo")))
//			throw new HandlerException("param.emptyaccountno.error");
//		if (request.get("AccountName") == null
//				|| "".equals(request.get("AccountName")))
//			throw new HandlerException("param.emptyaccountname.error");
//		if (request.get("AccountBelong") == null
//				|| "".equals(request.get("AccountBelong")))
//			throw new HandlerException("param.emptyaccountbelong.error");
		if (request.get("Amount") == null || "".equals(request.get("Amount")))
			throw new HandlerException("param.emptyamount.error");

		double withDrawAmount = 0d;
		try {
			withDrawAmount = Double.parseDouble(request.get("Amount")
					.toString());
		} catch (NumberFormatException e2) {
			throw new HandlerException("param.formartamount.error");
		}
		if (withDrawAmount <= 0) {
			throw new HandlerException("param.limitamount.error");
		}
		String sUserID = request.get("UserID").toString();// 用户编号
		String sSerialNo = request.get("SerialNo").toString();
//		String sAccountNo = request.get("AccountNo").toString();
//		String sAccountName = request.get("AccountName").toString();
//		String sAccountBelong = request.get("AccountBelong").toString();

		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		try {
			tx = jbo.createTransaction();

			// 获取当前时间
			TimeTool tool = new TimeTool();
			String sTransDate = tool.getsCurrentDate();
			String sInputTime = tool.getsCurrentMoment();
			
			// 获取用户账户银行卡信息
			String sAccountBelong = getAccountBelong(jbo, sSerialNo);
			
			double chargeOrPayCount = getChargeOrPayCount(jbo, sUserID);
			if(chargeOrPayCount == 0){
				// 冻结提现金额
				makeAccountFreeze(jbo, tx, sUserID);
				// 添加异常交易记录
				saveTransRecord(jbo, tx,sUserID, sSerialNo,withDrawAmount, sAccountBelong,sInputTime,"30");
				tx.commit();
				result.put("WithDrawFlag", "02");
				result.put("WithDrawResultFlag", "异常提现，未充值操作提现");
				//throw new HandlerException("useraccount.abnormal.error");
				return result;
			}

			//账户可用余额校验
			double usablebalance = getUserUseableBalance(jbo, sUserID);// 获取账户可用余额
			if (usablebalance < withDrawAmount) {// 可用余额小于提现金额
				result.put("WithDrawFlag", "02");
				result.put("WithDrawResultFlag", "金额不足");
				//throw new HandlerException("usablebalance.notenough");
				return result;
			}
			
			// 限额校验
			double limitAmount = getLimitCount(jbo, sSerialNo);
			if (limitAmount!=0.0&&withDrawAmount > limitAmount) {// 提现金额大于用户设置的提现金额
				result.put("WithDrawResultFlag", "提现金额超过限额");
				//throw new HandlerException("withdraw.limitamount.error");
				result.put("WithDrawFlag", "02");
				return result;
			}			

			//提现次数校验
			double withDrawCount = getWithDrawCountByDate(jbo, sUserID,
					sTransDate);
			int iMaxCount = ARE.getProperty("WithDrawCountByDate", 3);
			if (withDrawCount >= iMaxCount) {// 当日提现次数已达到s次
				result.put("WithDrawFlag", "02");
				result.put("WithDrawResultFlag", "当日提现次数已达到3次");
				//throw new HandlerException("withdraw.limitcount.error");
				return result;
			}
			
			// 冻结提现金额
			//modify by xjqin 20150126 新浪支付不再冻结用户余额
			//makeAmountFrozen(jbo, tx, sUserID, withDrawAmount);

			// 添加交易记录 以及发送提现申请记录
			BizObject recordBo = saveTransRecord(jbo, tx, sUserID, sSerialNo, withDrawAmount,sAccountBelong,
					sInputTime,"01");
			
			String status = recordBo.getAttribute("status").toString();
			if(InterfaceConstant.TRANS_STATUS_03.equals(status))
			{
				//TODO 提现申请已经提交 但未成功
			}
			else if(InterfaceConstant.TRANS_STATUS_10.equals(status))
			{
				//TODO 提现申请已经成功
			}
			else
			{
				//TODO 提现申请已经提交 但未失败
			}
			
			tx.commit();
			result.put("WithDrawFlag", status);
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
	 * 获取用户的可用余额
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @return 用户设置的限额
	 * @throws HandlerException
	 */
	private double getUserUseableBalance(JBOFactory jbo, String sUserID)
			throws Exception {
		try {
			double usablebalance = ObjectBalanceUtils.queryObjectBalance(sUserID, ObjectConstants.OBJECT_TYPE_001, ObjectConstants.ACCOUNT_TYPE_001);
			return usablebalance;
			/*BizObjectManager accManager = jbo
					.getManager("jbo.trade.user_account");
			BizObjectQuery query = accManager
					.createQuery("select USABLEBALANCE from o where userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject accBo = query.getSingleResult(false);
			double usablebalance = 0;
			if (accBo != null) {
				usablebalance = Double.parseDouble(accBo.getAttribute(
						"USABLEBALANCE").toString());
				return usablebalance;
			} else {
				throw new HandlerException("getuseablebalance.nodata.error");
			}*/
		} catch (Exception e) {
			throw new HandlerException("getuseablebalance.error");
		}
	}

	/**
	 * 获取账户提现银行卡信息
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sSerialNo
	 *            流水号
	 * @return 
	 * @throws HandlerException
	 */
	private String getAccountBelong(JBOFactory jbo, String sSerialNo) throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = m
					.createQuery("serialno=:serialno");
			query.setParameter("serialno", sSerialNo);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sAccountBelong = o.getAttribute("ACCOUNTBELONG").toString();
				return sAccountBelong;
			} else {
				throw new HandlerException("queryaccountinfo.nodata.error");
			}
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("queryaccountinfo.error");
		}
	}
	
	/**
	 * 获取当前用户已成功充值记录
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @param transdate
	 *            当前日期
	 * @return 已进行提现交易的次数
	 * @throws HandlerException
	 */
	private double getChargeOrPayCount(JBOFactory jbo,
			String sUserID) throws HandlerException {
		double count = 0;
		try {
			BizObjectManager manager;
			manager = jbo.getManager("jbo.trade.transaction_record");
			BizObjectQuery query = manager
					.createQuery("select count(*) as v.count from o where userid=:userid  and transtype in (:transtype1,:transtype2,:transtype3,:transtype4,:transtype5) and status='10'");
			query.setParameter("userid", sUserID)
					.setParameter("transtype1", "1010")
					.setParameter("transtype2", "1011")
					.setParameter("transtype3", "1012")
					.setParameter("transtype4", "1060")
				    .setParameter("transtype5", "1013");

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				count = Double.parseDouble(o.getAttribute("count").toString());
			} else {
				count = 0;
			}
			return count;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getchargecount.error");
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
	 * @return 已进行提现交易的次数
	 * @throws HandlerException
	 */
	private double getWithDrawCountByDate(JBOFactory jbo,
			String sUserID, String transdate) throws HandlerException {
		double count = 0;
		try {
			BizObjectManager manager;
			manager = jbo.getManager("jbo.trade.transaction_record");
			BizObjectQuery query = manager
					.createQuery("select count(*) as v.count from o where userid=:userid and inputtime like :inputtime and transtype =:transtype and status<>'04'");
			query.setParameter("userid", sUserID)
					.setParameter("inputtime", transdate+"%")
					.setParameter("transtype", "1020");
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				count = Double.parseDouble(o.getAttribute("count").toString());
			} else {
				count = 0;
			}
			return count;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getwithdrawcount.error");
		}
	}

	/**
	 * 获取用户设置的提现限额
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @return 用户设置的限额
	 * @throws HandlerException
	 */
	/*private double getLimitCount(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
			BizObjectManager accManager = jbo
					.getManager("jbo.trade.account_info");
			BizObjectQuery query = accManager
					.createQuery("select limitamount from o where userid=:userid and status='2'");
			query.setParameter("userid", sUserID);

			BizObject accBo = query.getSingleResult(false);
			double limitAmount = 0;
			if (accBo != null) {
				limitAmount = Double.parseDouble(accBo.getAttribute(
						"limitamount").toString());
				return limitAmount;
			} else {
				throw new HandlerException("getlimitcount.nodata.error");
			}
		} catch (JBOException e) {
			throw new HandlerException("getlimitcount.error");
		}
	}*/
	 private double getLimitCount(JBOFactory jbo, String serialNo) throws HandlerException {
	        try {
	            String table = "jbo.trade.code_library";
	            String sql = "select attribute4 AS v.limitamount from o where o.codeno='BankNo' and o.itemno=(select ai.ACCOUNTBELONG from jbo.trade.account_info ai where ai.serialno=:serialno and ai.status='2')";
	            BizObjectManager accManager = jbo.getManager(table);
	            BizObjectQuery query = accManager.createQuery(sql);
	            query.setParameter("serialno", serialNo);

	            BizObject accBo = query.getSingleResult(false);
	            double limitAmount = 0;
	            if (accBo != null) {
	                
	                String limitA=accBo.getAttribute("limitamount").toString();
	                if(limitA==null || "".equals(limitA) || "null".equals(limitA)){
	                    limitA="0";
	                }
	                limitAmount = Double.parseDouble(limitA);
	                return limitAmount;
	            } else {
	                throw new HandlerException("getlimitcount.nodata.error");
	            }
	        } catch (JBOException e) {
	            e.printStackTrace();
	            throw new HandlerException("getlimitcount.error");
	        }
	    }

	/**
	 * 保存交易记录
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param tx
	 *            JBOTransaction
	 * @param sUserID
	 *            用户编号
	 * @param withDrawAmount
	 *            充值金额
	 * @param infoObj
	 *            账户信息
	 * @param sInputTime
	 *            创建时间
	 * @throws HandlerException
	 */
	private BizObject saveTransRecord(JBOFactory jbo, JBOTransaction tx,
			String sUserID,String sSerialNo, double withDrawAmount, String sAccountBelong,
			String sInputTime,String sStatus) throws HandlerException {
		//BizObjectManager recManager;
		//BizObjectManager accManager;
		BizObject o = null;
		try {
			/*BizObjectManager accManager= jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = accManager
					.createQuery("select USABLEBALANCE,FROZENBALANCE from o where userid=:userid");
			query.setParameter("userid", sUserID);
			
			BizObject accBo = query.getSingleResult(true); */
			/*double usableBalance = 0;// 账户可用余额
			double frozenBalance = 0;// 账户冻结金额
			double userBalance = 0;// 账户余额
			double reChargeAmount = 0;// 充值成功后的用户账户余额
			double reUsableBalance = 0;// 充值成功后的账户可用余额
			if (accBo != null) {
				usableBalance = Double.parseDouble(accBo.getAttribute(
						"USABLEBALANCE").toString());
				frozenBalance = Double.parseDouble(accBo.getAttribute(
						"FROZENBALANCE").toString());
				userBalance = usableBalance + frozenBalance;// 账户余额
				
			} else {
				throw new HandlerException("getuseablebalance.nodata.error");
			}
			*/
			
			BizObjectManager recManager = jbo.getManager("jbo.trade.transaction_record", tx);
			o = recManager.newObject();
			
			o.setAttributeValue("USERID", sUserID);// 用户编号
			o.setAttributeValue("DIRECTION", "P");// 发生方向(支出)
			o.setAttributeValue("AMOUNT", withDrawAmount);// 交易金额
			double handlCharge = GeneralTools.getCalTransFee(jbo, "0010", withDrawAmount);//计算手续费
			o.setAttributeValue("HANDLCHARGE", handlCharge);// 手续费
			o.setAttributeValue("ACTUALAMOUNT", withDrawAmount-handlCharge);
			o.setAttributeValue("TRANSTYPE", "1020");// 交易类型（提现）
			o.setAttributeValue("INPUTTIME", sInputTime);// 创建时间
			o.setAttributeValue("STATUS", sStatus);// 交易状态
			o.setAttributeValue("RELAACCOUNT",sSerialNo);// 关联账户流水号
			o.setAttributeValue("RELAACCOUNTTYPE", "001");// 交易关联账户类型
			o.setAttributeValue("USERACCOUNTTYPE", ObjectConstants.ACCOUNT_TYPE_001); 
			String sTransChannel = GeneralTools.getTransChannel(jbo, sAccountBelong, "ATTRIBUTE6");
			o.setAttributeValue("TRANSCHANNEL", sTransChannel);// 交易渠道
			o.setAttributeValue("REMARK", "提现");// + "|" + infoObj.get("ACCOUNTBELONGNAME").toString());// 备注
			
			
			/*reUsableBalance = usableBalance - withDrawAmount
					- handlCharge;// 提现成功后账户可用余额
			reChargeAmount = userBalance - withDrawAmount
					- handlCharge;// 提现成功后的用户账户余额
			o.setAttributeValue("BALANCE", reUsableBalance);
			
			accBo.setAttributeValue("USABLEBALANCE",reUsableBalance);
			accManager.saveObject(accBo);
			*/
			
			
			//发送交易
			if(!"30".equals(sStatus))
			{
				JSONObject obj = new JSONObject();
				for (int j = 0; j < o.getAttributeNumber(); j++) {
					obj.put(o.getAttribute(j).getName()
							.toUpperCase(), o.getAttribute(j)
							.getValue());
				}
				
				String channelId = o.getAttribute("TRANSCHANNEL").toString();
				String transCode = o.getAttribute("TRANSTYPE").toString();
	
				RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation(channelId, transCode);
				
				rttt.init(obj);
				rttt.execute();
				String logId = rttt.getLogId();
				String transStatus = InterfaceConstant.TRANS_STATUS_04;//默认失败状态
				HashMap<String,Double> balances = ObjectBalanceUtils.ObjectBalanceUtils(sUserID, ObjectConstants.OBJECT_TYPE_001);
				double userBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_001);
				double frozenBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_002);
				double allbalances =  userBalance + frozenBalance;
				double actualAmt =  0;
				if(rttt.getTemplet().isSuccess())
				{
					String withdraw_status = (String)InterfaceHelper.getFieldValue(rttt.getReponseMessage(), "withdraw_status","PROCESS");
					
					if("SUCCESS".equals(withdraw_status))
					{
						transStatus = InterfaceConstant.TRANS_STATUS_10;
						actualAmt = withDrawAmount;
					}
					//待处理状态
					else if(rttt.isProcessed())
					{
						transStatus = InterfaceConstant.TRANS_STATUS_03;
					}
					//其他状态默认失败
				}
				
				o.setAttributeValue("transDate", StringFunction.getToday("/"));
				o.setAttributeValue("transTime", StringFunction.getNow());
				o.setAttributeValue("BALANCE", allbalances);
				o.setAttributeValue("status", transStatus);
				o.setAttributeValue("transLogId", rttt.getLogId());
				o.setAttributeValue("ACTUALAMOUNT", actualAmt);
			}
			
			
			recManager.saveObject(o);
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("savetransrecord.error");
		}
		return o;
		
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

			BizObject accountBo = account.createQuery("userid=:userid")
					.setParameter("userid", sUserId).getSingleResult(true);
			if (accountBo != null) {
				accountBo.setAttributeValue("LOCKFLAG", "1");
				account.saveObject(accountBo);
				
				UsableBalance = Double.parseDouble(accountBo.getAttribute(
						"USABLEBALANCE").toString());
				FrozenBalance = Double.parseDouble(accountBo.getAttribute(
						"FROZENBALANCE").toString());
				
				reUsableBalance = UsableBalance - amount;
				reFrozenBalance = FrozenBalance + amount;
			} else {
				throw new HandlerException("quaryaccountamount.nodata.error");
			}
			accountBo.setAttributeValue("USABLEBALANCE", reUsableBalance);
			accountBo.setAttributeValue("FROZENBALANCE", reFrozenBalance);

			accountBo.setAttributeValue("LOCKFLAG", "2");
			account.saveObject(accountBo);
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			throw new HandlerException("makeamountfrozen.error");
		}
	}
	
	/**
	 * 冻结账户
	 * @param jbo
	 * @param tx
	 * @param sUserId
	 * @throws HandlerException
	 */
	private void makeAccountFreeze(JBOFactory jbo, JBOTransaction tx,
			String sUserId) throws HandlerException {
		BizObjectManager account;
		try {
			account = jbo.getManager("jbo.trade.account_freeze", tx);
			BizObject accountBo = account.newObject();
			accountBo.setAttributeValue("OBJECTTYPE", "UserAccount");
			accountBo.setAttributeValue("OBJECTNO", sUserId);
			accountBo.setAttributeValue("OPERATETYPE", "1");
			accountBo.setAttributeValue("OPERATEDATE", StringFunction.getToday());
			accountBo.setAttributeValue("REASON", "异常提现");
			accountBo.setAttributeValue("OPERATEUSERID", sUserId);
			accountBo.setAttributeValue("OPERATEORGID", sUserId);
			account.saveObject(accountBo);
		}  catch (Exception e) {
			throw new HandlerException("makeaccountfreeze.error");
		}
	}
}
