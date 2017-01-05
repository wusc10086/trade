package com.amarsoft.p2ptrade.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 获取当前日期和时间
 * @author yhxu
 *
 */
public class TimeTool {
	private String sCurrentDate;
	private String sCurrentTime;
	private String sCurrentMoment;
	private String sChDate;
	
	/**
	 * 获取日期
	 * @return
	 */
	public String getsCurrentDate() {
		return sCurrentDate;
	}

	/**
	 * 获取时间
	 * @return
	 */
	public String getsCurrentTime() {
		return sCurrentTime;
	}
	
	/**
	 * 获取日期+时间
	 * @return
	 */
	public String getsCurrentMoment() {
		return sCurrentMoment;
	}

	
	/**
	 * 获取中文格式时间
	 * @return
	 */
	public String getsChDate() {
		return sChDate;
	}

	/**
	 * 不带参数的构造方法
	 */
	public TimeTool() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String sDateAndTime = sdf.format(date);
		String[] sTimearray = sDateAndTime.split(" ");
		sCurrentMoment = sDateAndTime;
		sCurrentDate = sTimearray[0];
		sCurrentTime = sTimearray[1];
		//05/15 14:31
		sChDate = sDateAndTime.substring(5, 16);
//		sChDate = "("+sChDate;
//		sChDate = sChDate.replace("/", ")月(").replace(" ", ")日(").replace(":", ")时(");
//		sChDate = sChDate+")分";
		sChDate = sChDate.replace("/", "月").replace(" ", "日").replace(":", "时");
		sChDate = sChDate+"分";
	}

	/**
	 * 带参数的构造方法
	 * @param sTimeForm
	 */
	public TimeTool(String sTimeForm) {
		SimpleDateFormat sdf = new SimpleDateFormat(sTimeForm);
		Date date = new Date();
		String sDateAndTime = sdf.format(date);
		String[] sTimearray = sDateAndTime.split(" ");
		sCurrentMoment = sDateAndTime;
		sCurrentDate = sTimearray[0];
		sCurrentTime = sTimearray[1];
	}
}
