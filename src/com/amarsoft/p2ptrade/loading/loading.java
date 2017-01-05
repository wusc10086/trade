package com.amarsoft.p2ptrade.loading;

import java.util.Properties;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class loading extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return accessLoading(request);
	}
	private  Object accessLoading(JSONObject requst) throws HandlerException {
			return null;
}
}
