package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/**
 * 获取首页相关信息
 *
 */
public class GetPersonCenterGlobalHandler extends JSONHandler {

	JSONObject result;

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getPersonCenterGlobal(request);

	}

	/**
	 * 余额查询
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getPersonCenterGlobal(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}

		String sUserID = request.get("UserID").toString();// 用户编号

		result = new JSONObject();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			getInvestmentAssets(jbo, sUserID);
			getLoanLiabilities(jbo, sUserID);
//			getInvestmentAccountInfo(jbo, sUserID);
			getLoanAmount(jbo, sUserID);
			getEarnedAmount(jbo, sUserID);
			getLoanTimes(jbo, sUserID);
			getRepaymentAccountInfo(jbo, sUserID);
			getRecommendProject(jbo);
			return result;
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryaccountbalance.error");
		}
	}

	/**
	 * 获取投资资产
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getInvestmentAssets(JBOFactory jbo, String sUserID)
			throws HandlerException {
		// select ps.paycorpusamt,ps.payinteamt from
		// jbo.trade.acct_payment_schedule ps,jbo.trade.acct_loan li where
		// li.lufaxid=:userid and li.serialno=ps.objectno
		try {
			BizObjectManager m = jbo
					.getManager("jbo.trade.acct_payment_schedule");
			//全部待收本息
			BizObjectQuery query = m.createQuery(
					"select sum(ps.paycorpusamt+ps.payinteamt) as v.PaySum  " +
						    " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
						    " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps," +
						    " jbo.trade.acct_loan li " +
						    " where uc.userid=:userid and uc.relativetype='002' " +
						    " and uc.contractid=ci.contractid " +
							" and uc.contractid=pi.contractid and ci.loanno=ps.objectno " +
							" and ci.loanno=li.serialno and li.loanstatus in ('0','1')"  
					);
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			double investmentAssets = 0;
			if(o!=null){
				investmentAssets = o.getAttribute("PaySum").getValue()==null?0.00:o.getAttribute("PaySum").getDouble();
			}else{
				investmentAssets = 0.00;
			}
			result.put("InvestmentAssets", String.valueOf(GeneralTools.numberFormat(investmentAssets)));
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getinvestmentassets.error");
		}
	}
	
	
	/**
	 * 获取还款负债
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getLoanLiabilities(JBOFactory jbo, String sUserID)
			throws HandlerException {
		// select li.OVERDUEBALANCE,li.NORMALBALANCE from jbo.trade.acct_loan
		// li where li.lufaxid=:userid
		try {
			BizObjectManager m3 = jbo.getManager("jbo.trade.acct_payment_schedule");
			BizObjectQuery query3 = m3.createQuery(
					" select sum(ps.PAYCORPUSAMT+ps.PAYINTEAMT+ps.PAYFINEAMT-ps.ACTUALPAYCORPUSAMT-ps.ACTUALPAYINTEAMT-ps.ACTUALFINEAMT) as v.ReceiveBalance " +
			        		  " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
							  " jbo.trade.project_info pi,jbo.trade.acct_payment_schedule ps," +
							  " jbo.trade.acct_loan li " +
							  " where uc.userid=:userid and uc.relativetype='001' " +
							  " and uc.contractid=pi.contractid " +
							  " and uc.contractid=ci.contractid and ci.loanno=li.serialno " +
							  " and ps.objectno=ci.loanno " +
							  " and li.loanstatus in ('1','0')"
					  );
			  query3.setParameter("userid", sUserID);
			  BizObject o3 = query3.getSingleResult(false);
			  double loanLiabilities = 0;
				if (o3 != null) {
					loanLiabilities = o3.getAttribute("ReceiveBalance").getValue()==null?
	     					0.00:o3.getAttribute("ReceiveBalance").getDouble();//待还金额
				}else{
					loanLiabilities = 0.00;
				}
			result.put("LoanLiabilities", String.valueOf(GeneralTools.numberFormat(loanLiabilities)));
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getloanliabilities.error");
		}
	}
	
	/**
	 * 获取持有中的投资金额
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getLoanAmount(JBOFactory jbo, String sUserID)
			throws HandlerException {
		// select li.OVERDUEBALANCE,li.NORMALBALANCE from jbo.trade.acct_loan
		// li where li.lufaxid=:userid
		try {
			BizObjectManager m =jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query = m.createQuery("" +
					"select sum(li.businesssum) as v.InvestSum" +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					" jbo.trade.acct_loan li,jbo.trade.project_info pi " +
					" where uc.userid=:userid and uc.contractid=ci.contractid " +
					" and ci.loanno=li.serialno and li.loanstatus in ('0','1')" +
					" and uc.relativetype='002' and uc.contractid=pi.contractid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			double loanAmount = 0;
			if (o != null) {
				loanAmount = o.getAttribute("InvestSum").getValue()==null?
	     				0.00:o.getAttribute("InvestSum").getDouble();//待还金额
			}else{
				loanAmount = 0.00;
			}
			result.put("LoanAmount", String.valueOf(GeneralTools.numberFormat(loanAmount)));
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getloanamount.error");
		}
	}
	
	/**
	 * 获取已赚金额
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getEarnedAmount(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.acct_back_bill");
            BizObjectQuery query = m.createQuery(
            		"select sum(bb.ACTUALPAYINTEAMT+bb.ACTUALFINEAMT) as v.ActualPaySum " +
                    		" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
                    		" jbo.trade.acct_back_bill bb " +
                    		" where uc.userid=:userid and uc.relativetype='002' " +
                    		" and uc.contractid=ci.contractid and ci.loanno=bb.objectno " );
            query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);

			double earnedAmount = 0;
			if (o != null) {
				earnedAmount = o.getAttribute("ACTUALPAYSUM").getValue()==null?
	     				0.00:o.getAttribute("ACTUALPAYSUM").getDouble();//待还金额
			}else{
				earnedAmount = 0.00;
			}
			result.put("EarnedAmount", String.valueOf(GeneralTools.numberFormat(earnedAmount)));
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getloanamount.error");
		}
	}
	
	/**
	 * 持有数量
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getLoanTimes(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m =jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query = m.createQuery("" +
					"select count(1) as v.Cnt " +
	           		 " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
		            	 " jbo.trade.project_info pi,jbo.trade.acct_loan li " +
	           		 " where uc.userid=:userid and uc.relativetype='002' " +
	           		 " and uc.contractid=ci.contractid " +
	           		 " and uc.contractid=pi.contractid and ci.loanno=li.serialno " +
	           		 " and li.loanstatus in ('0','1','90')");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			
			int loanTimes = 0;
			if (o != null) {
				loanTimes = o.getAttribute("Cnt").getValue()==null?
	     				0:o.getAttribute("Cnt").getInt();//持有数量
			}else{
				loanTimes = 0;
			}
			result.put("LoanTime", String.valueOf(loanTimes));
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getloantimes.error");
		}
	}

//	/**
//	 * 获取投资账户信息
//	 * 
//	 * @param jbo
//	 * @param sUserID
//	 * @throws Exception
//	 */
//	private void getInvestmentAccountInfo(JBOFactory jbo, String sUserID)
//			throws HandlerException {
//		// select ci.loanno, sum(pi.LOANAMOUNT),
//		// sum(ps.ACTUALPAYCORPUSAMT+ps.ACTUALPAYINTEAMT+ps.ACTUALFINEAMT) from
//		// user_contract uc ,ti_contract_info ci ,acct_payment_schedule ps
//		// ,project_info pi where uc.userid = '2014053100000012' and
//		// uc.RELATIVETYPE = '002' and uc.CONTRACTID = ci.LOANNO and
//		// pi.CONTRACTID= ci.CONTRACTID
//		// and ci.LOANNO = ps.OBJECTNO and ps.finishdate is null group by
//		// ci.loanno ;
//		try {
//			BizObjectManager psManager = jbo
//					.getManager("jbo.trade.acct_payment_schedule");
//			BizObjectQuery psQuery = psManager
//					.createQuery("select ci.loanno as v.loanno, sum(pi.LOANAMOUNT) as v.loanamount, sum(ACTUALPAYCORPUSAMT+ACTUALPAYINTEAMT+ACTUALFINEAMT) as v.earnedamount "
//							+ "from o ,jbo.trade.user_contract uc ,jbo.trade.ti_contract_info ci ,jbo.trade.project_info pi "
//							+ "where uc.userid = :userid and uc.RELATIVETYPE = '002' and  uc.CONTRACTID = ci.LOANNO "
//							+ "and pi.CONTRACTID= ci.CONTRACTID and ci.LOANNO = OBJECTNO and finishdate is null group by ci.loanno");
//			psQuery.setParameter("userid", sUserID);
//			BizObject psObejct = psQuery.getSingleResult(false);
//			double loanAmount = 0;
//			double earnedAmount = 0;
//			if (psObejct != null) {
//				loanAmount = Double.parseDouble(psObejct.getAttribute(
//						"loanamount").toString() == null ? "0.0" : psObejct
//						.getAttribute("loanamount").toString());
//				earnedAmount = Double.parseDouble(psObejct.getAttribute(
//						"earnedamount").toString() == null ? "0.0" : psObejct
//						.getAttribute("earnedamount").toString());
//			}
//			result.put("LoanAmount", String.valueOf(loanAmount));
//			result.put("EarnedAmount", String.valueOf(earnedAmount));
//
//			BizObjectManager piManager = jbo
//					.getManager("jbo.trade.project_info");
//			BizObjectQuery piQuery = piManager
//					.createQuery("select  count(1) as v.loantime from o, jbo.trade.user_contract uc ,jbo.trade.ti_contract_info  ci  where uc.userid = :userid and uc.RELATIVETYPE = '002' and  uc.CONTRACTID = ci.LOANNO and CONTRACTID= ci.CONTRACTID");
//			piQuery.setParameter("userid", sUserID);
//			BizObject piObject = piQuery.getSingleResult(false);
//			int loanTime = 0;
//			if (piObject != null) {
//				loanTime = piObject.getAttribute("loantime").getInt();
//			}
//			result.put("LoanTime", String.valueOf(loanTime));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new HandlerException("getinvestmentaccountinfo.error");
//		}
//	}
	
	/**
	 * 获取还款账户信息
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getRepaymentAccountInfo(JBOFactory jbo, String sUserID)
			throws HandlerException {
		try {
            BizObjectManager m =jbo.getManager("jbo.trade.user_contract");
            BizObjectQuery query = m.createQuery(
            		"select ps.objectno,pi.projectname,pi.serialno,li.putoutdate,li.maturitydate," +
        					" li.businesssum,sum(ps.paycorpusamt+ps.payinteamt+ps.payfineamt+" +
        					" ps.payfeeamt1+ps.payfeeamt2) as v.sum1,li.loanstatus " +
        					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
        					" jbo.trade.project_info pi,jbo.trade.acct_loan li," +
        					" jbo.trade.acct_payment_schedule ps " +
        					" where uc.userid=:userid and uc.relativetype='001' " +
        					" and uc.contractid=pi.contractid " +
        					" and uc.contractid=ci.contractid and ci.loanno=li.serialno " +
        					" and ps.objectno=ci.loanno " +
        					" group by ps.objectno,pi.projectname,pi.serialno," +
        					" li.putoutdate,li.maturitydate,li.businesssum,li.loanstatus"
					);
          
			query.setParameter("userid",sUserID);
			
			List<BizObject> list = query.getResultList(false);
			JSONArray array = new JSONArray();
			if(list != null){
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					double sPaySum = Double.parseDouble(
							o.getAttribute("SUM1").toString()==null?
									"0":o.getAttribute("SUM1").toString());
					obj.put("ObjectNo", o.getAttribute("OBJECTNO").getValue()==null?
							"":o.getAttribute("OBJECTNO").getString());//借据号
					obj.put("ProjectName", o.getAttribute("PROJECTNAME").getValue()==null?
							"":o.getAttribute("PROJECTNAME").getString());//项目名称|申请号
					obj.put("SerialNo", o.getAttribute("SERIALNO").getValue()==null?
							"":o.getAttribute("SERIALNO").getString());//项目名称|申请号
					obj.put("PutOutDate", o.getAttribute("PUTOUTDATE").getValue()==null?
							"":o.getAttribute("PUTOUTDATE").getString());//申请时间
					obj.put("MaturityDate", o.getAttribute("MATURITYDATE").getValue()==null?
							"":o.getAttribute("MATURITYDATE").getString());//结束时间
					obj.put("BusinessSum", o.getAttribute("BUSINESSSUM").getValue()==null?
							"":o.getAttribute("BUSINESSSUM").getDouble());//借款本金(元) 
					obj.put("PaySum", GeneralTools.numberFormat(sPaySum, 0, 2));//还款总额(元)
					obj.put("LoanStatus", o.getAttribute("LOANSTATUS").getValue()==null?
							"":o.getAttribute("LOANSTATUS").getString());//状态
					array.add(obj);
				}
 			}
			
			result.put("RepaymentArray", array);
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getinvestmentaccountinfo.error");
		}
	}
	

	/**
	 * 获取推荐项目
	 * 
	 * @param jbo
	 * @param sUserID
	 * @throws Exception
	 */
	private void getRecommendProject(JBOFactory jbo)
			throws HandlerException {
		// select * from o where (INVALIDDATE < :INVALIDDATE and BEGINDATE
		// >=:BEGINDATE) or INVALIDDATE is null and status='1' order by
		// TopRecordTime desc
		try {
			BizObjectManager m = jbo
					.getManager("jbo.trade.project_info");
			BizObjectQuery query = m
					.createQuery("select * from  o where (INVALIDDATE < :INVALIDDATE and BEGINDATE >=:BEGINDATE) or  INVALIDDATE is null and status=:status order by  TopRecordTime desc ");
			query.setParameter("INVALIDDATE",GeneralTools.getDate());
			query.setParameter("BEGINDATE",GeneralTools.getDate());
			query.setParameter("status","1");
			query.setFirstResult(0);
			query.setMaxResults(2);
			List<BizObject> list = query.getResultList(false);
			JSONArray array = new JSONArray();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					obj.put("Serialno",o.getAttribute("SERIALNO").toString()==null ? "" : o.getAttribute("SERIALNO").toString());
					obj.put("ProjectName",o.getAttribute("PROJECTNAME").toString()==null ? "" : o.getAttribute("PROJECTNAME").toString());
					obj.put("LoanTerm",o.getAttribute("LOANTERM").toString()==null ? "" : o.getAttribute("LOANTERM").toString());
					obj.put("LoanRate",o.getAttribute("LOANRATE").toString()==null ? "" : o.getAttribute("LOANRATE").toString());
					obj.put("BeginTime",o.getAttribute("BEGINTIME").toString()==null ? "" : o.getAttribute("BEGINTIME").toString());
					obj.put("BeginDate",o.getAttribute("BEGINDATE").toString()==null ? "" : o.getAttribute("BEGINDATE").toString());
					obj.put("LoanAmount",o.getAttribute("LOANAMOUNT").toString()==null ? "" : o.getAttribute("LOANAMOUNT").toString());
					obj.put("Status",o.getAttribute("STATUS").toString()==null ? "" : o.getAttribute("STATUS").toString());
					obj.put("PaymentMethod",o.getAttribute("PAYMENTMETHOD").toString()==null ? "" : o.getAttribute("PAYMENTMETHOD").toString());
					array.add(obj);
				}
 			}
			result.put("ProjectArray", array);
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getinvestmentaccountinfo.error");
		}
	}
	
	
}
