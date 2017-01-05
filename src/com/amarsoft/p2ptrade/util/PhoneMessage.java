package com.amarsoft.p2ptrade.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.amarsoft.are.ARE;

/**http接口
 * 此版本使用document 对象封装XML，解决发送短信内容包涵特殊字符而出现无法解析，如 短信为：“你好，<%&*&*&><<<>fds测试短信”
 * 
 */
public class PhoneMessage implements MobileServiceInvoker{
	// ############################此部分参数需要修改############################
	private String userName = "dh21988";//"dh21399"; // 接口账号
	private String password = "#8@QDg4S";//"123loan2014"; // 密码
	private String phone = "13588888888"; // 要发送的手机号码
	private String content = "注册验证码：8909"; // 短信内容
	private String sign = "【厚本金融】";//"【易贷中国】"; // 短信签名，该签名必须提前报备
	private String msgid = ""; // 自定义msgid
	private String subcode = ""; // 扩展字号
	private String sendtime = ""; // 定时发送时间，时间格式201305051230
	private String url = "http://3tong.net"; // 三网通使用地址
	private String sReturn = ""; //发送短信后返回的报文
	public PhoneMessage(){
		this.userName = ARE.getProperty("sms_userid");
		this.password = ARE.getProperty("sms_password");
		this.sign = ARE.getProperty("sms_sign");
	}
	
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public  String getContent() {
		return content;
	}

	public  void setContent(String content) {
		this.content = content;
	}

	// ####################此部分参数需要修改###################
	public static void main(String[] args) {
		PhoneMessage msg = new PhoneMessage();
		ARE.getProperty("");
		msg.setPhone("15121161173");
		msg.setContent("恭喜您修改手机成功，请重新登录查看.");
		// 发送短信
		System.out.println("*************发送短信*************");
		//msg.send();
		msg.isSuccess();
//		// 获取状态报告
//		System.out.println("*************状态报告*************");
//		msg.getReport();
//		// 获取上行
//		System.out.println("*************获取上行*************");
//		msg.getSms();
//
//		
//		// 获取余额
//		System.out.println("*************获取余额*************");
//		msg.getBalance();
	}

	private String getsReturn() {
		return sReturn;
	}

	private void setsReturn(String sReturn) {
		this.sReturn = sReturn;
	}

	// MD5加密函数
	public String MD5Encode(String sourceString) {
		String resultString = null;
		try {
			resultString = new String(sourceString);
			MessageDigest md = MessageDigest.getInstance("MD5");
			resultString = byte2hexString(md.digest(resultString.getBytes()));
		} catch (Exception ex) {
		}
		return resultString;
	}

