package com.amarsoft.awe.common.pdf.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringConvertor {
	
	/**
	 * 字节流到文本转换
	 * @param inStream
	 * @param charsetName 编码方式
	 * @return String
	 */
	public static String inputStream2String(InputStream inStream,String charsetName){
		try {
			ByteArrayOutputStream ois = new ByteArrayOutputStream();
	    	byte[] buffer = new byte[10240];
	    	int b = inStream.read(buffer);
	    	while(b>-1){
	    		ois.write(buffer,0,b);
	    		b = inStream.read(buffer);
	    	}
			String sResult =new String(ois.toByteArray(),charsetName); 
			return sResult;
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}	
	
	/**
	 * 文本到流的转换
	 * @param content
	 * @param charsetName
	 */
	public static InputStream string2InputStream(String content,String charsetName){
		try{
			byte[] bytes = content.getBytes(charsetName);
			ByteArrayInputStream ins = new ByteArrayInputStream(bytes);
			return ins;
		}
		catch(Exception e){
			return null;
		}
	}
}
