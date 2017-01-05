package com.amarsoft.p2ptrade.loan;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.are.lang.DataElement;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 获取我的借款列表的信息
 * 
 * @author Mbmo
 *
 */
public class GetApplyInfoListHandler extends JSONHandler {
	private int pageSize = 1;
	private int pageNo = 0;// 当前页
	// 注册sql函数
	static {
		Parser.registerFunction("add_months");
		Parser.registerFunction("sum");
		Parser.registerFunction("to_date");
		Parser.registerFunction("nvl");
		Parser.registerFunction("sysdate");
	}

	@Override
	public Object createResponse(JSONObject request, Properties params) throws HandlerException {
		Object o = null;
		try {
			o = selectUserApplyInfo(request);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * 返回列表信息
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 * @throws JBOException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject selectUserApplyInfo(JSONObject request) throws HandlerException, JBOException {

		JSONObject result = new JSONObject();
		String userId = (String) request.get("userId");
		String status = (String) request.get("status");
		List<BizObject> loansApplyResults = getLoanApplyInfoResults(userId);
		List<BizObject> projectInfoResults = getProjectInfoResults(userId);
		List<BizObject> loanInfoResults = getLoanInfoResults(userId);
		JSONArray loanApplyArray = new JSONArray();
		JSONArray projectInfoArray = new JSONArray();
		JSONArray loanInfoArray = new JSONArray();
		// 根据状态位判断显示的结果集
		if ("apply".equals(status)) {
			loanApplyArray = putLoanApplyArray(loansApplyResults);// 贷款申请列表
		} else if ("project".equals(status)) {
			projectInfoArray = putProjectInfoArray(projectInfoResults);// 项目信息列表
		} else if ("loan".equals(status)) {
			loanInfoArray = putLoanInfoArray(loanInfoResults);// 借据信息列表
		} else {
			loanApplyArray = putLoanApplyArray(loansApplyResults);// 贷款申请列表
			projectInfoArray = putProjectInfoArray(projectInfoResults);// 项目信息列表
			loanInfoArray = putLoanInfoArray(loanInfoResults);// 借据信息列表
		}
		result.put("loanApplyArray", loanApplyArray);
		result.put("projectInfoArray", projectInfoArray);
		result.put("loanInfoArray", loanInfoArray);
		result.put("Statistics", setRepayStatistics(userId));// 获取统计数据
		return result;
	}

	/**
	 * 存放loanInfo内容到loanInfoArray中
	 * 
	 * @throws JBOException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray putLoanInfoArray(List<BizObject> loanInfoResults) throws JBOException {
		JSONArray array = new JSONArray();
		for (int i = 0; i < loanInfoResults.size(); i++) {
			BizObject o = loanInfoResults.get(i);
			JSONObject loanInfoItem = new JSONObject();// 我的借款项
			// 存入贷款编号，贷款标题，贷款额度，投标产品，投标日期，投标状态，操作

			loanInfoItem.put("serialNo", o.getAttribute("SERIALNO") == null ? "" : o.getAttribute("SERIALNO").toString());// 编号，用于详情页查询
			loanInfoItem.put("projectName", o.getAttribute("PROJECTNAME") == null ? "" : o.getAttribute("PROJECTNAME").toString());// 贷款标题
			loanInfoItem.put("loanAmount", o.getAttribute("LOANAMOUNT") == null ? "" : o.getAttribute("LOANAMOUNT").toString());// 贷款额度
			loanInfoItem.put("beginDate", o.getAttribute("BEGINDATE") == null ? "" : o.getAttribute("BEGINDATE").toString().substring(0, 10));// 开始时间
			loanInfoItem.put("endDate", o.getAttribute("endTime") == null ? "" : o.getAttribute("endTime").toString().substring(0, 10));// 结束时间
			loanInfoItem.put("status", o.getAttribute("loanstatus") == null ? "" : o.getAttribute("loanstatus").toString());// 根据状态显示
			loanInfoItem.put("loanno", o.getAttribute("loanno") == null ? "" : o.getAttribute("loanno").toString());//

			String loanNo = o.getAttribute("loanno").toString();
			try {
				DataElement paymentSum = getSumResult(loanNo).getAttribute("paymentSum");
				loanInfoItem.put("paymentSum", paymentSum == null ? "" : paymentSum.toString());// 根据状态显示
			} catch (HandlerException e) {
				e.printStackTrace();
			}
			array.add(loanInfoItem);
		}
		return array;
	}

	/**
	 * 存放projectInfo内容到projectInfoArray中
	 * 
	 * @throws JBOException
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray putProjectInfoArray(List<BizObject> projectInfoResults) throws JBOException, HandlerException {
		JSONArray array = new JSONArray();
		for (int i = 0; i < projectInfoResults.size(); i++) {
			BizObject o = projectInfoResults.get(i);
			JSONObject projectInfoItem = new JSONObject();// 我的借款项
			// 存入贷款编号，贷款标题，贷款额度，投标产品，投标日期，投标状态，操作
			projectInfoItem.put("serialNo", o.getAttribute("SERIALNO") == null ? "" : o.getAttribute("SERIALNO").toString());// 编号，用于详情页查询
			projectInfoItem.put("projectName", o.getAttribute("PROJECTNAME") == null ? "" : o.getAttribute("PROJECTNAME").toString());// 贷款标题
			projectInfoItem.put("loanAmount", o.getAttribute("LOANAMOUNT") == null ? "" : o.getAttribute("LOANAMOUNT").toString());// 贷款额度
			projectInfoItem.put("beginDate", o.getAttribute("BEGINDATE") == null ? "" : o.getAttribute("BEGINDATE").toString().substring(0, 10));// 开始时间
			projectInfoItem.put("endDate", o.getAttribute("endTime") == null ? "" : o.getAttribute("endTime").toString().substring(0, 10));// 结束时间
			projectInfoItem.put("status", o.getAttribute("STATUS") == null ? "" : o.getAttribute("STATUS").toString());// 根据状态显示

			String serialNo = o.getAttribute("serialNo").toString();
			try {
				DataElement paymentSum = getTempSumResult(serialNo).getAttribute("paymentSum");
				projectInfoItem.put("paymentSum", paymentSum == null ? "" : paymentSum.toString());// 根据状态显示
			} catch (HandlerException e) {
				e.printStackTrace();
				throw new HandlerException("undefined.error");
			}
			array.add(projectInfoItem);
		}

		return array;
	}

	/**
	 * 存放loanApply内容到loanApplyArray中
	 * 
	 * @param loansApplyResults
	 * @return
	 * @throws JBOException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray putLoanApplyArray(List<BizObject> loansApplyResults) throws JBOException {
		JSONArray loanApplyArray = new JSONArray();
		for (int i = 0; i < loansApplyResults.size(); i++) {
			BizObject o = loansApplyResults.get(i);
			JSONObject loansApplyItem = new JSONObject();// 我的借款项
			// 存入贷款编号，贷款标题，贷款额度，投标产品，投标日期，投标状态，操作
			loansApplyItem.put("applyno", o.getAttribute("applyno") == null ? "" : o.getAttribute("applyno").toString());// 编号，用于详情页查询
			loansApplyItem.put("projectname", o.getAttribute("projectname") == null ? "" : o.getAttribute("projectname").toString());// 贷款标题
			loansApplyItem.put("businesssum", o.getAttribute("businesssum") == null ? "" : o.getAttribute("businesssum").toString());// 贷款额度
//			o.getAttribute("applytime").getDate().
			loansApplyItem.put("applytime", o.getAttribute("applytime") == null ? "" : o.getAttribute("applytime").toString().substring(0, 10));// 申请时间
			loansApplyItem.put("endtime", o.getAttribute("endtime") == null ? "" : o.getAttribute("endtime").toString().substring(0, 10));// 结束时间
			loansApplyItem.put("applystatus", o.getAttribute("applystatus") == null ? "" : o.getAttribute("applystatus").toString());// 根据状态显示
			loansApplyItem.put("paymentTotal", "");// 申请状态下无还款总额

			loanApplyArray.add(loansApplyItem);
		}
		return loanApplyArray;
	}

	/**
	 * 获取借据表结果集
	 * 
	 * @param userId
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private List<BizObject> getLoanInfoResults(String userId) throws HandlerException {
		// userId="12314325245";//测试
		List<BizObject> result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_loan";
		String sql = "select pi.serialNo,pi.projectName,pi.loanAmount,pi.beginDate,pi.LOANTERM,o.SERIALNO as V.loanno,o.loanstatus,add_months(to_date(nvl(pi.BEGINDATE,'1900/01/01 00:00:00'),'yyyy/mm/dd hh24:mi:ss'),nvl(pi.LOANTERM,0)) as v.endTime from o,jbo.trade.project_info pi   where pi.contractid = o.contractserialno and o.customerid=:CUSTOMERID and pi.status='2'";
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("CUSTOMERID", userId).getResultList(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	/**
	 * 获取在还款中的信息的借款统计
	 * 
	 * @param loanNo
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject setRepayStatistics(String userId) throws HandlerException {
		BizObject resultSet = null;
		JSONObject statistics = new JSONObject();
		
		try {
			DataElement readyToPayCountIn10 = getReadyToPayCountIn10Results(userId).getAttribute("readyToPayCountIn10");
			DataElement	readyToPaySumIn10=getReadyToPaySumIn10Results(userId).getAttribute("readyToPaySumIn10");
			DataElement	readyToPaySumIn30=getReadyToPaySumIn30lResults(userId).getAttribute("readyToPaySumIn30");
			DataElement	readytopaysum=getReadytopaysumResults(userId).getAttribute("readytopaysum");
			DataElement	paymentTotal=getPaymentTotalResults(userId).getAttribute("paymentTotal");
			DataElement	overduePayment=getOverduePaymentResults(userId).getAttribute("overduePayment");
			statistics.put("readyToPayCountIn10", readyToPayCountIn10==null?"0":readyToPayCountIn10.toString());
			statistics.put("readyToPaySumIn10", readyToPaySumIn10==null?"0":readyToPaySumIn10.toString());
			statistics.put("readyToPaySumIn30", readyToPaySumIn30==null?"0":readyToPaySumIn30.toString());
			statistics.put("readytopaysum", readytopaysum==null?"0":readytopaysum.toString());
			statistics.put("paymentTotal", paymentTotal==null?"0":paymentTotal.toString());
			statistics.put("overduePayment", overduePayment==null?"0":overduePayment.toString());
		} catch (JBOException e) {
			e.printStackTrace();
		}
		
		
		
//		JBOFactory factory = JBOFactory.getFactory();
//		BizObjectManager manager;
//		String tableaps = "jbo.trade.acct_payment_schedule";
//		String tableac = "jbo.trade.acct_loan";
//		// 贷款总额的sql，table：acct_payment_schedule
//		String paymentTotalSql = "select (sum(o.paycorpusamt)+sum(o.payinteamt)) as v.paymentTotal from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId)";
//		// 待还总额table:acct_payment_schedule
//		String readyToPaySumSql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readytopaysum from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='')";
//		// 还款计划表里标记为逾期的实际未还金额，table:acct_loan
//		String overduePaymentSql = "select (sum(o.overduebalance)+sum(o.interestbalance)+sum(o.fineintebalance)) as v.overduePayment from o where o.loanstatus in ('1') and o.customerid=:userId";
//		// 近30天内的待还金额,table:acct_payment_schedule
//		String readyToPaySumIn30Sql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readyToPaySumIn30 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
////				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+30)"
//				;
//		// 近10天内的待还金额,table:acct_payment_schedule
//		String readyToPaySumIn10Sql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readyToPaySumIn10 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
////				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+10)"
//				;
//		// 近10天内的待还金额数量,table:acct_payment_schedule
//		String readyToPayCountIn10Sql = "select count(*) as v.readyToPayCountIn10 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
////				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+10)"
//				;
//		try {
//			// 逾期应还
//			manager = factory.getManager(tableac);
//			resultSet = manager.createQuery(overduePaymentSql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("overduePayment", resultSet.getAttribute("overduePayment") == null ? "" : resultSet.getAttribute("overduePayment").toString());
//			// 贷款总额
//			manager = factory.getManager(tableaps);
//			resultSet = manager.createQuery(paymentTotalSql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("paymentTotal", resultSet.getAttribute("paymentTotal") == null ? "" : resultSet.getAttribute("paymentTotal").toString());
//			// 待还总额
//			resultSet = manager.createQuery(readyToPaySumSql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("readytopaysum", resultSet.getAttribute("readytopaysum") == null ? "" : resultSet.getAttribute("readytopaysum").toString());
//			// 30天待还
//			resultSet = manager.createQuery(readyToPaySumIn30Sql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("readyToPaySumIn30", resultSet.getAttribute("readyToPaySumIn30") == null ? "" : resultSet.getAttribute("readyToPaySumIn30")
//					.toString());
//			// 10天待还
//			resultSet = manager.createQuery(readyToPaySumIn10Sql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("readyToPaySumIn10", resultSet.getAttribute("readyToPaySumIn10") == null ? "" : resultSet.getAttribute("readyToPaySumIn10")
//					.toString());
//			// 10天待还数
//			resultSet = manager.createQuery(readyToPayCountIn10Sql).setParameter("userId", userId).getSingleResult(false);
//			statistics.put("readyToPayCountIn10", resultSet.getAttribute("readyToPayCountIn10") == null ? "" : resultSet.getAttribute("readyToPayCountIn10")
//					.toString());
//		} catch (JBOException e) {
//			e.printStackTrace();
//			throw new HandlerException("sql.err");
//		}
		return statistics;
	}
	
	//逾期应还
	private BizObject getOverduePaymentResults(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_loan";
		String sql = "select (sum(o.overduebalance)+sum(o.interestbalance)+sum(o.fineintebalance)) as v.overduePayment from o where o.loanstatus in ('1') and o.customerid=:userId";
		
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}
	
	//贷款总额
	private BizObject getPaymentTotalResults(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select (sum(o.paycorpusamt)+sum(o.payinteamt)) as v.paymentTotal from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId)";
		
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	// 待还总额
	private BizObject getReadytopaysumResults(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readytopaysum from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='')";
		
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}
	
	//30天待还
	private BizObject getReadyToPaySumIn30lResults(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readyToPaySumIn30 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
//				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+30)"
				;
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}
	//10天待还
	private BizObject getReadyToPaySumIn10Results(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select (sum(paycorpusamt)+sum(payinteamt)) as v.readyToPaySumIn10 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
//				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+10)"
				;
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}
	
	//10天待还数
	private BizObject getReadyToPayCountIn10Results(String userId) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.acct_payment_schedule";
		String sql = "select count(*) as v.readyToPayCountIn10 from o where exists (select 1 from jbo.trade.acct_loan al where o.objectNo=al.serialno and al.customerid=:userId) and (o.finishdate is null or o.finishdate='') "
//				+ "and (to_date(o.paydate,'yyyy/mm/dd hh24:mi:ss') between sysdate and sysdate+10)"
				;
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}
	
	/**
	 * 获取项目表结果集
	 * 
	 * @param userId
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private List<BizObject> getProjectInfoResults(String userId) throws HandlerException {
		List<BizObject> result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.project_info";
		String sql = "select o.SERIALNO,o.projectName,o.loanAmount,o.status,O.LOANTERM,O.BEGINDATE,add_months(to_date(nvl(O.BEGINDATE,'1900/01/01 00:00:00'),'yyyy/mm/dd hh24:mi:ss'),nvl(O.LOANTERM,0)) as V.endTime from o,jbo.trade.business_contract bc where o.contractid = bc.serialno and bc.customerid=:userid and o.status in '101,102,103'";
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getResultList(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	/**
	 * 获得贷款申请信息的结果集
	 * 
	 * @param userId
	 *            用户id
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private List<BizObject> getLoanApplyInfoResults(String userId) throws HandlerException {
		List<BizObject> result = null;
		JBOFactory f = JBOFactory.getFactory();
		String table = "jbo.trade.loan_apply";
		String sql = "select o.applyno,o.projectname,o.businesssum,o.applytime,o.repaytimes,o.applystatus,add_months(to_date(nvl(o.applytime,'1900/01/01 00:00:00'),'yyyy/mm/dd hh24:mi:ss'),nvl(o.repaytimes,0)) as v.endtime from o where userid=:userid and o.applystatus not in ('080')";
		try {
			BizObjectManager m = f.getManager(table);
			result = m.createQuery(sql).setParameter("userid", userId).getResultList(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	/**
	 * 贷款申请项目进入到获得还款总额的结果集
	 * 
	 * @param loanNo
	 *            借据号
	 * @return
	 * @throws HandlerException
	 */
	private BizObject getSumResult(String loanNo) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		try {
			BizObjectManager m = f.getManager("jbo.trade.acct_payment_schedule");
			result = m
					.createQuery(
							"select  (sum(o.ACTUALPAYCORPUSAMT)+sum(o.ACTUALPAYINTEAMT)+sum(o.ACTUALFINEAMT))  as v.paymentSum from o where o.objectno=:loanno")
					.setParameter("loanno", loanNo).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	/**
	 * 虚拟还款计划表还款总额
	 * 
	 * @param serialno
	 * @return
	 * @throws HandlerException
	 */
	private BizObject getTempSumResult(String serialno) throws HandlerException {
		BizObject result = null;
		JBOFactory f = JBOFactory.getFactory();
		try {
			BizObjectManager m = f.getManager("jbo.trade.acct_payment_schedule_temp");
			result = m
					.createQuery(
							"select  (sum(o.ACTUALPAYCORPUSAMT)+sum(o.ACTUALPAYINTEAMT)+sum(o.ACTUALFINEAMT))  as v.paymentSum from o where o.objectno=(select loanno from jbo.trade.ti_contract_info ci where ci.CONTRACTID=(select contractid from jbo.trade.project_info pi where serialno=:serialno))")
					.setParameter("serialno", serialno).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return result;
	}

	/**
	 * 设置分页信息表
	 * 
	 * @param request
	 * @param recordCount
	 * @return
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private JSONObject setPage(JSONObject request, int recordCount) {
		JSONObject pageInfo = new JSONObject();
		if (request.containsKey("pageSize"))
			this.pageSize = Integer.parseInt(request.get("pageSize").toString());
		if (request.containsKey("pageNo"))
			this.pageNo = Integer.parseInt(request.get("pageNo").toString());
		System.out.println("pageSize-->>" + pageSize);
		int pageCount = (recordCount + pageSize - 1) / pageSize;
		if (pageNo > pageCount)
			pageNo = pageCount;
		if (pageNo < 1)
			pageNo = 1;
		pageInfo.put("recordCount", String.valueOf(recordCount));
		pageInfo.put("pageNo", String.valueOf(pageNo));
		pageInfo.put("pageSize", String.valueOf(pageSize));
		return pageInfo;
	}
}
