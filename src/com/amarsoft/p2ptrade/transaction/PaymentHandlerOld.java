package com.amarsoft.p2ptrade.transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.DataConvert;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/*
 *  *@ 还款
 * 2014-5-16
 * 输入
 *     Paytype： 还款类型   1一般还款 Payment  ,3提前还款REPayment    合同号 
 * 输出    
 *     还款计划列表：期次、应还款日、本金、利息、平台服务费、担保费、平台管理费、担保管理费、提前还款违约金。
 * 所记录
 */
public class PaymentHandlerOld extends TradeHandler{
	
	//请求报文参数
	private double amtDouble;//请求还款额
	private String loanSerialnoString;// 借据号 信贷
	private String contractnoString;// 合同号 信贷
	private String paytypeString; //还款类型
	private Map<String, Object> map = new HashMap<String, Object>();
	private String sContractID = "";
	
	//信贷返回报文参数
	private String payamt;//总金额
	private double insuremanagement_fee;//	担保管理费
	private double penal_value;//	还款违约金
	private double thaw_amount;//解冻金额
	private double plantmange;//平台管理费
	private double managefee;//担保费
	private double plantfee;//平台服务费
	private double actualPayCorpusAmt;//本金
	private double actualPayInteAmt;//利息
	private double actualPayFineAmt;//罚息
	private double actualPayCompoundInte;//复利
	private String userPhoneNo;
	private String tuserPhoneNo;
	private String proName;
	private String billSerialno;
	//nn
	@Override
	protected Object requestObject(JSONObject request, JBOFactory jbo) throws HandlerException {
			if (request.get("PayType")==null||request.get("ContractId")==null||request.get("Amt")==null) {
				throw new HandlerException("request.invalid");
			}
		
			try{
				sContractID = (String)request.get("ContractId")==null?"":(String)request.get("ContractId");//合同号
				if("".equals(sContractID)||null==sContractID) throw new HandlerException("common.contractnotexist");
				
				paytypeString = (String)request.get("PayType")==null?"":(String)request.get("PayType");//还款类型
				if("".equals(paytypeString)||null==paytypeString) throw new HandlerException("transrun.err");
				
				amtDouble = Double.parseDouble((String)request.get("Amt")==null?"0":(String)request.get("Amt"));//金额
				if(amtDouble<=0) throw new HandlerException("transrun.err");
				map = getMapName(jbo);//得分配金额账户
			    
			    //用户状态判断  account_freeze
			    GeneralTools.userAccountStatus((String)map.get("userid"),"") ;
			
			    double usbblance = tUsablebalance(jbo,(String)map.get("userid"));// 投资人账户可用金额  
				if (usbblance < amtDouble||amtDouble<=0) {
					throw new HandlerException("tusaamtnoenough.error");
				}
			    
				String[] strings = getloanserialno(jbo).split("@",-1);
				loanSerialnoString = strings[0];
				contractnoString = strings[1];
				if ("3".equals(paytypeString)) {
					request.put("Method", "REPayment");
				}else if ("1".equals(paytypeString)) {
					request.put("Method", "Payment");
				}else{
					request.put("Method", "");
				}
				
				//手机号
				userPhoneNo = getPhoneTel((String)map.get("userid"), jbo);
				tuserPhoneNo =  getPhoneTel((String)map.get("tuserid"), jbo);
				if(userPhoneNo.length()==0||tuserPhoneNo.length()==0){
				  throw new HandlerException("common.emptymobile");//未绑定卡
				}
				
				proName = getProName(jbo);
				if("".equals(proName)||null==proName) throw new HandlerException("writeaccount.error");//未绑定卡
				
				//锁 账户     借款人   担保公司  平台
				String frozenstatus = "1";
				frozenaccount(map,frozenstatus ,jbo);
	 			//冻结借款人金额
				frozensigler(map,frozenstatus,request, jbo);
				//账户记账
				request.put("Amt", amtDouble);
				request.put("LoanSerialNo", loanSerialnoString);
				request.put("ContractSerialNo", contractnoString);
				
//				try{
//					tx.commit();
//				}catch(Exception e){
//					tx.rollback();
//					e.printStackTrace();
//					throw new HandlerException("transrun.err");
//				}
//				
				
			}catch(Exception e){
				e.printStackTrace();
				throw new HandlerException("transrun.err");
			}
			
			return request;
	}

	@Override
	protected Object responseObject(JSONObject request, JSONObject response, String logid, String transserialno, 	JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		payamt =GeneralTools.numberFormat(Double.parseDouble((String)response.get("payamt")), 0, 2) ;//总金额
		billSerialno = (String)response.get("sbillSerialNo")==null?"":(String)response.get("sbillSerialNo");
		try {
			
			writeaccount(map ,response, jbo);
			// 解锁 账户
			frozenaccount(map,"2" ,jbo);
			//插入交易记录
			//TODO 插入还款交易，状态为03-待返回结果
			initTransactionRecord(request,jbo);
			insertTraction(map,payamt,request, jbo);
			
			try{
				senmsg();//还款借款人
			}catch(Exception e){
                ARE.getLog().info("短信发送失败借款人，合同号："+sContractID);
			}
			
			try{
				tsenmsg();//投资人
			}catch(Exception e){
                ARE.getLog().info("短信发送失败投资人，合同号："+sContractID);
			}
			try{
				//取结清时间
				String sFinishDate = (String)response.get("sFinishDate")==null?"":(String)response.get("sFinishDate");
				if(!("".equals(sFinishDate)||null==sFinishDate)){
				//不为空发送结清短信
					try{
						senmsgfinish();//借款人
					}catch(Exception e){
		                ARE.getLog().info("短信发送失败借款人，合同号："+sContractID);
					}
					
					try{
						sentmsgfinish();//投资人
					}catch(Exception e){
		                ARE.getLog().info("短信发送失败投资人，合同号："+sContractID);
					}
					
				}
			}catch(Exception e){
                ARE.getLog().info("短信发送失败投资人，合同号："+sContractID);
			}
				
				
			
			
		}catch (HandlerException e) {
			// TODO: handle exception
			e.printStackTrace();
			try {
				tx.rollback();
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new HandlerException("transrun.err");
			}
			throw new HandlerException("transrun.err");
		}
		return response;
	}
	

