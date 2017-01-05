package com.amarsoft.p2ptrade.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.amarsoft.are.ARE;


public class JavaSendMail {  
	
	private static int ConnectTimeout = 600000;
	
	private static int SocketTimeout = ConnectTimeout;
	
	 // 设置邮件服务器主机名  
	private static String mailHost= ARE.getProperty("mailHost");
	private static String mailProxy = ARE.getProperty("mailProxy", "");
	//设置发件人
	private static String sendPersonMail= ARE.getProperty("sendPersonMail");
	//设置邮件服务器名称
	private static String mailServiceName= ARE.getProperty("mailServiceName");
	//设置邮件服务器密码 
    private static String mailServicePassword= ARE.getProperty("mailServicePassword");
    //设置发件内容
    private String mailContent;
    //设置收件人邮箱
    private String receiveMail;
	public String getReceiveMail() {
		return receiveMail;
	}
	public void setReceiveMail(String receiveMail) {
		this.receiveMail = receiveMail;
	}
	public String getMailContent() {
		return mailContent;
	}
	public void setMailContent(String mailContent) {
		this.mailContent = mailContent;
	}
	
	public static void SendEmailByWeb(String sContent,String sReceiveMail) throws Exception{
		String sUrl = mailProxy.trim();
		CloseableHttpClient httpClient = null;//获取httpClient实例 ，采用closebleHttpClient
		try{
			httpClient = HttpClients.createDefault();//httpClients.createDefault()设置初始化的httpClent实例
			HttpPost httpMethod = new HttpPost(sUrl);//获得httpPost的实例，new HttpHost(传入服务器的主机名)
			List<NameValuePair> nameValuePairs =new ArrayList<NameValuePair>();//传入多个参数List<NameValuePair>
			nameValuePairs.add(new BasicNameValuePair("title","厚本金融"));
			ARE.getLog().info("填充post参数:title=" + "厚本金融");
			nameValuePairs.add(new BasicNameValuePair("content",sContent));
			ARE.getLog().info("填充post参数:content=" + sContent);
			nameValuePairs.add(new BasicNameValuePair("receiver",sReceiveMail));
			ARE.getLog().info("填充post参数:receiver=" + sReceiveMail);
			/*
			nameValuePairs.add(new BasicNameValuePair("mailHost",mailHost));
			ARE.getLog().info("填充post参数:mailHost=" + mailHost);
			nameValuePairs.add(new BasicNameValuePair("sender",sendPersonMail));
			ARE.getLog().info("填充post参数:sender=" + sendPersonMail);
			nameValuePairs.add(new BasicNameValuePair("senderpassword",mailServicePassword));
			ARE.getLog().info("填充post参数:senderpassword=" + mailServicePassword);
			*/
			httpMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs,"GBK"));//httpEntity
			
			RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(SocketTimeout)
					.setConnectTimeout(ConnectTimeout)
					.build();//设置请求和传输超时时间
			httpMethod.setConfig(requestConfig);
			
			ARE.getLog().info("connect to " + sUrl);
			CloseableHttpResponse response = httpClient.execute(httpMethod);//httpClient.excute()
			HttpEntity httpEntity = response.getEntity();
			int responseStatus = response.getStatusLine().getStatusCode();
			if(responseStatus==200){//正确返回
				String sResult = EntityUtils.toString(httpEntity).trim();
				if(sResult.equalsIgnoreCase("success")){
					ARE.getLog().info("send success");
					return;
				}
				else{
					throw new Exception("发送邮件失败：错误代码为" + sResult);
				}
			}
			else{
				throw new Exception("发送邮件失败：页面执行错误，错误代码为" + responseStatus);
			}
		}
		finally{
			if(httpClient!=null)httpClient.close();
		}
		
	}
	
    public static void SendEmail(String sContent,String sReceiveMail) throws Exception{
    	if(mailProxy!=null && mailProxy.trim().length()>0){
    		SendEmailByWeb(sContent,sReceiveMail);
    		return;
		}
    	 Properties props = new Properties();  
	        // 开启debug调试  
	        props.setProperty("mail.debug", "true");  
	        // 发送服务器需要身份验证  
	        props.setProperty("mail.smtp.auth", "true");  
	        // 设置邮件服务器主机名  
	        props.setProperty("mail.host", mailHost);  
	        // 发送邮件协议名称  
	        props.setProperty("mail.transport.protocol", "smtp");  
	        // 设置环境信息  
	        Session session = Session.getInstance(props);  
	        // 创建邮件对象  
	        Message msg = new MimeMessage(session);  
	        msg.setSubject("厚本金融");  
	        // 设置邮件内容  
	        msg.setText(sContent);  
	        // 设置发件人  
	        msg.setFrom(new InternetAddress(sendPersonMail));  
	          
	        Transport transport = session.getTransport();  
	        // 连接邮件服务器  
	        transport.connect(mailServiceName, mailServicePassword);  
	        // 发送邮件  
	        transport.sendMessage(msg, new Address[] {new InternetAddress(sReceiveMail)});  
	        // 关闭连接  
	        transport.close(); 
    	
    }
	
 // MD5加密函数
 	public static String MD5Encode(String sourceString) {
 		String resultString = null;
 		try {
 			resultString = new String(sourceString);
 			MessageDigest md = MessageDigest.getInstance("MD5");
 			resultString = byte2hexString(md.digest(resultString.getBytes()));
 		} catch (Exception ex) {
 		}
 		return resultString;
 	}

 	public final static String byte2hexString(byte[] bytes) {
 		StringBuffer bf = new StringBuffer(bytes.length * 2);
 		for (int i = 0; i < bytes.length; i++) {
 			if ((bytes[i] & 0xff) < 0x10) {
 				bf.append("0");
 			}
 			bf.append(Long.toString(bytes[i] & 0xff, 16));
 		}
 		return bf.toString();
 	}
	

}  
