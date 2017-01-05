package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.ASValuePool;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 预签合同 
 * 输入参数： 
 * 		UserID:			账户编号
 * 输出参数： 
 * 		SuccessFlag:	是否成功	S/F
 * @author dxu
 */

public class SignPreContractHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		String userID = (String) request.get("UserID");
		if(userID == null || userID.length() == 0){
			throw new HandlerException("common.emptyuserid");
		}
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager acctManager = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = acctManager.createQuery("UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAcct = query.getSingleResult(true);
			if(userAcct == null){
				throw new HandlerException("common.usernotexist");
			}else{
				String mainAccountFlag = "";
				String transPWD = userAcct.getAttribute("TRANSPWD").getString();
				String userIDUC = userAcct.getAttribute("UCUSERID").getString();
				String userSubIDUC = userAcct.getAttribute("UCSUBUSERID").getString();
				String highRisk = userAcct.getAttribute("HIGHRISK").getString();
				if (userIDUC != null && userIDUC.length() > 0) {
					mainAccountFlag = "Y";
				} else if (userSubIDUC != null && userSubIDUC.length() > 0) {
					mainAccountFlag = "N";
				} else {
					throw new HandlerException("common.usercenter.usernotexist");
				}
				if(transPWD == null || transPWD.length() == 0){
					throw new HandlerException("check.transpwd.error");
				}
				String securityQuestion = userAcct.getAttribute("SECURITYQUESTION").getString();
				String securityAnswer = userAcct.getAttribute("SECURITYANSWER").getString();
				if(securityQuestion == null || securityQuestion.length() == 0 || 
						securityAnswer == null || securityAnswer.length() == 0){
					throw new HandlerException("check.security.error");
				}
				BizObjectManager userManager = jbo.getManager("jbo.trade.account_detail");
				query = userManager.createQuery("UserID=:UserID");
				query.setParameter("UserID", userID);
				BizObject user = query.getSingleResult(false);
				if(user == null){
					throw new HandlerException("common.usernotauth");
				}else{
					String certType = user.getAttribute("CERTTYPE").getString();
					String certID = user.getAttribute("CERTID").getString();
					BizObjectManager preManager = jbo.getManager("jbo.trade.user_preagreement");
					query = preManager.createQuery("UserID=:UserID and ExpiryDate>:ExpiryDate");
					query.setParameter("UserID", userID);
					query.setParameter("ExpiryDate", StringFunction.getToday());
					BizObject pre = query.getSingleResult(false);
					if(pre != null){
						throw new HandlerException("generateprecontract.alreadysign");
					}else{
						pre = preManager.newObject();
						pre.setAttributeValue("USERID", userID);
						pre.setAttributeValue("SIGNDATE", StringFunction.getToday());
						pre.setAttributeValue("EXPIRYDATE", StringFunction.getRelativeDate(StringFunction.getToday(), 7));
						pre.setAttributeValue("INPUTTIME", StringFunction.getTodayNow());
						pre.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
						pre.setAttributeValue("CERTTYPE", certType);
						pre.setAttributeValue("CERTID", certID);
						preManager.saveObject(pre);
						
						//如果该客户为非高风险客户，则置为高风险客户并同步到用户中心
						if(!"1".equals(highRisk)){
							try {
								ASValuePool params = new ASValuePool();
	
								params.setAttribute("UserID","Y".equals(mainAccountFlag)?userIDUC:userSubIDUC);
								params.setAttribute("MainAccountFlag", mainAccountFlag);

							} catch (Exception e) {
								e.printStackTrace();
								throw new HandlerException("interface.usercenter.synchighrisk.error");
							}
							userAcct.setAttributeValue("HIGHRISK", "1");
							acctManager.saveObject(userAcct);
						}
						result.put("SuccessFlag", "S");
					}
				}
			}
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("signprecontract.error");
		}
		
		return result;
	}

}
