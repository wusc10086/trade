package com.amarsoft.p2ptrade.account;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.ASValuePool;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.p2ptrade.webservice.exception.InputValidException;

/**
 * 修改用户信息交易
 * 输入参数：
 * 		UserID		用户ID
 * 		FieldName   修改字段名
 * 		FieldType	修改信息类型
 * 		FieldValue	修改值
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 * 		FailCode:	失败原因
 * 		FailDesc:	失败原因说明		
 * @author dxu
 *
 */
public class ModifyUserAccountHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String userID = (String) request.get("UserID");
		String fieldType = (String) request.get("FieldType");
		String fieldName = (String) request.get("FieldName");
		String fieldValue = (String) request.get("FieldValue");
		String TransPwdType = null;
	
		if(userID == null || userID.length() == 0){
			throw new HandlerException("modifyuseraccount.emptyuserid");
		}
		if(fieldName == null || fieldName.length() == 0){
			throw new HandlerException("modifyuseraccount.emptyfieldname");
		}
		if(fieldType == null || fieldType.length() == 0){
			throw new HandlerException("modifyuseraccount.emptyfieldtype");
		}
		
		if(request.containsKey("TransPwdType")){
			TransPwdType = request.get("TransPwdType").toString();
		}
		
		JSONObject result = new JSONObject();
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = userAcctManager.createQuery("select * from o where UserID=:UserID");
			query.setParameter("UserID", userID);
			BizObject userAccount = query.getSingleResult(true);
			if(userAccount != null){
				if("BASIC".equalsIgnoreCase(fieldType) && ("PhoneTel".equalsIgnoreCase(fieldName) || "UserName".equalsIgnoreCase(fieldName) || "EMail".equalsIgnoreCase(fieldName))){
					duplicateCheck(request,jbo);
				}
				
				String oldValue = "";
//				String userIDUCMain = userAccount.getAttribute("UCUSERID").toString();
//				String userIDUCSub = userAccount.getAttribute("UCSUBUSERID").toString();
//				String mainAccountFlag = null,userIDUC = null;
//				if(userIDUCMain != null && userIDUCMain.length() > 0){
//					userIDUC = userIDUCMain;
//					mainAccountFlag = "Y";
//				}else if(userIDUCSub != null && userIDUCSub.length() > 0){
//					userIDUC = userIDUCSub;
//					mainAccountFlag = "N";
//				}else{
//					throw new HandlerException("common.usercenter.usernotexist");
//				}
//				request.put("UserIDUC", userIDUC);
//				request.put("MainAccountFlag", mainAccountFlag);
//			
				//修改基本信息
				if("BASIC".equalsIgnoreCase(fieldType)){
					if("USERID".equalsIgnoreCase(fieldName)){
						throw new HandlerException("modifyuseraccount.error");
					}
					oldValue = userAccount.getAttribute(fieldName).getString();
					if(oldValue == null)oldValue = "";
					if("TRANSPWD".equalsIgnoreCase(fieldName)){
						userAccount.setAttributeValue(fieldName.toUpperCase(), GeneralTools.OrigWord(fieldValue));
						HashMap<String, Object> params = new HashMap<String, Object>();
						String sDate = new TimeTool().getsChDate();
						params.put("Date", sDate);
						if(TransPwdType !=null && !"".equals(TransPwdType)){
							if(TransPwdType.equals("01")){
								//sendSMS("P2P_JYMMBGCG",userAccount.getAttribute("PHONETEL").getString(),params);
							}else if(TransPwdType.equals("02")){
								//sendSMS("P2P_JYMMZHCG",userAccount.getAttribute("PHONETEL").getString(),params);
							}
						}
					}else if("PHONETEL".equalsIgnoreCase(fieldName)){
						userAccount.setAttributeValue(fieldName.toUpperCase(), fieldValue);
						userAccount.setAttributeValue("PHONEAUTHFLAG", "2");
			//		}else if("EMAIL".equalsIgnoreCase(fieldName)){
			//			userAccount.setAttributeValue(fieldName.toUpperCase(), fieldValue);
					}else{
						userAccount.setAttributeValue(fieldName.toUpperCase(), fieldValue);
					}
					userAcctManager.saveObject(userAccount);
				}else if("DETAIL".equalsIgnoreCase(fieldType)){
					BizObjectManager userAcctDetailManager =jbo.getManager("jbo.trade.account_detail");
					query = userAcctDetailManager.createQuery("select * from o where UserID=:UserID");
					query.setParameter("UserID", userID);
					BizObject userAcctDetail = query.getSingleResult(true);
					if("RealName".equalsIgnoreCase(fieldName)){
						userAccount.setAttributeValue("USERAUTHFLAG", "2");
						userAcctManager.saveObject(userAccount);
					}
					if(userAcctDetail != null){
						userAcctDetail.setAttributeValue(fieldName.toUpperCase(), fieldValue);
						userAcctDetailManager.saveObject(userAcctDetail);
					}else{
						userAcctDetail = userAcctDetailManager.newObject();
						userAcctDetail.setAttributeValue("USERID", userID);
						userAcctDetail.setAttributeValue(fieldName.toUpperCase(), fieldValue);
						userAcctDetailManager.saveObject(userAcctDetail);
					}
				}else{
					throw new HandlerException("modifyuseraccount.invalidfieldtype");
				}
				if("BASIC".equalsIgnoreCase(fieldType) && !oldValue.equals(fieldValue)){
					/*BizObjectManager userAcctAuditManager =jbo.getManager("jbo.trade.account_audit");
					
					BizObject userAcctAudit = userAcctAuditManager.newObject();
					userAcctAudit.setAttributeValue("USERID", userID);
					userAcctAudit.setAttributeValue("CHANGETYPE", fieldName);
					userAcctAudit.setAttributeValue("OLDVALUE1", oldValue);
					userAcctAudit.setAttributeValue("NEWVALUE1", fieldValue);
					userAcctAudit.setAttributeValue("UPDATETIME", StringFunction.getTodayNow());
					userAcctAuditManager.saveObject(userAcctAudit);*/
				}
				if("BASIC".equalsIgnoreCase(fieldType) && ("PhoneTel".equalsIgnoreCase(fieldName) || "UserName".equalsIgnoreCase(fieldName))){
					//result = syncUserAccountKeyInfo(request);
				}else if(!"BASIC".equalsIgnoreCase(fieldType) ){
					//result = syncUserAccountDetailInfo(request);
				}
			}else{
				throw new HandlerException("modifyuseraccount.usernotexist");
			}
		}catch(InputValidException e){
			e.printStackTrace();
			throw new HandlerException(e.getMessage());
		}catch(JBOException e){
			e.printStackTrace();
			if("ARES1307".equals(e.getErrorCode())){
				throw new HandlerException("modifyuseraccount.invalidfieldname");
			}
			throw new HandlerException("modifyuseraccount.error");
		}catch(HandlerException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("modifyuseraccount.error");
		}
		result.put("flag","success");
		
		return result;
	}
	
	private void duplicateCheck(JSONObject request, JBOFactory jbo) throws HandlerException {
		String userID = (String) request.get("UserID");
		String fieldType = (String) request.get("FieldType");
		String fieldName = (String) request.get("FieldName");
		String fieldValue = (String) request.get("FieldValue");
		String validType = "",errorCode = "";
		//修改用户名、手机、邮箱时，先校验是否重复
		if("BASIC".equalsIgnoreCase(fieldType) && "PhoneTel".equalsIgnoreCase(fieldName)){
			validType = "M";
			errorCode = "mobile.unique.error";
		}else if("BASIC".equalsIgnoreCase(fieldType) && "UserName".equalsIgnoreCase(fieldName)){
			validType = "N";
			errorCode = "username.unique.error";
		}else if("BASIC".equalsIgnoreCase(fieldType) && "EMail".equalsIgnoreCase(fieldName)){
			validType = "E";
			errorCode = "email.unique.error";
		}else{
			return;
		}
		
		try {
			BizObjectManager userManager = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = userManager.createQuery(fieldName+"=:FieldValue and UserID<>:UserID");
			query.setParameter("FieldValue", fieldValue);
			query.setParameter("UserID", userID);
			BizObject user = query.getSingleResult(false);
			if(user != null){
				throw new HandlerException(errorCode);
			}
			
			ASValuePool params = new ASValuePool();
			params.setAttribute("AccountType", validType);
			params.setAttribute("AccountNo", fieldValue);

//			AbstractUserClient uc = UserClientFactory.getClient(UserClientFactory.TRANS_CODE_KEY_CHECK);
//			ASValuePool returns = uc.process(params);
//			String successFlag = (String) returns.getAttribute("SuccessFlag");
//			String businessResult = (String) returns.getAttribute("BusinessResult");
//			if("0".equals(successFlag)){
//				if(!"0".equals(businessResult)){
//					throw new HandlerException(errorCode);
//				}
//			}
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("accountvalid.error");
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("accountvalid.error");
		}
	}

