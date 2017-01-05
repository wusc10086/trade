package com.amarsoft.p2ptrade.account;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;

public class PlatformErrorHandler {
	/**
	 * 获取密码错误次数
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	public static JSONObject getTransPassErrorCount(String  sUserID,String sValidType)throws HandlerException {
		//参数校验
		if(sUserID==null || "".equals(sUserID)){
			throw new HandlerException("common.emptyuserid");
		}
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.account_validlog");
			String sCondition = "";
			if("T".equals(sValidType))
			{
				sCondition = " and validdate=:validdate ";
			}
			BizObjectQuery query = m.createQuery("userid=:userid " + sCondition + " and validtype='" + sValidType + "' order by validdate desc");//
			query.setParameter("userid", sUserID);
			if("T".equals(sValidType))
			{
				query.setParameter("validdate", StringFunction.getToday());
			}
			
			int continueCount = 0;//连续错误的次数
			int latelyCount = 0;//最近错误的次数
			
			List<BizObject> list = query.getResultList(false);
			if(list != null && list.size() != 0){
				continueCount++;
				for(int i = 0 ; i < list.size(); i++){
					if(i != 0){
							continueCount++;
					}
				}
			}
			JSONObject result = new JSONObject();
			result.put("ContinueCount", String.valueOf(continueCount));
			result.put("LatelyCount", String.valueOf(latelyCount));
			return result;
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("gettranspasserrorcount.error");
		}
	}
	
	/**
	 * 获取最后密码错误时间
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	public static String getLastPassErrorTime(String  sUserID,String sValidType) throws HandlerException {
		//参数校验
		if(sUserID==null || "".equals(sUserID)){
			throw new HandlerException("common.emptyuserid");
		}
		String sLastTime = "";
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.account_validlog");

			BizObjectQuery query = m.createQuery("userid=:userid and validtype='" + sValidType + "' order by validdate desc,validtime desc");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				String validdate = o.getAttribute("validdate").toString();
				String validtime = o.getAttribute("validtime").toString();
				sLastTime = validdate + " " + validtime;
			}
			return sLastTime;
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("gettranspasserrorcount.error");
		}
	}
	
	//是否在3个小时以内
	public static boolean isInThreeHours(String sStartTime,String sEndTime,String sType) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date d1 = sdf.parse(sEndTime);
		Date d2 = sdf.parse(sStartTime);
		long diff = d1.getTime() - d2.getTime();
		long leng = 0;
		if(sType.equals("00")){
			leng = 1000 * 60 * 180;
		}else{
			leng = 1000 * 60 * 175;
		}
		if(diff <= leng){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 
	 * @param jbo
	 * @param sUserID     用户编号
	 * @param sValidType  验证类型（U-用户密码/T-交易密码/MT-修改交易密码/MU-修改用户密码）
	 */
	public static void insertValidlog(JBOFactory jbo, String sUserID,String sValidType){
		try {
			BizObjectManager manager = jbo.getManager("jbo.trade.account_validlog");
			BizObject o = manager.newObject();
			o.setAttributeValue("USERID", sUserID);
			o.setAttributeValue("VALIDTYPE", sValidType);
			o.setAttributeValue("VALIDDATE", StringFunction.getToday());
			o.setAttributeValue("VALIDTIME", StringFunction.getNow());
			manager.saveObject(o);
		} catch (JBOException e) {
			e.printStackTrace();
		}
	}
	
	public static void insertFailedlog(JBOFactory jbo, String sUserID,String sValidType,String errorMsg,String iCount){
		try {
			BizObjectManager manager = jbo.getManager("jbo.trade.account_failedlist");
			BizObject o = manager.newObject();
			o.setAttributeValue("USERID", sUserID);
			o.setAttributeValue("VALIDTYPE", sValidType);
			o.setAttributeValue("VALIDDATE", StringFunction.getToday());
			o.setAttributeValue("VALIDTIME", StringFunction.getNow());
			o.setAttributeValue("ERRORMESSAGE", errorMsg);
			o.setAttributeValue("RECOUNT", iCount);			
			manager.saveObject(o);
		} catch (JBOException e) {
			e.printStackTrace();
		}
	}	
	
	public static void clearTransPassErrorCount(JBOFactory jbo, String sUserID,String sValidType)throws HandlerException {
		try{
			BizObjectManager m =jbo.getManager("jbo.trade.account_validlog");
			
			BizObjectQuery query = m.createQuery("delete from o where userid=:userid and validdate=:validdate and validtype='" + sValidType + "' ");
			query.setParameter("userid", sUserID);
			query.setParameter("validdate", StringFunction.getToday());
			query.executeUpdate();
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("cleartranspasserrorcount.error");
		}
	}	
	/**
	 * 锁定账户或者资金账户
	 * **/
	public static void lockUser(JBOFactory jbo, String sUserID,String sValidType){
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery q = m.createQuery("userid=:userid");
			q.setParameter("userid", sUserID);
			BizObject o = q.getSingleResult(true);
			if(o!=null){
				if("U".equals(sValidType))
					o.setAttributeValue("lockflag", "1");
				if("T".equals(sValidType))
					o.setAttributeValue("FrozenLockFalg", "1");
				o.setAttributeValue("updatetime", StringFunction.getTodayNow());
				m.saveObject(o);
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
	}	

	/**
	 * 解锁账户或者资金账户
	 * **/
	public static void unlockUser(JBOFactory jbo, String sUserID,String sValidType){
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery q = m.createQuery("userid=:userid");
			q.setParameter("userid", sUserID);
			BizObject o = q.getSingleResult(true);
			if(o!=null){
				if("U".equals(sValidType))
					o.setAttributeValue("lockflag", "2");
				if("T".equals(sValidType))
					o.setAttributeValue("FrozenLockFalg", "2");
				o.setAttributeValue("updatetime", StringFunction.getTodayNow());
				m.saveObject(o);
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
	}
}
