package com.amarsoft.p2ptrade.loan;

import java.util.Properties;


import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 留言提交
 * 输入参数：
 *      RealName:姓名
 *      Title:称谓
 *      NoteType:留言类型
 *      NoteText:留言
 *      Email:邮箱
 *      PhoneNo:手机
 *      
 * 输出参数： 
 * 	    STATUS:成功标识
 *      错误代码
 */
public class LoanUserNoteHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		 
		return saveLoanUserNote(request);
		 
	} 
	
	/**
	 * 留言提交
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject saveLoanUserNote(JSONObject request)throws HandlerException {
		if(request.get("RealName")==null || "".equals(request.get("RealName"))){
			throw new HandlerException("realname.error");
		}
		
		if(request.get("Title")==null || "".equals(request.get("Title"))){
			throw new HandlerException("title.error");
		}
		
		if(request.get("NoteType")==null || "".equals(request.get("NoteType"))){
			throw new HandlerException("notetype.error");
		}
		
		if(request.get("NoteText")==null || "".equals(request.get("NoteText"))){
			throw new HandlerException("notetext.error");
		}
		
		if("1".equals(request.get("NoteType").toString())){
		if(request.get("Email")==null || "".equals(request.get("Email"))){
			throw new HandlerException("email.error");
		}
		}
		
		if("2".equals(request.get("NoteType").toString())){
		if(request.get("PhoneNo")==null || "".equals(request.get("PhoneNo"))){
			throw new HandlerException("phoneno.error");
		}
		}
		
		String sRealName= request.get("RealName").toString();
		String sTitle = request.get("Title").toString();
		String sNoteType = request.get("NoteType").toString();
		String sNoteText = request.get("NoteText").toString();
		String sEmail = "";
		String sPhoneNo = "";
		if("1".equals(sNoteType)){
		    sEmail = request.get("Email").toString();
			sPhoneNo = "";
		}else if("2".equals(sNoteType)){
			sEmail = "";
			sPhoneNo = request.get("PhoneNo").toString();
		}
		JSONObject result = new JSONObject();
		JBOFactory jbo = JBOFactory.getFactory();
        JBOTransaction tx = null;
		try{  
			tx = jbo.createTransaction();
            BizObjectManager m = jbo.getManager("jbo.trade.user_note",tx);
            BizObject bo = m.newObject();
            bo.setAttributeValue("SERIALNO", "011");//流水号sRealName
            bo.setAttributeValue("RealName", sRealName);//姓名
            bo.setAttributeValue("Title", sTitle);//称谓
            bo.setAttributeValue("NoteType", sNoteType);//留言类型
            bo.setAttributeValue("NoteText", sNoteText);//留言
            bo.setAttributeValue("Email", sEmail);//邮箱
            bo.setAttributeValue("PhoneNo", sPhoneNo);//手机
            m.saveObject(bo);
            tx.commit();
            result.put("param", "ok");
		    return result;
		}
		catch(Exception e){
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			throw new HandlerException("saveusernote.error");
		}
	}
}