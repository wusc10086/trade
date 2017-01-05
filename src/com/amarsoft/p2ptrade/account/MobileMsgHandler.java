package com.amarsoft.p2ptrade.account;
/*
 * 免费获取验证码
 * */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tools.TimeTool;

public class MobileMsgHandler extends JSONHandler {
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		Parser.registerFunction("substr");
		return getRiskRevResult(request);
	}

	/**
	 * 
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getRiskRevResult(JSONObject request) throws HandlerException {

		JSONObject result = new JSONObject();
		String telphone = (String)request.get("mobile");
		String valid = (String)request.get("valid");
		if(telphone==null)
			throw new HandlerException("error.nomobile");
		String tDate = StringFunction.getToday();
		
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		
		Iterator it = request.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!(key.equals("valid") || key.equals("mobile"))) {
				if(key.contains("Amount") || key.contains("Balance")){
					parameters.put(key, GeneralTools.numberFormat(Double.parseDouble(request.get(key) == null?"0.0":request.get(key).toString())));
				}else{
					parameters.put(key, request.get(key) == null?"":request.get(key).toString());
				}
			}
		}
		try {
			if("P2P_SJBGYZ".equals(valid) || "P2P_REG".equals(valid)){//注册时候的发送验证码

				JBOFactory jbo = JBOFactory.getFactory();
				BizObjectManager m = jbo.getManager("jbo.trade.user_account");
				BizObjectQuery query = m.createQuery("select UserID from o where phonetel=:phonetel");
				query.setParameter("phonetel", telphone);
				BizObject o = query.getSingleResult(false);
				if(o!=null){//手机号码已经存在
					result.put("result", "USEREXISTS");

				}else{
					BizObjectManager m0 = jbo.getManager("jbo.trade.phone_msg");
					BizObjectQuery query0 = m0.createQuery("select chkmsg from o where substr(inputtime,1,10)=:inputtime and telphone=:telphone and (tradecode='P2P_REG' or tradecode='P2P_SJBGYZ')");
					query0.setParameter("telphone", telphone).setParameter("inputtime", tDate);
					List<BizObject> list = query0.getResultList(false);
					if(list.size()>2){//当天验证次数已达3次
						result.put("result","OVERTOP");

					}else{//执行发送短信
						
						BizObjectQuery q = m0.createQuery("select chkmsg,inputtime from o where substr(inputtime,1,10)=:inputtime and tradecode=:tradecode and telphone=:tradecode and status='0'");
						q.setParameter("tradecode",valid).setParameter("telphone", telphone).setParameter("inputtime", tDate);
						BizObject oo = q.getSingleResult(true);
						if(oo!=null){
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
							
							TimeTool tool = new TimeTool();
							String sCurrentDateAndTime = tool.getsCurrentMoment();
							String sSendDateAndTime = oo.getAttribute("inputtime").getString();
							
							Date d1 = sdf.parse(sCurrentDateAndTime);
							Date d2 = sdf.parse(sSendDateAndTime);
							long diff = d1.getTime() - d2.getTime();
							if(diff<300*100){//大于5分钟
								parameters.put("AuthCode", oo.getAttribute("chkmsg").getString());

								GeneralTools t = new GeneralTools();
								t.sendSMS(valid, telphone, parameters);
							}
						}else{
							String inputtime = StringFunction.getTodayNow();
							String chkmsg = getRandomString(6);
							
							parameters.put("AuthCode", chkmsg);

							GeneralTools t = new GeneralTools();
							t.sendSMS(valid, telphone, parameters);
								
							BizObject newobject = m0.newObject();
							newobject.setAttributeValue("tradecode",valid);
							newobject.setAttributeValue("telphone",telphone);
							newobject.setAttributeValue("content", "");
							newobject.setAttributeValue("chkmsg", chkmsg);
							newobject.setAttributeValue("status", "0");
							newobject.setAttributeValue("inputtime",inputtime);
							m0.saveObject(newobject);
						}
						
						result.put("result", "OK");						
					}
				}
			}else{//非注册时候的验证码

				JBOFactory jbo = JBOFactory.getFactory();
				BizObjectManager m = jbo.getManager("jbo.trade.user_account");
				BizObjectQuery query = m.createQuery("select UserID from o where phonetel=:phonetel");
				query.setParameter("phonetel", telphone);
				BizObject o = query.getSingleResult(false);
				if(o!=null){//手机号码已经存在
					
					String inputtime = StringFunction.getTodayNow();
					String chkmsg = getRandomString(6);
										
					parameters.put("AuthCode", chkmsg);
					GeneralTools tool = new GeneralTools();
					tool.sendSMS(valid, telphone, parameters);
					BizObjectManager m0 = jbo.getManager("jbo.trade.phone_msg");
					BizObject newobject = m0.newObject();
					newobject.setAttributeValue("tradecode",valid);
					newobject.setAttributeValue("telphone",telphone);
					newobject.setAttributeValue("content", "");
					newobject.setAttributeValue("chkmsg", chkmsg);
					newobject.setAttributeValue("status", "0");
					newobject.setAttributeValue("inputtime",inputtime);
					m0.saveObject(newobject);

					result.put("result", "OK");
				}else{
					result.put("result", "NO");
				}
			}
			ARE.getLog().info(result);
			return result;
		}catch (Exception e) {
			e.printStackTrace();
			ARE.getLog().error(e.toString());
			throw new HandlerException("riskreviews.error");
		}
	}
	
	public  String getRandomString(int length) { // length表示生成字符串的长度
		String base = "0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}
}
