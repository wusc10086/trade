package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.TimeTool;

/**
 * 港澳台用户实名认证第三步，录入护照或身份证信息 
 * 输入参数： 
 * 			UserID:用户ID 
 * 			IdType:证件类型：（护照或身份证） 
 * 			DocID:证件号码
 * 
 * 输出参数：成功标志
 * 
 */
public class SetHMTUserRealNameStepThreeHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return setUserRealName(request);
	}

	/**
	 * 实名认证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject setUserRealName(JSONObject request)
			throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("IdType") == null || "".equals(request.get("IdType")))
			throw new HandlerException("idtype.error");
		if (request.get("DocID") == null || "".equals(request.get("DocID")))
			throw new HandlerException("docid.error");

		String sUserID = request.get("UserID").toString();// 用户编号
		String sIdType = request.get("IdType").toString();// 证件类型
		String sDocID = request.get("DocID").toString();// 证件号码

		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		try {
			tx = jbo.createTransaction();
			TimeTool tool = new TimeTool();
			String sCurrentTime = tool.getsCurrentMoment();

			// 账户详细信息处理对象
			BizObjectManager detailManager = jbo.getManager(
					"jbo.trade.account_detail", tx);
			BizObject detailBo = detailManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);

			detailBo.setAttributeValue("UPDATETIME", sCurrentTime);
			detailBo.setAttributeValue("CERTTYPE", sIdType);
			detailBo.setAttributeValue("CERTID", sDocID);
			detailManager.saveObject(detailBo);

			// 用户认证信息处理对象
			BizObjectManager authenManager = jbo.getManager(
					"jbo.trade.user_authentication", tx);
			BizObject authenBo = authenManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);

			authenBo.setAttributeValue("UPDATETIME", sCurrentTime);
			authenBo.setAttributeValue("IDTYPE", sIdType);
			authenBo.setAttributeValue("DOCID", sDocID);
			authenBo.setAttributeValue("STATUS", "1");
			authenManager.saveObject(authenBo);
			
			//用户帐号信息认证标志
			BizObjectManager accountManager = jbo.getManager(
					"jbo.trade.user_account",tx);
			BizObject accountBo = accountManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			if(null !=  accountBo){
				accountBo.setAttributeValue("USERAUTHFLAG", "1");
				accountManager.saveObject(accountBo);
			}else{
				throw new HandlerException("queryuseraccount.nodata.error");
			}
			
			tx.commit();
			return null;
		}catch (HandlerException e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			throw e;
		}
		catch (Exception e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			throw new HandlerException("setuserrealname.error");
		}
	}

}
