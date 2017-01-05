package com.amarsoft.p2ptrade.loan;

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
 * 确认合同
 * 输入参数：
 * 		UserID:用户 名
 *      ContractId:合同编号
 * 输出参数： 
 * 		成功标识
 *      错误代码
 *  
 */
public class ConfirmPreContractHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getInvestmentList(request);
	}
	
	/**
	 * 确认合同
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getInvestmentList(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		
		if(request.get("ContractId")==null || "".equals(request.get("ContractId"))){
			throw new HandlerException("contractid.error");
		}
		 
		String sUserID = request.get("UserID").toString();
		String sContractId = request.get("ContractId").toString();
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query = m.createQuery("userid=:userid and contractid=:contractid");
			query.setParameter("userid", sUserID);
			query.setParameter("contractid", sContractId);
			
			List<BizObject> list = query.getResultList(false);
			JSONObject result = new JSONObject();
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
//					obj.put("SerialNo", o.getAttribute("SERIALNO"));// 
					array.add(obj);
				}
				result.put("RootType", "020");
				result.put("array", array);
			}
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("confirmprecontract.error");
		}
	}
}
