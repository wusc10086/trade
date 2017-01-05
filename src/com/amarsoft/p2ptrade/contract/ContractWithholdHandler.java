package com.amarsoft.p2ptrade.contract;
/**
 * 代扣合同 
 * 输入参数：
 * 		Serialno 流水号
 * 		UserID	客户号
 * 输出参数：
 * 		
 */
import java.util.Properties;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.CodeManager;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class ContractWithholdHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		try{
			if(request.containsKey("UserID")==false)throw new HandlerException("request.invalid");
			if(request.containsKey("SerialNo")==false)throw new HandlerException("request.invalid");
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.account_info");
			String sql = "select o.ACCOUNTBELONG,o.ACCOUNTNO,o.ACCOUNTNAME,u.username,pi.contractid,ad.REALNAME,ci.inputtime"//,tr.inputtime"
				+ " from o,jbo.trade.user_account u,jbo.trade.project_info pi,jbo.trade.acct_loan li,jbo.trade.ti_contract_info ci"
				+ ",jbo.trade.user_contract uc,jbo.trade.account_detail ad"//,jbo.trade.transaction_record tr"
				+ "  where o.userid=u.userid and o.userid=:userid and o.userid=ad.userid"// and tr.TRANSACTIONSERIALNO like li.SERIALNO"
				+ " and pi.contractid=ci.contractid and li.contractserialno=pi.contractid and li.contractserialno=uc.contractid and uc.userid=:UserID and pi.SERIALNO=:SerialNo and uc.relativetype='001'";
			BizObjectQuery query = manager.createQuery(sql);
			query.setParameter("userid", request.get("UserID").toString());
			query.setParameter("SerialNo", request.get("SerialNo").toString());
			BizObject obj = query.getSingleResult(false);
			JSONObject result = new JSONObject();
			if(obj==null){
				throw new HandlerException("查无结果");
			}
			else{
				result.put("AccountBelong", CodeManager.getCodeManager().getItemName("BankNo",obj.getAttribute("AccountBelong").getString()));
				result.put("AccountNo", obj.getAttribute("AccountNo").getString());
				result.put("AccountName", obj.getAttribute("AccountName").getString());
				result.put("UserName", obj.getAttribute("UserName").getString());
				result.put("UserName", obj.getAttribute("UserName").getString());
				result.put("RealName", obj.getAttribute("REALNAME").getString());
				result.put("ContractID", obj.getAttribute("ContractID").getString());
				result.put("inputdate", getCHDate(obj.getAttribute("inputtime").getString()));
				String sContractID = obj.getAttribute("contractid").getString();
				//查询投资人信息
				manager = JBOFactory.getBizObjectManager("jbo.trade.account_detail");
				sql = "select o.realname,ua.username,o.certid,o.certtype from "
						+" jbo.trade.user_contract uc,jbo.trade.user_account ua,o"
						+ " where uc.contractid=:contractid and uc.userid=ua.userid and ua.userid=o.userid and uc.relativetype ='002'"
						+ " order by uc.relativetype desc";
				query = manager.createQuery(sql);
				query.setParameter("contractid", sContractID);
				BizObject objx = query.getSingleResult(false);
				if(objx!=null){
					result.put("Investor", objx.getAttribute("UserName").getString());
					result.put("InvestorRealName", objx.getAttribute("realname").getString());
				}
				
				//出借人
				result.put("BorrowerID", "");
			}
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	private String getCHDate(String date){
		if(date==null)return "";
		if(date.length()!=10){
			return date;
		}
		else{
			return date.substring(0,4) + "年" + date.substring(5,7) + "月" + date.substring(8,10) + "日";
		}
	}

}
