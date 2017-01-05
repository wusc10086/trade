package com.amarsoft.p2ptrade.personcenter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.ModifyUserAccountHandler;
import com.amarsoft.p2ptrade.tools.CheckTransPasswordHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 获取交易密码错误次数
 * 输入参数：
 * 		UserID:账户编号
 * 		OldPwd:旧密码
 * 		NewPwd:新密码
 * 输出参数：
 * 		
 *
 */
public class ModifyTransPwdHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		//参数校验
		if(request.get("UserID")==null || "".equals(request.get("UserID").toString())){
			throw new HandlerException("common.emptyuserid");
		}
		/*
		if(request.get("OldPwd")==null || "".equals(request.get("OldPwd").toString())){
			throw new HandlerException("transpwd.error");
		}
		*/
		
		if(request.get("NewPwd")==null || "".equals(request.get("NewPwd").toString())){
			throw new HandlerException("transpwd.error");
		}
		
		String sUserID = request.get("UserID").toString();//用户编号
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account"); 
			
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			BizObject obj = query.getSingleResult(false);
			if(obj==null){
				throw new HandlerException("common.usernotexist");
			}
			if(obj.getAttribute("transpwd").getValue()==null){//account.tranpassword.empty
				throw new HandlerException("account.tranpassword.empty");
			}
			if(request.containsKey("OldPwd")){
				//出错3次的检查
				GetTransPassErrorCountHandler h0 = new GetTransPassErrorCountHandler();
				JSONObject request0 = new JSONObject();
				request0.put("UserID", request.get("UserID"));
				JSONObject r0 = (JSONObject)h0.createResponse(request0, null);
				if(Integer.parseInt(r0.get("ContinueCount").toString())>=3){
					throw new HandlerException("account.tranpassword.counterror");
				}
				
				CheckTransPasswordHandler h = new CheckTransPasswordHandler();
				JSONObject request2 = new JSONObject();
				request2.put("UserID", request.get("UserID"));
				request2.put("TranPassword", request.get("OldPwd").toString());
				h.createResponse(request2, null);
			}
			if(GeneralTools.OrigWord(request.get("NewPwd").toString()).equalsIgnoreCase(obj.getAttribute("transpwd").getString())){//account.tranpassword.empty
				throw new HandlerException("account.tranpassword.eqold");
			}
			ModifyUserAccountHandler h= new ModifyUserAccountHandler();
			JSONObject joRequestParams = new JSONObject();
			joRequestParams.put("UserID",sUserID);
			joRequestParams.put("FieldName","TRANSPWD");
			joRequestParams.put("FieldType","BASIC");
			if(request.containsKey("OldPwd")){
				joRequestParams.put("TransPwdType","01");
			}
			else{
				joRequestParams.put("TransPwdType","02");
			}
			joRequestParams.put("FieldValue",request.get("NewPwd").toString());
			JSONObject result = (JSONObject)h.createResponse(joRequestParams, null);
			if("S".equals(result.get("SuccessFlag").toString())){
				ClearTransPassErrorCountHandler h3 = new ClearTransPassErrorCountHandler();
				JSONObject request3 = new JSONObject();
				request3.put("UserID",sUserID);
				h3.createResponse(request3, null);
			}
			return result;
		}catch(HandlerException e){
			throw e;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("gettranspasserrorcount.error");
		}
	}
	
}
