package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;


import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 现有银行帐号信息查询交易
 * 输入参数： UserID:账户编号 
 * 输出参数： 
 * 			AccountNo:账户号
 * 			AccountEndNo:账户号尾号
 * 			AccountBelong：开户银行 
 */
public class GetExitingAccountInfoHandler extends JSONHandler {

	static{
		Parser.registerFunction("getitemname");
	}
	public Object createResponse(JSONObject request, Properties arg1)throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		
		String sUserID = request.get("UserID").toString();
		
		JSONObject result = new JSONObject();
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.account_info");
			BizObjectQuery query = m
					.createQuery("select SERIALNO,ACCOUNTNAME,ACCOUNTNO,ACCOUNTBELONG,ISRETURNCARD from O where userid=:userid and status=:status");
			query.setParameter("userid", sUserID).setParameter("status", "2");//.setParameter("isreturncard", "0");
			List<BizObject> list = query.getResultList(false);

			JSONArray array = new JSONArray();
			for (int i = 0; i < list.size(); i++) {
				BizObject o = list.get(i);
				JSONObject obj = new JSONObject();
				String sSerialNo = o.getAttribute("SERIALNO").toString();
				String sAccountName = o.getAttribute("ACCOUNTNAME").toString();
				String sAccountNo = o.getAttribute("ACCOUNTNO").toString();
				String sHideAccountNo = "";
				if(sAccountNo.length()>8){
					 sHideAccountNo =  sAccountNo.substring(0,4)+" **** **** "+sAccountNo.substring(sAccountNo.length()-4,sAccountNo.length());//隐藏卡号中间的部分
				}else{
					 sHideAccountNo =  sAccountNo.substring(0,4)+" **** **** "+sAccountNo.substring(sAccountNo.length()-2,sAccountNo.length());//隐藏卡号中间的部分
				}
				
				obj.put("SerialNo", sSerialNo);
				obj.put("AccountName", sAccountName);
				obj.put("AccountNo", sAccountNo);
				obj.put("AccountEndNo", sHideAccountNo); 
				obj.put("AccountBelong", o.getAttribute("AccountBelong").toString());
				obj.put("isReturnCard", o.getAttribute("ISRETURNCARD")+"");
				array.add(obj);
			}
			result.put("array", array);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("queryaccountinfo.error");
		}
		
	}
}