	/**
	 * @param request
	 * @param jbo
	 * @return
	 * @throws HandlerException 
	 */
	private String getloanserialno(JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		String loanseriaString ="";
		String contractnoString ="";
		BizObjectManager manager = null;
		try {
			manager = jbo.getManager("jbo.trade.ti_contract_info");
			BizObjectQuery query = manager.createQuery("select LOANNO ,CONTRACTNO from o where  CONTRACTID  =:CONTRACTID");
			query.setParameter("CONTRACTID",sContractID);
			BizObject bObject = query.getSingleResult(false);
			if (bObject!=null) {
				loanseriaString = bObject.getAttribute("LOANNO").getValue()==null?"":bObject.getAttribute("LOANNO").getString();
				contractnoString = bObject.getAttribute("CONTRACTNO").getValue()==null?"":bObject.getAttribute("CONTRACTNO").getString();
			}else {
				throw new HandlerException("common.contractidnotexist");
			}
		}catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return loanseriaString+"@"+contractnoString;
	}
	
	/**
	 * @throws HandlerException 
	 * 
	 */
	private void tsenmsg() throws HandlerException {
		// TODO Auto-generated method stub
		// 发送短信提醒 投资人
		 GeneralTools.sendSMS("P2P_SKCG", tuserPhoneNo, setP2P_SKCG());
	}
	
	private void senmsg() throws HandlerException {
		// TODO Auto-generated method stub
		// 发送短信提醒 借款人
		 GeneralTools.sendSMS("P2P_HKCG", userPhoneNo, setP2P_HKCG());
	}
	
	private void sentmsgfinish() throws HandlerException {
		// TODO Auto-generated method stub
		// 发送短信提醒还款还清,投资人贷款还清
		 GeneralTools.sendSMS("P2P_SKCGLAST", tuserPhoneNo, setP2P_SKCGLAST());
	}
	
	private void senmsgfinish() throws HandlerException {
		// TODO Auto-generated method stub
		// 发送短信提醒还款还清,借款人贷款还清
		 GeneralTools.sendSMS("P2P_DKHQ", userPhoneNo, setP2P_DKHQ());
	}
	
	private HashMap<String, Object> setP2P_SKCGLAST() {
		// TODO Auto-generated method stub
		HashMap<String , Object> map   = new HashMap<String, Object>();
		map.put("ProjectName", proName);
		return map;
	}

	private HashMap<String, Object> setP2P_DKHQ() {
		// TODO Auto-generated method stub
		HashMap<String , Object> map   = new HashMap<String, Object>();
		map.put("ContractNo", contractnoString);
		return map;
	}
	
	private HashMap<String, Object> setP2P_HKCG()  {
		// TODO Auto-generated method stub
		HashMap<String , Object> map   = new HashMap<String, Object>();
		map.put("ContractNo", contractnoString); 
		map.put("PayAmount", GeneralTools.numberFormat(GeneralTools.round(amtDouble,2),0,2));
		return map;
	}
	
	private HashMap<String, Object> setP2P_SKCG()  {
		// TODO Auto-generated method stub
		HashMap<String , Object> map   = new HashMap<String, Object>();
		map.put("Amount", GeneralTools.numberFormat(GeneralTools.round(penal_value+actualPayCorpusAmt+actualPayInteAmt+actualPayFineAmt+actualPayCompoundInte,2), 0, 2));
		map.put("Date", StringFunction.getToday());
		map.put("ProjectName", proName);
		return map;
	}
	
	/**
	 * @return
	 * @throws HandlerException 
	 */

	private String getProName(JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		BizObjectManager manager;
		String projectname = "";
		try {
			manager = jbo.getManager("jbo.trade.project_info");
			BizObjectQuery query = manager.createQuery("  CONTRACTID = :CONTRACTID");
			query.setParameter("CONTRACTID",contractnoString);
			BizObject boBizObject = query.getSingleResult(false);
			projectname = boBizObject.getAttribute("PROJECTNAME").getValue()==null?"":boBizObject.getAttribute("PROJECTNAME").getString()
					+"|"+boBizObject.getAttribute("SERIALNO").getValue()==null?"":boBizObject.getAttribute("SERIALNO").getString();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//throw new HandlerException("sms.templetid.error");
		}
		return projectname;
	}
	
