package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/**
 * 投资统计查询 输入参数： UserId:用户 名 输出参数： 已赚金额 实本理发 acct_payment_schedule
 * ACTUALPAYCORPUSAMT ACTUALPAYINTEAMT ACTUALFINEAMT ps 累计投资 sum（BUSINESSSUM）
 * loan 持有项目 count（1） 还款中 finshdate loan 逾期 loanstatus loan 代偿中 loanstatus loan
 * 
 */
public class InvestmentStatHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getInvestmentStat(request);

	}

	/**
	 * 投资统计查询
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getInvestmentStat(JSONObject request)
			throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}

		String sUserId = request.get("UserID").toString();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();

			// 已赚金额
			BizObjectManager m1 = jbo.getManager("jbo.trade.income_detail");
			BizObjectQuery query1 = m1
					.createQuery("select sum(o.actualpayinteamt+o.actualexpiationsum) as v.actualpaysum "
							+ " from o,jbo.trade.user_contract uc "
							+ " where uc.userid=:userid and uc.relativetype='002' "
							+ " and uc.subcontractno = o.subcontractno ");
			query1.setParameter("userid", sUserId);
			BizObject o1 = query1.getSingleResult(false);
			if (o1 != null) {
				result.put(
						"ActualPaySum",GeneralTools.numberFormat(o1.getAttribute("actualpaysum").getDouble()));// 已赚金额
			} else {
				result.put("ActualPaySum", 0);
			}

			// 累计投资额
			BizObjectManager m2 = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query2 = m2
					.createQuery("select sum(investsum) as v.businesssum,count(investsum) as v.count"
							+ " from o "
							+ " where userid=:userid and relativetype='002' and status  not in ('2')");
			query2.setParameter("userid", sUserId);
			BizObject o2 = query2.getSingleResult(false);
			if (o2 != null) {
				result.put("BusinessSum", GeneralTools.numberFormat(o2.getAttribute("businesssum").getDouble()));// 累计投资
			} else {
				result.put("BusinessSum", 0);
			}

			// 在投项目总数和投资金额总计
			BizObjectManager m3 = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query3 = m3
					.createQuery("select count(investsum) as v.Cnt,sum(investsum) as v.investsum "
							+ " from o "
							+ " where o.userid=:userid and o.relativetype='002' and o.status='1'"
							);
			query3.setParameter("userid", sUserId);
			BizObject o3 = query3.getSingleResult(false);
			double investsum = 0;
			if (o3 != null) {
				result.put("Cnt", o3.getAttribute("Cnt").getInt());// 持有项目数
				result.put("investsum",GeneralTools.numberFormat(o2.getAttribute("investsum").getDouble()));// 持有项目金额
			} else {
				result.put("Cnt", 0);
				result.put("investsum", 0);
			}

			// 还款中
			BizObjectManager m4 = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query4 = m4
					.createQuery("select count(1) as v.Normal "
							+ " from jbo.trade.user_contract uc,jbo.trade.project_info pi, "
							+ " jbo.trade.business_contract ci left join jbo.trade.acct_loan li on ci.baserialno=li.serialno "
							+ " where uc.userid=:userid and uc.relativetype='002' "
							+ " and uc.contractid=ci.serialno "
							+ " and uc.contractid=pi.contractid and li.loanstatus='0' ");
			query4.setParameter("userid", sUserId);
			BizObject o4 = query4.getSingleResult(false);
			if (o4 != null) {
				result.put("Normal",
						o4.getAttribute("Normal").getValue() == null ? 0 : o4
								.getAttribute("Normal").getInt());// 还款中
			} else {
				result.put("Normal", 0);
			}

			// 逾期
			BizObjectManager m5 = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query5 = m5
					.createQuery("select count(1) as v.OVER "
							+ " from jbo.trade.user_contract uc,jbo.trade.project_info pi, "
							+ " jbo.trade.business_contract ci left join jbo.trade.acct_loan li on ci.baserialno=li.serialno "
							+ " where uc.userid=:userid and uc.relativetype='002' "
							+ " and uc.contractid=ci.serialno "
							+ " and uc.contractid=pi.contractid and li.loanstatus='1' ");
			query5.setParameter("userid", sUserId);
			BizObject o5 = query5.getSingleResult(false);
			if (o5 != null) {
				result.put("Over",
						o5.getAttribute("Over").getValue() == null ? 0 : o5
								.getAttribute("Over").getInt());// 逾期
			} else {
				result.put("Over", 0);
			}

			/*// 代偿中
			BizObjectManager m6 = jbo.getManager("jbo.trade.acct_loan");
			BizObjectQuery query6 = m6
					.createQuery("select count(1) as v.Interest "
							+ " from jbo.trade.user_contract uc,jbo.trade.project_info pi, "
							+ " jbo.trade.business_contract ci left join jbo.trade.acct_loan li on ci.baserialno=li.serialno "
							+ " where uc.userid=:userid and uc.relativetype='002' "
							+ " and uc.contractid=ci.serialno "
							+ " and uc.contractid=pi.contractid and li.loanstatus='90' ");
			query6.setParameter("userid", sUserId);
			BizObject o6 = query6.getSingleResult(false);
			if (o6 != null) {
				result.put("Interest",
						o6.getAttribute("Interest").getValue() == null ? 0 : o6
								.getAttribute("Interest").getInt());// 代偿中
			} else {
				result.put("Interest", 0);
			}

			// 持有项目种类
			BizObjectManager m7 = jbo
					.getManager("jbo.trade.CORE_BUSINESS_TYPE");
			BizObjectQuery query7 = m7
					.createQuery(" select o.TYPENO,o.TYPENAME "
							+ " from jbo.trade.CORE_BUSINESS_TYPE o"
							+ " where o.typeno in  "
							+ " ("
							+ "   select bc.businesstype from jbo.trade.business_contract bc,jbo.trade.user_contract uc "
							+ "   where uc.contractid=bc.serialno "
							+ "   and uc.userid=:userid " + " )  ");// 已下架，并且有借据的
			query7.setParameter("userid", sUserId);
			List<BizObject> list = query7.getResultList(false);
			JSONArray array = new JSONArray();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					JSONObject obj = new JSONObject();
					BizObject o = list.get(i);
					obj.put("typeno",
							o.getAttribute("typeno").getValue() == null ? ""
									: o.getAttribute("typeno").getString());// 产品编号
					obj.put("typename",
							o.getAttribute("typename").getValue() == null ? ""
									: o.getAttribute("typename").getString());// 产品名称
					array.add(obj);
				}
			}

			result.put("array", array);*/
			
			//最近投资的项目
			BizObjectManager m8 = jbo.getManager("jbo.trade.user_contract");
			String sQuerySql = " o.userid=:userid and o.contractid=bc.serialno " +
					           " and o.contractid=pi.contractid " +
					           " and o.relativetype='002'" ;
			
			BizObjectQuery q8 = m8.createQuery(" select pi.serialno,bc.serialno as v.baserialno,o.contractid,"
							+ " pi.projectname,pi.begintime,pi.endtime,al.putoutdate,al.maturitydate,"
							+ " o.investsum,o.subcontractno,o.status " 
							+ " from o,jbo.trade.project_info pi, " 
							+ " jbo.trade.business_contract bc left join jbo.trade.acct_loan al on bc.serialno=al.contractserialno "
							+ " where " + sQuerySql
							+ " order by al.putoutdate desc");
			q8.setParameter("userid", sUserId);
			List<BizObject> list8 = q8.getResultList(false);
			JSONArray arry = new JSONArray();
			for(BizObject o : list8){
				JSONObject j = new JSONObject();
				String serialno = o.getAttribute("serialno").toString();
				String subcontractno = o.getAttribute("subcontractno").toString();
				String projectname = o.getAttribute("projectname").toString();
				String putoutdate = o.getAttribute("putoutdate").toString();
				String maturitydate = o.getAttribute("maturitydate").toString();
				String sum = o.getAttribute("investsum").toString();
				String status = o.getAttribute("status").toString();
				
				j.put("serialno",serialno);
				j.put("subcontractno",subcontractno);
				j.put("projectname",projectname);
				j.put("putoutdate",putoutdate);
				j.put("maturitydate",maturitydate);
				j.put("sum",sum);
				j.put("status",status);
				arry.add(j);
			}
			result.put("arry", arry);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryinvestmentstat.error");
		}
	}
}