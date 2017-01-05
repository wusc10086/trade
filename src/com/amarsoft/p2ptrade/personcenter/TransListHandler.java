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
 * 交易明细查询交易 
 * 输入参数： 
 * 			UserID:账户编号 
 * 	
 * 			（非必输参数）
 * 			PageSize：每页的条数;
 *			CurPage：当前页;
 *			StartDate：起始创建日期
 *			EndDate：终止创建日期
 *			TransType：交易类型 10：充值 20 提现 30：收款  40 还款 50 投资 60 放款
 * 输出参数：（列表） 
 * 			SerialNo:流水号（待确定） 
 * 			TransTime:时间
 * 			TransTypeCode:类型 
 * 			TransTypeName:类型名称 
 * 			InAmount:收入 
 * 			OutAmount:支出 
 * 			Balance:账户余额 
 * 			Remark:备注
 */
public class TransListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	private String sStartDate;
	private String sEndDate;
	private String sTransType;
	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getTransList(request);

	}

	/**
	 * 获取交易明细查询列表
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getTransList(JSONObject request) throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}

		if(request.containsKey("PageSize"))
			this.pageSize = Integer.parseInt(request.get("PageSize").toString());
		if(request.containsKey("CurPage"))
			this.curPage = Integer.parseInt(request.get("CurPage").toString());
		if(request.containsKey("StartDate"))
			this.sStartDate = request.get("StartDate").toString();
		if(request.containsKey("EndDate"))
			this.sEndDate = request.get("EndDate").toString();
		if(request.containsKey("TransType"))
			this.sTransType = request.get("TransType").toString();
		
		String sUserID = request.get("UserID").toString();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			
			JSONObject statusObj = GeneralTools.getItemName(jbo, "TransCode");//获取所有交易类型的码值说明
			
			BizObjectManager m = jbo
					.getManager("jbo.trade.transaction_record");
			String sQuerySql = getQuerySql();
			BizObjectQuery query = m.createQuery(sQuerySql);
			query.setParameter("userid", sUserID);
			if(sTransType != null && !("".equals(sTransType))){
				if(sTransType.equals("10")){//充值(1010,1011,1012,1050)
					query.setParameter("transtype1", "1010");
					query.setParameter("transtype2", "1011");
					query.setParameter("transtype3", "1012");
					query.setParameter("transtype4", "1050");
				}else if(sTransType.equals("20")){//提现(1020)
					query.setParameter("transtype", "102%");
				}else if(sTransType.equals("30")){//收款(1090)
					query.setParameter("transtype", "1090");
				}else if(sTransType.equals("40")){//还款(1030,1032,1040)
					query.setParameter("transtype1", "1030");
					query.setParameter("transtype2", "1032");
					query.setParameter("transtype3", "1040");
				}else if(sTransType.equals("50")){//投资(1061)
					query.setParameter("transtype", "1061");
				}else if(sTransType.equals("60")){//放款(1060)
					query.setParameter("transtype", "1060");
				}
			}else{
				query.setParameter("transtype1", "1070");
				query.setParameter("transtype2", "1071");
				query.setParameter("transtype3", "1080");
			}
			if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
				query.setParameter("startdate", sStartDate + "00:00:00");
				query.setParameter("enddate", sEndDate + "23:59:59" );
			}
			
			int firstRow = curPage * pageSize;
			if(firstRow < 0){
				firstRow = 0;
			}
			int maxRow = pageSize;
			if(maxRow <= 0){
				maxRow = 10;
			}
			query.setFirstResult(firstRow);
			if(request.containsKey("PageSize")){
				query.setMaxResults(maxRow);
			}
			
			int totalAcount = query.getTotalCount();
			int temp = totalAcount % pageSize;
			int pageCount = totalAcount / pageSize;
			if(temp != 0){
				pageCount += 1;
			}
			
			List<BizObject> list = query.getResultList(false);

			JSONArray array = new JSONArray();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();

					String sUpdateTime = o.getAttribute("UPDATETIME").toString();// 更新时间（回盘时间）

					double realAmount = Double.parseDouble(null == (o
							.getAttribute("ACTUALAMOUNT").toString()) ? "0" : o
							.getAttribute("ACTUALAMOUNT").toString());// 实际到帐金额
					
					// 交易类型码值
					String sTransTypeCode = o.getAttribute("TRANSTYPE").toString();
					// 交易类型码值说明
					String sTransTypeName  = statusObj.containsKey(sTransTypeCode)?statusObj.get(sTransTypeCode).toString():sTransTypeCode;
					
					if(sTransTypeName.contains("充值")){//几种充值形式都划分为充值
						sTransTypeName = "充值";
					}else if(sTransTypeName.contains("提现")){//几种提现形式都划分为提现
						sTransTypeName = "提现";
					}else if(sTransTypeName.contains("还款")){//几种还款形式都划分为还款
						sTransTypeName = "还款";
					}else if(sTransTypeName.contains("收款") || sTransTypeName.contains("收益")){//几种收款形式都划分为收款
						sTransTypeName = "收款";
					}else if(sTransTypeName.contains("贷款发放")){//几种还款形式都划分为还款
						sTransTypeName = "放款";
					}
					
					String sDirection = o.getAttribute("DIRECTION").toString() == null ?"":o.getAttribute("DIRECTION").toString();//发生方向
					
					if(sDirection.equals("R")){//收入
						obj.put("InAmount", String.valueOf(realAmount));// 收入金额
						obj.put("OutAmount", "");// 支出金额
					}else{//支出
						obj.put("InAmount", "");// 收入金额
						obj.put("OutAmount", String.valueOf(realAmount));// 支出金额
					}
					
					String sBalance = o.getAttribute("BALANCE").toString() == null ?"0":o.getAttribute("BALANCE").toString();
					String sRemark = o.getAttribute("Remark").toString() == null ?"":o.getAttribute("Remark").toString();
					obj.put("TransTime", sUpdateTime);// 时间
					obj.put("TransTypeCode", sTransTypeCode);// 类型code
					obj.put("TransTypeName", sTransTypeName);// 类型name
					obj.put("Balance", String.valueOf(Double.parseDouble(sBalance)));// 余额
					obj.put("Remark", sRemark);// 备注

					array.add(obj);
				}
			}
			JSONObject result = new JSONObject();
			result.put("RootType", "030");// 返回形式为列表
			result.put("TotalAcount", String.valueOf(totalAcount));// 该条件下从数量
			result.put("PageCount", String.valueOf(pageCount));
			result.put("array", array);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryusertranslist.error");
		}
	}

	private String getQuerySql(){
		String sQuerySql = "userid=:userid and status = '10'";
		if(sTransType != null && !("".equals(sTransType))){
			if(sTransType.equals("10")){//充值(1010,1011,1012,1050)
				sQuerySql = sQuerySql + " and transtype in (:transtype1,:transtype2,:transtype3,:transtype4)";
			}else if(sTransType.equals("20")){//提现(1020)
				sQuerySql = sQuerySql + " and transtype like :transtype";
			}else if(sTransType.equals("30")){//收款(1090)
				sQuerySql = sQuerySql + " and transtype = :transtype";
			}else if(sTransType.equals("40")){//还款(1030,1032,1040)
				sQuerySql = sQuerySql + " and transtype in (:transtype1,:transtype2,:transtype3)";
			}else if(sTransType.equals("50")){//投资(1061)
				sQuerySql = sQuerySql + " and transtype  = :transtype";
			}else if(sTransType.equals("60")){//放款(1060)
				sQuerySql = sQuerySql + " and transtype  = :transtype";
			}
		}else{
			sQuerySql = sQuerySql + " and transtype not in (:transtype1,:transtype2,:transtype3)";
		}
		if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
			sQuerySql = sQuerySql + " and updatetime between :startdate and :enddate";
		}
		sQuerySql = sQuerySql + " order by updatetime desc,serialno desc";
		return sQuerySql;
	}
	
}
