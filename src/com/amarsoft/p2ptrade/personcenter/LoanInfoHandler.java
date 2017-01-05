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
 * 借款信息查询
 * 输入参数：
 * 		ProjectSerialNo:申请号
 * 输出参数： 
 * 项目基本信息：
 *      CONTRACTID合同号
 *      PROJECTNAME|SERIALNO项目名称|申请号  project
 *      还款期PUTOUTDATE-MATURITYDATE loan 
 *      总期数LOANTERM loan
 *      BUSINESSSUM借款本金(元) loan
 *      正常本金 NORMALBALANCE
 *      逾期本金+逾期利息+罚息+复利
 *      OVERDUEBALANCE+INTERESTBALANCE+FINEINTEBALANCE+COMPINTEBALANCE
 *      实际还款总额(元) 实 ACTUALPAYCORPUSAMT+ACTUALPAYINTEAMT+ACTUALFINEAMT payment
 *      LOANSTATUS状态 loan
 * 还款明细： 
 *      ActualPayCorpusAmt已还本金
 *      ActualPayInteAmt已还利息
 *      ActualFineAmt已付逾期罚息
 *      ActualPayFeeAmt1已付担保费
 *      已付提前还款违约金
 *      已付担保管理费
 *      已付追偿款
 * 还款计划表：
 *      SeqId期数
 *      PAYDATE还款截止日 project
 *      PayCorpusAmt+PayInteAmt+PayFineAmt+PayFeeAmt1应还总额(元)
 *      PayCorpusAmt应还本金(元)
 *      PayInteAmt应还利息(元)
 *      PayFineAmt应付逾期罚息(元)
 *      PayFeeAmt1应付担保费(元)
 *      Status状态
 */
