package com.amarsoft.p2ptrade.tools;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.personcenter.GetTransPassErrorCountHandler;

/**
 * 验证交易密码
 * 输入参数： 
 * 		CodeNo:码值变好
 * 输出参数：码值键值对
 * 
 */
public class GetItemNameHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		if (request.get("CodeNo") == null || "".equals(request.get("CodeNo"))) {
			throw new HandlerException("codeno.error");
		}
		String sCodeNo = request.get("CodeNo").toString();
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject itemResult = GeneralTools.getItemName(jbo, sCodeNo);
			return itemResult;
		}catch(HandlerException e){
			throw e;
		}catch(Exception e){
			throw new HandlerException("getitemnamehandler");
		}
	}
}
