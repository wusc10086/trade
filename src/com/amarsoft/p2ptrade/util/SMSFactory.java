package com.amarsoft.p2ptrade.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class SMSFactory {

	public static ArrayList<SMSProperty> smsProperties = new ArrayList<SMSProperty>();

	public static HashMap<String, SMSTemplet> smsTemplets = new HashMap<String, SMSTemplet>();

	public static void initSMSConstant() throws Exception {
		initSMSConstant("SMSConfig.xml");
		//initSMSConstant("D:\\loanwork\\qsh_trade\\WebContent\\WEB-INF\\etc\\SMSConfig.xml");
	}
	public static void main(String[] args) throws Exception {
		initSMSConstant("F:\\BaiduYunDownload\\java\\hb\\hbtrade\\WebContent\\WEB-INF\\etc\\SMSConfig.xml");
	}
	public static void initSMSConstant(String configFile) throws Exception {
		InputStream in = null;
		try {
			in = getInputStream(configFile);
			SAXBuilder sb = new SAXBuilder();
			Document document = null;
			document = sb.build(in);
			Element root = document.getRootElement();

			Element commonElement = root.getChild("Properties");
			if (commonElement != null) {
				List list = commonElement.getChildren("property");
				for (int i = 0; i < list.size(); i++) {
					Element proElement = (Element) list.get(i);
					SMSProperty config = getSMSProperty(proElement);
					smsProperties.add(config);
				}
			}

			List templetElement = root.getChild("Templets").getChildren(
					"Templet");
			for (int i = 0; i < templetElement.size(); i++) {
				Element element = (Element) templetElement.get(i);
				SMSTemplet smsTemplet = getSMSTemplet(element);
				smsTemplets.put(smsTemplet.getId(), smsTemplet);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				in.close();
		}
	}

	private static final SMSTemplet getSMSTemplet(Element element) {
		SMSTemplet st = new SMSTemplet();
		st.setId(element.getAttributeValue("id"));
		st.setDesc(element.getAttributeValue("desc"));

		List properties = element.getChild("ExtendProperties").getChildren("property");

		for (int i = 0; i < properties.size(); i++) {
			st.addProperties(getSMSProperty((Element) properties.get(i)));
		}
		st.setContent(element.getChildText("SMSText"));
		
		return st;
	}

	private static final SMSProperty getSMSProperty(Element element) {
		SMSProperty congfig = new SMSProperty();
		congfig.setId(element.getAttributeValue("id"));
		congfig.setType(element.getAttributeValue("type"));
		congfig.setValue(element.getAttributeValue("value"));
		congfig.setValueSource(element.getAttributeValue("valuesource"));
		congfig.setDesc(element.getAttributeValue("desc"));
		return congfig;
	}

	private static InputStream getInputStream(String propertyFile) throws IOException {
		InputStream in = null;
		in = SMSService.class.getResourceAsStream(propertyFile);

		if (in == null) {
			in = SMSService.class.getClassLoader().getResourceAsStream(
					propertyFile);
		}
		if (in == null) {
			in = new FileInputStream(new File(propertyFile));
		}
		return in;
	}

	public static ArrayList<SMSProperty> getSMSProperties() {
		return smsProperties;
	}

	public static SMSTemplet getSmsTemplet(String templetName) {
		return (SMSTemplet) smsTemplets.get(templetName);
	}
}
