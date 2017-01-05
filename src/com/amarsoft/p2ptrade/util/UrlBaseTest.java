package com.amarsoft.p2ptrade.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

public class UrlBaseTest {
	public static void main(String[] args) throws UnsupportedEncodingException {  
		       String str = "2014120500000002";  
			String s="您好~<br>感谢您注册厚本金融，请验证您的邮箱。点击立即验证邮箱：<br> http://localhost:8080/qsh_p2p/member/account/mail_success.jsp?userid=MjAxNDExMTYwMDAyMQ==&serialno=MjAxNDEyMDUwMDAwMDAwOA==&chkmsg=MDgyODQy<br>如果该链接无法点击,请您直接复制以下地址到浏览器，完成验证：<br>http://localhost:8080/qsh_p2p/member/account/mail_success.jsp?userid=MjAxNDExMTYwMDAyMQ==&serialno=MjAxNDEyMDUwMDAwMDAwOA==&chkmsg=MDgyODQy,_KEY_SERIALNO=2014120500000008";
	       System.out.println("sssssssssssssssssssssssssssssssss"+s.length());
			// 加密该字符串  
	       String encodedString = UrlBase64.encoded(str);  
		      System.out.println(encodedString);  
	        // 解密该字符串  
		       String decodedString = UrlBase64.decode(encodedString);  
		       System.out.println(decodedString);  
		   }  

}
