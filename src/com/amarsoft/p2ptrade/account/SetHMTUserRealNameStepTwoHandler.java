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

/**
 * 港澳台用户实名认证第二步，录入通行证信息
 * 输入参数：
 * 		UserID		用户ID
 * 		PassNo      通行证号码
 * 		PassType    通行证类型：（港澳台）
 * 		PassExpiryDate    通行证有效期

 * 输出参数：成功标志 
 *
 */
public class SetHMTUserRealNameStepTwoHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return setUserRealName(request);
	}
	
	/**
	 * 实名认证
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject setUserRealName(JSONObject request) throws HandlerException {
		// 参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		if (request.get("PassNo") == null || "".equals(request.get("PassNo")))
			throw new HandlerException("passno.error");
		if (request.get("PassType") == null || "".equals(request.get("PassType")))
			throw new HandlerException("passtype.error");
		if (request.get("PassExpiryDate") == null || "".equals(request.get("PassExpiryDate")))
			throw new HandlerException("passexpirydate.error");

		String sUserID = request.get("UserID").toString();//用户编号
		String sPassNo = request.get("PassNo").toString();//通行证号码
		String sPassType = request.get("PassType").toString();//通行证类型
		String sPassExpiryDate = request.get("PassExpiryDate").toString();//通行证有效期
		
		JBOFactory jbo = JBOFactory.getFactory();
		try {
			TimeTool tool = new TimeTool();
			String sCurrentTime = tool.getsCurrentMoment();
			
			// 用户认证信息处理对象
			BizObjectManager authenManager = jbo.getManager(
					"jbo.trade.user_authentication");
			BizObject authenBo = authenManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			//判断详细信息是否有此记录
			if(authenBo == null){
				authenBo = authenManager.newObject();
				authenBo.setAttributeValue("USERID", sUserID);
				authenBo.setAttributeValue("INPUTTIME", sCurrentTime);
			}else{
				authenBo.setAttributeValue("UPDATETIME", sCurrentTime);
			}
			authenBo.setAttributeValue("PASSNO", sPassNo);
			authenBo.setAttributeValue("PASSTYPE", sPassType);
			authenBo.setAttributeValue("PASSEXPIRYDATE", sPassExpiryDate);
			authenManager.saveObject(authenBo);
			return null;
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("setuserrealname.error");
		}
	}

}
