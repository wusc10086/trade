package com.amarsoft.p2ptrade.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.amarsoft.are.ARE;
import com.amarsoft.are.log.Log;

import org.apache.commons.lang.StringUtils;

//∂Ã–≈∑¢ÀÕ≈‰÷√
public class SendMobileMessage {
	Log logger = ARE.getLog();

	private MobileServiceInvoker msi = null;

	public MobileServiceInvoker getMobileService() {
		return msi;
	}

	public SendMobileMessage() {
		try {
			this.msi = SendMessageFactory.getClientSend(SendMessageFactory.SMS_3TONG_CONFIG);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void execute(String msgTempletId, String receiver, HashMap<String, Object> parameters) throws Exception {

		//SMSFactory.initSMSConstant();
		HashMap properties = new HashMap();

		ArrayList configs = SMSFactory.getSMSProperties();

		SMSTemplet smtTemplet = SMSFactory.getSmsTemplet(msgTempletId);

		cloneProperties(properties, configs);

		cloneProperties(properties, parameters);

		cloneProperties(properties, smtTemplet.getProperties());

		String contents = getContent(properties, smtTemplet);

		if (contents == null)
			contents = "";
		contents = StringUtils.trim(contents);
		contents = contents.replaceAll("\n", "");
		contents = contents.replaceAll("\r", "");

		msi.setPhone(receiver);
		msi.setContent(contents);
		msi.send();
	}

	private final String getContent(HashMap<String, Object> properties, SMSTemplet smtTemplet) throws Exception {
		String contents = smtTemplet.getContent();

		for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			Object value = properties.get(key);
			contents = StringUtils.replace(contents,key,
					String.valueOf(value));
		}

		return contents;
	}

	private void cloneProperties(HashMap<String, Object> properties, ArrayList<SMSProperty> p) {
		for (int i = 0; i < p.size(); i++) {
			SMSProperty sp = (SMSProperty) p.get(i);
			if (("DEFAULT".equals(sp.getValueSource())) || ("".equals(sp.getValueSource())) || (sp.getValueSource() == null))
				addProperties(properties, (SMSProperty) p.get(i));
		}
	}

	private void cloneProperties(HashMap<String, Object> properties,
			HashMap<String, Object> p) {
		for (Iterator iter = p.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			properties.put(key, p.get(key));
		}
	}

	private void addProperties(HashMap<String, Object> properties, SMSProperty sp) {
		properties.put(sp.getId(), sp.getValue());
	}
}
