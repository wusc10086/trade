<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@page import="com.amarsoft.mobile.webservice.MobileService,com.amarsoft.are.jbo.*"%>
<%
try{

	 com.amarsoft.p2ptrade.account.LogonHandler l = new com.amarsoft.p2ptrade.account.LogonHandler();
	 com.amarsoft.awe.util.json.JSONObject obj = new com.amarsoft.awe.util.json.JSONObject();
	 obj.put("loginId", "loginId");
	 obj.put("password", "password");
	 
	 l.createResponse(obj, null);
	 out.println("OK");
	
}catch(Exception e){
	e.printStackTrace();
	out.print("response.unkown");
}   
%>