		/**
	 *    新增还款记录详情表
		 * @throws HandlerException 
	 */
	private void insertbackdetail( JSONObject response ,JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		BizObjectManager manager;
		List<JSONObject> payment_detail=  (List<JSONObject>) response.get("payment_detail");
		try {
		manager = jbo.getManager("jbo.trade.acct_back_detail",tx);
		for (int i = 0; i < payment_detail.size(); i++) {
		    BizObject o =manager.newObject();
			JSONObject jObjectpaymentdetail = payment_detail.get(i);
			if ("3".equals(paytypeString)) {
				o.setAttributeValue("SEQID", jObjectpaymentdetail.get("ahead_rpterm"));//期次
				o.setAttributeValue("ACTUALPAYCORPUSAMT", jObjectpaymentdetail.get("ahead_capital"));//还款本金
				o.setAttributeValue("ACTUALPAYINTEAMT", jObjectpaymentdetail.get("ahead_int"));//还款利息
				o.setAttributeValue("ACTUALFINEAMT", jObjectpaymentdetail.get("ahead_oint"));//还款罚息
				o.setAttributeValue("ACTUALPAYFEEAMT1", jObjectpaymentdetail.get("ahead_insureamount"));//实还担保费
				o.setAttributeValue("ACTUALPAYFEEAMT2", jObjectpaymentdetail.get("ahead_plantservicefee"));//实还平台服务费
				o.setAttributeValue("ACTUALEXPIATIONSUM", response.get("penal_value"));//还款违约金
				o.setAttributeValue("PAYTYPE", paytypeString);//还款类型   1一般还款 Payment  ,3提前还款
			}else if ("1".equals(paytypeString)) {
				o.setAttributeValue("SEQID", jObjectpaymentdetail.get("manual_rpterm"));//期次
				o.setAttributeValue("ACTUALPAYCORPUSAMT", jObjectpaymentdetail.get("manual_capital"));//还款本金
				o.setAttributeValue("ACTUALPAYINTEAMT", jObjectpaymentdetail.get("manual_int"));//还款利息
				o.setAttributeValue("ACTUALFINEAMT", jObjectpaymentdetail.get("manual_oint"));//还款罚息
				o.setAttributeValue("ACTUALPAYFEEAMT1", jObjectpaymentdetail.get("manual_insureamount"));//实还担保费
				o.setAttributeValue("ACTUALPAYFEEAMT2", jObjectpaymentdetail.get("plantservicefee"));//实还平台服务费
			}
				o.setAttributeValue("ACTUALPLANTMANAGE", response.get("plantmange"));//实还平台管理费
				o.setAttributeValue("ACTUALGUARANTEEDMANAGE", response.get("insuremanagement_fee"));//实还担保管理费
				o.setAttributeValue("LOANSERIALNO",loanSerialnoString);//借据号
			    manager.saveObject(o);
		}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}

		/**
	 * @param map
	 * @param response
	 * @param jbo
		 * @throws HandlerException 
		 * 
	 */
	private void writeaccount(Map<String, Object> map ,JSONObject response, JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		BizObjectManager manager= null;
		
		insuremanagement_fee = GeneralTools.round(Double.parseDouble((String) response.get("insuremanagement_fee")), 2);//	担保管理费
		penal_value = GeneralTools.round(Double.parseDouble((String) response.get("penal_value")), 2);//	还款违约金
		thaw_amount = GeneralTools.round(Double.parseDouble((String) response.get("thaw_amount")), 2);//解冻金额(多扣金额  作为充值)
		plantmange = GeneralTools.round(Double.parseDouble((String) response.get("plantmange")), 2);//平台管理费
		managefee = GeneralTools.round(Double.parseDouble((String) response.get("managefee")), 2);//担保费
		plantfee = GeneralTools.round(Double.parseDouble((String) response.get("plantfee")), 2);//平台服务费
		actualPayCorpusAmt = GeneralTools.round(Double.parseDouble((String) response.get("actualPayCorpusAmt")), 2);//本金
		actualPayInteAmt = GeneralTools.round(Double.parseDouble((String) response.get("actualPayInteAmt")), 2);//利息
		actualPayFineAmt = GeneralTools.round(Double.parseDouble((String) response.get("actualPayFineAmt")), 2);//罚息
		actualPayCompoundInte = GeneralTools.round(Double.parseDouble((String) response.get("actualPayCompoundInte")), 2);//复利
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid  in ( :tuserid, :userid)");
			query.setParameter("tuserid",(String) map.get("tuserid"));//投资人账户
			query.setParameter("userid",(String) map.get("userid"));//借款人账户
			List<BizObject> list = query.getResultList(true);
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					//double a =0d;
					double usablebalance = Double.parseDouble(o.getAttribute("USABLEBALANCE").getValue()==null?"0":o.getAttribute("USABLEBALANCE").getString());
					String userid = o.getAttribute("USERID").getValue()==null?"":o.getAttribute("USERID").getString();
					//double frozenbalance =Double.parseDouble(o .getAttribute("frozenbalance").getValue()==null?"0":o.getAttribute("frozenbalance").getString());//虚拟账户可用余额
					 //账户记账 
					if (((String) map.get("tuserid")).equals(userid)) {
						//投资人记账    本 利 罚
						usablebalance = usablebalance + actualPayCorpusAmt+actualPayInteAmt+actualPayFineAmt+actualPayCompoundInte+penal_value;//本 利 罚 复 违约金
					}else if (((String) map.get("userid")).equals(userid)) {
					//	a = amtDouble;
						// 多扣作为充值的金额
						if (thaw_amount>0) {
							usablebalance=usablebalance+thaw_amount;
						}
					} 
					o.setAttributeValue("USABLEBALANCE", GeneralTools.round(usablebalance, 2));
					//o.setAttributeValue("frozenbalance", GeneralTools.round(frozenbalance-a,2));
					manager.saveObject(o);
				}
			}else {
				throw new HandlerException("writeaccount.error");
			}

//			//机构账户
			manager = jbo.getManager("jbo.trade.org_account_info", tx);
			BizObjectQuery bObjectQuery = manager.createQuery(" serialno in (:puseridserialno,:duseridserialno)");
			bObjectQuery.setParameter("puseridserialno",(String) map.get("puseridserialno"));//平台账户orgid
			bObjectQuery.setParameter("duseridserialno",(String) map.get("duseridserialno"));//担保公司账户orgid
			List<BizObject> listorgid = bObjectQuery.getResultList(true);