//	private JSONObject syncUserAccountKeyInfo(JSONObject request) throws Exception,InputValidException{
//		JSONObject result = new JSONObject();
//		String userIDUC = (String) request.get("UserIDUC");
//		String fieldName = (String) request.get("FieldName");
//		String fieldValue = (String) request.get("FieldValue");
//		String mainAccountFlag = (String) request.get("MainAccountFlag");
//		
//			
//		ASValuePool params = new ASValuePool();
//		params.setAttribute("UserID", userIDUC);
//		params.setAttribute("MainAccountFlag", mainAccountFlag);
//		params.setAttribute("UserName", "UserName".equalsIgnoreCase(fieldName)?fieldValue:null);
//		params.setAttribute("Mobile", "PhoneTel".equalsIgnoreCase(fieldName)?fieldValue:null);
//		AbstractUserClient uc = UserClientFactory.getClient(UserClientFactory.TRANS_CODE_REINFORCE);
//		ASValuePool returns = uc.process(params);
//		String successFlag = (String) returns.getAttribute("SuccessFlag");
//		String failCode = (String) returns.getAttribute("FailCode");
//		String failDesc = (String) returns.getAttribute("FailDesc");
//			
//		if("0".equals(successFlag)){
//			result.put("SuccessFlag", "S");
//		}else{
//			result.put("SuccessFlag", "F");
//			result.put("FailCode", failCode==null?"":failCode);
//			result.put("FailDesc", failDesc==null?"":failDesc);
//		}
//		
//		return result;
//	}	
	
