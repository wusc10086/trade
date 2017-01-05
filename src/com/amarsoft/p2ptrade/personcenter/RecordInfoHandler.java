package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 交易详情查询 
 * 输入参数：  
 * 			UserID：账户编号
 *			SerialNo：流水号
 * 输出参数：
 * 			SerialNo：流水号
 * 			CreateTime：创建时间
 * 			TransTType：类型明细
 * 			Direction：操作
 * 			Balance：余额
 * 			Amount：操作数额
 */
public class RecordInfoHandler extends JSONHandler {	
	JSONObject result = new JSONObject();

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		Parser.registerFunction("getitemname");
		return getRecordInfo(request);

	}

	/**
	 * 获取记录
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getRecordInfo(JSONObject request)
			throws HandlerException {
		
		//参数初始化
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");

		if(!request.containsKey("SerialNo"))
			throw new HandlerException("common.emptyserialno");
			
		String SerialNo = request.get("SerialNo").toString();		
		String sUserID = request.get("UserID").toString();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.transaction_record");

			BizObjectQuery query = m.createQuery(" userid=:userid and serialno=:serialno");
			query.setParameter("userid", sUserID).setParameter("serialno", SerialNo);
	
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				//交易流水号
				String sSerialNo = o.getAttribute("SERIALNO").toString();
				//交易创建时间
				String sInputTime = o.getAttribute("INPUTTIME").toString();
				//交易金额
				double amount = Double.parseDouble(o.getAttribute("AMOUNT").toString()==null ? "0" : o.getAttribute("AMOUNT").toString());
				double in = 0.0;
				double out = 0.0;
				//交易方向
				String direction = o.getAttribute("DIRECTION").toString();
				//备注说明
				String remark = o.getAttribute("REMARK").toString();
				if("P".equals(direction)){
					 out = amount;
				}else{
					 in = amount;
				}
				//账户余额
				String balance = o.getAttribute("BALANCE").toString()==null?"":o.getAttribute("BALANCE").toString();
				//交易类型
				String transtype = o.getAttribute("TRANSTYPE").toString()==null?"":o.getAttribute("TRANSTYPE").toString();
				//交易状态
				String status = o.getAttribute("status").toString()==null?"":o.getAttribute("status").toString();
				//卡号编号
				String sRelaAccount = o.getAttribute("RELAACCOUNT").toString()==null?"":o.getAttribute("RELAACCOUNT").toString();
				System.out.println("sRelaAccount*****************"+sRelaAccount);
				//投资关联的合同号 借据号
				String sTRANSACTIONSERIALNO = o.getAttribute("TRANSACTIONSERIALNO").toString()==null?"":o.getAttribute("TRANSACTIONSERIALNO").toString();
				
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
				
				//对外显示的交易类型
				String sTransType = null;
				if("1010,1011,1012,1013,1015,1050".contains(transtype))
					sTransType = "充值";
				else if("1020,1025".contains(transtype))
					sTransType = "提现";
				else if("3010".contains(transtype))
					sTransType = "投资";
				else if("3050".contains(transtype))
					sTransType = "收益入账";
				else 
					sTransType = "其他";
				
				//查询银行卡信息
				if(!"".equals(sRelaAccount)){
					BizObjectManager m1 = jbo.getManager("jbo.trade.account_info");

					BizObjectQuery query1 = m1.createQuery(" select accountname,accountno,getitemname('BankNo',accountbelong) as v.bankname from o where serialno=:serialno and status='2'");
					query1.setParameter("userid", sUserID).setParameter("serialno", sRelaAccount);
			
					BizObject o1 = query1.getSingleResult(false);
					if(o1!=null){
						//账户名
						String accountname = o1.getAttribute("accountname").toString();
						//账户卡号
						String accountno = o1.getAttribute("accountno").toString();
						//银行名称
						String bankname = o1.getAttribute("bankname").toString();
						
						result.put("AccountName", accountname);
						result.put("AccountNo", accountno);
						result.put("BankName", bankname);
					}
				}
				
				//若为投资以及收益相关的交易，查询关联项目信息
				if("3010,3050".contains(transtype)){
					
					// ///==============替换投资类型交易详情中项目信息获取方法====hhcai
					// 2015/03/18===================///////////
					String projectserialno;
					try {
						projectserialno = sTRANSACTIONSERIALNO.split("@")[0];
					} catch (Exception e) {
						// TODO: handle exception
						projectserialno = "";
					}
					if (projectserialno.length() != 0) {						
						BizObjectManager m1 = jbo
								.getManager("jbo.trade.project_info");
						BizObjectQuery query1 = m1
								.createQuery(" select o.projectname,o.loanterm from o where o.serialno=:serialno");
						query1.setParameter("serialno", projectserialno);

						BizObject o1 = query1.getSingleResult(false);
						if (o1 != null) {
							result.put("projectname", o1.getAttribute("projectname")==null?"":o1.getAttribute("projectname").toString());
							result.put("loanterm", o1.getAttribute("loanterm")==null?"":o1.getAttribute("loanterm").toString()+"个月");
						}
					}
					
					/////==============替换投资类型交易详情中项目信息获取方法====hhcai 2015/03/18===================///////////
					
					//替换内容
					/****
					BizObjectManager m1 = jbo.getManager("jbo.trade.project_info");

					BizObjectQuery query1 = m1.createQuery(" select o.projectname,o.enddate,o.endtime,uc.updatetime from o,jbo.trade.user_contract uc where o.contractid=:contractid and uc.userid=:userid and o.serialno=uc.projectid");
					query1.setParameter("userid", sUserID).setParameter("contractid", sTRANSACTIONSERIALNO);
			
					BizObject o1 = query1.getSingleResult(false);
					if(o1!=null){
						//项目名称
						String projectname = o1.getAttribute("projectname").toString();
						//到期日期
						String enddate = o1.getAttribute("enddate").toString();
						//到期时间
						String endtime = o1.getAttribute("endtime").toString();
						//投资时间
						String updatetime = o1.getAttribute("updatetime").toString();
						
						result.put("projectname", projectname);
						result.put("enddate", enddate);
						result.put("endtime", endtime);
						result.put("investtime", updatetime);
					}				
					***/
				}
				
				result.put("SerialNo", sSerialNo);
				result.put("InputTime", sInputTime);
				result.put("AmountIn", in);
				result.put("AmountOut", out);
				result.put("Balance", balance);
				result.put("TransType", sTransType);
				result.put("Status", sStatusName);
				result.put("Remark", remark);
			}
			return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryhistory.error");
		}
	}
}