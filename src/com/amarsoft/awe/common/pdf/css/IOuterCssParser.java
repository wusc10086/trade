package com.amarsoft.awe.common.pdf.css;

/**
 * 外部css文件解析器接口
 * @author flian
 *
 */
public interface IOuterCssParser {
	String parse(String html,String resourcePath);
}
