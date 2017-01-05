package com.amarsoft.p2ptrade.contract;
/**
 * 催收合同
 * 输入参数：
 * 		Serialno 流水号
 * 		UserID	客户号
 * 输出参数：
 * 		
 */
import java.util.List;
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

public class ContractCollectionHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		if(request.containsKey("UserID")==false)throw new HandlerException("缺少账号参数");
		try{
			//查询主要信息
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.acct_loan");
			String sql = "select pi.loanpurpose,o.businesssum,o.loanrate,o.loanterm,o.putoutdate,o.defaultpaydate,o.maturitydate,ci.inputtime"
					+ ",o.pmtamount,o.fee1rate,o.pmtguefee,o.fee2rate,o.pmtmonthfee,pi.contractid"
					+ " from o,jbo.trade.project_info pi,jbo.trade.user_contract uc,jbo.trade.ti_contract_info ci"
					+ " where o.contractserialno=pi.contractid and pi.contractid=ci.contractid and  o.contractserialno=uc.contractid and uc.userid=:UserID and pi.SERIALNO=:SerialNo and uc.relativetype='002'";
			BizObjectQuery query = manager.createQuery(sql);
			query.setParameter("SerialNo", request.get("SerialNo").toString());
			query.setParameter("UserID", request.get("UserID").toString());
			BizObject obj = query.getSingleResult(false);
			JSONObject result = new JSONObject();
			if(obj==null){
				throw new HandlerException("查无结果");
			}
			else{
				result.put("ContractID", obj.getAttribute("ContractID").getString());
				result.put("inputdate", getCHDate(obj.getAttribute("inputtime").getString()));
			}
			
			//查询投资人和借款人信息信息
			manager = JBOFactory.getBizObjectManager("jbo.trade.account_detail");
			sql = "select o.realname,ua.username,o.certid,o.certtype from "
					+" jbo.trade.user_account ua,o"
					+ " where ua.userid=o.userid and o.userid=:userid";
			
			query = manager.createQuery(sql);
			query.setParameter("userid", request.get("UserID").toString());
			BizObject bizObj = query.getSingleResult(false);
			if(bizObj!=null){
				//JSONObject obju = new JSONObject();
				result.put("realname", bizObj.getAttribute("realname").getString());
				result.put("username", bizObj.getAttribute("username").getString());
				result.put("certid", bizObj.getAttribute("certid").getString());
				result.put("certtype", CodeManager.getCodeManager().getItemName("CertType", bizObj.getAttribute("certtype").getString()) );
				return result;
			}
			else{
				throw new HandlerException("无效的客户号");
			}
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
