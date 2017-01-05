package com.amarsoft.p2ptrade.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

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
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.awe.util.json.JSONValue;
import com.amarsoft.p2ptrade.tools.Des3Encryption;
import com.amarsoft.p2ptrade.tools.RSATool;

public class RunTradeServiceTemp {

	private static String signKey = null;
	private static String transKey = null;
	private static String serviceURL = null;
	
	private static HttpClient httpclient;
	
	static {
		try {
			signKey = RSATool.decryptString(ARE.getProperty("SignKeyValue"),ARE.getProperty("SignKeyPath"));
			transKey = RSATool.decryptString(ARE.getProperty("TransKeyValue"),ARE.getProperty("TransKeyPath"));
			serviceURL = ARE.getProperty("P2pTradeService");
			
			//signKey = RSATool.decryptString("mkf5zlNfP6gZ75amzUzxrZEHa8LIRBm/kOnq9w4SUXkg8UII52U/+/XqeJGk Ar8EmNXOxlsYMdBhURPEfTBg4DG6FTgU9WtNtPsYm9uzQwcTWXaeG0JN8m5+ B2ad9d3TwfEeugJ3UN9mi7AjbGHXVFRXW7LN/oe3+AX/CAbikw8=","D:/WorkSpace/P2P_Loan/WebRoot/WEB-INF/etc/rsa_private_key.pem");
			//transKey = RSATool.decryptString("V1Knx+v7DcMRNi9btvnXLFu0sXLwV9NZ2Vl6hu2DU+bbtrvvZcMsL7WDIBZk EY5CrrgefpJaFDDVn8eLkC6TGUF+3UJG1E1mqlw+U7FBFaYxIiFBzSyojamt n4XDc8jiQBy+Cu2aasQi9HJlFJRLXSeJdXG8CJuMCjGEQDmMSow=","D:/WorkSpace/P2P_Loan/WebRoot/WEB-INF/etc/rsa_private_key.pem");
		} catch (Exception e) {
			ARE.getLog().error("签名或传输密钥还原失败，请检查配置文件[are.xml]");
			e.printStackTrace();
		}
	}
	
	public static String createSignKey(String method,String requestFormat,String requestStr)throws Exception{
		String sToSignString =  signKey + method + requestFormat + requestStr;
		return MessageDigest.getDigestAsLowerHexString("MD5", sToSignString.getBytes("UTF-8"));
	}
	
	public static String runHttp(String url,String method,String requestFormat,String requestStr,String sSignKey)throws Exception{
		return runHttp(method,requestFormat,requestStr,sSignKey);
	}
	
	public static String runHttp(String method,String requestFormat,String requestStr,String sSignKey)throws Exception{
		if(transKey == null || transKey.length() <= 0 || transKey.indexOf(",") < 0){
			throw new Exception("签名或传输密钥为空，请检查配置文件[are.xml]");
		}
		//加密
		String iv = transKey.split(",")[1];
		String key = transKey.split(",")[0];
		Des3Encryption des3Encryption = new Des3Encryption();
		des3Encryption.setIV(iv);
		des3Encryption.setEncryptKey(key.getBytes());
		byte[] data = des3Encryption.encrypt(requestStr.getBytes("UTF-8"));
		requestStr = RSATool.getBase64Encode(data);
		
		if(serviceURL == null){
			throw new Exception("服务URL未配置，请检查[are.xml]");
		}
		String sResult = sendRequest(serviceURL,method,requestFormat,requestStr,sSignKey);
		int iDot = sResult.indexOf(",");
		if(iDot>-1){
			sResult = sResult.substring(iDot+1);
		}
		
		//解密
		byte[] dResult = des3Encryption.decrypt(RSATool.getBase64Decode(sResult));
		sResult =   new String(dResult,"UTF-8");
		return sResult;
	}
	
