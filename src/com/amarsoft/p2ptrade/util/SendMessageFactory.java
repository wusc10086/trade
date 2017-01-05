package com.amarsoft.p2ptrade.util;

public class SendMessageFactory {
	 //短信发送---三通的接口
	public static final String SMS_3TONG_CONFIG = "3tong.com";
	
	public static MobileServiceInvoker getClientSend(String transCode) throws Exception{
		if(SMS_3TONG_CONFIG.equals(transCode)){
			return new PhoneMessage();
		}else{
			throw new Exception("交易码不正确");
		}		
	}
}
