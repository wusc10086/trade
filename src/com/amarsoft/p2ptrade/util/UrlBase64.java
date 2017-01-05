package com.amarsoft.p2ptrade.util;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;  
public class UrlBase64 {
	/*------SerialNo--------2014120500000002
	------serialNo1--------2014120500000002
	------Chkmsg--------200721
	------chkmsg1--------200721
	------userID--------2014111600033*/
	
	
	 public final static String ENCODING = "UTF-8";  
	   // 加密  
	   public static String encoded(String data) throws UnsupportedEncodingException {  
	       byte[] b = Base64.encodeBase64(data.getBytes(ENCODING));  
           return new String(b, ENCODING);  
	    }  
	   // 解密  
	    public static String decode(String data) throws UnsupportedEncodingException {  
            byte[] b = Base64.decodeBase64(data.getBytes(ENCODING));  
	        return new String(b, ENCODING);  
	    }  


	/*public static void main(String[] args) {
	 
	       // 加密该字符串  
	       String encodedString = Base64.encoded("2014120500000002");  
	       System.out.println(encodedString);  
	       // 解密该字符串  
	       String decodedString = Base64.decode(encodedString);  
	       System.out.println(decodedString);  

	}*/

}
