package com.amarsoft.p2ptrade.personcenter;


import java.util.Iterator;
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
import com.amarsoft.p2p.interfaces.utils.InterfaceHelper;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 发起绑卡交易 输入参数： UserID:账户编号 AccountNo:银行卡号 AccountName:账户名 AccountBelong:开户行
 * BelongArea:开户地区 输出参数： 成功标识 BindFlag：是否绑卡申请成功 true-->成功 false-->失败
 * 
 * 
 */
public class BindingCardApplyHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return runBindingCardApply(request);
	}

	/**
	 * 发起绑卡
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject runBindingCardApply(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("AccountNo") == null
				|| "".equals(request.get("AccountNo")))
			throw new HandlerException("accountno.error");
		if (request.get("AccountBelong") == null
				|| "".equals(request.get("AccountBelong")))
			throw new HandlerException("accountbelong.error");
		if (request.get("AccountName") == null
				|| "".equals(request.get("AccountName")))
			throw new HandlerException("accountname.error");

		if (request.get("province") == null
				|| "".equals(request.get("province")))
			throw new HandlerException("province.error");
		
		if (request.get("city") == null
				|| "".equals(request.get("city")))
			throw new HandlerException("city.error");
		
		String sUserID = request.get("UserID").toString();
		String sAccountNo = request.get("AccountNo").toString();
		String sCertID = (String)request.get("CertID");
		if(sCertID==null)sCertID="";
		String sAccountBelong = request.get("AccountBelong").toString();
		String sisReturnCard = (String)request.get("isReturnCard");
		String sAccountName = request.get("AccountName").toString();
		String province = request.get("province").toString();
		String city = request.get("city").toString();
		String phoneno = request.get("phoneno").toString();
		JSONObject result = new JSONObject();

		JBOFactory jbo = JBOFactory.getFactory();
		BizObjectManager accountManager = null;
		BizObject accountBo = null;
		//String sAccountName = getRealName(jbo,sUserID);
		try {
			CheckNeedBindBankHandler h0 = new CheckNeedBindBankHandler();
			JSONObject request0 = new JSONObject();
			request0.put("UserID", request.get("UserID"));
			request0.put("AccountNo", request.get("AccountNo"));
			request0.put("AccountName", request.get("AccountName"));
			request0.put("AccountBelong", request.get("AccountBelong"));
			request0.put("province", request.get("province"));
			request0.put("city", request.get("city"));
			JSONObject r0 = (JSONObject) h0.createResponse(request0, null);
			String againFlag = r0.get("againFlag").toString();

			// 获取当前时间
			TimeTool tool = new TimeTool();
			String sInputTime = tool.getsCurrentMoment();
			//String againFlag = "true";
			if ("true".equals(againFlag)) {
				if(!sCertID.equals("")){
				//未实名认证禁止绑卡	
					throw new HandlerException("queryuseraccount.nodata.error");				
				}

				// 改变原有已绑定银行卡的状态-->失效
				//changeOriginalCardStatus(jbo, sUserID);

				// 帐号信息处理对象
				accountManager = jbo
						.getManager("jbo.trade.account_info");
				accountBo = accountManager.newObject();

				accountBo.setAttributeValue("USERID", sUserID);// 用户编号
				accountBo.setAttributeValue("ACCOUNTTYPE", "001");// 账户类型
				accountBo.setAttributeValue("ACCOUNTNO", sAccountNo);// 账户名
				accountBo.setAttributeValue("ACCOUNTNAME", sAccountName);// 账户名
				accountBo.setAttributeValue("ACCOUNTBELONG", sAccountBelong);// 开户银行
				//accountBo.setAttributeValue("BELONGAREA", sBelongArea);// 开户地区
				accountBo.setAttributeValue("STATUS", "1");// 是否认证(认证中)
				accountBo.setAttributeValue("ISRETURNCARD", sisReturnCard);// 是否还款卡
				accountBo.setAttributeValue("CHECKNUMBER", "0");// 已校验次数
				accountBo.setAttributeValue("LIMITAMOUNT", 200000);// 限额
				accountBo.setAttributeValue("INPUTTIME", sInputTime);// 创建时间
				accountBo.setAttributeValue("PHONENO", phoneno);// 创建时间
				accountBo.setAttributeValue("province", province);// 创建时间
				accountBo.setAttributeValue("city", city);// 创建时间
				accountManager.saveObject(accountBo);
				String sAccountSerialNo = accountBo.getAttribute("SERIALNO")
						.toString();
				// 打款金额
				double amount = 0;

				String sStatus = null;
				String sTransType = null;
				
				sTransType = "1070";
				/*
				if (!("0302".equals(sAccountBelong))) {// 不是中信银行
					sStatus = "00";// 交易状态
					sTransType = "1070";
				} else {
					sStatus = "01";// 交易状态
					sTransType = "1071";
					result.put("BindFlag", "true");
				}
				*/
				JSONObject items = GeneralTools.getItemName(jbo, "BankNo");
				// 交易记录处理对象
				BizObjectManager recordManager = jbo.getManager(
						"jbo.trade.transaction_record");
				BizObject recordBo = recordManager.newObject();
				
				String sTranSerialNo = saveTransRecord(jbo, recordManager,
						recordBo, sUserID, sAccountSerialNo, sAccountBelong,
						amount, sInputTime, sStatus, sTransType, items);
				
				JSONObject obj = new JSONObject();
				for (int i = 0; i < recordBo.getAttributeNumber(); i++) {
					obj.put(recordBo.getAttribute(i).getName()
							.toUpperCase(), recordBo.getAttribute(i)
							.getValue());
				}
				
				//暂时写死 实名认证信息
				String channel = String.valueOf(obj.get("TRANSCHANNEL"));
				String transCode = String.valueOf(obj.get("TRANSTYPE"));
				
				RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation(channel, transCode);
				rttt.init(obj);
				rttt.execute();
				String sLogId = rttt.getLogId();
				
				/*if (!("0302".equals(sAccountBelong))) {// 不是中信银行
					// 添加交易时间
					recordBo.setAttributeValue("TRANSDATE",
							tool.getsCurrentDate());
					recordBo.setAttributeValue("TRANSTIME",
							tool.getsCurrentTime());
					
					
					//////////////////////////////////////////////////////
					JSONObject obj = new JSONObject();
					for (int i = 0; i < recordBo.getAttributeNumber(); i++) {
						obj.put(recordBo.getAttribute(i).getName()
								.toUpperCase(), recordBo.getAttribute(i)
								.getValue());
					}
					
					String channel = String.valueOf(obj.get("TRANSCHANNEL"));
					String transCode = String.valueOf(obj.get("TRANSTYPE"));
					RealTimeTradeTranscation rttt = TranscationFactory
							.getRealTimeTradeTranscation(channel, transCode);
					rttt.init(obj);
					rttt.execute();
					String sLogId = rttt.getLogId();
					*/
					//绑卡结果
					if(rttt.getTemplet().isSuccess())
					{
						recordBo.setAttributeValue("STATUS", "10");// 状态（成功）
						recordBo.setAttributeValue("TRANSLOGID", sLogId);// 交易日志编号
						recordBo.setAttributeValue("ACTUALAMOUNT", amount);// 实际到帐金额
						recordBo.setAttributeValue("UPDATETIME",
								new TimeTool().getsCurrentMoment());// 添加更新时间
						recordManager.saveObject(recordBo);

						
						//新浪支付，是否需要推进 如需要推进,则后续还需要弹出短信验证码完成推进操作
						String ticket = (String)InterfaceHelper.getFieldValue(rttt.getReponseMessage(), "ticket","");
						if(!InterfaceHelper.isEmpty(ticket))
						{//待推进
							result.put("is_verified", "true");
							result.put("BindFlag", "0");
							result.put("serialno", accountBo.getAttribute("serialno").toString());
							result.put("transSerialNo", obj.get("SERIALNO"));
							
						}else{//绑卡成功
							accountBo.setAttributeValue("UPDATETIME",
									new TimeTool().getsCurrentMoment()).setAttributeValue("status","2");
							accountManager.saveObject(accountBo);
							result.put("BindFlag", "1");
						}
						
					}
					else
					{
								recordBo.setAttributeValue("STATUS", "04");// 状态（失败）
								recordBo.setAttributeValue("TRANSLOGID", sLogId);// 交易日志编号
								recordBo.setAttributeValue("UPDATETIME",
										new TimeTool().getsCurrentMoment());// 添加更新时间
								recordManager.saveObject(recordBo);

								accountBo.setAttributeValue("STATUS", "6");// 是否认证(已失败)
								accountBo.setAttributeValue("UPDATETIME",
										new TimeTool().getsCurrentMoment());
								accountManager.saveObject(accountBo);
								result.put("BindFlag", "2");
								
					}
			} else {
				result.put("BindFlag", "2");
			}
			
			return result;
		} catch (HandlerException e) {
			if(!sCertID.equals("")){
				RollbackRealName(jbo,sUserID);
			}
			throw e;
		} catch (Exception e) {
			if(!sCertID.equals("")){
				RollbackRealName(jbo,sUserID);
			}
			e.printStackTrace();
			throw new HandlerException("bindingcardapply.error");
		}

	}

	/**
	 * 改变原有已绑定银行卡的状态，原状态 -->失效
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param tx
	 *            JBOTransaction
	 * @param sUserID
	 *            用户ID
	 * @throws HandlerException
	 */
	private void changeOriginalCardStatus(JBOFactory jbo, JBOTransaction tx,
			String sUserID) throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.account_info", tx);
			BizObjectQuery query = m
					.createQuery("userid=:userid order by inputtime desc");
			query.setParameter("userid", sUserID);

			List<BizObject> list = query.getResultList(true);
			if (list != null) {
				for (Iterator<BizObject> it = list.iterator(); it.hasNext();) {
					BizObject o = it.next();
					o.setAttributeValue("STATUS", "5");
					o.setAttributeValue("UPDATETIME",
							new TimeTool().getsCurrentMoment());
					m.saveObject(o);
				}
			}
		} catch (Exception e) {
			throw new HandlerException("changeoriginalcardstatus.error");
		}
	}

	/**
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param recordManager
	 *            BizObjectManager
	 * @param recordBo
	 *            BizObject
	 * @param sUserID
	 *            用户编号
	 * @param sSerialNo
	 *            帐号流水
	 * @param sAccountBelong
	 *            开户银行
	 * @param amount
	 *            交易金额
	 * @param sInputTime
	 *            插入时间
	 * @param sStatus
	 *            交易状态
	 * @param sTransType
	 *            交易类型
	 * @param items
	 *            开户银行码值键值对
	 * @return
	 * @throws HandlerExceptionsaveTransRecord
	 */
	private String saveTransRecord(JBOFactory jbo,
			BizObjectManager recordManager, BizObject recordBo, String sUserID,
			String sSerialNo, String sAccountBelong, double amount,
			String sInputTime, String sStatus, String sTransType,
			JSONObject items) throws HandlerException {
		try {
			String sAccountBelongName = items.containsKey(sAccountBelong) ? items
					.get(sAccountBelong).toString() : sAccountBelong;
			recordBo.setAttributeValue("USERID", sUserID);// 用户编号
			recordBo.setAttributeValue("DIRECTION", "P");// 发生方向(付)
			recordBo.setAttributeValue("AMOUNT", amount);// 交易金额
			recordBo.setAttributeValue("HANDLCHARGE", 0);// 手续费
			recordBo.setAttributeValue("TRANSTYPE", sTransType);// 交易类型(绑卡打款)
			recordBo.setAttributeValue("STATUS", sStatus);// 交易状态
			recordBo.setAttributeValue("INPUTTIME", sInputTime);// 创建时间
			recordBo.setAttributeValue("RELAACCOUNT", sSerialNo);// 关联账户流水号
			recordBo.setAttributeValue("RELAACCOUNTTYPE", "001");// 交易关联账户类型
			String sTransChannel  = null;
			
			sTransChannel = GeneralTools.getTransChannel(jbo,
					sAccountBelong, "ATTRIBUTE2");
			
			/*if(!("0302".equals(sAccountBelong))){
				sTransChannel = GeneralTools.getTransChannel(jbo,
						sAccountBelong, "ATTRIBUTE2");
			}else{
				sTransChannel = GeneralTools.getTransChannel(jbo,
						sAccountBelong, "ATTRIBUTE6");
			}
			*/
			
			recordBo.setAttributeValue("TRANSCHANNEL", sTransChannel);// 交易渠道
			recordBo.setAttributeValue("REMARK", "" + "银行卡绑卡|"
					+ sAccountBelongName);// 备注

			recordManager.saveObject(recordBo);
			return recordBo.getAttribute("SERIALNO").toString();

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
	private String getPhoneTel(JBOFactory jbo, String sUserID)
			throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("PHONETEL").getString();
			} else {
				return "";
			}
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}


