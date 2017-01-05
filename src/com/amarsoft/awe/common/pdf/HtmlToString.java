package com.amarsoft.awe.common.pdf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amarsoft.awe.util.json.JSONObject;



public class HtmlToString {
	
	
	
	public static String getHtmlString(String sHtml,JSONObject params)throws Exception{
		if(params==null)return sHtml;
		
		String reg = "#\\{[a-zA-Z0-9\\-\\_\\:]+?\\}";
		StringBuffer sbResult = new StringBuffer();
		Pattern pattern = Pattern.compile(reg);
		Matcher m = pattern.matcher(sHtml);
		while (m.find()) {
			String v = m.group(0);
			String sKey = v.substring(2, v.length()-1);
			if("WEBROOTPATH".equals(sKey))
				continue;
			else if(params.containsKey(sKey)){
				m.appendReplacement(sbResult, params.get(sKey).toString());
			}
			else
				m.appendReplacement(sbResult, "");
		}
		m.appendTail(sbResult);
		
		/*
		java.util.Iterator it = params.keySet().iterator();
		while(it.hasNext()){
			String sKey = it.next().toString();
			String sValue = params.get(sKey).toString();
			sHtml = sHtml.replaceAll("#\\{"+ sKey +"\\}", sValue);
		}
		*/
		return sbResult.toString();
	}
	
	
}
