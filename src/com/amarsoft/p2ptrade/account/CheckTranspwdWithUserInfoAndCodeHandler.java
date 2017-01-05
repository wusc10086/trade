package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.personcenter.HistoryListCountHandler;

/**
 * 用户注册交易
 * 输入参数：
 * 		userid:	用户id
 * 		phone:手机号
 * 		certid:身份证号
 * 输出参数：
 * 		result:是否验证成功	:OK表示成功
 *
 */
public class CheckTranspwdWithUserInfoAndCodeHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		//获取参数值
		String userID = (String) request.get("userid");
		String phone = (String) request.get("phonetel");
		String certid = (String) request.get("certid");
		String codenum = (String) request.get("codenum");
		if(codenum==null || codenum.trim().equals("")){
			throw new HandlerException("otpcode.check.error");
		}
		request.put("UserID", userID);
		//检查身份信息
		CheckTranspwdHandler handler = new CheckTranspwdHandler();
		handler.createResponse(request, arg1);
		//检查校验码
		RegisterCheckHandler rch1 = new RegisterCheckHandler();
		request.put("operate", "validatecode_clear");
		request.put("objname", phone);
		request.put("objvalue", codenum);
		JSONObject resultObj = (JSONObject)rch1.createResponse(request, arg1);
		if(!"OK".equalsIgnoreCase((String)resultObj.get("result"))){
			throw new HandlerException("otpcode.check.error");
		}
		return "OK";
	}
}
