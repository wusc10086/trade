package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
import com.amarsoft.p2ptrade.account.UserSignCountHandler;
import com.amarsoft.p2ptrade.front.TopRecordProHandler;
/**
 * 个人中心首页的交易
 * 输入参数：
 * 		UserID:账户编号
 *
 */
public class MemberIndexHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getAccount(request);		
	}
	  
	/**
	 * 余额查询
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getAccount(JSONObject request)throws HandlerException {
		
		JSONObject result = new JSONObject();
		//参数校验
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("common.emptyuserid");
		}
	
		//用户信息
		QueryUserAccountInfoHandler ua = new QueryUserAccountInfoHandler();
		JSONObject user = (JSONObject)ua.createResponse(request, null);
		result.put("userinfo", user);
		//签到
		UserSignCountHandler sign = new UserSignCountHandler();
		JSONObject signo = (JSONObject)sign.createResponse(request, null);
		result.put("sign", signo);
		//推荐的投资
		TopRecordProHandler invest = new TopRecordProHandler();
		request.put("z", "5");
		JSONObject ob = (JSONObject) invest.createResponse(request, null);
		
		result.put("prolist", ob.get("proList"));
		//已投资统计
		UserIncomeHandler total = new UserIncomeHandler();
		JSONObject userIncome = (JSONObject)total.createResponse(request, null);
		result.put("userIncome", userIncome);
		//近期投资
		InvestmentListHandler pro = new InvestmentListHandler();
		JSONObject invPro = (JSONObject)pro.createResponse(request, null);
		result.put("invPro", invPro.get("array"));
		
		return result;
	}
}