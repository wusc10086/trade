package com.amarsoft.p2ptrade.contract;
/**
 * 借款及担保合同（投资人）
 * 输入参数：
 * 		Serialno 流水号
 * 		UserID	客户号
 * 输出参数：
 * 		
 */
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.CodeManager;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class ContractInvestorHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		try{
			if(request.containsKey("SerialNo")==false)
				throw new HandlerException("request.invalid");
			String sContractID = "";//合同编号
			//查询主要信息
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.acct_loan");
			String sql = "select pi.loanpurpose,o.businesssum,o.loanrate,o.loanterm,o.putoutdate,o.defaultpaydate,o.maturitydate"
					+ ",o.pmtamount,o.fee1rate,o.pmtguefee,o.fee2rate,o.pmtmonthfee,pi.contractid"
					+ " from o,jbo.trade.project_info pi,jbo.trade.user_contract uc"
					+ " where o.contractserialno=pi.contractid and o.contractserialno=uc.contractid and uc.userid=:UserID and pi.SERIALNO=:SerialNo and uc.relativetype='002'";
			BizObjectQuery query = manager.createQuery(sql);
			query.setParameter("SerialNo", request.get("SerialNo").toString());
			query.setParameter("UserID", request.get("UserID").toString());
			BizObject obj = query.getSingleResult(false);
			JSONObject result = new JSONObject();
			JSONObject basicInfo = new JSONObject();
			if(obj==null){
				throw new HandlerException("查无结果");
			}
			else{
				basicInfo.put("loanpurpose",CodeManager.getCodeManager().getItemName("PACBApplyPurpose", obj.getAttribute("loanpurpose").getString()) );
				basicInfo.put("businesssum", getMoneyNumber(obj.getAttribute("businesssum").getDouble()));
				basicInfo.put("businesssumCH", StringFunction.numberToChinese(obj.getAttribute("businesssum").getDouble()));
				basicInfo.put("loanrate", getMoneyNumber(obj.getAttribute("loanrate").getDouble()));
				basicInfo.put("loanterm", obj.getAttribute("loanterm").getInt());
				basicInfo.put("putoutdate", getCHDate(obj.getAttribute("putoutdate").getString()));
				basicInfo.put("defaultpaydate", getCHDate(obj.getAttribute("defaultpaydate").getString()));
				basicInfo.put("maturitydate", getCHDate(obj.getAttribute("maturitydate").getString()));
				double pmtamount=obj.getAttribute("pmtamount").getDouble();
				basicInfo.put("pmtamount",getMoneyNumber(pmtamount));
				basicInfo.put("fee1rate", getMoneyNumber(obj.getAttribute("fee1rate").getDouble()));
				double pmtguefee = obj.getAttribute("pmtguefee").getDouble();
				basicInfo.put("pmtguefee", getMoneyNumber(pmtguefee));
				basicInfo.put("fee2rate", getMoneyNumber(obj.getAttribute("fee2rate").getDouble()));
				double pmtmonthfee = obj.getAttribute("pmtmonthfee").getDouble();
				System.out.println("pmtmonthfee="+ pmtmonthfee);
				basicInfo.put("pmtmonthfee",getMoneyNumber(pmtmonthfee) );
				sContractID = obj.getAttribute("contractid").getString();
				basicInfo.put("contractid", sContractID);
				basicInfo.put("totalsum", getMoneyNumber(pmtamount+pmtguefee+pmtmonthfee));
				result.put("basicInfo", basicInfo);
				//查询投资人和借款人信息信息
				manager = JBOFactory.getBizObjectManager("jbo.trade.account_detail");
				sql = "select o.realname,ua.username,o.certid,o.certtype from "
						+" jbo.trade.user_contract uc,jbo.trade.user_account ua,o"
						+ " where uc.contractid=:contractid and uc.userid=ua.userid and ua.userid=o.userid and uc.relativetype in ('001','002')"
						+ " order by uc.relativetype desc";
				query = manager.createQuery(sql);
				query.setParameter("contractid", sContractID);
				List<BizObject> list = query.getResultList(false);
				if(list!=null){
					JSONArray array = new JSONArray();
					for(BizObject bizObj : list){
						JSONObject obju = new JSONObject();
						obju.put("realname", bizObj.getAttribute("realname").getString());
						obju.put("username", bizObj.getAttribute("username").getString());
						obju.put("certid", bizObj.getAttribute("certid").getString());
						String[] arr = CodeManager.getCodeManager().getCodes("CertType");
						obju.put("certtype", CodeManager.getCodeManager().getItemName("CertType", bizObj.getAttribute("certtype").getString()) );
						array.add(obju);
					}
					result.put("users", array);
				}
			}
			
			
			
			
			//查询投资人信息
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	private String getMoneyNumber(String value){
		value = value.replace(",", "");
		return value;
		//Double dValue = Double.parseDouble(value);
		//return getMoneyNumber(dValue);
	}
	
	private String getMoneyNumber(double value){
		DecimalFormat df = new DecimalFormat("###,##0.00");
		return df.format(value);
	}
	
	private String getCHDate(String date){
		if(date.length()!=10){
			return date;
		}
		else{
			return date.substring(0,4) + "年" + date.substring(5,7) + "月" + date.substring(8,10) + "日";
		}
	}
}
