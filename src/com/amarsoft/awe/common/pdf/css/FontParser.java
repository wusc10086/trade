package com.amarsoft.awe.common.pdf.css;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字体替换
 * @author Administrator
 *
 */
public class FontParser extends DefaultOuterCssParser {
	public String parse(String html, String resourcePath) {
		//通过正则表达式循环获取link标签
		StringBuffer sb = new StringBuffer();
		Pattern pattern = Pattern.compile("[Ff][Oo][Nn][Tt][\\-][Ff][Aa][Mm][Ii][Ll][Yy]\\s*:[\\w\\W]+?;");
		Matcher m = pattern.matcher(html);
		while (m.find()) {
			String sFont = m.group(0);
			String[] arr = sFont.split(":");
			arr[1] = arr[1].substring(0,arr[1].length()-1).trim().toLowerCase();
			arr[1] = arr[1].replaceAll("\"", "").trim();
			if(arr[1].equals("宋体")){
				sFont = "font-family:SimSun;";
			}
			else if(arr[1].equals("黑体") || arr[1].equals("simhei")){
				sFont = "font-family:SimHei;";
			}
			else if(arr[1].indexOf("楷体")>-1 || arr[1].equals("simkai")){
				sFont = "font-family:KaiTi_GB2312;";
			}
			else{
				sFont = "font-family:SimSun;";
			}
			m.appendReplacement(sb, sFont);
		}
		m.appendTail(sb);
		//System.out.println(sb.toString());
		return sb.toString();
	}
}
