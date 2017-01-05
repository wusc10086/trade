package com.amarsoft.p2ptrade.util;

public interface MobileServiceInvoker {

	//设置手机号
	public void setPhone(String phone);
	//设置发送内容
	public void setContent(String content);
	//启动发送
	public void send();
	//发送状态
	public boolean isSuccess();
}
