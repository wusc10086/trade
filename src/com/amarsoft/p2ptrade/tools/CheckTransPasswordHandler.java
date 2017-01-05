package com.amarsoft.p2ptrade.tools;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.security.MessageDigest;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.personcenter.GetTransPassErrorCountHandler;

/**
 * 验证交易密码
 * 输入参数： 
 * 		UserID:用户编号
 * 		TranPassword:交易密码
 * 输出参数：成功标志
 * 
 */
public class CheckTransPasswordHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkTransPassword(request);
	}

	/**
	 * 校验短信验证码
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject checkTransPassword(JSONObject request)
			throws HandlerException {

		JSONObject result = new JSONObject();
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}
		if (request.get("TranPassword") == null || "".equals(request.get("TranPassword"))) {
			throw new HandlerException("tranpassword.error");
		}
		try {
			String sUserID = request.get("UserID").toString();//流水号
			String sTranPassword = request.get("TranPassword").toString();//验证码
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m =jbo.getManager("jbo.trade.user_account");
			
			BizObjectQuery query = m.createQuery("userid=:userid");
			query.setParameter("userid", sUserID);
			
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				String sTransPwd = o.getAttribute("TRANSPWD").toString();//交易密码

				if(sTranPassword.equalsIgnoreCase(sTransPwd)){
					//出错3次的检查
					GetTransPassErrorCountHandler h0 = new GetTransPassErrorCountHandler();
					JSONObject request0 = new JSONObject();
					request0.put("UserID", request.get("UserID"));
					JSONObject r0 = (JSONObject)h0.createResponse(request0, null);
					if(Integer.parseInt(r0.get("ContinueCount").toString())>=ARE.getProperty("TransPwdErrorCount", 5)){
						//throw new HandlerException("account.tranpassword.counterror");
						result.put("result", "" + ARE.getProperty("TransPwdErrorCount", 3));
						return result;
					}
					clearTransPassErrorCount(jbo, sUserID);
					result.put("result", "OK");
					return result;
				}else{
					
					
					int iCount = jbo.getManager("jbo.trade.account_validlog")
						.createQuery("VALIDTYPE='T' and USERID=:USERID and VALIDDATE=:VALIDDATE")
						.setParameter("USERID", sUserID)
						.setParameter("VALIDDATE", StringFunction.getToday())
						.getTotalCount();
					if(iCount<ARE.getProperty("TransPwdErrorCount", 5)){
						insertValidlog(jbo, sUserID);
					}
					if(iCount>=ARE.getProperty("TransPwdErrorCount", 5))
						result.put("result", "" + iCount);
					else{
						result.put("result", "" + (iCount+1));
					}
					return result;
					//throw new HandlerException("transpwd.error");
				}
			}
			else{
				throw new HandlerException("gettranspwd.error");
			}
		}catch (HandlerException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("transpwd.error");
		}
	}
	
	private void clearTransPassErrorCount(JBOFactory jbo, String sUserID)throws HandlerException {
		try{
			BizObjectManager m =jbo.getManager("jbo.trade.account_validlog");
			
			BizObjectQuery query = m.createQuery("delete from o where userid=:userid and validdate=:validdate and validtype='T' ");
			query.setParameter("userid", sUserID);
			query.setParameter("validdate", StringFunction.getToday());
			query.executeUpdate();
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("cleartranspasserrorcount.error");
		}
	}
	
	/**
	 * 解析
	 * @param bts
	 * @return
	 */
	private String bytes2Hex(byte[] bts) {
		String des = "";
		String tmp = null;
		for (int i = 0; i < bts.length; i++) {
			tmp = (Integer.toHexString(bts[i] & 0xFF));
			if (tmp.length() == 1) {
				des += "0";
			}
			des += tmp;
		}
		return des;
	}
	
	
	private void insertValidlog(JBOFactory jbo, String sUserID){
		try {
			BizObjectManager manager = jbo.getManager("jbo.trade.account_validlog");
			BizObject o = manager.newObject();
			o.setAttributeValue("USERID", sUserID);
			o.setAttributeValue("VALIDTYPE", "T");
			o.setAttributeValue("VALIDDATE", StringFunction.getToday());
			o.setAttributeValue("VALIDTIME", StringFunction.getNow());
			manager.saveObject(o);
		} catch (JBOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