/**
 * 获取用户真实姓名
 * 
 * @param accountBo
 * @throws HandlerException
 */
private String getRealName(JBOFactory jbo, String sUserID)
		throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.account_detail");
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("REALNAME").getString();
			} else {
				return "";
			}
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}

/**
 * 回滚实名认证
 * 
 * @param accountBo
 * @throws HandlerException
 */
private void RollbackRealName(JBOFactory jbo, String sUserID)
		throws HandlerException {
		BizObjectManager ma;
		BizObjectManager mb;
		BizObjectManager mc;
		try {
			
			//清除实名和身份证
			ma = jbo.getManager("jbo.trade.account_detail");
			BizObject o1=ma.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			o1.setAttributeValue("realname", null);
			o1.setAttributeValue("CERTID", null);
			ma.saveObject(o1);
			
			//修改实名认证标志
			mb = jbo.getManager("jbo.trade.user_account");
			BizObject o2=mb.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			o2.setAttributeValue("USERAUTHFLAG", null);
			mb.saveObject(o2);
			
			//删除认证记录
			mc = jbo.getManager("jbo.trade.user_authentication");
			BizObject o3=mc.createQuery("userid=:userid").setParameter("userid", sUserID).getSingleResult(true);
			mc.deleteObject(o3);
			
		} catch (Exception e) {
			
		}
	}
}
