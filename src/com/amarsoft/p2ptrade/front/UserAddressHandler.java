package com.amarsoft.p2ptrade.front;


import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * app添加联系人信息
 * 输入参数：
 * 		UserID:账户编号
 * 		Action:操作类型：add,update,query,delete
 * 		add需要参数：contactname，contactphone
 * 		update需要参数：serialno,contactname，contactphone
 * 		delete需要参数：serialno
 * 
 */
public class UserAddressHandler extends JSONHandler {
	
	private String userID="";
	private int ATTR_LEN = 5;//额外参数个数

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException{
		
		if(!request.containsKey("UserID")){
			throw new HandlerException("nouser.error");
		}
		userID = request.get("UserID").toString();
		String sAction = "add";
		if(request.containsKey("Action"))
			sAction = request.get("Action").toString();
		try{
			if("add".equals(sAction)){
				return add(request);
			}
			else if("update".equals(sAction)){
				return update(request);
			}
			else if("delete".equals(sAction)){
				return delete(request);
			}
			else{
				return query(request);
			}
		}
		catch(HandlerException e){
			throw e;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	private JSONObject add(JSONObject request)throws Exception{
		JSONArray contacts = (JSONArray)request.get("contacts");
		JBOTransaction tx = null;
		try{
			tx = JBOFactory.createJBOTransaction();
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_phone_contacts",tx);
			for(int i=0;i<contacts.size();i++){
				JSONObject contact = (JSONObject)contacts.get(i);
				String contactname = (String)contact.get("contactname");
				String contactphone = (String)contact.get("contactphone");
				BizObject obj = manager.newObject();
				
				obj.setAttributeValue("contactname", contactname);
				obj.setAttributeValue("contactphone", contactphone);
				obj.setAttributeValue("customerid", userID);
				String sTodayNow = StringFunction.getTodayNow();
				obj.setAttributeValue("inputtime",sTodayNow);
				obj.setAttributeValue("updatetime",sTodayNow);
				for(int j=1;j<=ATTR_LEN;j++){
					String sAttrName = "attr" + j;
					if(request.containsKey(sAttrName)){
						obj.setAttributeValue(sAttrName,contact.get(sAttrName));
					}
				}
				manager.saveObject(obj);
			}
			tx.commit();
		}
		catch(Exception e){
			tx.rollback();
			throw e;
		}
		return null;
	}
	
	private JSONObject update(JSONObject request)throws Exception{
		return null;
	}
	
	private JSONObject query(JSONObject request)throws Exception{
		JSONObject result = new JSONObject();
		BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_phone_contacts");
		BizObjectQuery query = manager.createQuery("customerid=:customerid").setParameter("customerid", userID);
		result.put("count", query.getTotalCount());
		JSONArray array = new JSONArray();
		result.put("array", array);
		List<BizObject> list = query.getResultList(false);
		for(BizObject obj : list){
			JSONObject jo = new JSONObject();
			jo.put("serialno", obj.getAttribute("serialno").getString());
			jo.put("contactname", obj.getAttribute("contactname").getString());
			jo.put("contactphone", obj.getAttribute("contactphone").getString());
			jo.put("inputtime", obj.getAttribute("inputtime").getString());
			for(int i=1;i<ATTR_LEN;i++){
				String sAttrName = "attr" + i;
				if(obj.getAttribute(sAttrName).getValue()!=null){
					jo.put(sAttrName, obj.getAttribute(sAttrName).getString());
				}
				array.add(jo);
			}
		}
		return result;
	}
	
	private JSONObject delete(JSONObject request)throws Exception{
		return null;
		
	}
	
}
