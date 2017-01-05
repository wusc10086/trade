package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

/**
 * 充值历史记录查询交易 
 * 输入参数： 
 * 		UserID:账户编号 
 * 
 *   	（非必输参数）
 * 		PageSize：每页的条数;
 *		CurPage：当前页;
 *		StartDate：起始创建日期
 *		EndDate：终止创建日期
 *		TransStatus：交易状态  
 * 输出参数：（列表） 
 * 		SerialNo:流水号（待确定） 
 * 		InputTime:交易时间
 * 		Amount:充值金额 
 * 		HandlCharge:手续费 
 * 		RealCharge:实际到账金额 
 * 		TranMethod:交易方式
 * 		Status:状态 
 * 		Remark:备注
 */ 
public class ChargeHistoryListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	private String sTransStatus;
	private String sStartDate;
	private String sEndDate;
	private String sDates;
	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getChargeHistoryList(request);
	}

	/**
	 * 获取历史充值记录
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getChargeHistoryList(JSONObject request)
			throws HandlerException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}

		if(request.containsKey("PageSize"))
			this.pageSize = Integer.parseInt(request.get("PageSize").toString());
		if(request.containsKey("CurPage"))
			this.curPage = Integer.parseInt(request.get("CurPage").toString());
		if(request.containsKey("Dates"))
			this.sDates = request.get("Dates").toString();
		if(request.containsKey("StartDate"))
			this.sStartDate = request.get("StartDate").toString();
		if(request.containsKey("EndDate"))
			this.sEndDate = request.get("EndDate").toString();
		if(!"".equals(sDates)){
			this.sEndDate = sdf.format(cal.getTime());
			this.sStartDate = getStartDate(sDates, cal, sdf);
		}
		if(request.containsKey("TransStatus"))
			this.sTransStatus = request.get("TransStatus").toString();
		String sUserID = request.get("UserID").toString();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo
					.getManager("jbo.trade.transaction_record");
			String sQuerySql = getQuerySql();
			BizObjectQuery query = m.createQuery(sQuerySql);
			query.setParameter("transtype1", "1010");//交易类型：充值
			query.setParameter("transtype2", "1011");
			query.setParameter("transtype3", "1012");
			query.setParameter("transtype4", "1050");
			query.setParameter("transtype5", "1013");
			query.setParameter("userid", sUserID);
			
			if(sTransStatus != null && !("".equals(sTransStatus))){
				if(sTransStatus.equals("010")){//充值成功
					query.setParameter("status", "10");
				}else if(sTransStatus.equals("001")){//充值失败
					query.setParameter("status1", "04");
					query.setParameter("status2", "20");
					query.setParameter("status3", "00");
				}else if(sTransStatus.equals("100")){//充值中
					query.setParameter("status1", "04");
					query.setParameter("status2", "20");
					query.setParameter("status3", "10");
					query.setParameter("status4", "00");
				}else if(sTransStatus.equals("110")){//成功+处理中
					query.setParameter("status1", "04");
					query.setParameter("status2", "20");
					query.setParameter("status3", "00");
				}else if(sTransStatus.equals("011")){//成功+失败
					query.setParameter("status1", "04");
					query.setParameter("status2", "20");
					query.setParameter("status3", "10");
					query.setParameter("status4", "00");
				}else if(sTransStatus.equals("101")){//失败+处理中
					sQuerySql = sQuerySql + " and status <> :status";
					query.setParameter("status", "10");
				}
			}
			if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
				query.setParameter("startdate", sStartDate + " 00:00:00");
				query.setParameter("enddate", sEndDate + " 23:59:59");
			}
			
			//分页
			int totalAcount = query.getTotalCount();
    		int pageCount = (totalAcount + pageSize - 1) / pageSize;
    		if (curPage > pageCount)
    			curPage = pageCount;
    		if (curPage < 1)
    			curPage = 1;
    		query.setFirstResult((curPage - 1) * pageSize);
    		query.setMaxResults(pageSize);
    		
			List<BizObject> list = query.getResultList(false);
			JSONArray array = new JSONArray();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();

					String sSerialNo = o.getAttribute("SERIALNO").toString();// 流水号
					String sInputTime = o.getAttribute("INPUTTIME").toString();// 创建时间
					String sTranstype = o.getAttribute("TRANSTYPE").toString();// 创建时间
					String sTranMethod = null;
					if(sTranstype.equals("1012")){//网关
						sTranMethod = "网关";
					}else{
						sTranMethod = "银行卡";
					}

					double amount = Double.parseDouble(null == (o
							.getAttribute("AMOUNT").toString()) ? "0" : o
							.getAttribute("AMOUNT").toString());// 交易金额
					double handlCharge = Double.parseDouble(null == (o
							.getAttribute("HANDLCHARGE").toString()) ? "0" : o
							.getAttribute("HANDLCHARGE").toString());// 手续费
//					double realCharge = amount - handlCharge;// 实际到帐金额
					
					double realCharge = Double.parseDouble(null == (o
							.getAttribute("ACTUALAMOUNT").toString()) ? "0" : o
							.getAttribute("ACTUALAMOUNT").toString());// 实际到帐金额

					String sStatus = o.getAttribute("STATUS").toString();
					String sStatusName = null;
					//状态确定
					if (sStatus.equals("10")) {
						sStatusName = "成功";
					} else if (sStatus.equals("04") || sStatus.equals("20") || sStatus.equals("00")) {
						sStatusName = "失败";
					} else {
						sStatusName = "充值中";
					}
					String sRemark = o.getAttribute("REMARK").toString();

					obj.put("SerialNo", sSerialNo);// 流水号
					obj.put("InputTime", sInputTime);// 创建时间
					obj.put("Amount", String.valueOf(amount));// 充值金额
					obj.put("HandlCharge", String.valueOf(handlCharge));// 手续费
					obj.put("RealCharge", String.valueOf(realCharge));// 实际到账金额
					obj.put("TranType", sTranstype);// 交易类型
					obj.put("TranMethod", sTranMethod);// 交易方式
					obj.put("Status", sStatus);// 交易状态code
					obj.put("StatusName", sStatusName);// 交易状态name
					obj.put("Remark", sRemark);// 备注
					array.add(obj);
				}
			}
			
			JSONObject result = new JSONObject();
			result.put("RootType", "030");
			result.put("TotalAcount", String.valueOf(totalAcount));// 该条件下从数量
			result.put("PageSize", String.valueOf(pageSize));
			result.put("CurPage", String.valueOf(curPage));
			result.put("array", array);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("querychargehistory.error");
		}
	}
	
	/**
	 * 获取时间节点信息
	 * @param sDates
	 * @return
	 */
	private String getStartDate(String sDates,Calendar cal,SimpleDateFormat sdf ){
		if("0".equals(sDates)){
			cal.roll(Calendar.DAY_OF_MONTH, -7);
			sStartDate  = sdf.format(cal.getTime());
		}else if("1".equals(sDates)){
			cal.roll(Calendar.MONTH, -1);
			sStartDate  = sdf.format(cal.getTime());
		}else if("2".equals(sDates)){
			cal.roll(Calendar.MONTH, -2);
			sStartDate  = sdf.format(cal.getTime());
		}else if("3".equals(sDates)){
			cal.roll(Calendar.MONTH, -3);
			sStartDate  = sdf.format(cal.getTime());
		}else if("4".equals(sDates)){
			cal.roll(Calendar.MONTH, -6);
			sStartDate  = sdf.format(cal.getTime());
		}
		return sStartDate;
	}
	
	
	private String getQuerySql(){
		String sQuerySql = "transtype in (:transtype1,:transtype2,:transtype3,:transtype4,:transtype5) and userid=:userid";
		if(sTransStatus != null && !("".equals(sTransStatus))){
			if(sTransStatus.equals("010")){//充值成功
				sQuerySql = sQuerySql + " and status = :status";
			}else if(sTransStatus.equals("001")){//充值失败
				sQuerySql = sQuerySql + " and status in (:status1,:status2,:status3)";
			}else if(sTransStatus.equals("100")){//充值中
				sQuerySql = sQuerySql + " and status not in (:status1,:status2,:status3,:status4)";
			}else if(sTransStatus.equals("110")){//成功+处理中
				sQuerySql = sQuerySql + " and status not in (:status1,:status2,:status3)";
			}else if(sTransStatus.equals("011")){//成功+失败
				sQuerySql = sQuerySql + " and status in (:status1,:status2,:status3,:status4)";
			}else if(sTransStatus.equals("101")){//失败+处理中
				sQuerySql = sQuerySql + " and status <> :status";
			}
		}
		if(sStartDate !=null && !("".equals(sStartDate)) && sEndDate != null && !("".equals(sEndDate))){
			sQuerySql = sQuerySql + " and inputtime between :startdate and :enddate";
		}
		sQuerySql = sQuerySql + " order by serialno desc";
		return sQuerySql;
	}
}