	public static byte[] runHttpStream(String method,String requestFormat,String requestStr,String sSignKey)throws Exception{
		if(transKey == null || transKey.length() <= 0 || transKey.indexOf(",") < 0){
			throw new Exception("签名或传输密钥为空，请检查配置文件[are.xml]");
		}
		//加密
		String iv = transKey.split(",")[1];
		String key = transKey.split(",")[0];
		Des3Encryption des3Encryption = new Des3Encryption();
		des3Encryption.setIV(iv);
		des3Encryption.setEncryptKey(key.getBytes());
		byte[] data = des3Encryption.encrypt(requestStr.getBytes("UTF-8"));
		requestStr = RSATool.getBase64Encode(data);
		
		//String sResult = sendRequest(serviceurl,method,requestFormat,requestStr,sSignKey);
		int transactionTimeout = 30000;
		HttpClient httpclient = getHttpClient(transactionTimeout); 
		
		if(serviceURL == null){
			throw new Exception("服务URL未配置，请检查[are.xml]");
		}
		HttpPost httppost = new HttpPost(serviceURL);
		
		java.util.List<NameValuePair> nameValuePairs =new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("code",method));
        nameValuePairs.add(new BasicNameValuePair("requestFormat",requestFormat));
        nameValuePairs.add(new BasicNameValuePair("param",requestStr));
        nameValuePairs.add(new BasicNameValuePair("signKey",sSignKey));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        //ARE.getLog().info("connect to url:"+ serviceurl);
        HttpResponse  response = httpclient.execute(httppost);
        //HttpResponse  response=(HttpResponse)NetManager.runRemoteWithLog(new Object[]{httpclient,httppost}, "http",app);
        byte [] bytes =  EntityUtils.toByteArray(response.getEntity());
		
        return bytes;
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
	protected static String sendRequest(String serviceurl,String businessMethod,String requestFormat,String requestString,String signCode) throws Exception {
		int transactionTimeout = 30000;
		if(ARE.getProperty("P2pTransactionTimeout") != null){
			try {
				transactionTimeout = Integer.parseInt(ARE.getProperty("P2pTransactionTimeout"));
			} catch (RuntimeException e) {
				transactionTimeout = 30000;
				ARE.getLog().warn("连接超时时间设置有误，请检查配置文件[are.xml]。已恢复默认设置。");
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
	public static String createJsonString(HttpServletRequest request) throws Exception{
		
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
	
	public static JSONObject runTranProcess(String sMethod,String sRequestFormat,String sRequestStr) throws Exception{
		String sSignKey = createSignKey(sMethod, sRequestFormat, sRequestStr);
		String result = runHttp(sMethod,sRequestFormat,sRequestStr,sSignKey);
		
		JSONObject obj = (JSONObject) JSONValue.parse(result);
		String sReturnCode = obj.get("returnCode").toString();
		JSONObject responseParams = null;
		if("SUCCESS".equals(sReturnCode)){
			responseParams = (JSONObject) obj.get("ResponseParams");
			responseParams.put("TransFlag", "SUCCESS");
		}else{//异常情况下处理
			responseParams = new JSONObject();
			responseParams.put("TransFlag", "UNSUCCESS");
		}
		
		return responseParams;
	}
	
	
	public static void main(String[] args)throws Exception{
		String method = "runcharge";
		String requestFormat="json";
		String requestStr="{\"RequestParams\":{\"UserID\":\"003756391\",\"SerialNo\":\"11111111111\",\"AdvertLocation\":\"1\",\"FieldValue\":\"4444444\",\"Password\":\"111\",\"SessionID\":\"11231232131232131\",\"RemoteIP\":\"127.0.0.1\",\"ServiceName\":\"127.0.0.1\",\"ServerPort\":\"8088\"},\"deviceType\":\"Pc\"}";
		//requestStr="{\"RequestParams\":{\"Occurdate\":\"2014/04/04\",\"LoanSerialNo\":\"22123132\",\"ContractSerialNo\":\"12312323222\",\"TransID\":\"xdes111111\",\"Amt\":\"100000\",\"P2PTransactionSerialNo\":\"111111123123\",\"UserID\":\"Jerrylufax\"},\"deviceType\":\"Pc\"}";
		String sSignKey = createSignKey(method, requestFormat, requestStr);
		String result = runHttp(method,requestFormat,requestStr,sSignKey);
	}

}
