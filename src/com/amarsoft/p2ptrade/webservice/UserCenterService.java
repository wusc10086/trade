//package com.amarsoft.p2ptrade.webservice;
//
//import javax.annotation.Resource;
//import javax.jws.WebMethod;
//import javax.jws.WebParam;
//import javax.jws.WebResult;
//import javax.jws.WebService;
//import javax.servlet.http.HttpServletRequest;
//import javax.xml.ws.WebServiceContext;
//import javax.xml.ws.handler.MessageContext;
//
//import com.amarsoft.are.ARE;
//import com.amarsoft.are.jbo.BizObject;
//import com.amarsoft.are.jbo.BizObjectManager;
//import com.amarsoft.are.jbo.BizObjectQuery;
//import com.amarsoft.are.jbo.JBOException;
//import com.amarsoft.are.jbo.JBOFactory;
//import com.amarsoft.are.util.StringFunction;
//import com.amarsoft.message.Message;
//import com.amarsoft.p2p.interfac.InterfaceHelper;
//
///**
// * 官网接口服务调用入口
// * @author dxu
// *
// */
//@WebService(targetNamespace = "http://ws.service.amarsoft.com/")//定义命名空间
//public class UserCenterService {
//	
//	 @Resource
//	 private WebServiceContext wsContext;
//	 private String clientIP = "",errorCode = "",errorMsg = "";
//	 public static final String USER_CENTER = "USER_CENTER";
//	 public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
//	 
//	 public UserCenterService(){
//		 
//	 }
//	 
//	 public UserCenterService(String clientIP){
//		 this.clientIP = clientIP;
//	 }
//	 
//	/**
//	 * 调用服务
//	 * @param request 请求报文（XML格式）
//	 * @return
//	 */
//	 /*
//	@WebMethod
//	@WebResult(name="runTransaction")
//	public String runTransaction(@WebParam(name="request")String request){
//		if(wsContext!=null){
//			MessageContext messageContext = wsContext.getMessageContext();
//			HttpServletRequest hrequest = (HttpServletRequest) (messageContext.get(MessageContext.SERVLET_REQUEST));
//			String sRemortAddress = hrequest.getRemoteAddr();
//			ARE.getLog().info("来自"+ sRemortAddress+"的请求：request="+request);
//		}
//		
//		String response = "";
//		
//		
//		
//		return response;
//	}
//	*/
//	
//	/**
//	 * 密码找回通知
//	 * @param request 请求报文（XML格式）
//	 * @return
//	 */
//	@WebMethod
//	@WebResult(name="doService")
//	public String doService(@WebParam(name="request")String request){
//		if(wsContext!=null){
//			MessageContext messageContext = wsContext.getMessageContext();
//			HttpServletRequest hrequest = (HttpServletRequest) (messageContext.get(MessageContext.SERVLET_REQUEST));
//			String sRemortAddress = hrequest.getRemoteAddr();
//			ARE.getLog().info("来自"+ sRemortAddress+"的请求：request="+request);
//		}
//		errorCode = "";
//		errorMsg = "";
//		Message requestMessage = new UserCenterMessage();
//		try {
//			requestMessage.unpack(request);
//			String userID = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("USER_ID").getStringValue();
//			String mainAccountFlag = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("IS_MAIN_ACCOUNT").getStringValue();
//			String businessCode = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("BUSINESS_CODE").getStringValue();
//			
//			if(userID == null || userID.length() == 0){
//				errorCode = "901";
//				errorMsg = "用户ID为空";
//				throw new Exception(errorCode);
//			}
//			
//			if(mainAccountFlag == null || mainAccountFlag.length() == 0){
//				errorCode = "901";
//				errorMsg = "主账号标志为空";
//				throw new Exception(errorCode);
//			}
//			
//			if(!"Y".equals(mainAccountFlag) && !"N".equals(mainAccountFlag)){
//				errorCode = "902";
//				errorMsg = "主账号标志["+mainAccountFlag+"]不正确";
//				throw new Exception(errorCode);
//			}
//			
//			if(businessCode == null || businessCode.length() == 0){
//				errorCode = "901";
//				errorMsg = "业务操作代码为空";
//				throw new Exception(errorCode);
//			}
//			
//			//1：找回密码；2：修改手机号
//			ARE.getLog().info("BUSINESS_CODE="+businessCode);
//			if("1".equals(businessCode)){
//				return handleRetrievePasswordNotify(request);
//			}else if("2".equals(businessCode)){
//				return handleMobileChangeNotify(request);
//			}else{
//				errorCode = "902";
//				errorMsg = "业务操作代码["+businessCode+"]不正确";
//				throw new Exception(errorCode);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			return handleError(requestMessage);
//		}
//	}
//	
//	/**
//	 * 手机号码变更通知
//	 * @param request 请求报文（XML格式）
//	 * @return
//	 */
//	/*
//	@WebMethod
//	@WebResult(name="mobileChangeNotify")
//	public String mobileChangeNotify(@WebParam(name="request")String request){
//		if(wsContext!=null){
//			MessageContext messageContext = wsContext.getMessageContext();
//			HttpServletRequest hrequest = (HttpServletRequest) (messageContext.get(MessageContext.SERVLET_REQUEST));
//			String sRemortAddress = hrequest.getRemoteAddr();
//			ARE.getLog().info("来自"+ sRemortAddress+"的请求：request="+request);
//		}
//		
//		return handleMobileChangeNotify(request);
//	}
//	*/
//	
//	private String handleRetrievePasswordNotify(String request){
//		Message requestMessage = new UserCenterMessage();
//		String mainAccountFlag = null,userID = null,serialNo = null;
//		JBOFactory jbo = JBOFactory.getFactory();
//		try {
//			BizObjectManager transLogManager =jbo.getManager("jbo.trade.acct_transaction_log");
//			BizObject transLog = transLogManager.newObject();
//			transLog.setAttributeValue("TRANSCODE", "UserCenter.RetrievePasswordNotify");
//			transLog.setAttributeValue("TRANSCHANNEL", USER_CENTER);
//			transLog.setAttributeValue("TRANSDATE", StringFunction.getToday());
//			transLog.setAttributeValue("TRANSTIME", StringFunction.getNow());
//			transLog.setAttributeValue("REQUEST", request);
//			transLogManager.saveObject(transLog);
//			serialNo = transLog.getAttribute("LogID").getString();
//			
//			requestMessage.unpack(request);
//			userID = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("USER_ID").getStringValue();
//			mainAccountFlag = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("IS_MAIN_ACCOUNT").getStringValue();
//			
//			requestMessage.unpack(request);
//			
//			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
//			BizObjectQuery query = null;
//			if("Y".equals(mainAccountFlag)){
//				query = m.createQuery("UCUSERID=:UserID");
//			}else{
//				query = m.createQuery("UCSUBUSERID=:UserID");
//			}
//			query.setParameter("UserID", userID);
//			BizObject user = query.getSingleResult(true);
//			if(user != null){
//				user.setAttributeValue("RETRIEVEPASSWORDFLAG", "1");
//				m.saveObject(user);
//				
//			}else{
//				errorCode = "905";
//				errorMsg = "用户["+userID+"]不存在";
//				throw new Exception(errorCode);
//			}
//			
//		} catch (JBOException e1) {
//			e1.printStackTrace();
//			errorCode = "903";
//			errorMsg = "系统内部错误";
//		} catch (Exception e2) {
//			e2.printStackTrace();
//			if(e2.getMessage() == null){
//				errorCode = "903";
//				errorMsg = "报文格式错误";
//			}
//		}
//		
//		try{
//			Message response = new UserCenterMessage();
//			response.setMessageId("RESPONSE"); //这个是根
//			
//			//添加头
//			Message header = new UserCenterMessage();
//			header.setMessageId("CONTROL"); //这个头
//			
//			//添加一个Field
//			InterfaceHelper.addField(header, "REQUEST_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "SERVICE_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "APP_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_CODE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "ERROR_MESSAGE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_TIME", "", UserCenterField.class);
//			
//			//添加体
//			Message body = new UserCenterMessage();
//			body.setMessageId("DATA");
//			
//			//添加体
//			String successFlag = "";
//			if(errorCode != null && errorCode.length() > 0){
//				successFlag = "F";
//				InterfaceHelper.addField(body, "RESULT_FLAG", "1", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", errorCode, UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", errorMsg, UserCenterField.class);
//			}else{
//				successFlag = "S";
//				InterfaceHelper.addField(body, "RESULT_FLAG", "0", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", "", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", "", UserCenterField.class);
//			}
//			
//			//把头添加进报文
//			response.setMessage("CONTROL", header);
//			response.setMessage("DATA", body);
//			
//			BizObjectManager transLogManager =jbo.getManager("jbo.trade.acct_transaction_log");
//			BizObjectQuery query = transLogManager.createQuery("LogID=:LogID");
//			query.setParameter("LogID", serialNo);
//			BizObject transLog = query.getSingleResult(true);
//			
//			transLog.setAttributeValue("RESPONSE", response.toString());
//			transLog.setAttributeValue("STATUS", successFlag);
//			transLogManager.saveObject(transLog);
//			
//			return XML_HEADER+response.toString();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "error";
//		}
//	}
//	
//	private String handleMobileChangeNotify(String request){
//		Message requestMessage = new UserCenterMessage();
//		String userID = null,mobileNo = null,mainAccountFlag = null,serialNo = null;
//		JBOFactory jbo = JBOFactory.getFactory();
//		try {
//			BizObjectManager transLogManager =jbo.getManager("jbo.trade.acct_transaction_log");
//			BizObject transLog = transLogManager.newObject();
//			transLog.setAttributeValue("TRANSCODE", "UserCenter.MobileChangeNotify");
//			transLog.setAttributeValue("TRANSCHANNEL", USER_CENTER);
//			transLog.setAttributeValue("TRANSDATE", StringFunction.getToday());
//			transLog.setAttributeValue("TRANSTIME", StringFunction.getNow());
//			transLog.setAttributeValue("REQUEST", request);
//			transLogManager.saveObject(transLog);
//			serialNo = transLog.getAttribute("LogID").getString();
//			
//			requestMessage.unpack(request);
//			try{
//				mobileNo = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("MOBILE").getStringValue();
//			}catch (NullPointerException e1){
//				errorCode = "901";
//				errorMsg = "手机号为空";
//				throw new Exception(errorCode);
//			}catch (Exception e2){
//				throw e2;
//			}
//			
//			try{
//				Long.parseLong(mobileNo);
//			} catch(Exception e){
//				errorCode = "902";
//				errorMsg = "手机号格式错误";
//				throw new Exception(errorCode);
//			}
//						
//			if(mobileNo == null || mobileNo.length() == 0){
//				errorCode = "901";
//				errorMsg = "手机号为空";
//				throw new Exception(errorCode);
//			}
//			
//			userID = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("USER_ID").getStringValue();
//			mainAccountFlag = requestMessage.getSingleMessage("DATA").getSingleMessage("USER").getField("IS_MAIN_ACCOUNT").getStringValue();
//			
//			requestMessage.unpack(request);
//			
//			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
//			BizObjectQuery query = null;
//			if("Y".equals(mainAccountFlag)){
//				query = m.createQuery("UCUSERID=:UserID");
//			}else{
//				query = m.createQuery("UCSUBUSERID=:UserID");
//			}
//			query.setParameter("UserID", userID);
//			
//			BizObject user = query.getSingleResult(true);
//			if(user != null){
//				user.setAttributeValue("MOBILECHANGEFLAG", "1");
//				user.setAttributeValue("NEWMOBILENO", mobileNo);
//				m.saveObject(user);
//			}else{
//				errorCode = "905";
//				errorMsg = "用户不存在";
//			}
//			
//		} catch (JBOException e1) {
//			e1.printStackTrace();
//			errorCode = "903";
//			errorMsg = "系统内部错误";
//		} catch (Exception e2) {
//			e2.printStackTrace();
//			if(e2.getMessage() == null){
//				errorCode = "903";
//				errorMsg = "报文格式错误";
//			}
//		}
//		
//		try{
//			Message response = new UserCenterMessage();
//			response.setMessageId("RESPONSE"); //这个是根
//			
//			//添加头
//			Message header = new UserCenterMessage();
//			header.setMessageId("CONTROL"); //这个头
//			
//			//添加一个Field
//			InterfaceHelper.addField(header, "REQUEST_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "SERVICE_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "APP_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_CODE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "ERROR_MESSAGE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_TIME", "", UserCenterField.class);
//			
//			//添加体
//			Message body = new UserCenterMessage();
//			body.setMessageId("DATA");
//			
//			//添加体
//			String successFlag = "";
//			if(errorCode != null && errorCode.length() > 0){
//				successFlag = "F";
//				InterfaceHelper.addField(body, "RESULT_FLAG", "1", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", errorCode, UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", errorMsg, UserCenterField.class);
//			}else{
//				successFlag = "S";
//				InterfaceHelper.addField(body, "RESULT_FLAG", "0", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", "", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", "", UserCenterField.class);
//			}
//			
//			//把头添加进报文
//			response.setMessage("CONTROL", header);
//			response.setMessage("DATA", body);
//			
//			BizObjectManager transLogManager =jbo.getManager("jbo.trade.acct_transaction_log");
//			BizObjectQuery query = transLogManager.createQuery("LogID=:LogID");
//			query.setParameter("LogID", serialNo);
//			BizObject transLog = query.getSingleResult(true);
//			
//			transLog.setAttributeValue("RESPONSE", response.toString());
//			transLog.setAttributeValue("STATUS", successFlag);
//			transLogManager.saveObject(transLog);
//			
//			return XML_HEADER+response.toString();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "error";
//		}
//	}
//	
//	private String handleError(Message requestMessage){
//		try{
//			Message response = new UserCenterMessage();
//			response.setMessageId("RESPONSE"); //这个是根
//			
//			//添加头
//			Message header = new UserCenterMessage();
//			header.setMessageId("CONTROL"); //这个头
//			
//			//添加一个Field
//			InterfaceHelper.addField(header, "REQUEST_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "SERVICE_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "APP_ID", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_CODE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "ERROR_MESSAGE", "", UserCenterField.class);
//			InterfaceHelper.addField(header, "RESPONSE_TIME", "", UserCenterField.class);
//			
//			//添加体
//			Message body = new UserCenterMessage();
//			body.setMessageId("DATA");
//			
//			//添加体
//			if(errorCode != null){
//				InterfaceHelper.addField(body, "RESULT_FLAG", "1", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", errorCode, UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", errorMsg, UserCenterField.class);
//			}else{
//				InterfaceHelper.addField(body, "RESULT_FLAG", "1", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_CODE", "903", UserCenterField.class);
//				InterfaceHelper.addField(body, "ERROR_MESSAGE", "系统内部错误", UserCenterField.class);
//			}
//			
//			//把头添加进报文
//			response.setMessage("CONTROL", header);
//			response.setMessage("DATA", body);
//			
//			return XML_HEADER+response.toString();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "error";
//		}
//	}
//}
