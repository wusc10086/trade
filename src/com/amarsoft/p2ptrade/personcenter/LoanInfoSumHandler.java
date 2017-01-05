package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;


import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 借款信息查询
 * 输入参数：
 * 		UserId:用户 名
 * 输出参数： 
 *      BUSINESSSUM借款总金额 
 *      OVERDUEBALANCE+INTERESTBALANCE逾期金额
 *      待还金额 NORMALBALANCE+OVERDUEBALANCE
 *      PMTAMOUNT近30天应还金额 ps
 *      10天内有几笔金额需归还PAYDATE-当前<10 count(1) ps
 *      总额 sum(PAYCORPUSAMT PAYINTEAMT PAYFINEAMT PAYCOMPDINTEAMT) ps
 * 
 */
public class LoanInfoSumHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfoSum(request);
	}
	
	/**
	 * 借款信息查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfoSum(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		 
		String sUserID = request.get("UserID").toString();
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
			//借款总金额
			BizObjectManager m1 = jbo.getManager("jbo.trade.acct_loan");
	        BizObjectQuery query1 = m1.createQuery(
	        		  " select sum(li.BUSINESSSUM) as v.businesssum " +
	        		  " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					  " jbo.trade.project_info pi," +
					  " jbo.trade.acct_loan li " +
					  " where uc.userid=:userid and uc.relativetype='001' " +
					  " and uc.contractid=pi.contractid " +
					  " and uc.contractid=ci.contractid and ci.loanno=li.serialno " 
					  );
			  query1.setParameter("userid", sUserID);
			  BizObject o1 = query1.getSingleResult(false);
				if (o1 != null) {
					result.put("BusinessSum", o1.getAttribute("BUSINESSSUM").getValue()==null?
	     					"0":o1.getAttribute("BUSINESSSUM").getDouble());//借款总金额
				}else{
					result.put("BusinessSum", 0);//借款总金额
				}
		   
		     //逾期金额
			BizObjectManager m2 = jbo.getManager("jbo.trade.acct_loan");
	        BizObjectQuery query2 = m2.createQuery(
	        		  " select sum(ps.PAYCORPUSAMT+ps.PAYINTEAMT+ps.PAYFINEAMT+ps.payfeeamt1-ps.ACTUALPAYCORPUSAMT-ps.ACTUALPAYINTEAMT-ps.ACTUALFINEAMT-ps.ACTUALPAYFEEAMT1) as v.OverBalance " +
	        		  " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					  " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps " +
					  " where uc.userid=:userid and uc.relativetype='001' " +
					  " and uc.contractid=pi.contractid " +
					  " and uc.contractid=ci.contractid " +
					  " and ps.objectno=ci.loanno " +
					  " and ps.status='020' "
					  );
			  query2.setParameter("userid", sUserID);
			  BizObject o2 = query2.getSingleResult(false);
				if (o2 != null) {
					result.put("OverBalance", o2.getAttribute("OVERBALANCE").getValue()==null?
	     					"0":o2.getAttribute("OVERBALANCE").getDouble());//逾期金额
				}else{
					result.put("OverBalance", 0);//逾期金额
				}
				
			  //待还金额
			BizObjectManager m3 = jbo.getManager("jbo.trade.acct_payment_schedule");
	        BizObjectQuery query3 = m3.createQuery(
	        		  " select sum(ps.PAYCORPUSAMT+ps.PAYINTEAMT+ps.PAYFINEAMT+ps.payfeeamt1-ps.ACTUALPAYCORPUSAMT-ps.ACTUALPAYINTEAMT-ps.ACTUALFINEAMT-ps.ACTUALPAYFEEAMT1) as v.ReceiveBalance " +
	        		  " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					  " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps " +
					  " where uc.userid=:userid and uc.relativetype='001' " +
					  " and uc.contractid=pi.contractid " +
					  " and uc.contractid=ci.contractid " +
					  " and ps.objectno=ci.loanno " +
					  " and ps.status in ('020','040')"
					  );
			  query3.setParameter("userid", sUserID);
			  BizObject o3 = query3.getSingleResult(false);
				if (o3 != null) {
					result.put("ReceiveBalance", o3.getAttribute("RECEIVEBALANCE").getValue()==null?
	     					"0":o3.getAttribute("RECEIVEBALANCE").getDouble());//待还金额
				}else{
					result.put("ReceiveBalance", 0);//待还金额
				}
 
				 
			  //近30天应还金额
				Date d = new Date();
			    SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
			    String sPayDate30 = df.format(new Date(d.getTime()+(long)30*24*60*60*1000));
				BizObjectManager m4 =jbo.getManager("jbo.trade.acct_payment_schedule");
				BizObjectQuery query4 = m4.createQuery(
					    "select sum(ps.paycorpusamt+ps.payinteamt+ps.payfineamt+ps.payfeeamt1-ps.actualpaycorpusamt-ps.actualpayinteamt-ps.actualfineamt-ps.actualpayfeeamt1) as v.PaySum " +
					    " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					    " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps " +
					    " where uc.userid=:userid and uc.relativetype='001' " +
					    " and uc.contractid=pi.contractid " +
					    " and uc.contractid=ci.contractid " +
					    " and ps.objectno=ci.loanno " +
					    " and ps.status in ('020','040')" +
					    " and ps.paydate<=:paydate "  
						); 
				query4.setParameter("userid", sUserID);
				query4.setParameter("paydate", sPayDate30);
				BizObject o4 = query4.getSingleResult(false);
				if (o4 != null) {
						result.put("PmtAmount",  Double.parseDouble(o4.getAttribute("PAYSUM").getValue()==null?
								  "0":o4.getAttribute("PAYSUM").toString()));//近30天应还金额
				}else{
					result.put("PmtAmount", 0);//近30天应还金额
				}
				
				 //您最近10天内有  笔 
				Date d1 = new Date();
			    SimpleDateFormat df1 = new SimpleDateFormat("yyyy/MM/dd");
			    String sPayDate1 = df1.format(new Date(d1.getTime()+(long)10*24*60*60*1000));
				BizObjectManager m5 =jbo.getManager("jbo.trade.acct_payment_schedule");
				BizObjectQuery query5 = m5.createQuery(
					    "select count(1) as v.TenCount " +
					    " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					    " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps " +
					    " where uc.userid=:userid and uc.relativetype='001' " +
					    " and uc.contractid=pi.contractid " +
					    " and uc.contractid=ci.contractid " +
					    " and ps.objectno=ci.loanno " +
					    " and ps.status in ('020','040') " +
					    " and ps.paydate<=:paydate "  
						);
				query5.setParameter("userid", sUserID);
				query5.setParameter("paydate", sPayDate1);
			    BizObject o5 = query5.getSingleResult(false);
				if (o5 != null) {
					result.put("TenCount", o5.getAttribute("TENCOUNT").getValue()==null?
	     					0:o5.getAttribute("TENCOUNT").getDouble());//您最近10天内有  笔 
				}else{
					result.put("TenCount", 0);//您最近10天内有  笔 
				}
				
				 //您最近10天借款须归还，总额  元
				Date d10 = new Date();
			    SimpleDateFormat df10 = new SimpleDateFormat("yyyy/MM/dd");
			    String sPayDate10 = df10.format(new Date(d10.getTime()+(long)10*24*60*60*1000));
				BizObjectManager m6 =jbo.getManager("jbo.trade.acct_payment_schedule");
				BizObjectQuery query6 = m6.createQuery(
					    "select sum(ps.paycorpusamt+ps.payinteamt+ps.payfineamt+ps.payfeeamt1-ps.actualpaycorpusamt-ps.actualpayinteamt-ps.actualfineamt-ps.actualpayfeeamt1) as v.SumBalance  " +
					    " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					    " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps " +
					    " where uc.userid=:userid and uc.relativetype='001' " +
					    " and uc.contractid=pi.contractid " +
					    " and uc.contractid=ci.contractid " +
					    " and ps.objectno=ci.loanno " +
					    " and ps.status in ('020','040') " +
					    " and ps.paydate<=:paydate "
						);
				query6.setParameter("userid", sUserID);
				query6.setParameter("paydate", sPayDate10);
			    BizObject o6 = query6.getSingleResult(false);
				if (o6 != null) {
					result.put("SumBalance", o6.getAttribute("SUMBALANCE").getValue()==null?
	     					"0":o6.getAttribute("SUMBALANCE").getDouble()); 
				}else{
					result.put("SumBalance", 0);//您最近10天内有  笔 
				}
				result.put("RootType", "010");
			return result;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryloaninfosum.error");
		}
	}

 
}
