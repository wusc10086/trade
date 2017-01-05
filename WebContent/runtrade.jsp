<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@page import="com.amarsoft.mobile.webservice.MobileService"%>
<%
try{

	String sMethod = null;
	String sRequestFormat = request.getParameter("requestFormat");
	if(sRequestFormat==null)sRequestFormat="json";
	String sRequestStr = null;
	String sSignKey = null;
	//业务处理类映射名称
	sMethod = request.getParameter("code");
	//传输主体内容
	sRequestStr = request.getParameter("param");
	//签名
	sSignKey = request.getParameter("signKey");
	if(sMethod==null || sSignKey==null || sRequestStr==null){
		out.print("request.invalid");
	}
	else{
		MobileService m = new MobileService(request.getRemoteAddr());
		String message = m.runService(sMethod, sRequestFormat, sRequestStr, sSignKey);
		out.print(message);
	}
}catch(Exception e){
	e.printStackTrace();
	out.print("response.unkown");
}   
%>