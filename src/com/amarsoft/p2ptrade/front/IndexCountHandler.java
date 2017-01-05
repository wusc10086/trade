package com.amarsoft.p2ptrade.front;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import com.amarsoft.app.accounting.util.NumberTools;
import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.invest.P2pString;
/**
 * 推荐的项目列表
 * **/
public class IndexCountHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		try{
			//聪明的投资人（在投）
			BizObjectManager m = JBOFactory.getBizObjectManager("jbo.trade.distinct_investers");
			result.put("investCount", m.createQuery("1=1").getTotalCount());
			//投资额
			m = JBOFactory.getBizObjectManager("jbo.trade.user_contract");
			BizObjectQuery q = m.createQuery("select sum(investsum) as v.investsum from o where status='1'");
			BizObject o = q.getSingleResult(false);
			double investsum = 0;
			if(o!=null){
				investsum = o.getAttribute("investsum").getDouble();
			}else
				investsum = 0;
			DecimalFormat df1 = new DecimalFormat("#,##0.00");
			
			result.put("investSum", df1.format(investsum) );
			
			
			result.put("investIncome",df1.format(getRewardIncome("")+getHistoryInvestSum()+getRuningInvestSum()));//投资收益
			return result;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	private String getPloanSetupDate()throws JBOException{
		return JBOFactory.getBizObjectManager("jbo.trade.ploan_setup").createQuery("").getSingleResult(false).getAttribute("curdeductdate").getString();
	}
	
	public static int getCurrentMonthLastDay(){  
	    Calendar a = Calendar.getInstance();  
	    a.set(Calendar.DATE, 1);//把日期设置为当月第一天  
	    a.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天  
	    int maxDate = a.get(Calendar.DATE);  
	    return maxDate;  
	}  
	
	//在投收益
	public  double getRuningInvestSum() throws JBOException{
		String sPloanSetupDate = getPloanSetupDate();
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.income_detail");
		String sSql = "Select Sum(ActualPayInteAmt+ActualExpiationSum) as v.ActualAmount From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And uc.Status = '1' ";
		BizObject obj = m3.createQuery(sSql).getSingleResult(false);
		
		//所有在投已收
		double dInvestSum = 0;
		if(obj!=null)dInvestSum = obj.getAttribute("ActualAmount").getDouble();
		
		BizObjectManager m4 = JBOFactory.getBizObjectManager("jbo.trade.income_schedule");
		sSql = "Select Sum(PayInteAmt) as v.PayInteAmt,PayDate From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And PayDate like :month And uc.Status = '1' ";
		obj = m4.createQuery(sSql).setParameter("month", sPloanSetupDate.substring(0, 8)+"%").getSingleResult(false);
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
	protected double getHistoryInvestSum() throws JBOException{
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.income_detail");
		String sSql = "Select Sum(ActualPayInteAmt+ActualExpiationSum) as v.s From o,jbo.trade.user_contract uc Where uc.userid=o.userid and uc.status='3'";
		BizObject obj = m3.createQuery(sSql).getSingleResult(false);
		if(obj==null)return 0d;
		return obj.getAttribute("s").getDouble();
	}
		
	//获得平台奖励
	protected double getRewardIncome(String endDate)throws JBOException{
		//平台奖励
		BizObjectManager m3 = JBOFactory.getBizObjectManager("jbo.trade.transaction_record");
		String sql = " select sum(amount) as v.amount from o where status='10' and direction='R' and transtype in ('2030','2040','2050') ";
		if(endDate!=null && endDate.length()>0){
			sql += " and updatetime like :enddate";
		}
		BizObjectQuery q3 = m3.createQuery(sql);
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
}
