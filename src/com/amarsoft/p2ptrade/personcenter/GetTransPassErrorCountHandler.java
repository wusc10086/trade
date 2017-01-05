package com.amarsoft.p2ptrade.personcenter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 获取交易密码错误次数
 * 输入参数：
 * 		UserID:账户编号
 * 输出参数：
 * 		ContinueCount:连续三个小时错误次数
 * 		LatelyCount:最近三个小时错误次数
 *
 */
public class GetTransPassErrorCountHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getTransPassErrorCount(request);
	}
	  
	/**
	 * 获取交易密码错误次数
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getTransPassErrorCount(JSONObject request)throws HandlerException {
		//参数校验
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("common.emptyuserid");
		}
		
		String sUserID = request.get("UserID").toString();//用户编号
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.account_validlog");
			
			BizObjectQuery query = m.createQuery("userid=:userid and validdate=:validdate and validtype='T' order by validtime desc");
			query.setParameter("userid", sUserID);
			query.setParameter("validdate", StringFunction.getToday());
			int continueCount = 0;//连续错误的次数
			int latelyCount = 0;//最近错误的次数
			String sFirstValidTime = null;
			String sCurrentTime = StringFunction.getNow();
			
			List<BizObject> list = query.getResultList(false);
			if(list != null && list.size() != 0){
				sFirstValidTime = list.get(0).getAttribute("validtime").getString();
				if(isInThreeHours(sFirstValidTime, sCurrentTime,"01")){
					latelyCount++;
				}
				continueCount++;
				for(int i = 0 ; i < list.size(); i++){
					if(i != 0){
						BizObject tempBiz = list.get(i);
						String sTempValidDate = tempBiz.getAttribute("validtime").getString();
						if(isInThreeHours(sFirstValidTime, sTempValidDate,"00")){
							continueCount++;
						}
						if(isInThreeHours(sTempValidDate, sCurrentTime,"01")){
							latelyCount++;
						}
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
	
	private boolean isInThreeHours(String sStartTime,String sEndTime,String sType) throws ParseException{
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
}
