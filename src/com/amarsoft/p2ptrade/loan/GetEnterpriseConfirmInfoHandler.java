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
public class GetEnterpriseConfirmInfoHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {

		return getSingleConfirmInfo(request);
	}
/**
 * 获取企业认证信息
 * @throws HandlerException 
 */
	@SuppressWarnings("unchecked")
	private JSONObject getSingleConfirmInfo(JSONObject request) throws HandlerException {
		JSONObject result =new JSONObject();
		String userId = (String) request.get("userId");
		BizObject enterpriseInfoRresult = getEnterpriseInfoResult(userId);
		JSONObject enterpriseCI=new JSONObject();
		try {
			enterpriseCI.put("Istenement", enterpriseInfoRresult.getAttribute("Istenement") == null ? "" : enterpriseInfoRresult.getAttribute("Istenement").toString());
			enterpriseCI.put("Properties", enterpriseInfoRresult.getAttribute("Properties") == null ? "" : enterpriseInfoRresult.getAttribute("Properties").toString());
			
			
			
		} catch (JBOException e) {
			e.printStackTrace();
		}
		result.put("enterpriseCI",enterpriseCI);
		return result;
	}
/**
 * 返回企业验证信息的结果集
 * @param userId 
 * @return
 */
	private BizObject getEnterpriseInfoResult(String userId)throws HandlerException {
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m = null;
		BizObject result = null;
		try {
			m = f.getManager("jbo.trade.capital_info");
			result = m.createQuery("CustomerID=:userId").setParameter("userId", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("checkenterpriseconfirm.error");
		}
		return result;
	}

}
