package com.amarsoft.p2ptrade.account;

import java.util.Properties;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 检查用户交易密码是否设置
 * 输入参数：
 * 		UserID:	用户号
 * 输出参数：
 * 		flag:是否成功	S/F
 *
 */
public class CheckTradePwdHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String sUserID = (String) request.get("UserID");
		
		JSONObject result = new JSONObject();
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObject obj =jbo.getManager("jbo.trade.user_account")
				.createQuery("userid=:userid")
				.setParameter("userid", sUserID)
				.getSingleResult(false);
			if(obj==null){
				result.put("flag", "F");
			}
			else{
				if(obj.getAttribute("transpwd").getValue()==null){
					result.put("flag", "F");
				}
				else if(obj.getAttribute("transpwd").getString().trim().equals("")){
					result.put("flag", "F");
				}
				else{
					result.put("flag", "S");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}	
		
		return result;
	}
}
