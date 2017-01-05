package com.amarsoft.p2ptrade.invest;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/*
 * @DrawDown  投资 交易-查询队列执行结果
 * 
 */
public class InvestTransactionStatusWithQueeHandler  extends JSONHandler{

	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		
		JSONObject result = new JSONObject();
		
		if(request.containsKey("UserID")==false){
			//throw new HandlerException("common.emptyusername");
			result.put("status", "common.emptyusername");
		}
		if(request.containsKey("ProjectId")==false){
			//throw new HandlerException("common.projectnotexist");
			result.put("status", "common.projectnotexist");
		}
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.inf_tran_queue");
			BizObject obj = manager.createQuery("projectid=:projectid and investuser=:investuser")
				.setParameter("projectid", request.get("ProjectId").toString())
				.setParameter("investuser", request.get("UserID").toString())
				.getSingleResult(false);
			if(obj==null){
				//throw new HandlerException("common.projectnotexist");
				result.put("status", "common.projectnotexist");
			}
			result.put("status", obj.getAttribute("status").getString());
			
		}
		catch(JBOException ex){
			ex.printStackTrace();
			//throw new HandlerException("transrun.err");
			result.put("status", "transrun.err");
		}
		return result;
		
	}

}