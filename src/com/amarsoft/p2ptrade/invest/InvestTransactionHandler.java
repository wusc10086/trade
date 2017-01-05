package com.amarsoft.p2ptrade.invest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

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
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
import com.amarsoft.p2ptrade.project.ProjectdetailHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;
import com.amarsoft.utils.jbo.JBOHelper;

/*
 * @DrawDown  投资 交易
 * 
 */
@SuppressWarnings("unchecked")
public class InvestTransactionHandler  extends JSONHandler{

	protected String investuser;//投资用户
	protected String loanuser;//借款用户
	protected double tamtString; // 投资人投资金额
	protected String proserialno; // 项目编号
	protected String  sContractid;//合同编号
	protected double  remainamount;//可投金额

	protected double tusaamt;
	protected double tlDouble;
	protected JBOTransaction tx = null;
	
	private String sSUBCONTRACTNO = "";
	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		return tradeCenter(request,arg1);
	}

	protected Object tradeCenter(JSONObject request,Properties arg1) throws HandlerException {
		JSONObject  result = new JSONObject();
		
		JBOFactory jbo = JBOFactory.getFactory();
		
		try {
			//交易检查
			beforeTrans(request, jbo);			
			//投资交易操作
			runTrans(request, jbo);
			result.put("chk", "S");				
		
		} catch (HandlerException e) {
			throw e;
		} 	catch (Exception e) {
			e.printStackTrace();
		} 				
		return result;
	}
	protected void beforeTrans(JSONObject request, JBOFactory jbo)throws HandlerException {
		if(request.get("Serialno")==null||request.get("TUserID")==null||request.get("TAmt")==null){
			throw new HandlerException("request.invalid");
		}
		proserialno = (String)request.get("Serialno");//项目编号
		investuser = (String)request.get("TUserID");//投资用户
		sContractid = getContractid(jbo);//借款合同
		loanuser = geinvestuser(jbo);//借款用户
		tamtString = Double.parseDouble((String)request.get("TAmt")==null?"0":(String)request.get("TAmt"));// 投资人投资金额
		if(loanuser==null||loanuser.length()==0){
			throw new HandlerException("common.usernotexist");//未找到借款用户
		}
		//用户状态判断  ACCOUNT_FREEZE
		GeneralTools.userAccountStatus(loanuser, investuser) ;//用户状态异常
		//添加校验,必须有绑定的卡才行
		if(checkAccountStatus()==false){
			throw new HandlerException("borrownobindcard.error");//未绑定卡
		}
		//检查项目和投资金额是否合法
		checkInvestSum(request,jbo);
		
	}


	protected Object runTrans(JSONObject request, JBOFactory jbo) throws HandlerException {
		
		
		try {
			tx  = jbo.createTransaction();
//			String lockflagString= "1";
//			frozenproject(lockflagString,jbo);
//			// 锁 账户
//			frozenaccoutn(lockflagString,jbo);
//			map = getamountanMap(jbo);// 项目金额及 状态
//			
//			// 项目状态验证
//			String status = (String) map.get("Status");
//			if (!"1".equals(status)) {   // 状态     1 已上架待投资  2 已下架   
//				throw new HandlerException("projectend.error");
//			}
			// 项目处理
			Updateproloanamt(jbo);
			//投资人金额冻结
			tfrozenbanlance(jbo);
//			lockflagString= "2";
//			// 解锁 项目
//			unfrozenproject(lockflagString,jbo);
//			// 解锁 账户
//			unfrozenaccoutn(lockflagString,jbo);
//			String[] args =  getSereialno().split("@",-1);
//			pName = args[0];
//			geeName = args[1];

			// 插入投资合同信息
			UserContract(request, jbo);
			//判断是否添加邀请返利
			isTransfer();
			
			
			
			tx.commit();
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

	
	/**锁定项目
	 * @param jbo
	 * @param request
	 * @param tx
	 * @throws HandlerException
	 */
	private void frozenproject(String lockflagString ,JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("update o set LOCKFLAG=:flag where serialno=:serialno and status='1' ");
			query.setParameter("serialno", proserialno);
			query.setParameter("flag", lockflagString);
			query.executeUpdate();
		}
		 catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");
		}
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
	 * 项目处理 资金 状态 变更
	 * 
	 * @param tlDouble
	 *            投资剩余金额
	 * @param jbo
	 * @throws JBOException
	 */
	private void Updateproloanamt(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;

		tlDouble = remainamount - tamtString;// 可投资剩余项目金额
		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo!=null) {
				if (tlDouble > 0) {
					// 更新项目可投资金额
					bo.setAttributeValue("remainamount", tlDouble);
					bo.setAttributeValue("OPERATETIME",StringFunction.getNow());
				} else  if (GeneralTools.round(tlDouble, 2) == 0) {
					// 项目已满表
					bo.setAttributeValue("remainamount", 0);
					bo.setAttributeValue("STATUS", "104"); // 项目状态 已满表
					bo.setAttributeValue("ENDUSER", "system");
					bo.setAttributeValue("ENDREASON", " 该项目没有可投金额,投资成功");
					bo.setAttributeValue("ENDDATE", StringFunction.getToday());
					bo.setAttributeValue("ENDTIME", StringFunction.getNow());
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
			query.setParameter("userid", investuser);
			BizObject bizObject = query.getSingleResult(true);
			if (bizObject!=null) {
//				ObjectBalanceUtils.freezeObjectBalance(investuser, ObjectConstants.OBJECT_TYPE_001,jboHelper, tusaamt, jsonObject.toJSONString());
			/*	double tusablebalance = Double.parseDouble(bizObject.getAttribute(
						"USABLEBALANCE").getValue()==null?"0":bizObject.getAttribute("USABLEBALANCE").getString());// 虚拟账户可用余额
				double frozenbalance =Double.parseDouble(bizObject .getAttribute("frozenbalance").getValue()==null?"0":bizObject.getAttribute("frozenbalance").getString());//虚拟账户可用余额
				tusaamt = tusablebalance - tamtString;// 调减 投资人可用金额
				bizObject.setAttributeValue("USABLEBALANCE", GeneralTools.numberFormat(GeneralTools.round(tusaamt,2), 0, 2));
				bizObject.setAttributeValue("frozenbalance", GeneralTools.numberFormat(GeneralTools.round(frozenbalance+tamtString,2), 0, 2));
				manager.saveObject(bizObject);
				*/
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
					.createQuery("userid  in ( :investuser, :userid)");
			query.setParameter("investuser", investuser);
			query.setParameter("userid", loanuser);
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
					.createQuery("userid  in ( :investuser, :userid)");
			query.setParameter("investuser", investuser);
			query.setParameter("userid", loanuser);

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


	/**
	 * 项目处理 资金 状态 回滚
	 * 
	 * @param tlDouble
	 *            投资剩余金额
	 * @param jbo
	 * @throws JBOException
	 */
	private void UpdateProject(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;

		try {
			manager = jbo.getManager("jbo.trade.project_info", tx);
			BizObjectQuery query = manager.createQuery("serialno=:serialno");
			query.setParameter("serialno", proserialno);
			BizObject bo = query.getSingleResult(true);
			if (bo!=null) {
				bo.setAttributeValue("remainamount", tamtString);
				bo.setAttributeValue("STATUS", "1"); // 项目状态 恢复
				bo.setAttributeValue("ENDUSER", "system");
				bo.setAttributeValue("ENDREASON", " 最后一笔投资失败，恢复");
				bo.setAttributeValue("DEALTIME", StringFunction.getToday() + " "+ StringFunction.getNow());
				bo.setAttributeValue("OPERATETIME", StringFunction.getToday() + " "+ StringFunction.getNow());
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
	 * 投资人账户金额解冻恢复
	 */
	private void frozenbanlance(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		try {
			manager = jbo.getManager("jbo.trade.user_account", tx);
			BizObjectQuery query = manager.createQuery("userid=:userid");
			query.setParameter("userid", investuser);
			BizObject bizObject = query.getSingleResult(true);
			if (bizObject!=null) {
				double tusablebalance = Double.parseDouble(bizObject.getAttribute(
						"USABLEBALANCE").getValue()==null?"0":bizObject.getAttribute("USABLEBALANCE").getString());// 虚拟账户可用余额
				double frozenbalance =Double.parseDouble(bizObject .getAttribute("frozenbalance").getValue()==null?"0":bizObject.getAttribute("frozenbalance").getString());//虚拟账户可用余额
				tusaamt = tusablebalance + tamtString;// 恢复 投资人可用金额
				bizObject.setAttributeValue("USABLEBALANCE", GeneralTools.numberFormat(GeneralTools.round(tusaamt,2), 0, 2));
				bizObject.setAttributeValue("frozenbalance", GeneralTools.numberFormat(GeneralTools.round(frozenbalance-tamtString,2), 0, 2));
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

	
	//校验帐户是否正常 判断借款用户是否已经绑定还款卡
	private boolean checkAccountStatus() throws HandlerException {

		JBOFactory jbo = JBOFactory.getFactory();
		BizObjectManager manager;
		boolean isok = false;
		try {
			manager = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = manager.createQuery("userid =:userid and status='2' ");//and ISRETURNCARD='1'");
			query.setParameter("userid", loanuser);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				isok = true;
			}
			return isok;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");
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
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}
	
	/*
	 * 获取预签合同号
	 */
	private String getPreContractid(JBOFactory jbo) throws HandlerException {
		BizObjectManager manager;
		String precontractno = "";
		try {
			manager = jbo.getManager("jbo.trade.loan_apply");
			BizObjectQuery query = manager.createQuery(" select o.precontractno from o,jbo.trade.business_contract bc where o.precontractno=bc.serialno and bc.serialno=:serialno");
			query.setParameter("serialno", sContractid);
			BizObject bo = query.getSingleResult(false);
			if (bo!=null) {
				precontractno = bo.getAttribute("precontractno").getValue()==null?"":bo.getAttribute("precontractno").getString();
			}else {
				throw new HandlerException("common.projectnotexist");
			}
			return precontractno;
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.error");
		}
	}
	
	/*
	 * 借款人
	 */
	private String geinvestuser(JBOFactory jbo) throws HandlerException{
		BizObjectManager manager;
		String sUserID;
		try {
			manager = jbo.getManager("jbo.trade.business_contract");
			BizObjectQuery query = manager.createQuery(" SERIALNO=:SERIALNO");
			query.setParameter("SERIALNO", sContractid);
			BizObject bo = query.getSingleResult(false);
			if (bo!=null) {
				sUserID = bo.getAttribute("CUSTOMERID").getValue()==null?"":bo.getAttribute("CUSTOMERID").getString();
			}else {
				throw new HandlerException("common.contractnotexist");
			}
			return sUserID;
		} catch (HandlerException e) {
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("transrun.err");//未找到该用户绑定的账户
		}
	}


	// 插入投资人合同信息
	private void UserContract(JSONObject request, JBOFactory jbo) throws HandlerException {
		Connection conn = null;
		try {
			// 插入 投资人合同
			BizObjectManager manager = jbo.getManager("jbo.trade.user_contract",tx);
			BizObjectQuery query = manager.createQuery("userid=:userid and contractid=:contractid")
			.setParameter("userid", investuser)
			.setParameter("contractid", sContractid);
			BizObject o = query.getSingleResult(true);
			if(o==null){
				o = manager.newObject();
				//SUBCONTRACTNO
				//预签合同
				conn = ARE.getDBConnection(ARE.getProperty("dbName"));
				String precontractno =  sContractid;//getPreContractid(jbo);
				sSUBCONTRACTNO = P2pString.getSerialNo("user_contract", "subcontractno", precontractno, conn);
				o.setAttributeValue("SUBCONTRACTNO", sSUBCONTRACTNO);
				o.setAttributeValue("CONTRACTID", sContractid);
				o.setAttributeValue("CONTRACTNO", sContractid);
				o.setAttributeValue("INPUTTIME", StringFunction.getToday()+" "+StringFunction.getNow());
				o.setAttributeValue("UPDATETIME", StringFunction.getToday()+" "+StringFunction.getNow());
				o.setAttributeValue("USERID", investuser);
				o.setAttributeValue("projectid", proserialno);
				o.setAttributeValue("investsum", tamtString);
				o.setAttributeValue("LastInvestSum",tamtString);
				o.setAttributeValue("status", "0");
				o.setAttributeValue("RELATIVETYPE", "002");// codeno='UserRelativeType'
				manager.saveObject(o);
				
			
				
			}else{
				double investsum = Double.parseDouble(o.getAttribute("investsum")==null?"0":o.getAttribute("investsum").toString());
				o.setAttributeValue("UPDATETIME", StringFunction.getToday()+" "+StringFunction.getNow());
				o.setAttributeValue("LastInvestSum",tamtString);
				o.setAttributeValue("investsum", (investsum + tamtString));
				manager.saveObject(o);
			}
			
			
			JBOHelper jboHelper = new JBOHelper(tx);

			JSONObject jsonObject = new JSONObject();
			String transCode = "7001";
			jsonObject.put("REMARK", "资金冻结处理");
			jsonObject.put("PROJECTNO", proserialno);
			jsonObject.put("SERIALNO", o.getAttribute("SUBCONTRACTNO").getValue());
			jsonObject.put("USERID", investuser);
			jsonObject.put("USERACCOUNTTYPE", ObjectConstants.ACCOUNT_TYPE_001);
			jsonObject.put("AMOUNT", tamtString);
			jsonObject.put("TRANSCHANNEL", "3010");
			jsonObject.put("TRANSTYPE", transCode);
			
			RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation("3010", transCode);
			rttt.init(jsonObject);
			rttt.execute();
			
			if(!rttt.getTemplet().isSuccess())
			{
				throw new HandlerException("transrun.err");
			}

			
		}  catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("transrun.error");
		}finally{
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	// 检查投资金额是否合法
	private void checkInvestSum(JSONObject request, JBOFactory jbo) throws HandlerException {
		request.put("UserID", investuser);
		//获取项目信息
		ProjectdetailHandler pd = new ProjectdetailHandler();
		JSONObject rp  = (JSONObject)pd.createResponse(request, null);
		remainamount = Double.parseDouble(rp.get("remainamount").toString());
		//检查时间是否有效
		if((rp.get("BeginDate").toString() +  " " + rp.get("BeginTime").toString()).compareToIgnoreCase(StringFunction.getTodayNow())>0){
			throw new HandlerException("invest.project.error");
		}
		//判断项目状态
		if(!"1".equals(rp.get("Status").toString())){
			throw new HandlerException("invest.project.error");			
		}
		//检查是否当前用户
		if(loanuser.equals(investuser)){
			throw new HandlerException("invest.check.sameuser");
		}
		//获取会员信息
		QueryUserAccountInfoHandler qd = new QueryUserAccountInfoHandler();
		JSONObject rq = (JSONObject)qd.createResponse(request, null);
		//实名认证
		if(!"2".equals(rq.get("UserAuthFlag"))){
			throw new HandlerException("invest.user.error");	
		}
		//安全问题
//		if(!"Y".equals(rq.get("SecurityQuestionFlag"))){
//			throw new HandlerException("invest.user.error");
//		}
		//交易密码
		if(!"Y".equals(rq.get("TransPWDFlag"))){
			throw new HandlerException("invest.user.error");	
		}
		//检查资金账户是否异常
		if("1".equals(rq.get("FrozenLockFalg"))){
			throw new HandlerException("invest.user.error");	
		}

		//判断输入的投资金额
		String TAmt = request.get("TAmt").toString();

		if(TAmt!=null&&TAmt.length()>0) {
			
			//投资金额大于账户余额
			if(Double.parseDouble(TAmt)>Double.parseDouble(rq.get("UsableBalance").toString())){
				throw new HandlerException("invest.sum.error");
				
			}
			
			//投资金额大于可投金额
			if(Double.parseDouble(TAmt)>remainamount){
				throw new HandlerException("invest.sum.error");
				
			}
			
			//足额投资
			if(Double.parseDouble(TAmt)==remainamount){			
				
			}else{
				//可投金额小于起投金额
				if(remainamount<Double.parseDouble(rp.get("BEGINAMOUNT").toString())){
					//必须投足余额
					if(Double.parseDouble(TAmt)!=remainamount){
						throw new HandlerException("invest.sum.error");
						
					}
				}else{
					//投资金额小于起投金额
					if(Double.parseDouble(TAmt)<Double.parseDouble(rp.get("BEGINAMOUNT").toString())){
						throw new HandlerException("invest.sum.error");					
					}
					
					//投资金额必须满足 起投金额加上递增金额；
					if((Double.parseDouble(TAmt)-Double.parseDouble(rp.get("BEGINAMOUNT").toString()))%Double.parseDouble(rp.get("ADDAMOUNT").toString())==0){
						
					}else{
						throw new HandlerException("invest.sum.error");					
					}
				}
			}
			
		} else {
			throw new HandlerException("invest.sum.error");		
		}		
	}
	
	
	//是否邀请返利
	private void isTransfer() {
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager manager = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery q0 = manager.createQuery(" select count(userid) as v.count from o where userid=:userid");
			q0.setParameter("userid", investuser);
			BizObject o0 = q0.getSingleResult(false);
			double count = 0;
			if(o0!=null){
				count = o0.getAttribute("count").getDouble();
			}
			//第一次投资
			if(count<1){
				//根据投资人查询邀请者
				BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
				BizObjectQuery qq = userAcctManager.createQuery(" select usercode from o where userid=:userid");
				qq.setParameter("userid", investuser);
				BizObject oo = qq.getSingleResult(false);
				String usercode = "";
				if(oo!=null){
					usercode = oo.getAttribute("usercode").toString();
				}
				BizObjectQuery q = userAcctManager.createQuery(" select userid from o where ( userid=:usercode or username=:usercode or phonetel=:usercode or invitecode=:usercode) and userauthflag='2'");
					q.setParameter("usercode", usercode);
				BizObject o = q.getSingleResult(false);
				if(o!=null){
					String sUserID = o.getAttribute("userid").toString();
					//插入邀请返利记录
					setTransfer(sUserID, sSUBCONTRACTNO, tamtString);
				}
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 插入邀请返利信息
	 * */
	private void setTransfer(String sUserID,String investCode,double investsum){
		JBOFactory jbo = JBOFactory.getFactory();
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.restore_rule");
			//从签到返利规则查询当前可返利金额
			BizObjectQuery ruleq = m.createQuery(" select restoresum,restoresum2,restoretype,restoretype2 from o where rulecode='invite_invest' and minimum <=:ucsum order by minimum desc");
			ruleq.setParameter("ucsum", investsum);
			BizObject ruleo = ruleq.getSingleResult(false);
			double restoresum = 0;
			double restoresum2 = 0;
			if(ruleo!=null){
				restoresum = ruleo.getAttribute("restoresum").getDouble();
				restoresum2 = ruleo.getAttribute("restoresum2").getDouble();
				
				if(2==ruleo.getAttribute("restoretype").getInt())
					restoresum = restoresum*investsum/100;
				
				if(2==ruleo.getAttribute("restoretype2").getInt())
					restoresum2 = restoresum2*investsum/100;
			}
			
			//推荐人
			if(restoresum>0){
				//插入交易记录
				BizObjectManager recordm =jbo.getManager("jbo.trade.transaction_record",tx);
				
				//插入交易记录
				BizObject recordo1 = recordm.newObject();
				recordo1.setAttributeValue("USERID", ARE.getProperty("HouBankSerialNo"));
				recordo1.setAttributeValue("relaaccount", "1");
				recordo1.setAttributeValue("DIRECTION", "P");
				recordo1.setAttributeValue("AMOUNT", restoresum);
				recordo1.setAttributeValue("BALANCE", 0);
				recordo1.setAttributeValue("TRANSTYPE", "2040");
				recordo1.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo1.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo1.setAttributeValue("STATUS", "99");
				recordo1.setAttributeValue("USERACCOUNTTYPE", "003");
				recordm.saveObject(recordo1);
				
				BizObject recordo = recordm.newObject();
				recordo.setAttributeValue("USERID", sUserID);
				recordo.setAttributeValue("relaaccount", "2");
				recordo.setAttributeValue("DIRECTION", "R");
				recordo.setAttributeValue("AMOUNT", restoresum);
				recordo.setAttributeValue("BALANCE", 0);
				recordo.setAttributeValue("TRANSTYPE", "2040");
				recordo.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo.setAttributeValue("STATUS", "99");
				recordo.setAttributeValue("USERACCOUNTTYPE", "001");
				recordm.saveObject(recordo);
				
				//插入打款交易
				BizObjectManager mm =jbo.getManager("jbo.trade.acct_transfer",tx);
				
				//平台营销账户
				BizObject transfer1 = mm.newObject();
				transfer1.setAttributeValue("objectno", investCode);
				transfer1.setAttributeValue("objecttype", "040");
				transfer1.setAttributeValue("seqid", "1");
				transfer1.setAttributeValue("userid", ARE.getProperty("HouBankSerialNo"));
				transfer1.setAttributeValue("direction", "R");
				transfer1.setAttributeValue("amount", restoresum);
				transfer1.setAttributeValue("status", "99");
				transfer1.setAttributeValue("inputdate", StringFunction.getToday());
				transfer1.setAttributeValue("inputtime", StringFunction.getNow());
				transfer1.setAttributeValue("transserialno", recordo1.getAttribute("serialno"));
				transfer1.setAttributeValue("remark", "代收邀请投资收益");
				transfer1.setAttributeValue("transcode", "1001");
				transfer1.setAttributeValue("useraccounttype", "003");
				mm.saveObject(transfer1);
				
				//邀请人
				BizObject transfer = mm.newObject();						
				transfer.setAttributeValue("objectno", investCode);
				transfer.setAttributeValue("objecttype", "040");
				transfer.setAttributeValue("seqid", "2");
				transfer.setAttributeValue("userid", sUserID);
				transfer.setAttributeValue("direction", "P");
				transfer.setAttributeValue("amount", restoresum);
				transfer.setAttributeValue("status", "99");
				transfer.setAttributeValue("inputdate", StringFunction.getToday());
				transfer.setAttributeValue("inputtime", StringFunction.getNow());
				transfer.setAttributeValue("transserialno", recordo.getAttribute("SERIALNO"));
				transfer.setAttributeValue("remark", "代付邀请投资收益");
				transfer.setAttributeValue("transcode", "2001");
				transfer.setAttributeValue("useraccounttype", "001");
				mm.saveObject(transfer);
			}
			
			//投资人
			if(restoresum2>0){
				//插入交易记录
				BizObjectManager recordm =jbo.getManager("jbo.trade.transaction_record",tx);
				
				//插入交易记录
				BizObject recordo1 = recordm.newObject();
				recordo1.setAttributeValue("USERID", ARE.getProperty("HouBankSerialNo"));
				recordo1.setAttributeValue("relaaccount", "1");
				recordo1.setAttributeValue("DIRECTION", "P");
				recordo1.setAttributeValue("AMOUNT", restoresum2);
				recordo1.setAttributeValue("BALANCE", 0);
				recordo1.setAttributeValue("TRANSTYPE", "2040");
				recordo1.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo1.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo1.setAttributeValue("STATUS", "99");
				recordo1.setAttributeValue("USERACCOUNTTYPE", "003");
				recordm.saveObject(recordo1);
				
				BizObject recordo = recordm.newObject();
				recordo.setAttributeValue("USERID", investuser);
				recordo.setAttributeValue("relaaccount", "3");
				recordo.setAttributeValue("DIRECTION", "R");
				recordo.setAttributeValue("AMOUNT", restoresum2);
				recordo.setAttributeValue("BALANCE", 0);
				recordo.setAttributeValue("TRANSTYPE", "2040");
				recordo.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo.setAttributeValue("STATUS", "99");
				recordo.setAttributeValue("USERACCOUNTTYPE", "001");
				recordm.saveObject(recordo);
				
				//插入打款交易
				BizObjectManager mm =jbo.getManager("jbo.trade.acct_transfer",tx);
				
				//平台营销账户
				BizObject transfer1 = mm.newObject();
				transfer1.setAttributeValue("objectno", investCode);
				transfer1.setAttributeValue("objecttype", "040");
				transfer1.setAttributeValue("seqid", "1");
				transfer1.setAttributeValue("userid", ARE.getProperty("HouBankSerialNo"));
				transfer1.setAttributeValue("direction", "R");
				transfer1.setAttributeValue("amount", restoresum2);
				transfer1.setAttributeValue("status", "99");
				transfer1.setAttributeValue("inputdate", StringFunction.getToday());
				transfer1.setAttributeValue("inputtime", StringFunction.getNow());
				transfer1.setAttributeValue("transserialno", recordo1.getAttribute("serialno"));
				transfer1.setAttributeValue("remark", "代收邀请投资收益");
				transfer1.setAttributeValue("transcode", "1001");
				transfer1.setAttributeValue("useraccounttype", "003");
				mm.saveObject(transfer1);
				
				//投资人
				BizObject transfer = mm.newObject();						
				transfer.setAttributeValue("objectno", investCode);
				transfer.setAttributeValue("objecttype", "040");
				transfer.setAttributeValue("seqid", "3");
				transfer.setAttributeValue("userid", investuser);
				transfer.setAttributeValue("direction", "P");
				transfer.setAttributeValue("amount", restoresum2);
				transfer.setAttributeValue("status", "99");
				transfer.setAttributeValue("inputdate", StringFunction.getToday());
				transfer.setAttributeValue("inputtime", StringFunction.getNow());
				transfer.setAttributeValue("transserialno", recordo.getAttribute("SERIALNO"));
				transfer.setAttributeValue("remark", "代付邀请投资收益");
				transfer.setAttributeValue("transcode", "2001");
				transfer.setAttributeValue("useraccounttype", "001");
				mm.saveObject(transfer);
			}
			
		}catch(Exception e){}
	}
}