public class LoanInfoHandler extends JSONHandler {
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfo(request);
	}
	
	/**
	 * 借款信息查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfo(JSONObject request)throws HandlerException {
		
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		
		if(request.get("SerialNo")==null || "".equals(request.get("SerialNo"))){
			throw new HandlerException("serialno.error");
		}
		 
		if(request.get("ObjectNo")==null || "".equals(request.get("ObjectNo"))){
			throw new HandlerException("objectno.error");
		}
		
		String sUserID = request.get("UserID").toString();
		String sSerialNo = request.get("SerialNo").toString();
		String sObjectNo = request.get("ObjectNo").toString();
		String sPaymentFlag = paymentFlag(sObjectNo,sUserID);
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query = m.createQuery(
					 "select uc.contractid,pi.projectname,pi.serialno,li.putoutdate,li.maturitydate," +
					 " li.loanterm,li.businesssum,li.loanstatus " +
					 " from jbo.trade.project_info pi, " +
					 " jbo.trade.acct_loan li,jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci " +
					 " where uc.userid=:userid and uc.relativetype='001' and " +
					 " pi.serialno=:serialno and uc.contractid=pi.contractid " +
					 " and uc.contractid=ci.contractid and ci.loanno=li.serialno "  
					);
			query.setParameter("userid",sUserID);
			query.setParameter("serialno",sSerialNo);

			BizObject o = query.getSingleResult(false);
			if(o!=null){
				String sProjectName = o.getAttribute("PROJECTNAME").getValue()==null?
						"":o.getAttribute("PROJECTNAME").getString();
				String sSerialNo1 = o.getAttribute("SERIALNO").getValue()==null?
						"":o.getAttribute("SERIALNO").getString();
				String sPutoutDate = o.getAttribute("PUTOUTDATE").getValue()==null?
						"":o.getAttribute("PUTOUTDATE").getString();
				String sMaturityDate = o.getAttribute("MATURITYDATE").getValue()==null?
						"":o.getAttribute("MATURITYDATE").getString();
				String sProjectSerialNo = sProjectName + "|" + sSerialNo1;
				String sPutMatDate = sPutoutDate + "-" + sMaturityDate;
				
				result.put("RootType", "030");
				result.put("ContractId", o.getAttribute("CONTRACTID").getValue()==null?
						"":o.getAttribute("CONTRACTID").getString());//合同号
				result.put("PayFlag", sPaymentFlag);//状态 
				result.put("ProjectSerialNo", sProjectSerialNo);//项目名称|申请号
				result.put("PayDate", sPutMatDate);//还款期
				result.put("LoanTerm", o.getAttribute("LOANTERM").getValue()==null?
						0:o.getAttribute("LOANTERM").getInt());//总期数
				result.put("BusinessSum", GeneralTools.numberFormat(Double.parseDouble(o.getAttribute("BUSINESSSUM").getValue()==null?
						"0":o.getAttribute("BUSINESSSUM").toString()), 0, 2));//借款本金(元)
//				result.put("PayFlag", sLoanStatusCode);//状态 
//				result.put("LoanStatusName", sLoanStatusName);//状态中文
			}
			
		    BizObjectManager m1 = jbo.getManager("jbo.trade.acct_back_bill");
			BizObjectQuery query1 = m1.createQuery(
					"select sum(bb.ActualPayCorpusAmt) as v.ActualPayCorpusAmt," +
					"sum(bb.ActualPayInteAmt) as v.ActualPayInteAmt," +
					"sum(bb.ActualFineAmt) as v.ActualFineAmt," +
					"sum(bb.ActualPayFeeAmt1) as v.ActualPayFeeAmt1," +
					"sum(bb.PayExpiationSum) as v.PayExpiationSum," +
					"sum(bb.PayGuaranteeDmanage) as v.PayGuaranteeDmanage," +
					"sum(bb.ActualCredere) as v.ActualCredere " +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
					" jbo.trade.acct_back_bill bb " +
					" where uc.userid=:userid and uc.relativetype='001' " +
					" and bb.objectno=:objectno and " +
					" uc.contractid=ci.contractid and ci.loanno=bb.objectno ");
			  query1.setParameter("userid",sUserID);
			  query1.setParameter("objectno",sObjectNo);
			  BizObject o1 = query1.getSingleResult(false);
			  if(o1!=null){
				result.put("ActualPayCorpusAmt", o1.getAttribute("ACTUALPAYCORPUSAMT").getValue()==null?
				        "0":o1.getAttribute("ACTUALPAYCORPUSAMT").getDouble());//已还本金
				result.put("ActualPayInteAmt", o1.getAttribute("ACTUALPAYINTEAMT").getValue()==null?
						"0":o1.getAttribute("ACTUALPAYINTEAMT").getDouble());//已还利息
				result.put("ActualFineAmt", o1.getAttribute("ACTUALFINEAMT").getValue()==null?
						"0":o1.getAttribute("ACTUALFINEAMT").getDouble());//已付逾期罚息
				result.put("ActualPayFeeAmt1", o1.getAttribute("ACTUALPAYFEEAMT1").getValue()==null?
						"0":o1.getAttribute("ACTUALPAYFEEAMT1").getDouble());//已付担保费
				result.put("PayExpiationSum", o1.getAttribute("PAYEXPIATIONSUM").getValue()==null?
						"0":o1.getAttribute("PAYEXPIATIONSUM").getDouble());//已付提前还款违约金 
				result.put("PayGuaranteeDmanage", o1.getAttribute("PAYGUARANTEEDMANAGE").getValue()==null?
						"0":o1.getAttribute("PAYGUARANTEEDMANAGE").getDouble());//已付担保管理费 
				result.put("ActualCredere", o1.getAttribute("ACTUALCREDERE").getValue()==null?
						"0":o1.getAttribute("ACTUALCREDERE").getDouble());//已付追偿款
			  }
				
				BizObjectManager m2 =jbo.getManager("jbo.trade.acct_payment_schedule");
				BizObjectQuery query2 = m2.createQuery(
						"select ps.objectno,ps.seqid,ps.paydate,ps.paycorpusamt," +
						" ps.payinteamt,ps.payfineamt,ps.payfeeamt1,ps.status " +
						" from  jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci," +
						" jbo.trade.acct_payment_schedule ps " +
						" where uc.userid=:userid and uc.relativetype='001' " +
						" and  ps.objectno=:objectno and " +
						" uc.contractid=ci.contractid and ci.loanno=ps.objectno" +
						" order by ps.seqid");
				query2.setParameter("userid",sUserID);
				query2.setParameter("objectno",sObjectNo);
				 
				List<BizObject> list = query2.getResultList(false);
				if (list != null) {
					JSONArray array = new JSONArray();
					for (int i = 0; i < list.size(); i++) {
						BizObject o2 = list.get(i);
						JSONObject obj = new JSONObject();
						String sStatusCode = o2.getAttribute("STATUS").getValue()==null?
								"":o2.getAttribute("STATUS").getString();
						JSONObject sStatusName = GeneralTools.getItemName(jbo, "PaymentStatus");
						double sPayCorpusAmt = Double.parseDouble(o2.getAttribute("PAYCORPUSAMT").getValue()==null?
								"0":o2.getAttribute("PAYCORPUSAMT").toString());
						double sPayInteAmt = Double.parseDouble(o2.getAttribute("PAYINTEAMT").getValue()==null?
								"0":o2.getAttribute("PAYINTEAMT").toString());
						double sPayFineAmt = Double.parseDouble(o2.getAttribute("PAYFINEAMT").getValue()==null?
								"0":o2.getAttribute("PAYFINEAMT").toString());
						double sPayFeeAmt1 = Double.parseDouble(o2.getAttribute("PAYFEEAMT1").getValue()==null?
								"0":o2.getAttribute("PAYFEEAMT1").toString());
						double sActualSum = sPayCorpusAmt + sPayInteAmt + sPayFineAmt + sPayFeeAmt1;
						String sPayDate = o2.getAttribute("PAYDATE").getValue()==null?
								"":o2.getAttribute("PAYDATE").getString();
						obj.put("ObjectNo", o2.getAttribute("OBJECTNO").getValue()==null?
								"":o2.getAttribute("OBJECTNO").getString());//借据号
						
						obj.put("SeqId", o2.getAttribute("SeqId").getValue()==null?
								0:o2.getAttribute("SeqId").getInt());//期数
						
						obj.put("PayDate", sPayDate);//还款截止日 
						obj.put("ActualSum",GeneralTools.numberFormat(sActualSum, 0, 2));//应还总额(元)
						obj.put("PayCorpusAmt", sPayCorpusAmt);//应还本金(元)
						obj.put("PayInteAmt", sPayInteAmt);//应还利息(元)
						obj.put("PayFineAmt", sPayFineAmt);//应付逾期罚息(元)
						obj.put("PayFeeAmt1", sPayFeeAmt1);//应付担保费(元)
						obj.put("StatusCode", sStatusCode);//状态 
						obj.put("StatusName", sStatusName);//状态 中文
						array.add(obj);
			}
					result.put("array", array);
				}
				// 	实际还款总额
				BizObjectManager m3 = jbo.getManager("jbo.trade.acct_back_bill");
				BizObjectQuery query3 = m3.createQuery(
						 "select sum(bb.ActualPayCorpusAmt+bb.ActualPayInteAmt+bb.ActualFineAmt+bb.ActualPayFeeAmt1+" +
						 " bb.PayExpiationSum+bb.PayGuaranteeDmanage+bb.ActualCredere+BB.ACTUALPAYFEEAMT2+BB.ACTUALPLANTMANAGE)  as v.sum1 " +
						 " from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci,jbo.trade.acct_back_bill bb" +
						 " where uc.userid=:userid and uc.relativetype='001' and bb.objectno=:objectno and uc.contractid=ci.contractid and ci.loanno=bb.objectno"
						);
				query3.setParameter("userid",sUserID);
				query3.setParameter("objectno",sObjectNo);
				BizObject o3 = query3.getSingleResult(false);
				if(o3!=null){
					result.put("PaySum", o3.getAttribute("SUM1").getValue()==null?
							"0":o3.getAttribute("SUM1").getDouble());//实际还款总额(元)
				}else{
					result.put("PaySum", 0);//实际还款总额(元)
				}
				 
			return result;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryloanlist.error");
		}
	}
	
	/**
	 * 手动还款 提前还款标识
	 * @param sObjectNo
	 * @param sUserID
	 * @return
	 * @throws HandlerException
	 */
	private String  paymentFlag(String  sObjectNo ,String sUserID) throws HandlerException {
		String sFlag = "";
		JBOFactory jbo=JBOFactory.getFactory();
		try {
			BizObjectManager manager = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query =manager.createQuery(
					"select (li.OVERDUEBALANCE+li.INTERESTBALANCE+li.FINEINTEBALANCE+li.COMPINTEBALANCE+li.FEEAMTBALANCE+li.FEEAMT2BALANCE) as v.OverBalance " +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci,jbo.trade.acct_loan li " + 
					" where uc.userid=:userid and uc.relativetype='001' and li.serialno=:objectno and li.loanstatus in ('0','1') " +
					" and uc.contractid=ci.contractid and ci.loanno=li.serialno");
			query.setParameter("userid",sUserID);
			query.setParameter("objectno",sObjectNo);
			BizObject o = query.getSingleResult(false);	
 			if(o!=null){
 			double sOverBalance = Double.parseDouble(o.getAttribute("OVERBALANCE").getValue()==null?
 					"0":o.getAttribute("OVERBALANCE").toString());
 			if(sOverBalance>0){
 				sFlag = "1";//手动还款
 			} 
 			}
 			
 			BizObjectManager manager1 = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query1 =manager1.createQuery(
					"select (li.OVERDUEBALANCE+li.INTERESTBALANCE+li.FINEINTEBALANCE+li.COMPINTEBALANCE+li.FEEAMTBALANCE+li.FEEAMT2BALANCE) as v.OverBalance " +
					" from jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci,jbo.trade.acct_loan li " + 
					" where uc.userid=:userid and uc.relativetype='001' and li.serialno=:objectno and li.loanstatus='0' and li.batchstatus='0'" +
					" and uc.contractid=ci.contractid and ci.loanno=li.serialno and " +
					" li.nextpaydate<>li.maturitydate");
			query1.setParameter("userid",sUserID);
			query1.setParameter("objectno",sObjectNo);
			BizObject o1 = query1.getSingleResult(false);	
 			if(o1!=null){
 			double sOverBalance = Double.parseDouble(o1.getAttribute("OVERBALANCE").getValue()==null?
 					"0":o1.getAttribute("OVERBALANCE").toString());
 			if(sOverBalance==0){
 				sFlag = "3";//提前还款
 			} 
 			}
			return sFlag;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		} 
	}
}
