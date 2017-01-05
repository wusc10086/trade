package com.amarsoft.p2ptrade.account;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.util.UrlBase64;

/**
 * 绑定手机
 * 输入参数：
 * 		UserID		用户ID
 * 		mail  ：邮箱
 * 		EMAILAUTHFLAG：发送邮件，‘1’为已发送，待认证；认证成功EMAILAUTHFLAG为‘2’
 * 
 * 输出参数：
 * 		SuccessFlag:成功标识	S/F
 * 		FailCode:	失败原因
 * 		FailDesc:	失败原因说明		
 * @author yxpan 2014/9/24
 *
 */                     
public class  SuccessMailHandler extends JSONHandler {


	@SuppressWarnings("unchecked")
	@Override
	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//String userID = (String) request.get("UserID");
		String urlSerialNo = (String) request.get("serialno");
		String urlChkmsg = (String) request.get("chkmsg");
		
		String SerialNo ="";
		String Chkmsg="";

		
		 //判断流水号是否为空
		if(urlSerialNo == null || urlSerialNo.length() == 0){
				throw new HandlerException("mailserialno.isempty");
		}
		 if(urlSerialNo.split("__chkmsg=").length>1){
			 String str [] = urlSerialNo.split("__chkmsg=");
			 SerialNo = str[0];
			 Chkmsg = str[1];
		 }else{
			 throw new HandlerException("mailserialno.isempty");
		 }
			try {
				SerialNo = UrlBase64.decode(SerialNo);
				Chkmsg = UrlBase64.decode(Chkmsg);
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		//数据库中查询的流水号
		JBOFactory jbo1 = JBOFactory.getFactory();
    	String userID=getUserId(jbo1,SerialNo);
       //判断用户ID是否为空
		if(userID == null || userID.length() == 0){
			throw new HandlerException("modifyuseraccount.emptyuserid");
		}

		 
	 //判断验证信息是否为空
//		if(Chkmsg == null || Chkmsg.length() == 0){
//					throw new HandlerException("mailchkmsg.isempty");
//				}
				JBOFactory jbo = JBOFactory.getFactory();
				//数据库中查询的流水号
		    	String serialNo1=getSerialNo(jbo,userID,SerialNo);
				//数据库中查询邮箱验证信息
		    	String chkmsg1=getChkmsg(jbo,userID,SerialNo);

		     JSONObject result = new JSONObject();
		     if(SerialNo.equals(serialNo1)&&Chkmsg.equals(chkmsg1)){
			try{
				BizObjectManager userAcctManager =jbo.getManager("jbo.trade.user_account");
				BizObjectQuery query = userAcctManager.createQuery("select * from o where UserID=:UserID");
				query.setParameter("UserID", userID);
				BizObject userAccount = query.getSingleResult(true);
				if(userAccount != null){
					//默认将邮件标志设为2
					userAccount.setAttributeValue("emailauthflag","2");
				    userAcctManager.saveObject(userAccount);
				    //往mail_msg表中插入数据,邮箱认证，emailauthflag设为2
					JBOTransaction tx = jbo.createTransaction();;
					BizObjectManager m0 = jbo.getManager("jbo.trade.email_msg",tx);
					BizObject detailBo = m0.createQuery("o.userid=:userid and o.serialno=:SerialNo")
							.setParameter("userid", userID).setParameter("serialno", SerialNo).getSingleResult(true);
					if(null !=  detailBo){
						detailBo.setAttributeValue("emailauthflag", "2");
						m0.saveObject(detailBo);
					}else{
						throw new HandlerException("queryuseraccount.nodata.error");
					}
					tx.commit();
				    
				}else{
					throw new HandlerException("modifyuseraccount.usernotexist");
				}
			}catch(JBOException e){
				e.printStackTrace();
				throw new HandlerException("modifyuseraccount.error");
			}catch(HandlerException e){
				e.printStackTrace();
				throw e;
			}catch(Exception e){
				e.printStackTrace();
				throw new HandlerException("modifyuseraccount.error");
			}
		     }else{
					throw new HandlerException("mailserialno.isempty");
		     }

		 result.put("flag","success");
		 return result;
	}

	
	//判断地址栏的信息中包含的流水和验证信息是否和邮件内容里面的地址一致
	@SuppressWarnings("unused")
	private String getSerialNo(JBOFactory jbo, String userID,String SerialNo)
			throws HandlerException {
			BizObjectManager m;
			try {
				m = jbo.getManager("jbo.trade.email_msg");
				BizObjectQuery query = m.createQuery("select serialno from o where o.userid=:userid and o.serialno=:SerialNo");
				query.setParameter("userid", userID);
				query.setParameter("serialno",SerialNo);
				BizObject o = query.getSingleResult(false);
				if (o != null) {
					return o.getAttribute("serialno").getString();
				} else {
					return "";
				}
			} catch (Exception e) {
				throw new HandlerException("mailserialno.iserror");
			}
		}
	//获取邮箱验证信息
	private String getChkmsg(JBOFactory jbo, String userID,String SerialNo)
			throws HandlerException {
			BizObjectManager m;
			try {
				m = jbo.getManager("jbo.trade.email_msg");
				BizObjectQuery query = m.createQuery("select chkmsg from o where o.userid=:userid and o.serialno=:SerialNo");
				query.setParameter("userid", userID);
				query.setParameter("serialno",SerialNo);
				BizObject o = query.getSingleResult(false);
				if (o != null) {
					return o.getAttribute("chkmsg").getString();
				} else {
					return "";
				}
			} catch (Exception e) {
				throw new HandlerException("mailchkmsg.iserror");
			}
		}
	
	//获取用户ID
		private String getUserId(JBOFactory jbo,String SerialNo)
				throws HandlerException {
				BizObjectManager m;
				try {
					m = jbo.getManager("jbo.trade.email_msg");
					BizObjectQuery query = m.createQuery("select userid from o where o.serialno=:SerialNo");
					query.setParameter("serialno",SerialNo);
					BizObject o = query.getSingleResult(false);
					if (o != null) {
						return o.getAttribute("userid").getString();
					} else {
						return "";
					}
				} catch (Exception e) {
					throw new HandlerException("modifyuseraccount.emptyuserid");
				}
			}
		
	
}
