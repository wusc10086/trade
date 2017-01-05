package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 销毁Session服务
 * 输入参数：
 * 		UserID		用户ID
 * 		SessionID   SessionID
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 *
 */
public class ClearSessionHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		String userID = (String) request.get("UserID");
		String sessionID = (String) request.get("SessionID");
		
		if(sessionID == null || sessionID.length() == 0){
			throw new HandlerException("clearsession.emptysessionid");
		}
		if(userID == null || userID.length() == 0){
			throw new HandlerException("clearsession.emptyuserid");
		}

		try {
			BizObjectManager manager=JBOFactory.getBizObjectManager("jbo.trade.ti_user_list");
			BizObjectQuery query = manager.createQuery("select SessionID,UserID,finishTime from o where SessionID=:SessionID and UserID=:UserID");
			query.setParameter("SessionID", sessionID);
			query.setParameter("UserID", userID);
			BizObject user = query.getSingleResult(true);
			if(user != null){
				user.setAttributeValue("finishTime",StringFunction.getTodayNow());
				manager.saveObject(user);
			}
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("clearsession.error");
		}
		
		result.put("SuccessFlag","S");
		
		return result;
	}

}
