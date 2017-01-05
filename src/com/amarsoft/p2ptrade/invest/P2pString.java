package com.amarsoft.p2ptrade.invest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;

public class P2pString {

	/**截取字符串**/
	public static String screenStr(String str,int len){

		if(str==null)
			return "";
		else{
			if(str.length()<len)
				return str;
			else
				return str.substring(0, len)+"...";
		}
	}
	
	/***
	 * @param 
	 * day  日期字符串
	 * ft   格式(按照分钟、小时、天、月、年)
	 * x    加几
	 * @return  日期字符串
	 *  
	 * */
	public static String addDateFormat(String day, int ft,int x,String fmt){
		
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");// 24小时制  
        SimpleDateFormat format1 = new SimpleDateFormat(fmt);// 24小时制  
        int caft=0;
        if(ft==1){//分钟
        	caft = Calendar.MINUTE;
        }else if(ft==2){//小时
        	caft = Calendar.HOUR;
        }else if(ft==3){//天
        	caft = Calendar.DATE;
        }else if(ft==4){//月
        	caft = Calendar.MONTH;
        }else {//年
        	caft = Calendar.YEAR;
        }
        day = day.replaceAll("-", "/");
        Date date = null;    
        try {    
            date = format.parse(day);    
        } catch (Exception ex) {    
            ex.printStackTrace();    
        }    
        if (date == null)    
            return "";    
        //System.out.println("front:" + format.format(date)); //显示输入的日期   
        Calendar cal = Calendar.getInstance();    
        cal.setTime(date);    
        cal.add(caft, x);// 24小时制    
        date = cal.getTime();    
        //System.out.println("after:" + format.format(date));  //显示更新后的日期  
        cal = null;    
        return format1.format(date);    	   
	}
	/***
	 * @param 
	 * day  日期字符串
	 * ft   格式(按照分钟、小时、天、月、年)
	 * x    加几
	 * @return  日期字符串
	 *  
	 * */
	public static String addDateFormat(String day, int ft,int x){		        
        return addDateFormat(day, ft, x, "yyyy年MM月dd日 HH时mm分ss秒");    	   
	}
	
	public static String getBetweenTime(String str){
		SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		String sReturn = "";

		str = str.replaceAll("/", "-");
		try {
			java.util.Date begin = new Date();
			java.util.Date end = dfs.parse(str);   
			long between = (end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
			long day = between/(24*3600);   
			long hour = between%(24*3600)/3600;   
			long minute = between%3600/60;   
			long second = between%60/60;   
			if(day<0||hour<0||minute<0||second<0)
				sReturn = "已结束";
			else{
				if(day>0)
					sReturn += day+"天";
				if(hour>0)
					sReturn += hour+"小时";
				if(minute>0)
					sReturn += minute+"分";
				if(second>0)
					sReturn += second+"秒";
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}   		
		return sReturn;
	}
	/**
	 * @param
	 * 表名
	 * 字段名
	 * 格式
	 * 前缀
	 * 数据库连接
	 * uc合同编号生成规则
	 * **/
	private static String getSerialNo(String sTable, String sColumn,String sNoFmt,String sPrefix, Connection conn) throws Exception
	{
		DecimalFormat dfTemp = new DecimalFormat(sNoFmt);
		ResultSet rsTemp = null;

	
		String sSql = "select count(" + sColumn + ") from " + sTable + " where " + sColumn + " like '" + sPrefix + "%' ";
		Statement st = conn.createStatement();
		rsTemp = st.executeQuery(sSql);
		int iMaxNo = 0;
		if (rsTemp.next()) {
			String sCount = rsTemp.getString(1);
			if (sCount != null)
				iMaxNo = Integer.valueOf(sCount).intValue();
		}
		st.close();
		rsTemp.close();
	
		String sNewSerialNo = sPrefix + "-" + dfTemp.format(iMaxNo + 1);
		ARE.getLog().info("newSerialNo[" + sTable + "][" + sColumn + "]=[" + sNewSerialNo + "]");
		return sNewSerialNo;
	}
	
	//生成流水号
	public static String getSerialNo(String sTable, String sColumn,String sPrefix, Connection conn) throws Exception
	{
		return getSerialNo(sTable, sColumn,"000",sPrefix, conn);
	}

	public static String getRandomString(int length) { // length表示生成字符串的长度
		String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}
	//A00001~Z99999 随机生成，字母不使用I，O
	public static String getRandomString() { // length表示生成字符串的长度
		
		String base = "ABCDEFGHJKLMNPQRSTUVWXYZ";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 1; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		String base1 = "0123456789";
		Random random1 = new Random();
		for (int i = 0; i < 5; i++) {
			int number = random1.nextInt(base1.length());
			sb.append(base1.charAt(number));
		}
		return sb.toString();
	}
	
	
	/**
	 * 初始化个人邀请码
	 * */
	public static String getInviteCode(){

		String code = getRandomString();
		JBOFactory jbo = JBOFactory.getFactory();
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery q = m.createQuery("select invitecode from o ");
			List <BizObject> list = q.getResultList(false);
			for(int i =0;i<list.size();i++){
				BizObject o = list.get(i);
				String invitecode = o.getAttribute("invitecode").getString();
				if(code.equalsIgnoreCase(invitecode)){
					getInviteCode();
					break;
				}
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return code;
	}
	
	public static void main(String[] args) {
		getInviteCode();
	}
}