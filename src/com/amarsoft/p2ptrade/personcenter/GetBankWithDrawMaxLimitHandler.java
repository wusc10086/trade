package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 最大待付限额查询交易
 * 输入参数：
 * 		AccountBelong:开户行
 * 输出参数：
 * 		LimitAmount:最大待付限额
 *
 */
public class GetBankWithDrawMaxLimitHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getBankWithDrawMaxLimit(request);
		
	}
	  
	/**
	 * 余额查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getBankWithDrawMaxLimit(JSONObject request)throws HandlerException {
		//参数校验
		if(request.get("AccountBelong")==null || "".equals(request.get("AccountBelong"))){
			throw new HandlerException("accountbelong.error");
		}
		
		String sAccountBelong = request.get("AccountBelong").toString();//用户编号
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			double limitAmount = GeneralTools.getLimitAmount(jbo, sAccountBelong, "ATTRIBUTE3");
			
			JSONObject result = new JSONObject();
			result.put("LimitAmount", String.valueOf(GeneralTools.numberFormat(limitAmount)));
			return result;
		}catch(HandlerException e){
			throw e;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("getlimitamount.error");
		}
	}
}
