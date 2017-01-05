package com.amarsoft.p2ptrade.loan;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 获取个人验证信息
 * @author Mbmo
 *
 */
public class GetSingleConfirmInfoHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {

		return getSingleConfirmInfo(request);
	}
/**
 * 获取个人验证信息主逻辑
 */
	@SuppressWarnings("unchecked")
	private JSONObject getSingleConfirmInfo(JSONObject request) {
		JSONObject result =new JSONObject();
		String userId = (String) request.get("userId");
		BizObject singleInfoResult = getSingleInfoResult(userId);
		JSONObject singleCI=new JSONObject();
		try {
			if(singleInfoResult!=null){
				singleCI.put("Istenement", singleInfoResult.getAttribute("Istenement") == null ? "" : singleInfoResult.getAttribute("Istenement").toString());
				singleCI.put("Properties", singleInfoResult.getAttribute("Properties") == null ? "" : singleInfoResult.getAttribute("Properties").toString());
				singleCI.put("CreditReport", singleInfoResult.getAttribute("CreditReport") == null ? "" : singleInfoResult.getAttribute("CreditReport").toString());
				singleCI.put("LoanStartTime", singleInfoResult.getAttribute("LoanStartTime") == null ? "" : singleInfoResult.getAttribute("LoanStartTime").toString());
				singleCI.put("BuyHouseTime", singleInfoResult.getAttribute("BuyHouseTime") == null ? "" : singleInfoResult.getAttribute("BuyHouseTime").toString());
				singleCI.put("BuildSpace", singleInfoResult.getAttribute("BuildSpace") == null ? 0 : singleInfoResult.getAttribute("BuildSpace").getDouble());
				singleCI.put("Sprice", singleInfoResult.getAttribute("Sprice") == null ? 0 : singleInfoResult.getAttribute("Sprice").getDouble());
				singleCI.put("LoanMoney", singleInfoResult.getAttribute("LoanMoney") == null ? 0 : singleInfoResult.getAttribute("LoanMoney").getDouble());
				singleCI.put("HouseAdd", singleInfoResult.getAttribute("HouseAdd") == null ? "" : singleInfoResult.getAttribute("HouseAdd").toString());
				singleCI.put("Zip", singleInfoResult.getAttribute("Zip") == null ? "" : singleInfoResult.getAttribute("Zip").toString());
				singleCI.put("Iscarapply", singleInfoResult.getAttribute("Iscarapply") == null ? "" : singleInfoResult.getAttribute("Iscarapply").toString());
				singleCI.put("Ispasstest", singleInfoResult.getAttribute("Ispasstest") == null ? "" : singleInfoResult.getAttribute("Ispasstest").toString());
				singleCI.put("Isruncar", singleInfoResult.getAttribute("Isruncar") == null ? "" : singleInfoResult.getAttribute("Isruncar").toString());
				singleCI.put("Carmodel", singleInfoResult.getAttribute("Carmodel") == null ? "" : singleInfoResult.getAttribute("Carmodel").toString());
				singleCI.put("Insuremodel", singleInfoResult.getAttribute("Insuremodel") == null ? "" : singleInfoResult.getAttribute("Insuremodel").toString());
				singleCI.put("Insure1", singleInfoResult.getAttribute("Insure1") == null ? "" : singleInfoResult.getAttribute("Insure1").toString());
				singleCI.put("Insure2", singleInfoResult.getAttribute("Insure2") == null ? "" : singleInfoResult.getAttribute("Insure2").toString());
				singleCI.put("Insure3", singleInfoResult.getAttribute("Insure3") == null ? "" : singleInfoResult.getAttribute("Insure3").toString());
				singleCI.put("BuyCarTime", singleInfoResult.getAttribute("BuyCarTime") == null ? "" : singleInfoResult.getAttribute("BuyCarTime").toString());
				singleCI.put("Carlicense", singleInfoResult.getAttribute("Carlicense") == null ? "" : singleInfoResult.getAttribute("Carlicense").toString());
				singleCI.put("CreditCards", singleInfoResult.getAttribute("CreditCards") == null ? 0 : singleInfoResult.getAttribute("CreditCards").getInt());
				singleCI.put("MaxLimit", singleInfoResult.getAttribute("MaxLimit") == null ? "" : singleInfoResult.getAttribute("MaxLimit").toString());
				singleCI.put("RecentlySixNo", singleInfoResult.getAttribute("RecentlySixNo") == null ? 0 : singleInfoResult.getAttribute("RecentlySixNo").getInt());
				singleCI.put("UploadCreditReport", singleInfoResult.getAttribute("UploadCreditReport") == null ? "" : singleInfoResult.getAttribute("UploadCreditReport").toString());
	
				singleCI.put("HouseholdDeposits", singleInfoResult.getAttribute("HouseholdDeposits") == null ? "" : singleInfoResult.getAttribute("HouseholdDeposits").toString());
				singleCI.put("TotalLoansRemaining", singleInfoResult.getAttribute("TotalLoansRemaining") == null ? "" : singleInfoResult.getAttribute("TotalLoansRemaining").toString());
				singleCI.put("TotalLoans", singleInfoResult.getAttribute("TotalLoans") == null ? "" : singleInfoResult.getAttribute("TotalLoans").toString());
				singleCI.put("MonthlyPayment", singleInfoResult.getAttribute("MonthlyPayment") == null ? "" : singleInfoResult.getAttribute("MonthlyPayment").toString());
				singleCI.put("loantimes", singleInfoResult.getAttribute("loantimes") == null ? "" : singleInfoResult.getAttribute("loantimes").toString());
				singleCI.put("LendingInstitution", singleInfoResult.getAttribute("LendingInstitution") == null ? "" : singleInfoResult.getAttribute("LendingInstitution").toString());
				singleCI.put("LoansOverdue", singleInfoResult.getAttribute("LoansOverdue") == null ? "" : singleInfoResult.getAttribute("LoansOverdue").toString());
			
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		result.put("singleCI",singleCI);
		return result;
	}
/**
 * 返回个人验证信息的结果集
 * @param userId 
 * @return
 */
	private BizObject getSingleInfoResult(String userId) {
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m = null;
		BizObject result = null;
		try {
			m = f.getManager("jbo.trade.capital_info");
			result = m.createQuery("CustomerID=:userId").setParameter("userId", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
