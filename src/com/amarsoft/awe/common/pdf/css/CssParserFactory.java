package com.amarsoft.awe.common.pdf.css;

public class CssParserFactory {
	
	public static IOuterCssParser getCssParser(){
		return new DefaultOuterCssParser();
	}
	
	public static IOuterCssParser getCssParser(String className){
		try{
			return (IOuterCssParser)Class.forName(className).newInstance();
		} 
		catch(Exception e){
			return null;
		}
	}
}
