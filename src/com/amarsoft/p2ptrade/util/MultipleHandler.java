package com.amarsoft.p2ptrade.util;

import java.util.Properties;

import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.ServiceFactory;
import com.amarsoft.mobile.webservice.TradeManager;
import com.amarsoft.mobile.webservice.business.BusinessHandler;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 复合交易
 * @author flian
 *
 */
public class MultipleHandler extends JSONHandler{
	public JSONObject createResponse(JSONObject businessRequest,
			Properties properties) throws HandlerException {
		JSONObject result = new JSONObject();
		java.util.Iterator it =  businessRequest.keySet().iterator();
		while(it.hasNext()){
			String sTradeNo = it.next().toString();
			TradeManager tm = getTradeManager(sTradeNo);
			BusinessHandler handler = tm.getBusinessHandler();
			handler.execute(properties);
			result.put(sTradeNo, handler.getResponseBusinessObject());
		}
		return result;
	}
	
	private TradeManager getTradeManager(String tradeNo)throws HandlerException {
		try{
			return ServiceFactory.getFactory().createTradeManager(tradeNo);
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("交易管理器获取失败,请检查交易配置文件");
		}
	}

}
