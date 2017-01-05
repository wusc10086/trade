package com.amarsoft.p2ptrade.precontract;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 预签合同提交
 * 输入： UserID 客户号
 * 输出：status 返回结果
 * @author flian
 *
 */
public class PreContractSubmitHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		try{
			if(request.containsKey("UserID")==false){
				throw new HandlerException("common.emptyusername");
			}
			PreContractStatusHandler pcs = new PreContractStatusHandler();
			String sResult = (String)pcs.createResponse(request, null);
			if(sResult.equals("")){
				//允许插入
				BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.pred_contract");
				BizObject obj = manager.newObject();
				obj.setAttributeValue("userid", request.get("UserID").toString());
				obj.setAttributeValue("inputtime", StringFunction.getTodayNow());
				manager.saveObject(obj);
			}
			else{
				throw new HandlerException("预签合同已经存在");
			}
			return "";
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
	}

}
