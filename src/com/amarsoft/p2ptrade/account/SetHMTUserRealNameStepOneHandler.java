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
 * 港澳台用户实名认证第一步，录入基本信息 
 * 输入参数：
 * 		UserID		用户ID
 * 		RealName    真实姓名
 * 		Sex         性别
 * 		BornDate    出生日期

 * 输出参数：成功标志
 *
 */
public class SetHMTUserRealNameStepOneHandler extends JSONHandler {

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
		if (request.get("RealName") == null || "".equals(request.get("RealName")))
			throw new HandlerException("realname.error");
		if (request.get("Sex") == null || "".equals(request.get("Sex")))
			throw new HandlerException("sex.error");
		if (request.get("BornDate") == null || "".equals(request.get("BornDate")))
			throw new HandlerException("birthday.error");

		String sUserID = request.get("UserID").toString();//用户编号
		String sRealName = request.get("RealName").toString();//真实姓名
		String sSex = request.get("Sex").toString();//性别
		String sBirthDay = request.get("BornDate").toString();//出生日期
		
		JBOFactory jbo = JBOFactory.getFactory();
		try {
			TimeTool tool = new TimeTool();
			String sCurrentTime = tool.getsCurrentMoment();
			
			// 账户详细信息处理对象
			BizObjectManager detailManager = jbo.getManager(
					"jbo.trade.account_detail");
			BizObject detailBo = detailManager.createQuery("userid=:userid")
					.setParameter("userid", sUserID).getSingleResult(true);
			//判断详细信息是否有此记录
			if(detailBo == null){
				detailBo = detailManager.newObject();
				detailBo.setAttributeValue("INPUTTIME", sCurrentTime);
				detailBo.setAttributeValue("USERID", sUserID);
			}else{
				detailBo.setAttributeValue("UPDATETIME", sCurrentTime);
			}
			detailBo.setAttributeValue("REALNAME", sRealName);
			detailBo.setAttributeValue("SEXUAL", sSex);
			detailBo.setAttributeValue("BORNDATE", sBirthDay);
			detailManager.saveObject(detailBo);
			return null;
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("setuserrealname.error");
		}
	}

}
