package com.amarsoft.p2ptrade.tools;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
 * 发送手机动态验证码 
 * 输入参数：
 * 		TempletID：短信类型 
 * 		UserID:客户编号 
 * 		PhoneTel:手机号码(绑定手机时传入)
 * 		其他参数：根据TempletID在SMSConfig.xml文件中匹配
 * 
 * 输出参数： 
 * 		SerialNo：流水号
 * 		PhoneTel：手机号码
 * 
 */
public class SendOTPMessageHandler extends JSONHandler {
	private String time = "120";
	private String sSerialNo;

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return sendOTPMessage(request);
	}

	/**
	 * 生成并发送验证码
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject sendOTPMessage(JSONObject request)
			throws HandlerException {
		String sPhoneTel = (String)request.get("PhoneTel");
		String sUserID = (String)request.get("UserID");
		if(sUserID == null){
			sUserID = "";
		}
		if(sPhoneTel == null){
			sPhoneTel = "";
		}
		if (sPhoneTel.length() == 0 && sUserID.length() == 0) {
			throw new HandlerException("request.error");
		}
		
		if (request.get("TempletID") == null
				|| "".equals(request.get("TempletID"))) {
			throw new HandlerException("sms.templetid.error");
		}

		String sTempletID = request.get("TempletID").toString();

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		
		Iterator it = request.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!(key.equals("TempletID") || key.equals("UserID") || key.equals("PhoneTel"))) {
				if(key.contains("Amount") || key.contains("Balance")){
					parameters.put(key, GeneralTools.numberFormat(Double.parseDouble(request.get(key) == null?"0.0":request.get(key).toString())));
				}else{
					parameters.put(key, request.get(key).toString());
				}
			}
		}

		JBOFactory jbo = JBOFactory.getFactory();
		if(sPhoneTel == null || "".equals(sPhoneTel)){
			if(sUserID == null || "".equals(sUserID)){
				throw new HandlerException("request.error");
			}
			sPhoneTel = getUserTel(jbo,sUserID);
		}
		
		JSONObject result = new JSONObject();
		if(sPhoneTel != null && !"".equals(sPhoneTel)){
			if(checkSendSMSLimit(jbo, sTempletID, sPhoneTel)){
				result.put("SerialNo", sSerialNo);
				result.put("PhoneTel", sPhoneTel);
				result.put("Time", time);
				return result;
			}
		}else{
			throw new HandlerException("request.error");
		}
		
		try {
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			int x = random.nextInt(899999) + 100000;
			String sOtpCode = String.valueOf(x);// 短信验证码

			parameters.put("AuthCode", sOtpCode);

			boolean sResult = GeneralTools.sendSMS(sTempletID, sPhoneTel,
					parameters);//发送验证码

			if (sResult) {
				TimeTool tool = new TimeTool();
				String sSendDate = tool.getsCurrentDate();
				String sSendTime = tool.getsCurrentTime();

				// OTP验证码发送记录
				BizObjectManager manager = jbo.getManager(
						"jbo.trade.ti_otp_message");
				BizObject bo = manager.newObject();

				bo.setAttributeValue("PHONETEL", sPhoneTel);
				bo.setAttributeValue("USERID", sUserID);
				bo.setAttributeValue("OTPCODE", sOtpCode);
				bo.setAttributeValue("SENDDATE", sSendDate);
				bo.setAttributeValue("SENDTIME", sSendTime);
				bo.setAttributeValue("EFFECTIVE", "1");
				bo.setAttributeValue("TEMPLETID", sTempletID);
				manager.saveObject(bo);
				
				String sSerialNo = bo.getAttribute("SERIALNO").toString();
				
				result.put("SerialNo", sSerialNo);
				result.put("PhoneTel", sPhoneTel);
				result.put("Time", time);
				return result;
			} else {
				throw new HandlerException("sendotpmessage.fail");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("sendotpmessage.error");
		}
	}

	/**
	 * 获取用户手机号码
	 * @param jbo  JBOFactory
	 * @param sUserID  用户编号
	 * @return
	 * @throws HandlerException
	 */
	private String getUserTel(JBOFactory jbo, String sUserID)
			throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery query = m
					.createQuery("select PHONETEL from o where userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("PHONETEL").toString();
			} else {
				throw new HandlerException("quaryphonetel.nodata.error");
			}
		} catch (HandlerException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("quaryphonetel.error");
		}

	}
	
	private boolean checkSendSMSLimit(JBOFactory jbo, String sTempletID, String sPhoneTel) throws HandlerException{
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.ti_otp_message");
			BizObjectQuery query = m
					.createQuery("select serialno,senddate,sendtime from o where phonetel=:phonetel and templetid=:templetid and effective=:effective order by serialno desc");
			query.setParameter("phonetel", sPhoneTel).setParameter("templetid", sTempletID).setParameter("effective", "1");
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				String sSendDate = o.getAttribute("SENDDATE").getString();
				String sSendTime = o.getAttribute("SENDTIME").getString();
				String sSendDateTime = sSendDate+" "+sSendTime;
				String sCurrentTime = StringFunction.getTodayNow();
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

				Date d1 = sdf.parse(sCurrentTime);
				Date d2 = sdf.parse(sSendDateTime);
				long diff = d1.getTime() - d2.getTime();
				if (diff < (115 * 1000)) {// 110秒以内
					time = String.valueOf(120 - (int)diff/1000);
					sSerialNo = o.getAttribute("SERIALNO").getString();
					return true;
				}else{
					return false;
				}
			} else {
				return false;
			}
		}  catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("checksendsmslimit.error");
		}
	}
}
