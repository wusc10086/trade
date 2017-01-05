package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 检查用户是否冻结
 * 输入参数：
 * 		UserID		用户ID
 * 输出参数：
 * 		校验结果
 *
 */
public class CheckUserAccountFreezeHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
		if(userID == null || userID.length() == 0){
			throw new HandlerException("common.emptyuserid");
		}

		//检查账户是否异常
		try{
			JSONObject result = new JSONObject();
			BizObject boz = JBOFactory.getBizObjectManager("jbo.trade.user_account")
					.createQuery("select FrozenLockFalg from o where userid=:userid")
					.setParameter("userid", request.get("UserID").toString())
					.getSingleResult(false);
			if(boz!=null){
				if("1".equals(boz.getAttribute("FrozenLockFalg").getString())){
					result.put("resultFlag", "account.freeze");
				}
			}
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}

}
