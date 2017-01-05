package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 借款用户信息 
 * 输入参数： UserID用户编号 
 * 输出参数： UserDetail
 * 
 */
public class QueryApplicationUserHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getApplicationUser(request);
	}

	@SuppressWarnings("unchecked")
	private JSONObject getApplicationUser(JSONObject request)
			throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}

		String sUserID = request.get("UserID").toString();
		JSONObject result = new JSONObject();

		try {
			boolean auth = false;
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.ind_info");

			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);

			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sUserIdQuery = o.getAttribute("USERID").toString();
				String sPhonetel = o.getAttribute("PHONETEL").toString();
				String sUserAuthFlag = o.getAttribute("USERAUTHFLAG")
						.toString();
				String sTransPwd = o.getAttribute("TRANSPWD").toString();
				String sSecurityQuestion = o.getAttribute("SECURITYQUESTION")
						.toString();
				String sSecurityAnswer = o.getAttribute("SECURITYANSWER")
						.toString();
				if ((sUserIdQuery != null) && (sPhonetel != null)
						&& (sUserAuthFlag != null) && (sTransPwd != null)
						&& (sSecurityQuestion != null)
						&& (sSecurityAnswer != null)) {
					auth = true;

				} else {
					throw new HandlerException("queryapplicationauth.error");
				}
			}
			result.put("array", auth);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryapplicationauth.error");
		}
	}
}