//	private JSONObject syncUserAccountDetailInfo(JSONObject request) throws Exception,InputValidException{
//		JSONObject result = new JSONObject();
//		String userIDUC = (String) request.get("UserIDUC");
//		String fieldName = (String) request.get("FieldName");
//		String fieldValue = (String) request.get("FieldValue");
//		String mainAccountFlag = (String) request.get("MainAccountFlag");
//		
//		ASValuePool params = new ASValuePool();
//		params.setAttribute("UserID", userIDUC);
//		params.setAttribute("MainAccountFlag", mainAccountFlag);
//		if(fieldName.equalsIgnoreCase("MARRIAGE")){
//			params.setAttribute("Marriage", fieldValue);
//		}else if(fieldName.equalsIgnoreCase("INCOME")){
//			String incomeLevel = parseIncomeLevel(fieldValue);
//			params.setAttribute("IncomeLevel", incomeLevel);
//		}
//		AbstractUserClient uc = UserClientFactory.getClient(UserClientFactory.TRANS_CODE_UPDATE_USER);
//		ASValuePool returns = uc.process(params);
//		String successFlag = (String) returns.getAttribute("SuccessFlag");
//		String failCode = (String) returns.getAttribute("FailCode");
//		String failDesc = (String) returns.getAttribute("FailDesc");
//			
//		if("0".equals(successFlag)){
//			result.put("SuccessFlag", "S");
//		}else{
//			result.put("SuccessFlag", "F");
//			result.put("FailCode", failCode==null?"":failCode);
//			result.put("FailDesc", failDesc==null?"":failDesc);
//		}
//		
//		return result;
//	}
	
	private String parseIncomeLevel(String income){
		String incomeLevel = null;
		
		JBOFactory jbo = JBOFactory.getFactory();
		BizObjectManager userAcctManager;
		try {
			userAcctManager = jbo.getManager("jbo.sys.code_library");
			BizObjectQuery query = userAcctManager.createQuery("select * from o where CodeNo='IncomeLevel' and Attribute1<:Income and Attribute2>=:Income");
			query.setParameter("Income", Double.parseDouble(income));
			BizObject code = query.getSingleResult(false);
			if(code != null){
				incomeLevel = code.getAttribute("itemno").toString();
			}else{
				return null;
			}
		} catch (JBOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return incomeLevel;
	}
	
	
	/**
	 * 解析
	 * @param bts
	 * @return
	 */
	public String bytes2Hex(byte[] bts) {
		String des = "";
		String tmp = null;
		for (int i = 0; i < bts.length; i++) {
			tmp = (Integer.toHexString(bts[i] & 0xFF));
			if (tmp.length() == 1) {
				des += "0";
			}
			des += tmp;
		}
		return des;
	}
	
	private void sendSMS(String sTempletID,String sPhoneTel,HashMap<String, Object> parameters){
		try {
			GeneralTools.sendSMS(sTempletID,sPhoneTel, parameters);
		} catch (HandlerException e) {
			e.printStackTrace();
			ARE.getLog().error(e);
		}

	}
}
