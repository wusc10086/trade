package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 用户找回密码交易
 * 输入参数：
 * 		userid:	用户id
 * 		PassWord:	新密码	
 *      PassWord2  确认密码
 * 输出参数：
 * 		SuccessFlag:是否成功	S/F
 *
 */
public class FindPasswordHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String UserName = (String) request.get("UserName");
		String PassWord = (String) request.get("PassWord");
		String PassWord2 = (String) request.get("PassWord2");

		//新密码不能为空
		if(UserName == null || UserName.length() == 0){
			throw new HandlerException("find.emptyusername");
		}
		//新密码不能为空
		if(PassWord == null || PassWord.length() == 0){
			throw new HandlerException("find.emptypassword");
		}
		//新密码不能为空
		if(PassWord2 == null || PassWord2.length() == 0){
			throw new HandlerException("find.emptypassword");
		}

		JSONObject result = new JSONObject();
		
		try{
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
			
			BizObjectQuery query = m.createQuery("select UserID from o where (UserName=:UserName or PHONETEL=:UserName or email=:UserName) ");
			query.setParameter("UserName", UserName);
    
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				o.setAttributeValue("PASSWORD", MessageDigest.getDigestAsUpperHexString("MD5", PassWord));
				m.saveObject(o);			
				result.put("SuccessFlag", "S");
			}else{
				result.put("SuccessFlag", "F");				
			}
			
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("find.error");
		}	
		return result;
	}
}
