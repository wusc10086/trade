<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@page import="com.amarsoft.p2ptrade.util.ImageService"%><%@ page import="org.apache.commons.lang.StringEscapeUtils"%><%
try{
	response.setHeader("Cache-Control","no-store");
	response.setHeader("Pragma","no-cache");
	response.setDateHeader("Expires",0);
	response.setContentType("image/gif");
	
	String sMethod = null;
	String sRequestFormat = StringEscapeUtils.escapeJavaScript(request.getParameter("requestFormat"));
	if(sRequestFormat==null)sRequestFormat="json";
	String sRequestStr = null;
	String sSignKey = null;
	//业务处理类映射名称
	sMethod = StringEscapeUtils.escapeJavaScript(request.getParameter("code"));
	//传输主体内容
	sRequestStr = StringEscapeUtils.escapeJavaScript(request.getParameter("param"));
	//签名
	sSignKey = StringEscapeUtils.escapeJavaScript(request.getParameter("signKey"));
	if(sMethod==null || sSignKey==null || sRequestStr==null){
		out.print("request.invalid");
	}
	else{
		ImageService m = new ImageService(request.getRemoteAddr());
		Object message = m.genImage(sMethod, sRequestFormat, sRequestStr, sSignKey);
		if(message instanceof byte[]){
			ServletOutputStream sos = response.getOutputStream();
			sos.write((byte[])message);
			sos.close();
		}
		else{
			out.print(message);
		}
	}
}catch(Exception e){
	e.printStackTrace();
	out.print("response.unkown");
}   
%>