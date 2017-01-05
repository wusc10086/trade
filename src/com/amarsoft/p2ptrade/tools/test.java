package com.amarsoft.p2ptrade.tools;

import java.util.HashMap;

import com.amarsoft.mobile.webservice.business.HandlerException;

public class test {

	public static void main(String[] args) {
		GeneralTools gt = new GeneralTools();
		HashMap <String , Object> parameters = new HashMap();
		parameters.put("ProjectName", "齐发财-贷款旅游_测试的");
		parameters.put("Balance", "10000");
		try {
			gt.sendSMS("P2P_TZCG", "13764170217", parameters);
		} catch (HandlerException e) {
			e.printStackTrace();
		}
	}

}
