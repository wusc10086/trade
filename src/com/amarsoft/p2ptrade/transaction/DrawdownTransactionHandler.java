package com.amarsoft.p2ptrade.transaction;

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
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/*
 * @DrawDown  放款
 * 
 */
public class DrawdownTransactionHandler extends TradeHandler {

	private String tuserid;
	private String userid;
	private double tamtString; // 投资人投资金额
	private String proserialno; // 项目编号
	private String loanSerialnoString; // 项目编号
	private String  sContractid;
	private String Month;
	private String Day;
	private double amt;
	private Map<String, Object> map = new HashMap<String, Object>();
	private String pName;
	private String geeName;
	private String userPhoneNo;
	private String tuserPhoneNo;

	@Override
	protected Object requestObject(JSONObject request, JBOFactory jbo) throws HandlerException {
		if(request.get("Serialno")==null||request.get("TUserID")==null||request.get("TAmt")==null){
			throw new HandlerException("request.invalid");
		}
		 
		proserialno = (String)request.get("Serialno");
		tuserid = (String)request.get("TUserID");
		sContractid = getContractid(jbo);
		userid = getuserid(jbo);
		tamtString = Double.parseDouble((String)request.get("TAmt")==null?"0":(String)request.get("TAmt"));// 投资人投资金额
		try {
			
			if(userid==null||userid.length()==0){
				throw new HandlerException("common.usernotexist");//未找到用户
			}
			//用户状态判断  account_freeze
			GeneralTools.userAccountStatus(userid, tuserid) ;//用户状态异常
			//添加校验,必须有绑定的卡才行
			if(checkAccountStatus(userid,jbo)==false){
				throw new HandlerException("borrownobindcard.error");//未绑定卡
			}
			
			//手机号
			userPhoneNo = getPhoneTel(userid, jbo);
			tuserPhoneNo =  getPhoneTel(tuserid, jbo);
			if(userPhoneNo.length()==0||tuserPhoneNo.length()==0){
			  throw new HandlerException("common.emptymobile");//未绑定卡
			}
				
			String lockflagString= "1";
			// 锁定项目
			frozenproject(lockflagString,jbo);
			// 锁 账户
			frozenaccoutn(lockflagString,jbo);
			
			map = getamountanMap(jbo);// 项目金额及 状态
			
			// 查询投资人账户可用金额 足额
			double usbblance = tUsablebalance(jbo);// 投资人账户可用金额  
			if (usbblance < tamtString||tamtString<=0) {
				throw new HandlerException("tusaamtnoenough.error");
			}
			// 项目状态验证
			String status = (String) map.get("Status");
			if (!"1".equals(status)) {   // 状态     1 已上架待投资  2 已下架   
				throw new HandlerException("projectend.error");
			}
			
				// 项目处理
				Updateproloanamt(jbo);
				//投资人金额冻结
				tfrozenbanlance(jbo);
				// 借款人金额冻结
				frozenbanlance(jbo);
				
				//tfrozen(jbo);
				
				lockflagString= "2";
				// 解锁 项目
				unfrozenproject(lockflagString,jbo);
				// 解锁 账户
				unfrozenaccoutn(lockflagString,jbo);
				
				String[] args =  getSereialno().split("@",-1);
				pName = args[0];
				geeName = args[1];
				// 更新合同信息
				UserContract(request, jbo);
				//插入交易记录
				insertTraction(getMapName(jbo),tamtString,this.transserialno, jbo);
				
				request.put("ContractSerialNo", sContractid);
				request.put("Amt",  GeneralTools.numberFormat(tamtString,0,2));
				request.put("Method", "DrawDown");
				request.put("UserID", userid);
				request.put("detailList", setInvestUser(jbo));
				
//				try{
//					tx.commit();
//				}catch(Exception e){
//					tx.rollback();
//					e.printStackTrace();
//				throw new HandlerException("transrun.err");
//				}
//			
			
		} catch (HandlerException e) {
			e.printStackTrace();
			try {
				tx.rollback();  
				e.printStackTrace();
			} catch (JBOException e1) {
			}
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				tx.rollback();
			} catch (Exception e1) {
			}
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return request;
	}

	/*
	 * 返回报文组装
	 */

