package com.amarsoft.p2ptrade.loan;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/*
 *	用户家庭信息 认证
 * */
public class FamilyConfirmHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return createJsonObject(request);
	}	
	
	private JSONObject createJsonObject(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		String prov = (String)request.get("prov0");//省份
		String city = (String)request.get("city0");//城市
		String sex = (String)request.get("sex");//性别
		String borndate = (String)request.get("borndate");//出生日期
		String familyadd = (String)request.get("familyadd");//职业类型
		String familytel = (String)request.get("familytel");//贷款金额
		String iddoc1 = (String)request.get("iddoc1");//身份证附件1
		String iddoc2 = (String)request.get("iddoc2");//身份证附件2
		String userid = (String)request.get("userid");//身份证附件2
		try {

			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.account_detail");
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", userid);
			
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				o.setAttributeValue("prov", prov);
				o.setAttributeValue("city", city);
				o.setAttributeValue("SEXUAL", sex);
				o.setAttributeValue("borndate", borndate);
				o.setAttributeValue("familyadd", familyadd);
				o.setAttributeValue("familytel", familytel);
				m.saveObject(o);
				result.put("flag", "success");
			}else{
				result.put("flag", "error");
			}
		}catch(Exception e){
			e.printStackTrace();
		}	
		return result;
	}
}
