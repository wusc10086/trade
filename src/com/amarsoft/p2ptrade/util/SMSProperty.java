package com.amarsoft.p2ptrade.util;

public class SMSProperty {
	private String id = "";
	private String desc = "";
	private String type = "";
	private String valueSource = "";
	private String value = "";

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

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValueSource() {
		return this.valueSource;
	}

	public void setValueSource(String valueSource) {
		this.valueSource = valueSource;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
