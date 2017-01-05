package com.amarsoft.p2ptrade.personcenter;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
/**
 * 用户每日提现次数校验
 * 输入参数：
 * 		UserID:用户编号
 * 输出参数：
 * 		校验结果
 *
 */
public class CheckBankWithDrawMaxTimeLimitHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return checkBankWithDrawMaxTimeLimit(request);
		
	}
	  
	/**
	 * 用户每日提现次数校验
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject checkBankWithDrawMaxTimeLimit(JSONObject request)throws HandlerException {
		//参数校验
		if (request.get("UserID") == null || "".equals(request.get("UserID")))
			throw new HandlerException("common.emptyuserid");
		
		String sUserID = request.get("UserID").toString();//用户编号
		
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			double withDrawCount = getWithDrawCountByDate(jbo, sUserID, StringFunction.getToday());
			if (withDrawCount >= ARE.getProperty("WithDrawCountByDate", 3)) {// 当日提现次数已达到五次
				throw new HandlerException("withdraw.limitcount.error");
			}
			return null;
		}catch(HandlerException e){
			throw e;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("getwithdrawcount.error");
		}
	}
	
	
	/**
	 * 获取当日已进行提现交易的次数
	 * 
	 * @param jbo
	 *            JBOFactory
	 * @param sUserID
	 *            用户编号
	 * @param transdate
	 *            当前日期
	 * @return 已进行提现交易的次数
	 * @throws HandlerException
	 */
	private double getWithDrawCountByDate(JBOFactory jbo,
			String sUserID, String transdate) throws HandlerException {
		double count = 0;
		try {
			BizObjectManager manager;
			manager = jbo.getManager("jbo.trade.transaction_record");
			BizObjectQuery query = manager
					.createQuery("select count(*) as v.count from o where userid=:userid and inputtime like :inputtime and transtype =:transtype and status<>'04'");
			query.setParameter("userid", sUserID)
					.setParameter("inputtime", transdate+"%")
					.setParameter("transtype", "1020");
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				count = Double.parseDouble(o.getAttribute("count").toString());
			} else {
				count = 0;
			}
			return count;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("getwithdrawcount.error");
		}
	}
}