	public final String byte2hexString(byte[] bytes) {
		StringBuffer bf = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			if ((bytes[i] & 0xff) < 0x10) {
				bf.append("0");
			}
			bf.append(Long.toString(bytes[i] & 0xff, 16));
		}
		return bf.toString();
	}

	// 发送短信
	/**
	 * 发送短信方法使用document 方法封装XML字符串
	 */
	public void send() {
		String posturl = url+"/http/sms/Submit";
		Map<String, String> params = new LinkedHashMap<String, String>();
		String  message = DocXml(userName,  MD5Encode(password),  msgid,
		phone, content, sign, subcode, sendtime);
		ARE.getLog().debug(message);
		params.put("message", message);
		String resp = doPost(posturl, params);
		setsReturn(resp);
		ARE.getLog().debug(resp);
	}

	// 状态报告
	public void getReport() {
		String posturl = url + "/http/sms/Report";
		Map<String, String> params = new LinkedHashMap<String, String>();
		String message = "<?xml version='1.0' encoding='UTF-8'?><message>"
				+ "<account>" + userName + "</account>" + "<password>"
				+ MD5Encode(password) + "</password>"
				+ "<msgid></msgid><phone></phone></message>";
		params.put("message", message);
		String resp = doPost(posturl, params);
		ARE.getLog().debug(resp);
	}

	// 查询余额
	public void getBalance() {
		String posturl = url + "/http/sms/Balance";
		Map<String, String> params = new LinkedHashMap<String, String>();
		String message = "<?xml version='1.0' encoding='UTF-8'?><message><account>"
				+ userName
				+ "</account>"
				+ "<password>"
				+ MD5Encode(password)
				+ "</password></message>";
		params.put("message", message);
		//System.out.println(message);
		String resp = doPost(posturl, params);
		System.out.println(resp);
	}

	// 获取上行
	public void getSms() {
		String posturl = url + "/http/sms/Deliver";
		Map<String, String> params = new LinkedHashMap<String, String>();
		String message = "<?xml version='1.0' encoding='UTF-8'?><message><account>"
				+ userName
				+ "</account>"
				+ "<password>"
				+ MD5Encode(password)
				+ "</password></message>";
		params.put("message", message);
		String resp = doPost(posturl, params);
		System.out.println(resp);
	}

	/**
	 * 执行一个HTTP POST请求，返回请求响应的HTML
	 * 
	 * @param url
	 *            请求的URL地址
	 * @param params
	 *            请求的查询参数,可以为null
	 * @return 返回请求响应的HTML
	 */
	public String doPost(String url, Map<String, String> params) {
		String response = null;
		
		
		
		CloseableHttpClient httpClient = null;
		try{
			httpClient = HttpClients.createDefault();
			HttpHost proxy =null;
			if(ARE.getProperty("sms_useproxy", false)){
				String sHost = ARE.getProperty("sms_proxyhost", "");
				if(sHost.length()>0){
					int iPort = ARE.getProperty("sms_proxyport", 3128);
					proxy = new HttpHost(sHost,iPort,"http");
					ARE.getLog().debug("send sms use proxy:" + sHost+","+iPort);
				}
				
			}
			Builder builder = RequestConfig.custom()
					.setSocketTimeout(60000)
					.setConnectTimeout(60000);
			if(proxy!=null){
				builder.setProxy(proxy);
			}
			HttpPost httpMethod = new HttpPost(url);
			if (!params.isEmpty()) {
				int i = 0;
				List<NameValuePair> nameValuePairs =new ArrayList<NameValuePair>();
				for (Entry<String, String> entry : params.entrySet()) {
					nameValuePairs.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
				}
				httpMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs,"UTF-8"));
				RequestConfig requestConfig = builder.build();//设置请求和传输超时时间
				httpMethod.setConfig(requestConfig);
				ARE.getLog().info("connect to " + url);
				CloseableHttpResponse cresponse = httpClient.execute(httpMethod);
				HttpEntity httpEntity = cresponse.getEntity();
				int responseStatus = cresponse.getStatusLine().getStatusCode();
				if(responseStatus==200){//正确返回
					response = EntityUtils.toString(httpEntity).trim();
				}
				else{
					throw new Exception("发送短信失败：页面执行错误，错误代码为" + responseStatus);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			try{
				if(httpClient!=null)httpClient.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		/*
		
		PostMethod postMethod = new PostMethod(url);
		postMethod.getParams().setParameter(
				HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
		// 设置Post数据
		if (!params.isEmpty()) {
			int i = 0;
			NameValuePair[] data = new NameValuePair[params.size()];
			for (Entry<String, String> entry : params.entrySet()) {
				data[i] = new NameValuePair(entry.getKey(), entry.getValue());
				i++;
			}
			postMethod.setRequestBody(data);
		}
		try {
			client.executeMethod(postMethod);
			if (postMethod.getStatusCode() == HttpStatus.SC_OK) {
				response = postMethod.getResponseBodyAsString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			postMethod.releaseConnection();
		}
		*/
		return response;
	}

	/**
	 * 使用document 对象封装XML
	 */
	public String  DocXml(String  userName,String  pwd,String  msgid,String  phone,String
			contents,String sign,String  subcode,String sendtime) {
		Document doc = DocumentHelper.createDocument();
		doc.setXMLEncoding("UTF-8");
		Element message = doc.addElement("message");
		Element account = message.addElement("account");
		account.setText(userName);
		Element password = message.addElement("password");
		password.setText(pwd);
		Element msgid1 = message.addElement("msgid");
		msgid1.setText(msgid);
		Element phones = message.addElement("phones");
		phones.setText(phone);
		Element content = message.addElement("content");
		content.setText(contents);
		Element sign1 = message.addElement("sign");
		sign1.setText(sign);
		Element subcode1 = message.addElement("subcode");
		subcode1.setText(subcode);
		Element sendtime1 = message.addElement("sendtime");
		sendtime1.setText(sendtime);
		return message.asXML();
	}

	//解析返回的报文分析发送状态
	//<?xml version='1.0' encoding='UTF-8'?><response><msgid>bfe173212560473587d556e98f6fb60c</msgid><result>0</result><desc>提交成功</desc><blacklist></blacklist></response>
	public boolean isSuccess() {
		String sReturn = getsReturn();
		try {
			Document doc = DocumentHelper.parseText(sReturn);
			Element root = doc.getRootElement();
			String result = root.element("result").getText();
			String desc = root.elementText("desc");
			if("0".equals(result) && "提交成功".equals(desc)){
				setsReturn("");
				return true;
			}
		} catch (DocumentException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
}