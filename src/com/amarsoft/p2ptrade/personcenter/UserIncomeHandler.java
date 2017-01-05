package com.amarsoft.p2ptrade.personcenter;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.amarsoft.app.accounting.util.NumberTools;
import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.invest.P2pString;
/** 
 * 个人收益统计
 * 输入参数：
 * 		UserID:账户编号
 *
 */
public class UserIncomeHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		Parser.registerFunction("sum");
		Parser.registerFunction("count");
		return getAcount(request);		
	}
	  
	/**
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject getAcount(JSONObject request)throws HandlerException {
		
		JSONObject result = new JSONObject();
		//参数校验
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("common.emptyuserid");
		}

		String sUserID = request.get("UserID").toString();
		
		JBOFactory jbo = JBOFactory.getFactory();
		try {
			
			//持有项目金额
			BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery q = m.createQuery(" select sum(investsum) as v.investsum from o where status='1' and UserID=:UserID");
			q.setParameter("UserID", sUserID);
			BizObject o = q.getSingleResult(false);
			double investsum = 0;

			if(o!=null){
				investsum = o.getAttribute("investsum").getDouble();
			}else
				investsum = 0;
			result.put("investsum", investsum);
			
			//预收益
			/*
			BizObjectManager m1 = jbo.getManager("jbo.trade.income_schedule");
			BizObjectQuery q1 = m1.createQuery(" select sum(payinteamt) as v.payinteamt from o,jbo.trade.user_contract uc where uc.contractid = o.contractno and uc.status='1' and uc.UserID=:UserID");
			q1.setParameter("UserID", sUserID);
			BizObject o1 = q1.getSingleResult(false);
			double payinteamt = 0;

			if(o1!=null){
				payinteamt = o1.getAttribute("payinteamt").getDouble();
			}else
				payinteamt = 0;
				*/
			double payinteamt = getRuningInvestSum(sUserID);
			result.put("payinteamt", payinteamt);
						   
			//昨日收益
			/*
			String updatetime = P2pString.addDateFormat(StringFunction.getTodayNow(), 3, -1,"yyyy/MM/dd");	
			BizObjectManager m2 = jbo.getManager("jbo.trade.transaction_record");
			BizObjectQuery q2 = m2.createQuery(" select sum(amount) as v.amount from o where status='10' and direction='R' and updatetime=:updatetime and UserID=:UserID");
			q2.setParameter("UserID", sUserID).setParameter("updatetime", updatetime);
			BizObject o2 = q2.getSingleResult(false);
			double amount = 0;

			if(o2!=null){
				amount = o2.getAttribute("amount").getDouble();
			}else
				amount = 0;
			result.put("lastamount", amount);
			*/
			//平台奖励
			double amount1 = getRewardIncome(sUserID,"");
			result.put("restoreamount", amount1);
			//累计收益
			result.put("lastamount",payinteamt + getHistoryInvestSum(sUserID) + amount1);
			
			//昨日收益
			long lYestoday = (new java.util.Date()).getTime()-3600*24*1000;
			Date dYestoday = new Date(lYestoday);
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
			String sYestoday = formatter.format(dYestoday);
			
			result.put("yestodayamount", getYestodayInvestIncome(sUserID)+ getRewardIncome(sUserID,sYestoday));
			//昨日年化利率
			result.put("yestodayreate",getYestodyRate(sUserID));
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private Object getYestodyRate(String sUserID)throws JBOException {
		
		double dLoanRate = 0d ;
		double dLoanAmount = 0d ;
		double dInvestSum = 0d ;
		double dSumRate = 0d ;
		
		String sSql = "Select o.LoanRate,o.LoanAmount,uc.InvestSum From jbo.trade.user_contract uc,o Where uc.ProjectID = o.SerialNo And uc.UserID = :userid";
		BizObjectQuery query = JBOFactory.createBizObjectQuery("jbo.trade.project_info", sSql).setParameter("userid", sUserID);
		List<BizObject> list = query.getResultList(false);
		for(BizObject obj : list){
			dLoanRate = obj.getAttribute("LoanRate").getDouble();//项目利率
			dLoanAmount = obj.getAttribute("LoanAmount").getDouble();//项目金额
			dInvestSum = obj.getAttribute("InvestSum").getDouble();//投资金额
			dSumRate += NumberTools.round(dInvestSum/dLoanAmount*dLoanRate,2);//四舍五入
		}
		ARE.getLog().info("昨日年化收益率="+dSumRate+" %");
		return dSumRate ;
	}

	private String getPloanSetupDate()throws JBOException{
		return JBOFactory.getBizObjectManager("jbo.trade.ploan_setup").createQuery("").getSingleResult(false).getAttribute("curdeductdate").getString();
	}
	
	//在投收益
	public  double getRuningInvestSum(String sUserID) throws JBOException{
		String sPloanSetupDate = getPloanSetupDate();
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.income_detail");
		String sSql = "Select Sum(ActualPayInteAmt+ActualExpiationSum) as v.ActualAmount From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And uc.Status = '1' And uc.UserID = :userid";
		BizObject obj = m3.createQuery(sSql).setParameter("userid", sUserID).getSingleResult(false);
		
		//所有在投已收
		double dInvestSum = 0;
		if(obj!=null)dInvestSum = obj.getAttribute("ActualAmount").getDouble();
		
		BizObjectManager m4 = JBOFactory.getBizObjectManager("jbo.trade.income_schedule");
		sSql = "Select Sum(PayInteAmt) as v.PayInteAmt,PayDate From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And PayDate like :month And uc.Status = '1' And uc.UserID = :userid ";
		obj = m4.createQuery(sSql).setParameter("userid", sUserID).setParameter("month", sPloanSetupDate.substring(0, 8)+"%").getSingleResult(false);
		//本月应得利息
		double dMonthInteAmt = 0;
		if(obj!=null)
			dMonthInteAmt = obj.getAttribute("PayInteAmt").getDouble();
		int iMonthDay = getCurrentMonthLastDay();//当月有多少天
		int itoday = Integer.parseInt(sPloanSetupDate.substring(8));//当月第几天
		double dztSum = NumberTools.round(dInvestSum+(dMonthInteAmt/iMonthDay*itoday),2);
		return dztSum ;
	}
	
	//历史投资收益
	protected double getHistoryInvestSum(String sUserID) throws JBOException{
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.income_detail");
		String sSql = "Select Sum(ActualPayInteAmt+ActualExpiationSum) as v.s From o,jbo.trade.user_contract uc Where o.UserID = :userid and uc.userid=o.userid and uc.status='3'";
		BizObject obj = m3.createQuery(sSql).setParameter("userid", sUserID).getSingleResult(false);
		if(obj==null)return 0d;
		return obj.getAttribute("s").getDouble();
	}
	
	//获得平台奖励
	protected double getRewardIncome(String sUserID,String endDate)throws JBOException{
		//平台奖励
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.transaction_record");
		String sql = " select sum(amount) as v.amount from o where status='10' and direction='R' and transtype in ('2030','2040','2050') and UserID=:UserID";
		if(endDate!=null && endDate.length()>0){
			sql += " and updatetime like :enddate";
		}
		BizObjectQuery q3 = m3.createQuery(sql);
		q3.setParameter("UserID", sUserID);
		if(endDate!=null && endDate.length()>0){
			q3.setParameter("enddate", endDate);
		}
		BizObject o3 = q3.getSingleResult(false);
		double amount1 = 0;

		if(o3!=null){
			amount1 = o3.getAttribute("amount").getDouble();
		}else
			amount1 = 0;
		return amount1;
	}
	
	//取得当月天数(针对昨天) 
	private int getYestodayMonthLastDay(){  
	    Calendar a = Calendar.getInstance();  
	    a.add(Calendar.DATE, -1);
	    a.set(Calendar.DATE, 1);//把日期设置为当月第一天  
	    a.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天  
	    int maxDate = a.get(Calendar.DATE);  
	    return maxDate;  
	}  
		
	//投资人当前投资的昨日收益（预计），昨日收益是指 (所有在投投资)本月应得利息/本月天数
	protected double getYestodayInvestIncome(String sUserID)throws JBOException{
		BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.income_schedule");
		String sSql = "Select Sum(o.PayInteAmt) as v.PayInteAmt,PayDate From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And o.PayDate like :yesmonth And uc.Status = '1' And uc.UserID = :userid ";
		long lYestoday = (new java.util.Date()).getTime()-3600*24*1000;
		Date dYestoday = new Date(lYestoday);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM");
		String sYesMonth = formatter.format(dYestoday);
		BizObject obj = manager.createQuery(sSql).setParameter("userid", sUserID).setParameter("yesmonth", sYesMonth + "%").getSingleResult(false);
		if(obj==null)return 0;
		//当月有多少天
		int iMonthDay = getYestodayMonthLastDay();
		return obj.getAttribute("PayInteAmt").getDouble()/iMonthDay;
	}
	
	//昨日年化收益率
	/*
	public  double getLastLoanRate() throws JBOException{
		String sSql = "Select pj.LoanRate,pj.LoanAmount,uc.InvestSum From User_Contract uc,Project_Info pj Where uc.ProjectID = pj.SerialNo And uc.UserID = ?";
		
		PreparedStatement ps = null ;
		ResultSet rs = null ;
		double dLoanRate = 0d ;
		double dLoanAmount = 0d ;
		double dInvestSum = 0d ;
		double dSumRate = 0d ;
		try{
			ps = conn.prepareStatement(sSql);
			ps.setString(1, "2015020600000003");
			rs = ps.executeQuery();
			while(rs.next()){
				dLoanRate = rs.getDouble("LoanRate");//项目利率
				dLoanAmount = rs.getDouble("LoanAmount");//项目金额
				dInvestSum = rs.getDouble("InvestSum");//投资金额
				dSumRate += NumberTools.round2(dInvestSum/dLoanAmount*dLoanRate);//四舍五入
			}
			System.out.println("昨日年化收益率="+dSumRate+" %");
		}finally{
			if(ps!=null)ps.close();
			if(rs!=null)rs.close();
		}
		
		return dSumRate ;
	}
	*/
	
	/** 
	 * 取得当月天数 
	 * */  
	public static int getCurrentMonthLastDay(){  
	    Calendar a = Calendar.getInstance();  
	    a.set(Calendar.DATE, 1);//把日期设置为当月第一天  
	    a.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天  
	    int maxDate = a.get(Calendar.DATE);  
	    return maxDate;  
	}  
}