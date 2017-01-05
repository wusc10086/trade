package com.amarsoft.p2ptrade.tradeclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.awe.util.json.JSONValue;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.Des3Encryption;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.RSATool;

/*
 *  与信贷交易交互    
 *     发送请求    接收响应  
 *  
 *     sendRequest
 * 
 */
public class RunTradeService  {

	private static String signKey = null;
	private static String transKey = null;
	private static String reuqestip = null;
//	private static String reuqestip= "http://10.236.68.107:8080/P2P_Service/runtrade.jsp" ;
//	private static String reuqestip= "http://localhost:8080/P2P_Service/runtrade.jsp" ;
//	private static String reuqestip= "http://localhost:8080/TPXD_CORE/runtrade.jsp" ;
	
	private static HttpClient httpclient;
	
	static {
		try {
			signKey = RSATool.decryptString(ARE.getProperty("SignKeyValue"),ARE.getProperty("SignKeyPath"));
			transKey = RSATool.decryptString(ARE.getProperty("TransKeyValue"),ARE.getProperty("TransKeyPath"));
			reuqestip = ARE.getProperty("CoreTradeService"); 
		} catch (Exception e) {
			ARE.getLog().error("签名或传输密钥还原失败，请检查配置文件[are.xml]");
			e.printStackTrace();
		}
	}
	
	public static String createSignKey(String method,String requestFormat,String requestStr)throws Exception{
		String sToSignString =  signKey + method + requestFormat + requestStr;
		return MessageDigest.getDigestAsLowerHexString("MD5", sToSignString);
	}
	
	/*
	 * 交易类请求 未解决  问题  转为json
	 * 
	 * 
	 */
	public static JSONObject runHttp(String serviceurl,String method,String requestFormat,String requestStr,String sSignKey)throws Exception{
		if(transKey == null || transKey.length() <= 0 || transKey.indexOf(",") < 0){
			throw new Exception("签名或传输密钥为空，请检查配置文件[are.xml]");
		}
		
		//加密
		String iv = transKey.split(",")[1];
		String key = transKey.split(",")[0];
		Des3Encryption des3Encryption = new Des3Encryption();
		des3Encryption.setIV(iv);
		des3Encryption.setEncryptKey(key.getBytes());
		System.out.println("requestStr     "+requestStr);
		byte[] data = des3Encryption.encrypt(requestStr.getBytes("UTF-8"));
		requestStr = RSATool.getBase64Encode(data);
		String sResult =  sendRequest(serviceurl,method,requestFormat,requestStr,sSignKey);
		int iDot = sResult.indexOf(",");
		if(iDot>-1){
			sResult = sResult.substring(iDot+1);
		}
		//解密
		byte[] dResult = des3Encryption.decrypt(RSATool.getBase64Decode(sResult));
		sResult =   new String(dResult,"UTF-8");
		JSONObject jsonresult = null;
		jsonresult =(JSONObject) JSONValue.parse(sResult);
   		return jsonresult;
	}
	
	/**
	 * 获取HTTP客户端
	 * 
	 * @param requestTimeout
	 * @return httpclient
	 * */
	protected static HttpClient getHttpClient(int requestTimeout){
		BasicHttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, requestTimeout);
		HttpConnectionParams.setSoTimeout(httpParameters, requestTimeout);
		httpclient = new DefaultHttpClient(httpParameters);
		return httpclient;
	}
	
	/**
	 * 发送请求
	 * 
	 * @param serviceurl
	 * @param businessMethod
	 * @param requestFormat
	 * @param requestString
	 * @param signCode
	 * @return responseEntity
	 * */
	protected static  String sendRequest(String serviceurl,String businessMethod,String requestFormat,String requestString,String signCode) throws Exception {
		int transactionTimeout = 8000;
		if(ARE.getProperty("P2pTransactionTimeout") != null){
			try {
				transactionTimeout = Integer.parseInt(ARE.getProperty("P2pTransactionTimeout"));
			} catch (RuntimeException e) {
				transactionTimeout = 8000;
				ARE.getLog().warn("连接超时时间设置有误，请检查[are.xml]。已恢复默认设置。");
				e.printStackTrace();
			}
		}
		HttpClient httpclient = getHttpClient(transactionTimeout); 
		
		HttpPost httppost = new HttpPost(serviceurl);
		
		java.util.List<NameValuePair> nameValuePairs =new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("code",businessMethod));
        nameValuePairs.add(new BasicNameValuePair("requestFormat",requestFormat));
        nameValuePairs.add(new BasicNameValuePair("param",requestString));
        nameValuePairs.add(new BasicNameValuePair("signKey",signCode));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        //ARE.getLog().info("connect to url:"+ serviceurl);
        HttpResponse  response = httpclient.execute(httppost);
        //HttpResponse  response=(HttpResponse)NetManager.runRemoteWithLog(new Object[]{httpclient,httppost}, "http",app);
        return EntityUtils.toString(response.getEntity());
	}
	/**
	 * 根据request 生成有格式的 json字符串
	 * */
	public  String createJsonString(HttpServletRequest request) throws Exception{
		
		StringBuffer json = new StringBuffer();
		json.append("{\"RequestParams\":{");
		Enumeration e = request.getParameterNames();
		while(e.hasMoreElements()){
			String param = (String)e.nextElement();
			String value = java.net.URLEncoder.encode(request.getParameter(param),"UTF-8");
			json.append("\""+param+"\":\""+value+"\",");
		}
		json.append("},\"deviceType\":\"Pc\"}");
		return json.toString();
	}
	
	/*
	  *  根据request 生成有格式的 json字符串
	  **/
	private String createJsonString(JSONObject request) {
		// TODO Auto-generated method stub
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("RequestParams", request);
		jsonObject.put("deviceType", "Pc");
 		return jsonObject.toJSONString();
	}

	/* 
	 * 交易请求  
	 * */
	public  JSONObject requestTransaction(JSONObject request,JBOFactory jbo) throws HandlerException{
		JSONObject result = null;
//		String requestStr="{\"RequestParams\":{\"Occurdate\":\"2014/04/04\",
//		\"LoanSerialNo\":\"22123132\",\"ContractSerialNo\":\"12312323222\",
//		\"TransID\":\"xdes111111\",\"Amt\":\"100000\",\"P2PTransactionSerialNo
//		\":\"111111123123\",\"UserID\":\"Jerrylufax\"},\"deviceType\":\"Pc\"}";
		
		GeneralTools gTools  = new GeneralTools();
		String sInputDate = gTools.getDate();
		String sInputTime = gTools.getTime();
		String method = request.get("Method").toString();
		String requestFormat = "json";
		
		try {
			request.put("Occurdate", sInputDate);
			String requestStr = createJsonString(request);
			System.out.println("method : "+method +"         请求报文 ： "+requestStr);
            String  sSignKey = createSignKey(method, requestFormat,requestStr);
            System.out.println("method : "+method +"         请求报文 ： "+requestStr);
		    result = runHttp(reuqestip,method,requestFormat,requestStr,sSignKey);
		    System.out.println("result :   "+result.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("connect.error");
 		}
		return result;
	}
	 
	
}