	@Override
	protected Object responseObject(JSONObject request, JSONObject response,String logid, String transserialno, JBOFactory jbo)
			throws HandlerException {
		try {
			
			String NextPaydate = (String) response.get("NextPaydate");
			amt = Double.parseDouble((String) response.get("amt")==null?"0":(String) response.get("amt"));
			if(NextPaydate.length()>0&&NextPaydate.length()==10){
				String[] split = NextPaydate.split("/",-1);
				Month = split[1];
				Day = split[2];
				
				if(Month.startsWith("0")){
					Month = Month.substring(1);
				}
				
				if(Day.startsWith("0")){
					Day = Day.substring(1);
				}
				
			}
			
			try{
				senmsg(jbo);
			}catch(Exception e){
				e.printStackTrace();
				ARE.getLog().info("短信发送失败借款人，合同号："+sContractid);
				// throw new HandlerException("smsreminder.error");
			}
			try{
				sentmsg(jbo);
			}catch(Exception e){
				e.printStackTrace();
				ARE.getLog().info("短信发送失败投资人，合同号："+sContractid);
			// throw new HandlerException("smsreminder.error");
			}
			
			
		}  catch (Exception e) {
			e.printStackTrace();
			try {
				tx.rollback();  
				e.printStackTrace();
			} catch (JBOException e1) {
				e.printStackTrace();
				throw new HandlerException("transrun.err");
			}
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return response;
	}

	private Map<String, Object> getMapName( JBOFactory jbo) throws HandlerException {
		Map<String, Object> map = new HashMap<String, Object>();
		BizObjectManager manager = null;
		try {
			manager = jbo.getManager("jbo.trade.user_contract",tx);
			BizObjectQuery query = manager.createQuery("CONTRACTNO=:CONTRACTNO ");
			query.setParameter("CONTRACTNO", sContractid);
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
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("common.usernotexist");
		}
		return map;
	}
	
	/**
	 * @param map2
	 * @param tamtString2
	 * @param request
	 * @param response
	 * @param logid
	 * @param transserialno
	 * @param jbo
	 */
	/*
	 * 
	 * 插入交易记录
	 * */
	private  void  insertTraction(Map<String, Object> map,Double payamt,String transserialno,JBOFactory jbo) throws HandlerException {
		loanSerialnoString  = sContractid;
		
		Map<String, Object> tuserMap = new HashMap<String, Object>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String ,Object> smap = getmap(jbo,map);
		tuserMap = (Map<String, Object>) smap.get("tuserid");
		userMap = (Map<String, Object>) smap.get("userid");
		BizObjectManager m =null;
		String sInputDate =GeneralTools.getDate();
		String sInputTime =GeneralTools.getTime();
		try {
			 m = jbo.getManager("jbo.trade.transaction_record",tx);
			BizObject o = m.newObject();
			o.setAttributeValue("USERID", tuserid);//用户编号 投资人
			o.setAttributeValue("AMOUNT",payamt);//交易金额
			o.setAttributeValue("ACTUALAMOUNT",payamt);//交易金额
			o.setAttributeValue("TRANSTYPE", "1061");//交易类型
			o.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
			o.setAttributeValue("TRANSACTIONSERIALNO", loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
			o.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
			o.setAttributeValue("DIRECTION", "P");// 发生方向  p支出，r收入
			o.setAttributeValue("BALANCE",tuserMap.get("BALANCE"));// 余额
			o.setAttributeValue("RELAACCOUNT",tuserMap.get("RELAACCOUNT"));//关联账户流水号
			o.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
			o.setAttributeValue("REMARK","投资");//备注
			o.setAttributeValue("UPDATETIME",sInputDate+" "+sInputTime);//更新时间
			o.setAttributeValue("TRANSDATE", sInputDate);//交易日期
			o.setAttributeValue("TRANSTIME", sInputTime);//交易时间
			o.setAttributeValue("STATUS", "10");//交易状态
			o.setAttributeValue("HANDLCHARGE", "0");//手续费
			m.saveObject(o);
			
			BizObject oo = m.newObject();
			oo.setAttributeValue("USERID", userid);//用户编号 借款人
			oo.setAttributeValue("AMOUNT",payamt);//交易金额
			oo.setAttributeValue("ACTUALAMOUNT",payamt);//交易金额
			oo.setAttributeValue("TRANSTYPE", "1060");//交易类型
			oo.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
			oo.setAttributeValue("TRANSACTIONSERIALNO",  loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
			oo.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
			oo.setAttributeValue("DIRECTION", "R");// 发生方向  p支出，r收入
			oo.setAttributeValue("BALANCE",userMap.get("BALANCE"));// 余额
			oo.setAttributeValue("RELAACCOUNT",userMap.get("RELAACCOUNT"));//关联账户流水号
			oo.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
			oo.setAttributeValue("REMARK","借款放款");//备注
			oo.setAttributeValue("UPDATETIME",sInputDate+" "+sInputTime);//更新时间
			oo.setAttributeValue("TRANSDATE", sInputDate);//交易日期
			oo.setAttributeValue("TRANSTIME", sInputTime);//交易时间
			oo.setAttributeValue("STATUS", "10");//交易状态
			oo.setAttributeValue("HANDLCHARGE", "0");//手续费
			m.saveObject(oo);
			
			BizObject ooo = m.newObject();
			ooo.setAttributeValue("USERID", userid);//用户编号 借款人
			ooo.setAttributeValue("AMOUNT",payamt);//交易金额
			ooo.setAttributeValue("ACTUALAMOUNT",payamt);//交易金额
			ooo.setAttributeValue("TRANSTYPE", "1020");//交易类型
			ooo.setAttributeValue("INPUTTIME", sInputDate+" "+sInputTime);//创建日期
			ooo.setAttributeValue("TRANSACTIONSERIALNO",  loanSerialnoString+"@"+this.transserialno);//核算表交易流水号
			ooo.setAttributeValue("TRANSLOGID", this.logidString);//交易日志表logid
			ooo.setAttributeValue("DIRECTION", "P");// 发生方向  p支出，r收入
			ooo.setAttributeValue("BALANCE",userMap.get("BALANCE"));// 余额
			ooo.setAttributeValue("RELAACCOUNT",userMap.get("RELAACCOUNT"));//关联账户流水号
			ooo.setAttributeValue("RELAACCOUNTTYPE","001");//交易关联账户类型 （用户账户/机构账户）
			ooo.setAttributeValue("REMARK","放款自动提现");//备注
			ooo.setAttributeValue("Transchannel","1010");
			ooo.setAttributeValue("UPDATETIME",sInputDate+" "+sInputTime);//更新时间
			ooo.setAttributeValue("TRANSDATE", sInputDate);//交易日期
			ooo.setAttributeValue("TRANSTIME", sInputTime);//交易时间
			ooo.setAttributeValue("STATUS", "01");//交易状态
			ooo.setAttributeValue("HANDLCHARGE", "0");//手续费
			m.saveObject(ooo);
			
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
			String tuserid = (String) map.get("tuserid");
			BizObjectManager  manager = jbo.getManager("jbo.trade.account_info",tx);
			BizObjectQuery query = manager.createQuery(" select USERID,SERIALNO ,ACCOUNTTYPE  ,  tua.LOCKFLAG  ,tua.USABLEBALANCE  , tua.FROZENBALANCE   from o, jbo.trade.user_account tua    where   userid = tua.userid and userid in (:userid,:tuserid) and status='2'");
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
								double Usablebalance =  oo.getAttribute("USABLEBALANCE").getDouble() ;
								double Frozenbalance =  oo.getAttribute("FROZENBALANCE").getDouble() ;
								userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
								userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getValue()==null?"":oo.getAttribute("SERIALNO").getString());//关联账户流水号
								userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getValue()==null?"":oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
								resultmap.put("userid", userMap);
							} else if (tuserid.equals(suerid)) {
								double Usablebalance =  oo.getAttribute("USABLEBALANCE").getDouble() ;
								double Frozenbalance =  oo.getAttribute("FROZENBALANCE").getDouble() ;
								userMap.put("BALANCE",Usablebalance+Frozenbalance);// 余额
								userMap.put("RELAACCOUNT",oo.getAttribute("SERIALNO").getValue()==null?"":oo.getAttribute("SERIALNO").getString());//关联账户流水号
								userMap.put("RELAACCOUNTTYPE",oo.getAttribute("ACCOUNTTYPE").getValue()==null?"":oo.getAttribute("ACCOUNTTYPE").getString());//交易关联账户类型 （用户账户/机构账户）
								resultmap.put("tuserid", userMap);
							}
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
			throw new HandlerException("account.notexist.error");//未找到该用户绑定的账户
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
		return resultmap;
	}
	/**
	 * @param jbo
	 * @param request
	 * @param tx
	 * @throws HandlerException
	 */
	private void frozenproject(String lockflagString ,JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno and status='1' ");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo != null) {
				// 锁定项目
				
				//取开始结束时间比较是否可以进行投资
				String sEndDate = bo.getAttribute("EndDate").getValue()==null?"":bo.getAttribute("EndDate").getString();
				String sEndTime = bo.getAttribute("EndTime").getValue()==null?"":bo.getAttribute("EndTime").getString();
				String sBeginDate = bo.getAttribute("BeginDate").getValue()==null?"":bo.getAttribute("BeginDate").getString();
				String sBeginTime = bo.getAttribute("BeginTime").getValue()==null?"":bo.getAttribute("BeginTime").getString();
				//当前时间
				Calendar calendar = getCalendar(StringFunction.getToday(),StringFunction.getNow());
				//开始时间
				Calendar calendar1 =  getCalendar(sBeginDate,sBeginTime);
				//比当前时间大不能投资
				if(calendar.getTimeInMillis() < calendar1.getTimeInMillis()){
					throw new HandlerException("timeunlate.error");
				}
				if("".equals(sEndDate)||null==sEndDate||sEndDate.length()==0||"".equals(sEndTime)||null==sEndTime||sEndTime.length()==0){
					
				}else{
					//结束时间
					//比当前时间小不能投资
					Calendar calendar2 =  getCalendar(sEndDate,sEndTime);
					if(calendar.getTimeInMillis() > calendar2.getTimeInMillis()){
						throw new HandlerException("timelate.error");
					}
				}
				 bo.setAttributeValue("LOCKFLAG", lockflagString);
				manager.saveObject(bo);
			} else {
				throw new HandlerException("common.projectnotexist");
			}

		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}

	//时间
	private Calendar getCalendar(String sDate,String sTime){
		Calendar calendar = new GregorianCalendar();
		String[] sTimes = sTime.split(":");
		String[] sDates = sDate.split("/");
		int year = Integer.parseInt(sDates[0]);
		int month = Integer.parseInt(sDates[1]);
		int day = Integer.parseInt(sDates[2]);
		calendar.set(year, month, day, Integer.parseInt(sTimes[0]), Integer.parseInt(sTimes[1]),
				Integer.parseInt(sTimes[2]));
		return calendar;
	}
	
	
	/**
	 * 解锁项目
	 * 
	 * @param jbo
	 * @param tx
	 * @throws HandlerException
	 */
	private void unfrozenproject(String lockflagString,JBOFactory jbo) throws HandlerException {

		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo!=null) {
				// 解锁项目
				bo.setAttributeValue("LOCKFLAG", lockflagString);
				manager.saveObject(bo);
			}else {
				throw new HandlerException("common.projectnotexistt");
			}
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

	/**
	 * @param jbo
	 * @param request
	 * @return * 投资人账户 可用金额
	 */

	private double tUsablebalance(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		double tusablebalance =  0;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid=:userid ");
			query.setParameter("userid", tuserid);
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

	/**
	 * 项目处理 资金 状态 变更
	 * 
	 * @param tlDouble
	 *            投资剩余金额
	 * @param jbo
	 * @throws JBOException
	 */
	private void Updateproloanamt(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		Double sLoanamount = (Double) map.get("Loanamount"); // 项目金额
		Double tlDouble = sLoanamount - tamtString;// 可投资剩余项目金额
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo!=null) {
				if (tlDouble > 0) {
					// 更新项目可投资金额
					bo.setAttributeValue("LOANAMOUNT", tlDouble);
					bo.setAttributeValue("OPERATETIME",StringFunction.getNow());
				} else  if (GeneralTools.round(tlDouble, 2) == 0) {
					// 项目下架
					bo.setAttributeValue("STATUS", "3"); // 项目状态 下架
					bo.setAttributeValue("ENDDATE", StringFunction.getToday());
					bo.setAttributeValue("ENDTIME", StringFunction.getNow());
					bo.setAttributeValue("ENDUSER", "system");
					bo.setAttributeValue("ENDREASON", " 该项目没有可投金额,投资成功");
					bo.setAttributeValue("DEALTIME", StringFunction.getToday() + " "+ StringFunction.getNow());
					bo.setAttributeValue("OPERATETIME", StringFunction.getToday() + " "+ StringFunction.getNow());
				}
		    	manager.saveObject(bo);
			}else {
				throw new HandlerException("common.projectnotexist");
			}
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

	/*
	 * 投资人账户 冻结金额
	 */
	private void tfrozenbanlance(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid=:userid");
			query.setParameter("userid", tuserid);
			BizObject bizObject = query.getSingleResult(true);
			if (bizObject!=null) {
				double tusablebalance = Double.parseDouble(bizObject.getAttribute(
						"USABLEBALANCE").getValue()==null?"0":bizObject.getAttribute("USABLEBALANCE").getString());// 虚拟账户可用余额
				//double frozenbalance =Double.parseDouble(bizObject .getAttribute("frozenbalance").getValue()==null?"0":bizObject.getAttribute("frozenbalance").getString());//虚拟账户可用余额
				double tusaamt = tusablebalance - tamtString;// 调减 投资人可用金额
				bizObject.setAttributeValue("USABLEBALANCE", GeneralTools.numberFormat(GeneralTools.round(tusaamt,2), 0, 2));
				//bizObject.setAttributeValue("frozenbalance", GeneralTools.numberFormat(GeneralTools.round(frozenbalance+tusaamt,2), 0, 2));
				manager.saveObject(bizObject);
			}else {
				throw new HandlerException("common.usernotexist");
			}
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


	// 锁定 投资人借款人 account
	private void frozenaccoutn(String lockflagString,JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager
					.createQuery("userid  in ( :tuserid, :userid)");
			query.setParameter("tuserid", tuserid);
			query.setParameter("userid", userid);
			List<BizObject> list = query.getResultList(true);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					if (o!=null) {
						o.setAttributeValue("LOCKFLAG", lockflagString);
						manager.saveObject(o);
					}else {
						throw new HandlerException("common.usernotexist");
					}
				}
			}
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("common.usernotexist");//未找到该用户绑定的账户
		}
	}

