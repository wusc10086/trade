package com.amarsoft.p2ptrade.account;

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
 * 用户签到流水
 *
 */
public class UserSignListHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String userid = (String) request.get("UserID");
		JSONObject result = new JSONObject();
		
		try{
			//签到列表
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_sign");

			BizObjectQuery query = m.createQuery("select o.inputdate,o.inputtime,o.remark,tr.amount,tr.status from o,jbo.trade.transaction_record tr where o.inputdate=tr.transdate  and tr.UserID=o.UserID and o.UserID=:UserID and tr.transtype='2050' and tr.direction='R' order by o.inputdate desc");
			query.setParameter("UserID", userid);
			 
			List<BizObject> list = query.getResultList(false);
			JSONArray array = new JSONArray();
			for(BizObject o : list){
				JSONObject json = new JSONObject();
				json.put("status", o.getAttribute("status").toString());
				json.put("inputdate", o.getAttribute("inputdate").toString());
				json.put("inputtime", o.getAttribute("inputtime").toString());
				json.put("remark", o.getAttribute("remark").toString());
				json.put("amount", o.getAttribute("amount").toString());
				array.add(json);
			}
			result.put("array", array);
			//签到统计
			UserSignCountHandler sign = new UserSignCountHandler();
			JSONObject signo = (JSONObject)sign.createResponse(request, null);
			result.put("sign", signo);
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("register.error");
		}	
		return result;
	}
}
