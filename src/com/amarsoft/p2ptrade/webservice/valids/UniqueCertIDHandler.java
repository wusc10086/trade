package com.amarsoft.p2ptrade.webservice.valids;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 校验身份证唯一性
 * @author flian
 * 输入项：
 * 		UserID	当前用户号
 * 		CertID	身份证
 * 输出项目：
 * 		CheckResult 0-存在 1-不存在 2-已经校验过
 *
 */
public class UniqueCertIDHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		if(request.containsKey("UserID")==false)throw new HandlerException("request.invalid");
		if(request.containsKey("CertID")==false)throw new HandlerException("request.invalid");
		String sCertID = request.get("CertID").toString();
		String sUserID = request.get("UserID").toString();
		JSONObject result = new JSONObject();
		if(sCertID.length()!=15 && sCertID.length()!=18){
			throw new HandlerException("身份证位数错误");
		}
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_authentication");
			BizObjectQuery query = manager.createQuery("select userid from o where o.docid=:certID and status='2'");
			query.setParameter("certID", sCertID);
			BizObject obj = query.getSingleResult(false);
			if(obj==null)
				result.put("CheckResult", "1");
			else{
				if(sUserID.equals(obj.getAttribute("userid").getString())){
					result.put("CheckResult", "2");
				}
				else{
					result.put("CheckResult", "0");
				}
			}
			return result;
		}
		catch(JBOException je){
			throw new HandlerException("default.database.error");
		}
		
	}

}