	// 解锁 投资人借款人 account
	private void unfrozenaccoutn(String lockflagString,JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager
					.createQuery("userid  in ( :tuserid, :userid)");
			query.setParameter("tuserid", tuserid);
			query.setParameter("userid", userid);

			List<BizObject> list = query.getResultList(true);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					if (o!=null) {
						o.setAttributeValue("LOCKFLAG",lockflagString);
						manager.saveObject(o);
					}else {
						throw new HandlerException("common.usernotexist");
					}
				}
			}
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

	/*
	 * 借款人账户冻结金额
	 */

	private void frozenbanlance(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid =:userid");
			query.setParameter("userid", userid);

			BizObject bizObject = query.getSingleResult(true); // 借款人对象
			if (bizObject!=null) {
				double frozenbalance = Double.parseDouble(bizObject.getAttribute(
					"FROZENBALANCE").getValue()==null?"0":bizObject.getAttribute("FROZENBALANCE").getString());// 虚拟账户冻结金额

				double frozenamt = frozenbalance + tamtString; // 借款人 冻结金额 冻结
			bizObject.setAttributeValue("FROZENBALANCE", GeneralTools.round(frozenamt, 2));
			manager.saveObject(bizObject);
			}else {
				throw new HandlerException("common.usernotexist");
			}
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

	/*
	 * 项目金额 状态 验证
	 */
	private Map<String, Object> getamountanMap(JBOFactory jbo) throws HandlerException {
		Map<String, Object> map = new HashMap<String, Object>();
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo!=null) {
				// 查询项目可投资金额 状态
				Double sLoanamount = Double.parseDouble(bo.getAttribute(
						"LOANAMOUNT").getValue()==null?"0":bo.getAttribute("LOANAMOUNT").getString());
				String sStatus = bo.getAttribute("STATUS").getValue()==null?"":bo.getAttribute("STATUS").getString();
				map.put("Loanamount", sLoanamount);
				map.put("Status", sStatus);
			}else {
				throw new HandlerException("common.projectnotexist");
			}
			return map;
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("common.projectnotexist");//未找到该用户绑定的账户
		}
	}

	//校验帐户是否正常
	private boolean checkAccountStatus(String userid,JBOFactory jbo) throws HandlerException {
		Map<String, Object> map = new HashMap<String, Object>();
		BizObjectManager manager;
		boolean isok = false;
		try {
			manager = jbo.getManager("jbo.trade.account_info", tx);
			BizObjectQuery query = manager.createQuery("userid =:userid and status='2'");
			query.setParameter("userid", userid);
			List<BizObject> list = query.getResultList(false);
			if (list!=null) {
				int n=0;
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					String suserid = o.getAttribute("userid").getString();
					if (userid.equals(suserid)) {
						n ++;//借款人
					} else{
						isok = false;
					}
				}
				if(n<=0||n>1) {
					isok = false;
				}else{
					isok = true;
				}
			}else {
				isok = false;
			}
			return isok;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}
	
	/*
	 * 获取合同号
	 */
	private String getContractid(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		String sContractid = "";
		try {
			manager = jbo.getManager("jbo.trade.project_info");
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(false);
			if (bo!=null) {
				sContractid = bo.getAttribute("CONTRACTID").getValue()==null?"":bo.getAttribute("CONTRACTID").getString();
			}else {
				throw new HandlerException("common.projectnotexist");
			}
			return sContractid;
		} catch (HandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}
	
	/*
	 * 借款人
	 */
	private String getuserid(JBOFactory jbo) throws HandlerException{
		BizObjectManager manager;
		String sUserID;
		try {
			manager = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query = manager.createQuery(" RELATIVETYPE='001' AND  CONTRACTID=:CONTRACTID");
			query.setParameter("CONTRACTID", sContractid);
			BizObject bo = query.getSingleResult(false);
			if (bo!=null) {
				sUserID = bo.getAttribute("USERID").getValue()==null?"":bo.getAttribute("USERID").getString();
			}else {
				throw new HandlerException("common.contractnotexist");
			}
			return sUserID;
		} catch (HandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}

	/**
	 * @param string
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unused")
	private Map<String, Object> getmap(JBOFactory jbo, String useid) throws HandlerException {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			BizObjectManager manager = jbo
					.getManager("jbo.trade.account_info");
			BizObjectQuery query = manager
					.createQuery(" select  SERIALNO,ACCOUNTTYPE ,  tua.LOCKFLAG  ,tua.USABLEBALANCE  , tua.FROZENBALANCE   "
							+ "  from o, jbo.trade.user_account tua  where status='2' and userid = tua.userid and userid =:userid");
			query.setParameter("userid", useid);
			BizObject bizObject = query.getSingleResult(false);
			if (bizObject != null) {
					double Usablebalance = Double.parseDouble(bizObject
							.getAttribute("USABLEBALANCE").getValue()==null?"0":bizObject.getAttribute("USABLEBALANCE").getString());
					double Frozenbalance = Double.parseDouble(bizObject
							.getAttribute("FROZENBALANCE").getValue()==null?"0":bizObject.getAttribute("FROZENBALANCE").getString());
					map.put("BALANCE", GeneralTools.round(Usablebalance + Frozenbalance, 2));// 余额
					map.put("RELAACCOUNT", bizObject.getAttribute("SERIALNO")
							.getValue()==null?"":bizObject.getAttribute("SERIALNO").getString());// 关联账户流水号
					map.put("RELAACCOUNTTYPE",
							bizObject.getAttribute("ACCOUNTTYPE").getValue()==null?"":bizObject.getAttribute("ACCOUNTTYPE").getString());// 交易关联账户类型
																				// （用户账户/机构账户）
			} else {
				throw new HandlerException("account.notexist.error");// 未找到该用户绑定的账户
			}
		} catch (HandlerException e) {
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return map;
	}

	// 更新合同信息
	private void UserContract(JSONObject request, JBOFactory jbo) throws HandlerException {
		try {
			// 更新 借款人 投资人
			BizObjectManager manager = jbo.getManager("jbo.trade.user_contract",tx);
			BizObject o = manager.newObject();
			o.setAttributeValue("CONTRACTID", sContractid);
			o.setAttributeValue("CONTRACTNO", sContractid);
			o.setAttributeValue("INPUTTIME", StringFunction.getToday());
			o.setAttributeValue("UPDATETIME", StringFunction.getToday());
			o.setAttributeValue("USERID", tuserid);
			o.setAttributeValue("RELATIVETYPE", "002");// codeno='UserRelativeType'
			manager.saveObject(o);
			
			 manager = jbo.getManager("jbo.trade.ti_contract_info",tx);
			BizObject bizObject = manager.newObject();
			bizObject.setAttributeValue("CONTRACTID", sContractid); // p2p 合同号号
			bizObject.setAttributeValue("CONTRACTTYPE", "");// 合同类型
			bizObject.setAttributeValue("LOANNO", sContractid);// 信贷 借据号
			bizObject.setAttributeValue("CONTRACTNO", sContractid);// 信贷合同号
			bizObject.setAttributeValue("STATUS", "100");// 合同状态	Code:ContractStatus
			bizObject.setAttributeValue("INPUTTIME",  StringFunction.getToday());
			bizObject.setAttributeValue("UPDATETIME", StringFunction.getToday());
			bizObject.setAttributeValue("GUARANTEECOMPANY", geeName);// 担保公司
			manager.saveObject(bizObject);

		}  catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
	}
	
	private void tfrozen(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid  = :tuserid");
			query.setParameter("tuserid", tuserid);
			BizObject tbizObject = query.getSingleResult(true); // 投资人对象
			if (tbizObject!=null) {
				Double tfrozenbalance = Double.parseDouble(tbizObject.getAttribute("FROZENBALANCE").toString());// 投资人虚拟账户冻结金额
				Double tfrozenamt = tfrozenbalance - tamtString; // 投机人冻结金额 解冻
				tbizObject.setAttributeValue("FROZENBALANCE", tfrozenamt.toString());
				manager.saveObject(tbizObject);
			}else {
				throw new HandlerException("common.usernotexist");
			}
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");//未找到该用户绑定的账户
		}
	}

	
	private String getSereialno() throws HandlerException{
		String sProjectName ="";
		String sgeeName = "";
		BizObjectManager m;
		JBOFactory jbo =JBOFactory.getFactory();
		try {
			m = jbo.getManager("jbo.trade.project_info");
			BizObjectQuery query= m.createQuery(" SERIALNO=:SERIALNO");
			query.setParameter("SERIALNO",proserialno);
			BizObject o = query.getSingleResult(false);
			sProjectName=o.getAttribute("PROJECTNAME").getValue()==null?"":o.getAttribute("PROJECTNAME").getString();
			sgeeName=o.getAttribute("GRANANTEE").getValue()==null?"":o.getAttribute("GRANANTEE").getString();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
		return sProjectName+"@"+sgeeName;
	}
	
	/**
	 *  @throws HandlerException 
	 * 
	 */
	private void senmsg(JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
	// 发送短信提醒  借款人
	 GeneralTools.sendSMS("P2P_FKCG", userPhoneNo, setHashMap());
	}
	
	private void sentmsg(JBOFactory jbo) throws HandlerException {
		// TODO Auto-generated method stub
	// 发送短信提醒  借款人
	 GeneralTools.sendSMS("P2P_TZCG", tuserPhoneNo, settHashMap());
	}
	
	private JSONArray setInvestUser(JBOFactory jbo){
		//investOrgID  investBusiessSum  loanBusiessSum
		JSONArray ar = new JSONArray();
		JSONObject J = new JSONObject();
		J.put("investOrgID", tuserid);
		J.put("investBusiessSum", tamtString);
		J.put("loanBusiessSum", tamtString);
		ar.add(J);
		return ar;
	}
	
	/**
	 * @param jbo
	 * @return 应还 实还 金额
	 * @throws HandlerException 
	 */
	private HashMap<String, Object> setHashMap() throws HandlerException {
		// TODO Auto-generated method stub
		HashMap<String , Object> map1   = new HashMap<String, Object>();
		map1.put("PayAmount", GeneralTools.numberFormat(GeneralTools.round(amt,2),0,2));
		map1.put("ContractNo", sContractid);
		map1.put("Month", Month);
		map1.put("Day", Day);
		return map1;
	}
	
	private HashMap<String, Object> settHashMap() throws HandlerException {
		// TODO Auto-generated method stub
		HashMap<String , Object> map1   = new HashMap<String, Object>();
		map1.put("ProjectName", pName);
		map1.put("Balance", GeneralTools.numberFormat(GeneralTools.round(tamtString,2),0,2));
		return map1;
	}
}
