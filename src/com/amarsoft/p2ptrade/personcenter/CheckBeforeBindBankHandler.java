package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 用户绑定银行卡前验证 
 * 输入参数： UserID:用户编号 
 * 输出参数： 成功标志 
 * 
 */
public class CheckBeforeBindBankHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkBeforeBindBank(request);
	}

	/**
	 * 用户更换银行卡前验证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject checkBeforeBindBank(JSONObject request)
			throws HandlerException {

		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}

		try {
			String sUserID = request.get("UserID").toString();// 用户编号
			JBOFactory jbo = JBOFactory.getFactory();

			BizObjectManager accountManager = jbo
					.getManager("jbo.trade.user_account");
			BizObject accountBo = accountManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			// 获取用户余额信息
			if (accountBo != null) {
				//用户真实姓名和电话号码校验
				if (accountBo.getAttribute("USERNAME").toString() == null
						|| "".equals(accountBo.getAttribute("USERNAME")
								.toString())
						|| accountBo.getAttribute("PHONETEL").toString() == null
						|| "".equals(accountBo.getAttribute("PHONETEL")
								.toString())
						|| accountBo.getAttribute("PHONEAUTHFLAG").toString() == null
						|| !("2".equals(accountBo.getAttribute("PHONEAUTHFLAG")
								.toString()))) {
					throw new HandlerException("check.useraccount.error");
				}
				//实名认证校验
				if (accountBo.getAttribute("USERAUTHFLAG").toString() == null
						|| "".equals(accountBo.getAttribute("USERAUTHFLAG")
								.toString())) {
					throw new HandlerException("check.userauth.error");
				}
				//交易密码校验
				if (accountBo.getAttribute("TRANSPWD").toString() == null
						|| "".equals(accountBo.getAttribute("TRANSPWD")
								.toString())) {
					throw new HandlerException("check.transpwd.error");
				}
				//安全问题校验
				if (accountBo.getAttribute("SECURITYQUESTION").toString() == null
						|| "".equals(accountBo.getAttribute("SECURITYQUESTION")
								.toString())
						|| accountBo.getAttribute("SECURITYANSWER").toString() == null
						|| "".equals(accountBo.getAttribute("SECURITYANSWER")
								.toString())) {
					throw new HandlerException("check.security.error");
				}
			} else {
				throw new HandlerException("queryuseraccount.nodata.error");
			}
			return null;
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("checkbeforebindcark.error");
		}
	}
}
