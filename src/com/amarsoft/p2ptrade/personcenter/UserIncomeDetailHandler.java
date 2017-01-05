package com.amarsoft.p2ptrade.personcenter;

import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import com.amarsoft.app.accounting.util.NumberTools;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 个人收益明细（累计收益明细、在投收益明细）
 * 
 * 输入参数：
 * 
 * UserId： DetailType:010-累计收益明细；020-在投收益明细 curPage pageSize
 * 
 * @author hhCai 2015-2-13
 */
public class UserIncomeDetailHandler extends JSONHandler {

	private String DetailType;

	private int curPage = 0, pageSize = 20;

	private int totalAcount;

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		// TODO Auto-generated method stub
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}
		if (request.get("DetailType") == null
				|| "".equals(request.get("DetailType"))) {
			DetailType = "010";
		} else {
			DetailType = request.get("DetailType").toString();
		}
		if (request.containsKey("pageSize"))
			this.pageSize = Integer
					.parseInt(request.get("pageSize").toString());
		if (request.containsKey("pageNo"))
			this.curPage = Integer.parseInt(request.get("pageNo").toString());
		String sUserID = request.get("UserID").toString();
		JSONObject resultObject = new JSONObject();
		try {
			resultObject.put("InvestArray", getRuningInvestDetail(sUserID));// 获得收益明细.包含历史收益与在投收益
			resultObject.put("TotalAcount", totalAcount);
		} catch (Exception e) {
			// TODO: handle exception
			new HandlerException(e.getMessage());
		}
		return resultObject;
	}

	/**
	 * 获得平台奖励明细
	 * 
	 * @param userid
	 * @return
	 * @throws JBOException
	 */
	public JSONArray getRewardDetail(String userid) throws JBOException {

		return null;
	}

	/**
	 * 获得收益明细
	 * 
	 * @param sUserID
	 * @return
	 * @throws JBOException
	 */
	public JSONArray getRuningInvestDetail(String sUserID) throws JBOException {

		BizObjectManager manager = JBOFactory
				.getBizObjectManager("jbo.trade.income_detail");
		String statusrand = "010".equals(DetailType) ? "('1','3')" : "('1')";
		String sql = "select ActualPayInteAmt+ActualExpiationSum as v.ActualAmount,o.actualpaydate,o.inputdate,o.inputtime,uc.projectid,pi.PROJECTNAME,"
				+ "uc.investsum,uc.status from o,jbo.trade.user_contract uc,jbo.trade.project_info_listview pi "
				+ "where o.userid=:userid and uc.userid=o.userid and uc.subcontractno = o.subcontractno and uc.status in "
				+ statusrand
				+ "and pi.serialno=uc.projectid order by o.inputdate desc,o.inputtime desc";
		// 获取申请贷款的所有记录
		BizObjectQuery query = manager.createQuery(sql).setParameter("userid",
				sUserID);
		// 分页
		totalAcount = query.getTotalCount();
		int pageCount = (totalAcount + pageSize - 1) / pageSize;
		if (curPage > pageCount)
			curPage = pageCount;
		if (curPage < 1)
			curPage = 1;
		query.setFirstResult((curPage - 1) * pageSize);
		query.setMaxResults(pageSize);
		// 在投收益(已收)
		List<BizObject> list = query.getResultList(false);
		JSONArray jsonArray = new JSONArray();

		for (int i = 0; i < list.size(); i++) {
			BizObject o = list.get(i);
			if (o != null) {
				String projetid = o.getAttribute("PROJECTID").toString() == null ? ""
						: o.getAttribute("PROJECTID").toString();
				JSONObject jsonObject = null;
				String status = o.getAttribute("status").toString() == null ? ""
						: o.getAttribute("status").toString();
				jsonObject = new JSONObject();
				if ("3".equals(status)) {// 历史投资收益

				} else if ("1".equals(status)) {// 在投收益
					String CurMonthInteAmt, CurPayDate;
					try {
						JSONObject object = getRuningInvestCurMothIncome(
								projetid, sUserID);
						if (object != null) {
							CurMonthInteAmt = object
									.containsKey("CurMonthInteAmt") ? object
									.get("CurMonthInteAmt").toString() : "";
							CurPayDate = object.containsKey("CurPayDate") ? object
									.get("CurPayDate").toString() : "";
						} else {
							CurMonthInteAmt = CurPayDate = "";
						}

					} catch (Exception e) {
						// TODO: handle exception
						CurMonthInteAmt = CurPayDate = "";
					}
					jsonObject.put("CurMonthInteAmt", CurMonthInteAmt);
					jsonObject.put("CurPayDate", CurPayDate);
				}
				if (jsonObject != null) {
					jsonObject.put("Status", status);
					jsonObject
							.put("ActualAmount",
									o.getAttribute("ActualAmount").toString() == null ? ""
											: o.getAttribute("ActualAmount")
													.toString());// 该项目已收利息
					jsonObject
							.put("ActualPayDate",
									o.getAttribute("ACTUALPAYDATE").toString() == null ? ""
											: o.getAttribute("ACTUALPAYDATE")
													.toString());// 项目收款日期
					jsonObject.put("ProjectId", projetid);// 项目编号
					jsonObject
							.put("ProjectName",
									o.getAttribute("PROJECTNAME").toString() == null ? ""
											: o.getAttribute("PROJECTNAME")
													.toString());// 项目名称
					jsonObject.put("InvestSum",
							o.getAttribute("INVESTSUM").toString() == null ? ""
									: o.getAttribute("INVESTSUM").toString());// 项目投资总额
				}
				jsonArray.add(jsonObject);
			}
		}
		return jsonArray;
	}

	/**
	 * 获得某项目的当月应收收益
	 * 
	 * @param projetid
	 *            项目编号
	 * @return
	 */
	private JSONObject getRuningInvestCurMothIncome(String projetid,
			String userid) throws JBOException {
		String sPloanSetupDate = getPloanSetupDate();
		BizObjectManager manager = JBOFactory
				.getBizObjectManager("jbo.trade.income_schedule");
		String sql = "Select PayInteAmt as PayInteAmt,PayDate From o,jbo.trade.user_contract uc Where o.UserID = uc.UserID And PayDate like :month And uc.Status = '1' And uc.UserID =:userid and o.projectno=:projetid";
		BizObject obj = manager.createQuery(sql).setParameter("userid", userid)
				.setParameter("projetid", projetid)
				.setParameter("month", sPloanSetupDate.substring(0, 8) + "%")
				.getSingleResult(false);
		// 本月应得利息
		double dMonthInteAmt = 0;
		if (obj != null)
			dMonthInteAmt = obj.getAttribute("PayInteAmt").getDouble();
		int iMonthDay = getCurrentMonthLastDay();// 当月有多少天
		int itoday = Integer.parseInt(sPloanSetupDate.substring(8));// 当月第几天
		double MonthInteAmt = NumberTools.round(
				(dMonthInteAmt / iMonthDay * itoday), 2);// 该项目本月应得利息
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("CurMonthInteAmt", MonthInteAmt);// 该项目本月应得利息
		jsonObject.put("CurPayDate", obj.getAttribute("PayDate").toString());// 本月应收日期
		return jsonObject;
	}

	private String getPloanSetupDate() throws JBOException {
		return JBOFactory.getBizObjectManager("jbo.trade.ploan_setup")
				.createQuery("").getSingleResult(false)
				.getAttribute("curdeductdate").getString();
	}

	/**
	 * 取得当月天数
	 * */
	public static int getCurrentMonthLastDay() {
		Calendar a = Calendar.getInstance();
		a.set(Calendar.DATE, 1);// 把日期设置为当月第一天
		a.roll(Calendar.DATE, -1);// 日期回滚一天，也就是最后一天
		int maxDate = a.get(Calendar.DATE);
		return maxDate;
	}
}
