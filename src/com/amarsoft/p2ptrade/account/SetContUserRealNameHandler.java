package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.TimeTool;
import com.amarsoft.p2ptrade.webservice.valids.UniqueCertIDHandler;
import com.amarsoft.transcation.RealTimeTradeTranscation;
import com.amarsoft.transcation.TranscationFactory;

/**
 * 大陆用户实名认证
 * 输入参数：
 * 		UserID		用户ID
 * 		RealName    真实姓名
 * 		CertID		身份证号
 * 		ExpiryDate  证件有效期 
 * 输出参数：成功标志
 *
 */
public class SetContUserRealNameHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//检查身份证是否重复
		UniqueCertIDHandler handler = new UniqueCertIDHandler();
		JSONObject checkResult = (JSONObject)handler.createResponse(request, arg1);
		if(checkResult.containsKey("CheckResult")){
			String sCheckResult = checkResult.get("CheckResult").toString();
			if(sCheckResult.equals("1")){
				return setUserRealName(request);
			}
			else{
				return checkResult;
			}
		}
		else{
			throw new HandlerException("输出参数错误");
		}
	}
	
	/**
	 * 实名认证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject setUserRealName(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("userid.error");
		if (request.get("RealName") == null || "".equals(request.get("RealName")))
			throw new HandlerException("realname.error");
		if (request.get("CertID") == null || "".equals(request.get("CertID")))
			throw new HandlerException("certid.error");
//		if (request.get("ExpiryDate") == null || "".equals(request.get("ExpiryDate")))
//			throw new HandlerException("expirydate.error");

		String sUserID = request.get("UserID").toString();
		String sRealName = request.get("RealName").toString();//真实姓名
		String sCertID = request.get("CertID").toString();//证件号
//		String sExpiryDate = request.get("ExpiryDate").toString();//证件有效期
		
		JBOFactory jbo = JBOFactory.getFactory();
		JBOTransaction tx = null;
		try {
			tx = jbo.createTransaction();
			
			TimeTool tool = new TimeTool();
			String sCurrentTime = tool.getsCurrentMoment();
			
			// 账户详细信息处理对象
			BizObjectManager detailManager = jbo.getManager(
					"jbo.trade.account_detail",tx);
			BizObject detailBo = detailManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			//判断详细信息是否有此记录
			if(detailBo == null){
				detailBo = detailManager.newObject();
				detailBo.setAttributeValue("USERID", sUserID);
				detailBo.setAttributeValue("INPUTTIME", sCurrentTime);
			}else{
				detailBo.setAttributeValue("UPDATETIME", sCurrentTime);
			}
			
			int a = Integer.parseInt(sCertID.substring(sCertID.length()-2, sCertID.length()-1));
			String sBornDate = null;
			String sSex = null;
			if(a % 2 == 0){//偶数是女
				sSex = "2";
			}else{//奇数是男
				sSex = "1";
			}
			
			if(sCertID.length() == 18){
				sBornDate = sCertID.substring(6, 10) + "/" +sCertID.substring(10, 12)+"/"+sCertID.substring(12, 14);
			}
			
			detailBo.setAttributeValue("REALNAME", sRealName);
			detailBo.setAttributeValue("CERTTYPE", "Ind01");
			detailBo.setAttributeValue("CERTID", sCertID);
			if(sSex != null){
				detailBo.setAttributeValue("SEXUAL", sSex);
			}
			if(sBornDate != null){
				detailBo.setAttributeValue("BORNDATE", sBornDate);
			}
			detailManager.saveObject(detailBo);
			
			//用户认证信息
			BizObjectManager authenManager = jbo.getManager(
					"jbo.trade.user_authentication",tx);
			BizObject authenBo = authenManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			if(authenBo == null){
				authenBo = authenManager.newObject();
				authenBo.setAttributeValue("USERID", sUserID);
				authenBo.setAttributeValue("INPUTTIME", sCurrentTime);
			}else{
				authenBo.setAttributeValue("UPDATETIME", sCurrentTime);
			}
			authenBo.setAttributeValue("IDTYPE", "Ind01");
			authenBo.setAttributeValue("DOCID", sCertID);
//			authenBo.setAttributeValue("EXPIRYDATE", sExpiryDate);
			authenBo.setAttributeValue("STATUS", "2");
			authenManager.saveObject(authenBo);
			
			//用户帐号信息认证标志
			BizObjectManager accountManager = jbo.getManager(
					"jbo.trade.user_account",tx);
			BizObject accountBo = accountManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			if(null !=  accountBo){
				
				//发送第三方系统实名认证信息
				java.util.HashMap<String,Object> recordMap = new java.util.HashMap<String,Object>();
				recordMap.put("USERNAME", sRealName);
				recordMap.put("CERTID", sCertID);
				recordMap.put("USERID", sUserID);
				//暂时写死 实名认证信息
				RealTimeTradeTranscation rttt = TranscationFactory.getRealTimeTradeTranscation("3010", "1002");
				rttt.init(recordMap);
				rttt.execute();
				//如果不成功，则抛出异常
				if(rttt.getTemplet().isSuccess())
				{
					accountBo.setAttributeValue("USERAUTHFLAG", "2");
					accountManager.saveObject(accountBo);
				}
				else
					throw new HandlerException("setuserrealname.error");
			}else{
				throw new HandlerException("queryuseraccount.nodata.error");
			}
			tx.commit();
			result.put("SuccessFlag", "S");
			return result;
		} 
		catch (HandlerException e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			throw e;
		} 
		catch (Exception e) {
			try {
				if(tx != null){
					tx.rollback();
				}
			} catch (JBOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			throw new HandlerException("setuserrealname.error");
		}
	}

}
