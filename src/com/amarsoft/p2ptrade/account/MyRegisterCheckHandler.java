package com.amarsoft.p2ptrade.account;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class MyRegisterCheckHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {  
		Parser.registerFunction("substr");
		return selectInvestDetail(request);
	}
	
	private JSONObject selectInvestDetail(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		
		String operate = (String) request.get("operate");
		String objname = (String) request.get("objname");
		//String objvalue = (String) request.get("objvalue");
		String userid = (String) request.get("userid");
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.myloading");
			if("validateusername".equalsIgnoreCase(operate)){//验证用户名
				BizObjectQuery query = m.createQuery("select userId from o where username=:username");
				query.setParameter("username", objname);
				
				BizObject o = query.getSingleResult(false);
				if(o==null){
					result.put("result", "OK");
				}else{
					result.put("result", "USEREXISTS");
				}
			}else if ("validatemobile".equalsIgnoreCase(operate)){//验证手机号
			    BizObjectQuery query1 = m.createQuery("select userId from o where phonenum=:phonenum");
			    query1.setParameter("phonenum", objname);
			    BizObject o1 = query1.getSingleResult(false);
			    if(o1==null){
			        result.put("result", "OK");
			    }else{
			        result.put("result", "USEREXISTS");
			    }		
			}else if ("validateold_password".equalsIgnoreCase(operate)){//验证密码
				BizObjectQuery query1 = m.createQuery("select userId from o where userpassword=:password and userId=:userid");
				query1.setParameter("password", MessageDigest.getDigestAsLowerHexString("MD5", objname).toUpperCase());
				query1.setParameter("userid", userid);
				BizObject o1 = query1.getSingleResult(false);
				if(o1==null){
					result.put("result", "ERROR");
				}else{
					result.put("result", "OK");
				}
			}else 
				result.put("result", "ERROR");
		}catch (JBOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return result;
	}

}
