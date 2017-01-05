package com.amarsoft.p2ptrade.util;

import java.util.ArrayList;

public class SMSTemplet {

	private String id = "";
	private String desc = "";
	private String content = "";
	private ArrayList<SMSProperty> properties = new ArrayList();

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDesc() {
		return this.desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public ArrayList<SMSProperty> getProperties() {
		return this.properties;
	}

	public void addProperties(SMSProperty propertie) {
		this.properties.add(propertie);
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
