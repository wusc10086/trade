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
public class CheckTranspwdWithUserInfoHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject resultObj = new JSONObject();
		//获取参数值
		String userID = (String) request.get("userid");
		String phone = (String) request.get("phonetel");
		String certid = (String) request.get("certid");
		request.put("UserID", userID);
		
		
		if(userID == null || userID.length() == 0){
			throw new HandlerException("common.emptyuserid");
		}
		if(phone == null || phone.length() == 0){
			throw new HandlerException("common.emptymobile");
		}
		//验证手机号
		RegisterCheckHandler rch1 = new RegisterCheckHandler();
		request.put("operate", "validatemobile_user");
		request.put("objname", phone);
		resultObj = (JSONObject)rch1.createResponse(request, arg1);
		if(!"OK".equalsIgnoreCase((String)resultObj.get("result"))){
			throw new HandlerException( "phoneno.error");
		}
		HistoryListCountHandler hlch = new HistoryListCountHandler();
		int iTransactionRecordCount = (Integer)hlch.createResponse(request, arg1);
		if(iTransactionRecordCount>0){
			//需要验证身份证号
			if(certid == null || certid.length() == 0){
				throw new HandlerException("certid.error");
			}
			else{
				rch1 = new RegisterCheckHandler();
				request.put("operate", "validatecertid_user");
				request.put("objname", certid);
				resultObj = (JSONObject)rch1.createResponse(request, arg1);
				if(!"OK".equalsIgnoreCase((String)resultObj.get("result"))){
					throw new HandlerException( "0001");
				}
				else{
					return "OK";
				}
			}
		}
		else{
			return "OK";
		}
	}
}
