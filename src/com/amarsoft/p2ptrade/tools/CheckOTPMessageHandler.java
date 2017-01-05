package com.amarsoft.p2ptrade.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 验证手机动态码
 * 输入参数： 
 * 		SerialNo:流水号
 * 		OTPCode:验证码
 * 输出参数： 
 * 		成功标志
 * 
 */
public class CheckOTPMessageHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkOTPMessage(request);
	}

	/**
	 * 校验短信验证码
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject checkOTPMessage(JSONObject request)
			throws HandlerException {
		String sEffectiveFlag = "true";
		if (request.get("SerialNo") == null || "".equals(request.get("SerialNo"))) {
			throw new HandlerException("otpmessage.serialno.error");
		}
		if (request.get("OTPCode") == null || "".equals(request.get("OTPCode"))) {
			throw new HandlerException("otpcode.error");
		}
		if (request.get("EffectiveFlag") != null && !"".equals(request.get("EffectiveFlag"))) {
			sEffectiveFlag = request.get("OTPCode").toString();
		}
		
		try {
			String sSerialNo = request.get("SerialNo").toString();//流水号
			String sOTPCode = request.get("OTPCode").toString();//验证码
			
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			
			TimeTool tool = new TimeTool();
			String sCurrentDateAndTime = tool.getsCurrentMoment();
			
			JBOFactory jbo = JBOFactory.getFactory();
			
			BizObjectManager m =jbo.getManager("jbo.trade.ti_otp_message");
			
			BizObjectQuery query = m.createQuery("serialno=:serialno and effective=:effective");
			query.setParameter("serialno", sSerialNo).setParameter("effective", "1");
			
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				String sSendDate = o.getAttribute("SENDDATE").toString();//发送日期
				String sSendTime = o.getAttribute("SENDTIME").toString();//发送时间
				String sSendDateAndTime = sSendDate + " "+sSendTime;//合并
				
				Date d1 = sdf.parse(sCurrentDateAndTime);
				Date d2 = sdf.parse(sSendDateAndTime);
				long diff = d1.getTime() - d2.getTime();
				if(diff>GeneralTools.getOTPMesValidTime(jbo)){//大于2分钟
					throw new HandlerException("otpcode.timeout.error");
				}
				String sSaveOTPCode = o.getAttribute("OTPCODE").toString();//保存的验证码
				
				if(!(sSaveOTPCode.equals(sOTPCode))){
					throw new HandlerException("otpcode.check.error");
				}
				if("true".equals(sEffectiveFlag)){
					o.setAttributeValue("EFFECTIVE", "2");
					m.saveObject(o);
				}
				return null; 
			}
			else
				throw new HandlerException("otpmessage.effective.error");
		}catch (HandlerException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("checkotpmessage.error");
		}
	}
}
