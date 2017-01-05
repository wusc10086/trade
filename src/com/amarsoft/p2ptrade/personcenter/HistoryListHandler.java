package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.amarsoft.account.common.ObjectBalanceUtils;
import com.amarsoft.account.common.ObjectConstants;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.dict.als.manage.CodeManager;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/**
 * 交易历史记录查询 
 * 输入参数：  
 * 			UserID:账户编号
 *      	（非必输参数）
  * 			WithBalance:是否查询余额
 * 			PageSize：每页的条数;
 *			CurPage：当前页;
 *			StartDate：起始日期
 *			EndDate：终止日期
 *			TransType：交易类型
 *			Dates：日期范围
 * 输出参数：（列表） 
 * 			SerialNo：流水号（待确定） 
 * 			CreateTime：创建时间
 * 			TransTType：类型明细
 * 			Direction：操作
 * 			Balance：余额
 * 			Amount：操作数额
 */
public class HistoryListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	private String sTransType;
	private String sStartDate;
	private String sEndDate;
	private String sDates;
	private String signflag;
	
	JSONObject result = new JSONObject();

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getWithDrawHistoryList(request);

	}

	/**
	 * 获取记录
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getWithDrawHistoryList(JSONObject request)
			throws HandlerException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		//参数初始化
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
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
		if(request.containsKey("TransType"))
			this.sTransType = request.get("TransType").toString();
		
		String sUserID = request.get("UserID").toString();
		

		if(request.containsKey("signflag"))
			signflag = request.get("signflag").toString();
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			getAccountBalance(request,jbo, sUserID);
			BizObjectManager m = jbo
					.getManager("jbo.trade.transaction_record");
			String sQuerySql = getQuerySql();
			BizObjectQuery query = m.createQuery(sQuerySql);
			query.setParameter("userid", sUserID);
//			if(sTransType != null && !("".equals(sTransType))){
//				
//			}
			
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
					double amount = Double.parseDouble(o.getAttribute("AMOUNT").toString()==null ? "0" : o.getAttribute("AMOUNT").toString());
					double in = 0.0;
					double out = 0.0;
					String direction = o.getAttribute("DIRECTION").toString();
					String remark = o.getAttribute("REMARK").toString();
					if("P".equals(direction)){
						 out = amount;
					}else{
						 in = amount;
					}
					String balance = o.getAttribute("BALANCE").toString()==null?"":o.getAttribute("BALANCE").toString();
					String transtype = o.getAttribute("TRANSTYPE").toString()==null?"":o.getAttribute("TRANSTYPE").toString();
					String sTransType = null;
					if("2030".contains(transtype)){
						sTransType = "邀请好友-注册";
					} else if("2040".contains(transtype)){
						sTransType = "邀请好友-投资";
					} else if("2050".contains(transtype)){
						sTransType = "签到";
					}
					else if("1010,1011,1012,1013,1015,1050".contains(transtype))
						sTransType = "充值";
					else if("1020,1025".contains(transtype))
						sTransType = "提现";
					else if("1061".contains(transtype))
						sTransType = "投资";
					else if("1090".contains(transtype))
						sTransType = "收益入账";
					else {
						sTransType = CodeManager.getItemName("TransCode", transtype);
						if(sTransType==null || sTransType.trim().equals(""))
							sTransType = "其他";
					}
					//交易状态
					String status = o.getAttribute("status").toString()==null?"":o.getAttribute("status").toString();
					//对外显示的交易状态  status ="10,01,03,04";//成功、待处理、处理中、失败
					String sStatusName = "";
					if("10".equals(status)){
						sStatusName = "成功";
					}else if ("01".equals(status)){
						sStatusName = "待处理";
					}else if ("03".equals(status)){
						sStatusName = "处理中";
					}else if ("04".equals(status)){
						sStatusName = "失败";
					}
					else{
						sStatusName = CodeManager.getItemName("sStatusName", status);
					}
					
					obj.put("Status", sStatusName);
					obj.put("SerialNo", sSerialNo);
					obj.put("InputTime", sInputTime);
					obj.put("AmountIn", in);
					obj.put("AmountOut", out);
					obj.put("Balance", balance);
					obj.put("TransType", sTransType);
					obj.put("Remark", remark);
					array.add(obj);
				}
			}
			result.put("RootType", "030");
			result.put("TotalAcount", String.valueOf(totalAcount));// 该条件下从数量
			result.put("PageSize", String.valueOf(pageSize));
			result.put("CurPage", String.valueOf(curPage));
			result.put("array", array);

			return result;
		}catch (HandlerException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryhistory.error");
		}
	}

	/**
	 * 账户余额查询
	 * @param jbo   JBOFactory
	 * @param sUserID   用户编号
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private void getAccountBalance(JSONObject request,JBOFactory jbo,String sUserID)
			throws HandlerException {
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			
			double usableBalance = 0.0;
			double frozenBalance = 0.0;
			
					
			//获取客户名下所有的余额 modify by xjqin 20150120
			
			if(request.containsKey("WithBalance") && "false".equalsIgnoreCase(request.get("WithBalance").toString())){
				
			}
			else{
				HashMap<String,Double> balances = ObjectBalanceUtils.ObjectBalanceUtils(sUserID, ObjectConstants.OBJECT_TYPE_001);
				if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_001))
					usableBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_001); //查询可用余额
				
				if(balances.containsKey(ObjectConstants.ACCOUNT_TYPE_002))
					frozenBalance = balances.get(ObjectConstants.ACCOUNT_TYPE_002); //查询冻结余额
			}
			
			
			
			result.put("UsableBalance", GeneralTools.numberFormat(usableBalance));
			result.put("FrozenBalance", GeneralTools.numberFormat(frozenBalance));
			result.put("Balance", GeneralTools.numberFormat(usableBalance+frozenBalance, 0, 2));
			//modify end

			/*BizObjectQuery query = m
					.createQuery("select usablebalance,frozenbalance from o where userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				double usableBalance = Double.parseDouble(o.getAttribute("USABLEBALANCE").toString() == null?"0":o.getAttribute("USABLEBALANCE").toString());
				double frozenBalance = Double.parseDouble(o.getAttribute("FROZENBALANCE").toString() == null?"0":o.getAttribute("FROZENBALANCE").toString());
				result.put("UsableBalance", GeneralTools.numberFormat(usableBalance, 0, 2));
				result.put("FrozenBalance", GeneralTools.numberFormat(frozenBalance, 0, 2));
				result.put("Balance",GeneralTools.numberFormat(usableBalance+frozenBalance, 0, 2));
			} else {
				throw new HandlerException("common.user_account.usernotexist");
			}
			*/
			
			
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryaccountbalance.error");
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
	
	/**
	 * 根据筛选条件获取对应SQL语句
	 * @return
	 */
	private String getQuerySql(){
		String sQuerySql = "userid = :userid and inputtime is not null ";
		if("".endsWith(signflag))
			sQuerySql +=" and transtype not in('2030','2040','2050')";
		if(sTransType != null && !("".equals(sTransType))){
			if(sTransType.equals("1")){//充值
				sQuerySql = sQuerySql + " and transtype in ('1010','1011','1012','1015','1050')";
				//query.setParameter("transtypes", "1010,1011,1012,1015,1050");
			}else if(sTransType.equals("2")){//提现
				sQuerySql = sQuerySql + " and transtype in ('1020','1025')";
				//query.setParameter("transtypes", "\'1020\',\'1025\'");
			}else if(sTransType.equals("3")){//投资
				sQuerySql = sQuerySql + " and transtype in ('1061')";
				//query.setParameter("transtypes", "1061");
			}else if(sTransType.equals("4")){//收益入账
				sQuerySql = sQuerySql + " and transtype in ('1090')";
				//query.setParameter("transtypes", "1090");
			}else if(sTransType.equals("5")){//签到
				sQuerySql = sQuerySql + " and transtype in ('2050')";
				//query.setParameter("transtypes", "1090");
			}else if(sTransType.equals("6")){//邀请好友-注册
				sQuerySql = sQuerySql + " and transtype in ('2030')";
				//query.setParameter("transtypes", "1090");
			}else if(sTransType.equals("7")){//邀请好友-投资
				sQuerySql = sQuerySql + " and transtype in ('2040')";
				//query.setParameter("transtypes", "1090");
			}
		}
		if(!"".equals(sStartDate) && !"".equals(sEndDate)){
			sQuerySql = sQuerySql + " and (inputtime between :startdate and :enddate)";
		}
		//路易说全部都要显示
//		sQuerySql = sQuerySql + " and status  '10' and transtype <> '1070' order by serialno desc";
		sQuerySql = sQuerySql + " and  transtype <> '1070' order by updateTime desc";
		return sQuerySql;
	}
	public static void main(String[] args) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		System.out.println("---今天---"+sdf.format(cal.getTime()));
		cal.roll(Calendar.DAY_OF_MONTH, -1);
		System.out.println("---昨天---"+sdf.format(cal.getTime()));
		cal.roll(Calendar.MONTH, -1);
		System.out.println("---上个月---"+sdf.format(cal.getTime()));
		System.out.println("---今天---"+sdf.format(cal.getTime()));
//		String str = "1010,1011,1012,1015,1050";
//		String transtype = "1011";
//		System.out.println(str.contains(transtype));
		
		
		
	}
}

