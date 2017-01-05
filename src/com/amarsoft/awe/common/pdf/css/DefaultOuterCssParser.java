package com.amarsoft.awe.common.pdf.css;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amarsoft.awe.common.pdf.util.StringConvertor;
/**
 * 外部css文件解析器默认实现类
 * @author flian
 *
 */
public class DefaultOuterCssParser implements IOuterCssParser {
	
	protected DefaultOuterCssParser(){}

	public String parse(String html, String resourcePath) {
		//通过正则表达式循环获取link标签
		StringBuffer sb = new StringBuffer();
		Pattern pattern = Pattern.compile("<\\s*[Ll][Ii][Nn][Kk][\\w\\W]+?>");
		Matcher m = pattern.matcher(html);
		while (m.find()) {
			String sLink = m.group(0);
			HashMap hmLink = getTagProperties(sLink);
			if(hmLink.containsKey("href")){
				//获得此css文件路径
				String sCssFileName = resourcePath + hmLink.get("href").toString();
				m.appendReplacement(sb, getContent(sCssFileName));
			}
			else{//此link没有指向文件
				m.appendReplacement(sb, "");
			}
		}
		m.appendTail(sb);
		//System.out.println(sb.toString());
		return sb.toString();
	}

	//解析每个css样式
	private String getContent(String sCssFileName){
		File file = new File(sCssFileName);
		if(file.exists()==false)
			return "";
		java.io.FileInputStream fis = null;
		try{
			fis = new FileInputStream(file);
			String sContent = StringConvertor.inputStream2String(fis,"utf-8").trim();
			if(sContent.length()>1 && !sContent.substring(1).matches("[0-9a-zA-Z\\/\\.#]"))
				sContent = sContent.substring(1);
			sContent = "<style>" + sContent + "</style>\r\n";
			
			return sContent;
		}
		catch(Exception e){
			e.printStackTrace();
			return "";
		} finally{
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//将标签link代码解析成Hashtable对象
	protected HashMap getTagProperties(String tagHtml){
		tagHtml = tagHtml.substring(1,tagHtml.length()-1).trim();
		tagHtml = tagHtml.substring("link".length());
		if(tagHtml.endsWith("/"))
			tagHtml = tagHtml.substring(0,tagHtml.length()-1);
		HashMap result = new HashMap();
		String[] arr = tagHtml.split("\\s");
		for(int i=0;i<arr.length;i++){
			String sValue = arr[i].trim();
			if(sValue.length()==0)continue;
			int iDot = sValue.indexOf("=");
			if(iDot==-1){
				result.put(sValue, null);
				continue;
			}
			String sAttributeName = sValue.substring(0,iDot);
			String sAttributeValue = sValue.substring(iDot+1);
			if(sAttributeValue.startsWith("\"") && sAttributeValue.endsWith("\"")){
				sAttributeValue = sAttributeValue.substring(1,sAttributeValue.length()-1);
			}
			else if(sAttributeValue.startsWith("'") && sAttributeValue.endsWith("'")){
				sAttributeValue = sAttributeValue.substring(1,sAttributeValue.length()-1);
			}
			result.put(sAttributeName.toLowerCase(), sAttributeValue);
		}
		return result;
	}
}