			if (listorgid != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < listorgid.size(); i++) {
					BizObject oo = listorgid.get(i);
					double usablebalance = Double.parseDouble(oo.getAttribute("USABLEBALANCE").getValue()==null?"0":oo.getAttribute("USABLEBALANCE").getString());
					String sAccountType = oo.getAttribute("AccountType").getValue()==null?"":oo.getAttribute("AccountType").getString();
					if ("0103".equals(sAccountType)) {
						//平台账户   平台服务费
						usablebalance =usablebalance+plantmange+plantfee;// 平台管理费   平台服务费
					}else if ("0202".equals(sAccountType)) {
						//担保公司账户   担保费
						usablebalance = usablebalance +insuremanagement_fee+managefee; //担保管理费    担保费
					}				
					oo.setAttributeValue("USABLEBALANCE", GeneralTools.round(usablebalance, 2));
					manager.saveObject(oo);
				}
			}else {
				throw new HandlerException("writeaccount.error");
			}
		} catch (HandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("common.usernotexist");
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}

	/**
	 * @param request
	 * @param jbo
	 * @return 
	 * @throws HandlerException 
	 */
	private void frozensigler(Map<String, Object> map,String lockflag,JSONObject request, JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
		   BizObjectManager manager = null;
		   Double amt=Double.parseDouble((String) request.get("Amt"));
		try {   
			String  userid= (String) map.get("userid");
			manager = jbo.getManager("jbo.trade.user_account",tx);
			BizObjectQuery query = manager.createQuery("userid=:userid ");
			query.setParameter("userid", userid);
			BizObject bizObject = query.getSingleResult(true);
			double usablebalance =Double.parseDouble(bizObject .getAttribute("USABLEBALANCE").getValue()==null?"0":bizObject.getAttribute("USABLEBALANCE").getString());//虚拟账户可用余额
			//double frozenbalance =Double.parseDouble(bizObject .getAttribute("frozenbalance").getValue()==null?"0":bizObject.getAttribute("frozenbalance").getString());//虚拟账户可用余额
			if (usablebalance - amt>=0) {
				usablebalance = usablebalance - amt;
				// 足额      冻结金额     
				bizObject.setAttributeValue("USABLEBALANCE",GeneralTools.round(usablebalance, 2));
			//	bizObject.setAttributeValue("frozenbalance",GeneralTools.round(frozenbalance+amt, 2));
			}else {
				//账户余额不足 请先充值
				throw new HandlerException("usablebalance.notenough");
			}
			manager.saveObject(bizObject);
		} catch (HandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("usablebalance.notenough");
			
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
 	/*
 	 * 
 	 * 	 锁定 投资人借款人 担保 平台账户
 	 * 
 	 * */
		private void frozenaccount(Map<String, Object> map,String frozenstatus ,JBOFactory jbo) throws HandlerException {
			BizObjectManager manager;
			try { 
				//个人账户
				manager = jbo.getManager("jbo.trade.user_account", tx);
				BizObjectQuery query = manager.createQuery("userid  in (:tuserid, :userid)");
				query.setParameter("tuserid",(String) map.get("tuserid"));//投资人账户
				query.setParameter("userid",(String) map.get("userid"));//借款人账户
				List<BizObject> list = query.getResultList(true);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						BizObject o = list.get(i);
						o.setAttributeValue("LOCKFLAG", frozenstatus);
						manager.saveObject(o);
					}
				}
				//机构账户
				manager = jbo.getManager("jbo.trade.org_account_info", tx);
				BizObjectQuery bObjectQuery = manager.createQuery(" serialno in (:puseridserialno,:duseridserialno)");
				bObjectQuery.setParameter("puseridserialno",(String) map.get("puseridserialno"));//平台账户orgid
				bObjectQuery.setParameter("duseridserialno",(String) map.get("duseridserialno"));//担保公司账户orgid
				List<BizObject> listorg = bObjectQuery.getResultList(true);
				if (listorg != null) {
					JSONArray array = new JSONArray();
					for (int i = 0; i < listorg.size(); i++) {
						BizObject oo = listorg.get(i);
						oo.setAttributeValue("LOCKFLAG", frozenstatus);
						manager.saveObject(oo);
					}
				}
			} catch (JBOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new HandlerException("makeamountfrozen.error");
			}
		}

		/***
		 * 获取投资人tuserid、借款人userid、担保公司收入流水号duseridserialno、平台收入账户流水号puseridserialno
		 * 
		 * ***/
		
		private Map<String, Object> getMapName(JBOFactory jbo) throws HandlerException {
		Map<String, Object> map = new HashMap<String, Object>();
		 
		BizObjectManager manager = null;
		try {
			manager = jbo.getManager("jbo.trade.user_contract",tx);
			BizObjectQuery query = manager.createQuery("CONTRACTID=:CONTRACTID ");
			query.setParameter("CONTRACTID", sContractID);
			List<BizObject> list = query.getResultList(false);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					String userrelativetype = o.getAttribute("RELATIVETYPE").getValue()==null?"":o.getAttribute("RELATIVETYPE").getString();
					if ("001".equals(userrelativetype)) {
						map.put("userid", o.getAttribute("USERID").getValue()==null?"":o.getAttribute("USERID").getString()); // 借款人账户
					} else if ("002".equals(userrelativetype)) {
						map.put("tuserid", o.getAttribute("USERID").getValue()==null?"":o.getAttribute("USERID").getString());// 投资人账户
					}else {
						throw new HandlerException("common.usernotexist");
					}
				}
			}
			manager = jbo.getManager("jbo.trade.org_account_info",tx);
			BizObjectQuery qObjectQuery = manager.createQuery(
					" SELECT toa.serialno,toa.AccountType FROM jbo.trade.org_account_info toa WHERE " +
					"toa.accounttype in ('0202','0103') " +
					"AND toa.status='1' ");
			qObjectQuery.setParameter("CONTRACTID", sContractID);
			qObjectQuery.setParameter("CONTRACTSERIALNO", sContractID);
			List<BizObject> listtype = qObjectQuery.getResultList(false);
			if (listtype != null) {
				int m = 0;
				int n = 0;
				for (int i = 0; i < listtype.size(); i++) {
					BizObject oo = listtype.get(i);
					if (oo!=null) {
					
						String sAccountType = oo.getAttribute("AccountType").getValue()==null?"":oo.getAttribute("AccountType").getString();
						if ("0202".equals(sAccountType)) {
							// 担保公司
							map.put("duseridserialno", oo.getAttribute("serialno").getValue()==null?"":oo.getAttribute("serialno").getString());// 担保公司担保费收入账户
							m++;
						} else if ("0103".equals(sAccountType)) {
							// 平台账户
							//TODO 逻辑错误
							map.put("puseridserialno", oo.getAttribute("serialno").getValue()==null?"":oo.getAttribute("serialno").getString());// 网贷平台收入户
							n++;
						}
						
						if(i==listtype.size()-1){
							if(m<=0) {
								ARE.getLog().debug("未配置担保公司担保费收入账户数据！");
								throw new HandlerException("transrun.err");
							}
							if(n<=0) {
								ARE.getLog().debug("未配置网贷平台收入帐户数据！");
								throw new HandlerException("transrun.err");
							}
						}
						
						
					}else {
						ARE.getLog().debug("未配置担保公司及平台帐户数据！");
						throw new HandlerException("transrun.err");
					}
				}
			}else {
				throw new HandlerException("transrun.err");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return map;
	}
		
	/*
	 * 
	 * 插入交易记录
	 * */
	private  void  insertTraction(Map<String, Object> map,String payamt,JSONObject request,JBOFactory jbo) throws HandlerException {
		String sTUserID = (String) map.get("tuserid");
		String sUserID = (String) map.get("userid");
		Map<String, Object> tuserMap = new HashMap<String, Object>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, Object> porgMap = new HashMap<String, Object>();
		Map<String, Object> dorgMap = new HashMap<String, Object>();
		Map<String ,Object> smap = getmap(jbo,map);
		tuserMap = (Map<String, Object>) smap.get("tuserid");
		userMap = (Map<String, Object>) smap.get("userid");
		porgMap = (Map<String, Object>) smap.get("porgMap");//平台公司
		dorgMap = (Map<String, Object>) smap.get("dorgMap");//担保公司
		BizObjectManager m =null;
		String sInputDate =GeneralTools.getDate();
		String sInputTime =GeneralTools.getTime();
		try {
			m = jbo.getManager("jbo.trade.transaction_record",tx);
			double balance = Double.parseDouble(tuserMap.get("BALANCE").toString());
			balance = GeneralTools.round(balance-penal_value-actualPayCorpusAmt-actualPayInteAmt-actualPayFineAmt-actualPayCompoundInte, 2);
			if(penal_value+actualPayCorpusAmt+actualPayInteAmt+actualPayFineAmt+actualPayCompoundInte>0){
				if(penal_value>0){ 
					BizObject o = m.newObject();
					o.setAttributeValue("USERID", sTUserID);//用户编号 投资人
					o.setAttributeValue("AMOUNT", penal_value);//交易金额
					o.setAttributeValue("ACTUALAMOUNT", penal_value);//交易金额
					o.setAttributeValue("TRANSTYPE", "1090");//交易类型
					o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					o.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					o.setAttributeValue("BALANCE", balance+penal_value);// 余额
					o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
					o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
					o.setAttributeValue("REMARK","投资收益:还款违约金");//备注
					o.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 1000));//更新时间
					o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					o.setAttributeValue("STATUS", "10");//交易状态
					o.setAttributeValue("HANDLCHARGE", "0");//手续费
					o.setAttributeValue("ReBillNo",billSerialno);
					o.setAttributeValue("ACTUALEXPIATIONSUM",penal_value);//还款违约金
					m.saveObject(o);
				}
				if(actualPayCorpusAmt>0){
					BizObject o = m.newObject();
					o.setAttributeValue("USERID", sTUserID);//用户编号 投资人
					o.setAttributeValue("AMOUNT", actualPayCorpusAmt);//交易金额
					o.setAttributeValue("ACTUALAMOUNT", actualPayCorpusAmt);//交易金额
					o.setAttributeValue("TRANSTYPE", "1090");//交易类型
					o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					o.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					o.setAttributeValue("BALANCE",balance+penal_value+actualPayCorpusAmt);// 余额
					o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
					o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
					o.setAttributeValue("REMARK","投资收益:本金");//备注
					o.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 2000));//更新时间
					o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					o.setAttributeValue("STATUS", "10");//交易状态
					o.setAttributeValue("HANDLCHARGE", "0");//手续费
					o.setAttributeValue("ReBillNo",billSerialno);
					o.setAttributeValue("ACTUALPAYCORPUSAMT",actualPayCorpusAmt);//本金
					m.saveObject(o);
				}
				if(actualPayInteAmt>0){
					BizObject o = m.newObject();
					o.setAttributeValue("USERID", sTUserID);//用户编号 投资人
					o.setAttributeValue("AMOUNT", actualPayInteAmt);//交易金额
					o.setAttributeValue("ACTUALAMOUNT", actualPayInteAmt);//交易金额
					o.setAttributeValue("TRANSTYPE", "1090");//交易类型
					o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					o.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					o.setAttributeValue("BALANCE",balance+penal_value+actualPayCorpusAmt+actualPayInteAmt);// 余额
					o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
					o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
					o.setAttributeValue("REMARK","投资收益:利息");//备注
					o.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 3000));//更新时间
					o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					o.setAttributeValue("STATUS", "10");//交易状态
					o.setAttributeValue("HANDLCHARGE", "0");//手续费
					o.setAttributeValue("ReBillNo",billSerialno);
					o.setAttributeValue("ACTUALPAYINTEAMT",actualPayInteAmt);//利息
					m.saveObject(o);
				}
				if(actualPayFineAmt>0){
					BizObject o = m.newObject();
					o.setAttributeValue("USERID", sTUserID);//用户编号 投资人
					o.setAttributeValue("AMOUNT", actualPayFineAmt);//交易金额
					o.setAttributeValue("ACTUALAMOUNT", actualPayFineAmt);//交易金额
					o.setAttributeValue("TRANSTYPE", "1090");//交易类型
					o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					o.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					o.setAttributeValue("BALANCE",balance+penal_value+actualPayCorpusAmt+actualPayInteAmt+actualPayFineAmt);// 余额
					o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
					o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
					o.setAttributeValue("REMARK","投资收益:罚息");//备注
					o.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 4000));//更新时间
					o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					o.setAttributeValue("STATUS", "10");//交易状态
					o.setAttributeValue("HANDLCHARGE", "0");//手续费
					o.setAttributeValue("ReBillNo",billSerialno);
					o.setAttributeValue("ACTUALFINEAMT",actualPayFineAmt);//罚息
					m.saveObject(o);
				}
				if(actualPayCompoundInte>0){
					BizObject o = m.newObject();
					o.setAttributeValue("USERID", sTUserID);//用户编号 投资人
					o.setAttributeValue("AMOUNT", actualPayCompoundInte);//交易金额
					o.setAttributeValue("ACTUALAMOUNT", actualPayCompoundInte);//交易金额
					o.setAttributeValue("TRANSTYPE", "1090");//交易类型
					o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					o.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					o.setAttributeValue("BALANCE",balance+penal_value+actualPayCorpusAmt+actualPayInteAmt+actualPayFineAmt+actualPayCompoundInte);// 余额
					o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
					o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
					o.setAttributeValue("REMARK","投资收益:复利");//备注
					o.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 5000));//更新时间
					o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					o.setAttributeValue("STATUS", "10");//交易状态
					o.setAttributeValue("HANDLCHARGE", "0");//手续费
					o.setAttributeValue("ReBillNo",billSerialno);
					o.setAttributeValue("ACTUALCOMPDINTEAMT",actualPayCompoundInte);//复利
					m.saveObject(o);
				}
			
			}
			
			double balanceInsure = Double.parseDouble(dorgMap.get("BALANCE").toString());
			balanceInsure = GeneralTools.round(balanceInsure-insuremanagement_fee-managefee, 2);
			if(insuremanagement_fee+managefee>0){//担保费
				//机构
				if(insuremanagement_fee>0){
					BizObject bizObjectet = m.newObject();
					bizObjectet.setAttributeValue("USERID", dorgMap.get("ORGID"));//用户编号 投资人
					bizObjectet.setAttributeValue("AMOUNT",insuremanagement_fee);//交易金额
					bizObjectet.setAttributeValue("ACTUALAMOUNT",insuremanagement_fee);//交易金额
					bizObjectet.setAttributeValue("TRANSTYPE", "1090");//交易类型
					bizObjectet.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					bizObjectet.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					bizObjectet.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					bizObjectet.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					bizObjectet.setAttributeValue("BALANCE",balanceInsure+insuremanagement_fee);// 余额
					bizObjectet.setAttributeValue("RELAACCOUNT",dorgMap.get("RELAACCOUNT"));//关联账户流水号
					bizObjectet.setAttributeValue("RELAACCOUNTTYPE","002");//交易关联账户类型 （用户账户/机构账户）
					bizObjectet.setAttributeValue("REMARK","担保公司费用入账:担保管理费["+GeneralTools.numberFormat(GeneralTools.round(insuremanagement_fee,2), 0, 2)+"]");//备注
					bizObjectet.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 1000));//更新时间
					bizObjectet.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					bizObjectet.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					bizObjectet.setAttributeValue("STATUS", "10");//交易状态
					bizObjectet.setAttributeValue("HANDLCHARGE", "0");//手续费
					bizObjectet.setAttributeValue("ACTUALGUARANTEEDMANAGE",insuremanagement_fee);//担保管理费
					m.saveObject(bizObjectet);
				}
				
				if(managefee>0){
					BizObject bizObjectet = m.newObject();
					bizObjectet.setAttributeValue("USERID", dorgMap.get("ORGID"));//用户编号 投资人
					bizObjectet.setAttributeValue("AMOUNT",managefee);//交易金额
					bizObjectet.setAttributeValue("ACTUALAMOUNT",managefee);//交易金额
					bizObjectet.setAttributeValue("TRANSTYPE", "1090");//交易类型
					bizObjectet.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					bizObjectet.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					bizObjectet.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					bizObjectet.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					bizObjectet.setAttributeValue("BALANCE",balanceInsure+insuremanagement_fee+managefee);// 余额
					bizObjectet.setAttributeValue("RELAACCOUNT",dorgMap.get("RELAACCOUNT"));//关联账户流水号
					bizObjectet.setAttributeValue("RELAACCOUNTTYPE","002");//交易关联账户类型 （用户账户/机构账户）
					bizObjectet.setAttributeValue("REMARK","担保公司费用入账:担保费["+GeneralTools.numberFormat(GeneralTools.round(managefee, 2), 0, 2)+"]");//备注
					bizObjectet.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 2000));//更新时间
					bizObjectet.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					bizObjectet.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					bizObjectet.setAttributeValue("STATUS", "10");//交易状态
					bizObjectet.setAttributeValue("HANDLCHARGE", "0");//手续费
					bizObjectet.setAttributeValue("ReBillNo",billSerialno);
					bizObjectet.setAttributeValue("ACTUALPAYFEEAMT1",managefee);//担保费
					m.saveObject(bizObjectet);
				}
				
			}
			
			double balancePlant = Double.parseDouble(porgMap.get("BALANCE").toString());
			balancePlant = GeneralTools.round(balancePlant-plantmange-plantfee, 2);
			if(plantmange+plantfee>0){
				//平台
				if(plantmange>0){
					BizObject bizObjecto = m.newObject();
					bizObjecto.setAttributeValue("USERID", porgMap.get("ORGID"));//用户编号 投资人
					bizObjecto.setAttributeValue("AMOUNT",plantmange);//交易金额
					bizObjecto.setAttributeValue("ACTUALAMOUNT",plantmange);//交易金额
					bizObjecto.setAttributeValue("TRANSTYPE", "1090");//交易类型
					bizObjecto.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					bizObjecto.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					bizObjecto.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					bizObjecto.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					bizObjecto.setAttributeValue("BALANCE",balancePlant+plantmange);// 余额
					bizObjecto.setAttributeValue("RELAACCOUNT",porgMap.get("RELAACCOUNT"));//关联账户流水号
					bizObjecto.setAttributeValue("RELAACCOUNTTYPE","002");//交易关联账户类型 （用户账户/机构账户）
					bizObjecto.setAttributeValue("REMARK","平台费用入账:管理费["+GeneralTools.numberFormat(GeneralTools.round(plantmange,2), 0, 2)+"]");//备注
					bizObjecto.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 1000));//更新时间
					bizObjecto.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					bizObjecto.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					bizObjecto.setAttributeValue("STATUS", "10");//交易状态
					bizObjecto.setAttributeValue("HANDLCHARGE", "0");//手续费
					bizObjecto.setAttributeValue("ReBillNo",billSerialno);
					bizObjecto.setAttributeValue("ACTUALPLANTMANAGE",plantmange);//平台管理费
					m.saveObject(bizObjecto);
				}
				if(plantfee>0){
					BizObject bizObjecto = m.newObject();
					bizObjecto.setAttributeValue("USERID", porgMap.get("ORGID"));//用户编号 投资人
					bizObjecto.setAttributeValue("AMOUNT",plantfee);//交易金额
					bizObjecto.setAttributeValue("ACTUALAMOUNT",plantfee);//交易金额
					bizObjecto.setAttributeValue("TRANSTYPE", "1090");//交易类型
					bizObjecto.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
					bizObjecto.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
					bizObjecto.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
					bizObjecto.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
					bizObjecto.setAttributeValue("BALANCE",balancePlant+plantmange+plantfee);// 余额
					bizObjecto.setAttributeValue("RELAACCOUNT",porgMap.get("RELAACCOUNT"));//关联账户流水号
					bizObjecto.setAttributeValue("RELAACCOUNTTYPE","002");//交易关联账户类型 （用户账户/机构账户）
					bizObjecto.setAttributeValue("REMARK","平台费用入账:服务费["+GeneralTools.numberFormat(GeneralTools.round(plantfee, 2), 0, 2)+"]");//备注
					bizObjecto.setAttributeValue("UPDATETIME",addDateSeconds(StringFunction.getToday(), StringFunction.getNow(), 2000));//更新时间
					bizObjecto.setAttributeValue("TRANSDATE", sInputDate);//交易日期
					bizObjecto.setAttributeValue("TRANSTIME", sInputTime);//交易时间
					bizObjecto.setAttributeValue("STATUS", "10");//交易状态
					bizObjecto.setAttributeValue("HANDLCHARGE", "0");//手续费
					bizObjecto.setAttributeValue("ReBillNo",billSerialno);
					bizObjecto.setAttributeValue("ACTUALPAYFEEAMT2",plantfee);//平台服务费
					m.saveObject(bizObjecto);
				}
				
			}
			/***
			if(thaw_amount>0){
				//反向充值
				BizObject bizObjecto = m.newObject();
				bizObjecto.setAttributeValue("USERID", sUserID);//用户编号 投资人
				bizObjecto.setAttributeValue("AMOUNT",thaw_amount);//交易金额
				bizObjecto.setAttributeValue("ACTUALAMOUNT",thaw_amount);//交易金额
				bizObjecto.setAttributeValue("TRANSTYPE", "1010");//交易类型
				bizObjecto.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
				bizObjecto.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
				bizObjecto.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
				bizObjecto.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
				bizObjecto.setAttributeValue("BALANCE",userMap.get("BALANCE"));// 余额
				bizObjecto.setAttributeValue("RELAACCOUNT",userMap.get("RELAACCOUNT"));//关联账户流水号
				bizObjecto.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
				bizObjecto.setAttributeValue("REMARK","自动充值");//备注
				bizObjecto.setAttributeValue("UPDATETIME",sInputDate+" "+sInputTime);//更新时间
				bizObjecto.setAttributeValue("TRANSDATE", sInputDate);//交易日期
				bizObjecto.setAttributeValue("TRANSTIME", sInputTime);//交易时间
				bizObjecto.setAttributeValue("STATUS", "10");//交易状态
				bizObjecto.setAttributeValue("HANDLCHARGE", "0");//手续费
				bizObjecto.setAttributeValue("ReBillNo",billSerialno);
				m.saveObject(bizObjecto);
			}
			***/
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
	}
	
	/**
	 * @param string
	 * @return
	 * @throws HandlerException 
	 */
	private Map<String, Object> getmap(JBOFactory jbo,Map<String, Object> map) throws HandlerException {
		Map<String, Object> resultmap = new HashMap<String, Object>();
		try {
			String userid =(String) map.get("userid");
			String tuserid = (String) map.get("tuserid") ;
			
			BizObjectManager  manager = jbo.getManager("jbo.trade.account_info",tx);
			BizObjectQuery query = manager.createQuery(" select USERID,SERIALNO ,ACCOUNTTYPE  ,  tua.LOCKFLAG  ,tua.USABLEBALANCE  , tua.FROZENBALANCE   from o, jbo.trade.user_account tua    where   userid = tua.userid and userid in (:userid,:tuserid)");
			query.setParameter("userid", (String) map.get("userid"));
			query.setParameter("tuserid",(String) map.get("tuserid"));
			List<BizObject> bizObjectlist =query.getResultList(false);
			if (bizObjectlist!=null) {
					for (int i = 0; i < bizObjectlist.size(); i++) {
						BizObject oo = bizObjectlist.get(i);
						Map<String, Object> userMap = new HashMap<String, Object>();
						if (oo!=null) {
							String suerid = oo.getAttribute("USERID").getValue()==null?"":oo.getAttribute("USERID").getString();
							if (userid.equals(suerid)) {
								double Usablebalance =  Double.parseDouble(oo.getAttribute("USABLEBALANCE").getValue()==null?"0":oo.getAttribute("USABLEBALANCE").getString()) ;
								double Frozenbalance =  Double.parseDouble(oo.getAttribute("FROZENBALANCE").getValue()==null?"0":oo.getAttribute("FROZENBALANCE").getString()) ;
								userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
								userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getValue()==null?"":oo.getAttribute("SERIALNO").getString());//关联账户流水号
								userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getValue()==null?"":oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
								resultmap.put("userid", userMap);
							} else if (tuserid.equals(suerid)) {//投资人
								double Usablebalance =  oo.getAttribute("USABLEBALANCE").getDouble() ;
								double Frozenbalance =  oo.getAttribute("FROZENBALANCE").getDouble() ;
								userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
								userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getString());//关联账户流水号
								userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
								resultmap.put("tuserid", userMap);
							}
					}
				}
			}else {
				throw new HandlerException("account.notexist.error");//未找到该用户绑定的账户
			}
			
				//机构账户
			manager = jbo.getManager("jbo.trade.org_account_info", tx);
			BizObjectQuery bObjectQuery = manager.createQuery(" serialno in (:puseridserialno,:duseridserialno)");
			bObjectQuery.setParameter("puseridserialno",(String) map.get("puseridserialno"));//平台账户orgid
			bObjectQuery.setParameter("duseridserialno",(String) map.get("duseridserialno"));//担保公司账户orgid
			List<BizObject> listorgid = bObjectQuery.getResultList(true);

			if (listorgid != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < listorgid.size(); i++) {
					BizObject oo = listorgid.get(i);
					Double usablebalance = Double.parseDouble(oo.getAttribute("USABLEBALANCE").getValue()==null?"0":oo.getAttribute("USABLEBALANCE").getString());
					String sAccountType = oo.getAttribute("AccountType").getString();
					if ("0103".equals(sAccountType)) {
						//平台账户   平台服务费
						Map<String, Object> userMap = new HashMap<String, Object>();
						Double Usablebalance =  oo.getAttribute("USABLEBALANCE").getDouble() ;
						Double Frozenbalance =  oo.getAttribute("FROZENBALANCE").getDouble() ;
						userMap.put("ORGID",oo.getAttribute("ORGID").getValue()==null?"":oo.getAttribute("ORGID").getString());// 机构
						userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
						userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getValue()==null?"":oo.getAttribute("SERIALNO").getString());//关联账户流水号
						userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getValue()==null?"":oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
						userMap.put("payamt",plantmange+plantfee);//交易金额
						resultmap.put("porgMap", userMap);
					}else if ("0202".equals(sAccountType)) {
						//担保公司账户   担保费
						Map<String, Object> userMap = new HashMap<String, Object>();
						Double Usablebalance =  oo.getAttribute("USABLEBALANCE").getDouble() ;
						Double Frozenbalance =  oo.getAttribute("FROZENBALANCE").getDouble() ;
						userMap.put("ORGID",oo.getAttribute("ORGID").getValue()==null?"":oo.getAttribute("ORGID").getString());// 机构
						userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
						userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getValue()==null?"":oo.getAttribute("SERIALNO").getString());//关联账户流水号
						userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getValue()==null?"":oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
						userMap.put("payamt",insuremanagement_fee+managefee);//交易金额 担保管理费    担保费
						resultmap.put("dorgMap", userMap);
					}				
				}
			}else {
				throw new HandlerException("account.notexist.error");//未找到该用户绑定的账户
			}
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}catch (HandlerException e) {
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
		return resultmap;
	}
	
	
	
	private double tUsablebalance(JBOFactory jbo,String userid) throws HandlerException {
		BizObjectManager manager;
		double tusablebalance =  0;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid=:userid ");
			query.setParameter("userid", userid);
			BizObject bizObject = query.getSingleResult(false);
			if (bizObject!=null) {
				tusablebalance = bizObject.getAttribute("USABLEBALANCE").getValue() == null ? 0 : bizObject.getAttribute("USABLEBALANCE").getDouble();// 虚拟账户可用余额
			}else {
				throw new HandlerException("common.usernotexist");
			}
			return tusablebalance;
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}
	
	private void initTransactionRecord(JSONObject request,JBOFactory jbo) throws HandlerException{
		try{
		    Map<String ,Object> smap = getmap(jbo,map);
		    Map<String, Object> userMap = (Map<String, Object>) smap.get("userid");
			BizObjectManager m = jbo.getManager("jbo.trade.transaction_record",tx);
			BizObject oo = m.newObject();
			oo.setAttributeValue("USERID", map.get("userid"));//用户编号 借款人
			oo.setAttributeValue("AMOUNT",amtDouble);//交易金额
			oo.setAttributeValue("ACTUALAMOUNT",amtDouble);//交易金额
			oo.setAttributeValue("TRANSTYPE", "3".equals(paytypeString)?"1040":"1030");//交易类型
			oo.setAttributeValue("TRANSACTIONSERIALNO",  loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
			oo.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
			oo.setAttributeValue("DIRECTION", "P");// 发生方向  p支出，r收入
			oo.setAttributeValue("BALANCE",userMap.get("BALANCE"));// 余额
			oo.setAttributeValue("RELAACCOUNT",userMap.get("RELAACCOUNT"));//关联账户流水号
			oo.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
			oo.setAttributeValue("REMARK","3".equals(paytypeString)?"提前还款":"手动还款");//备注
			oo.setAttributeValue("INPUTTIME", StringFunction.getTodayNow());//创建日期
			oo.setAttributeValue("UPDATETIME",StringFunction.getTodayNow());//更新时间
			oo.setAttributeValue("TRANSDATE", StringFunction.getToday());//交易日期
			oo.setAttributeValue("TRANSTIME", StringFunction.getNow());//交易时间
			oo.setAttributeValue("STATUS", "10");//交易状态
			oo.setAttributeValue("HANDLCHARGE", "0");//手续费
			oo.setAttributeValue("ReBillNo",billSerialno);
			oo.setAttributeValue("ACTUALEXPIATIONSUM",penal_value);//还款违约金
			oo.setAttributeValue("ACTUALPAYCORPUSAMT",actualPayCorpusAmt);//本金
			oo.setAttributeValue("ACTUALPAYINTEAMT",actualPayInteAmt);//利息
			oo.setAttributeValue("ACTUALFINEAMT",actualPayFineAmt);//罚息
			oo.setAttributeValue("ACTUALCOMPDINTEAMT",actualPayCompoundInte);//复利
			oo.setAttributeValue("ACTUALGUARANTEEDMANAGE",insuremanagement_fee);//担保管理费
			oo.setAttributeValue("ACTUALPAYFEEAMT1",managefee);//担保费
			oo.setAttributeValue("ACTUALPLANTMANAGE",plantmange);//平台管理费
            oo.setAttributeValue("ACTUALPAYFEEAMT2",plantfee);//平台服务费
			m.saveObject(oo);
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		
	}
	
	private  static String addDateSeconds(String sDate,String sTime,int s){
		Calendar calendar = new GregorianCalendar();
		String[] sTimes = sTime.split(":");
		String[] sDates = sDate.split("/");
		int year = Integer.parseInt(sDates[0]);
		int month = Integer.parseInt(sDates[1]);
		int day = Integer.parseInt(sDates[2]);
		calendar.set(year, month-1, day, Integer.parseInt(sTimes[0]), Integer.parseInt(sTimes[1]),
				Integer.parseInt(sTimes[2]));
		calendar.add(Calendar.MILLISECOND, s);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return sdf.format(calendar.getTime());
	}
}
