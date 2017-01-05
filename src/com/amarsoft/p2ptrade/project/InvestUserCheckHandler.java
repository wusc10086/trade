package com.amarsoft.p2ptrade.project;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
/**
 * 投资前检查
 * 输入	
 * 		UserID	用户ID
 * 		Serialno 项目编号
 * @author
 *
 */
public class InvestUserCheckHandler extends JSONHandler {
	
	public static final boolean IGNORE_MOBILE_CHECK = false;
	
	public static final boolean IGNORE_PASSWORD_CHECK = false;
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		JSONObject result = new JSONObject();
		//获取项目信息
		ProjectdetailHandler pd = new ProjectdetailHandler();
		JSONObject rp  = (JSONObject)pd.createResponse(request, arg1);
		//检查时间是否有效
		if((rp.get("BeginDate").toString() +  " " + rp.get("BeginTime").toString()).compareToIgnoreCase(StringFunction.getTodayNow())>0){
			result.put("CheckFlag", "NOSTART");
			return result;
		}
		//判断项目状态
		if(!"1".equals(rp.get("Status").toString())){
			result.put("CheckFlag", "PROJECTNOEXIST");
			return result;
		}
		//检查是否当前用户
		String sInvestUserId = request.get("UserID").toString();
		String sLoanUserId = "";
		try{
			sLoanUserId = JBOFactory.getBizObjectManager("jbo.trade.business_contract")
					.createQuery("serialno=:contractid")
					.setParameter("contractid", (String)rp.get("Contractid"))
					.getSingleResult(false)
					.getAttribute("customerid").getString();
		}
		catch(Exception e){
			throw new HandlerException("default.database.error");
		}
		if(sLoanUserId.equals(sInvestUserId)){
			result.put("CheckFlag", "SAMEUSER");
			return result;
		}
		//获取会员信息
		QueryUserAccountInfoHandler qd = new QueryUserAccountInfoHandler();
		JSONObject rq = (JSONObject)qd.createResponse(request, arg1);

		//实名认证
		if(!"2".equals(rq.get("UserAuthFlag"))){
			result.put("CheckFlag", "UserAuthFlag");
			return result;
		}
		//安全问题
//		if(!"Y".equals(rq.get("SecurityQuestionFlag"))){
//			result.put("CheckFlag", "SecurityQuestionFlag");
//			return result;
//		}
		//交易密码
		if(!"Y".equals(rq.get("TransPWDFlag"))){
			result.put("CheckFlag", "TransPWDFlag");
			return result;
		}
		//检查资金账户是否异常
		if("1".equals(rq.get("FrozenLockFalg"))){
			result.put("CheckFlag", "ACCOUNTFREEZE");
			return result;
		}

		//判断输入的投资金额
		String TAmt = request.get("TAmt").toString();

		if(TAmt!=null&&TAmt.length()>0) {
			
			//投资金额大于账户余额
			if(Double.parseDouble(TAmt)>Double.parseDouble(rq.get("UsableBalance").toString())){
				result.put("CheckFlag", "TUSAMTNOENOUGH");
				return result;
			}
			//足额投资
			if(Double.parseDouble(TAmt)==Double.parseDouble(rp.get("remainamount").toString())){
				result.put("CheckFlag", "OK");
				return result;
			}
			//投资金额大于可投金额
			if(Double.parseDouble(TAmt)>Double.parseDouble(rp.get("remainamount").toString())){
				result.put("CheckFlag", "OUTTAmt");
				return result;
			}
			//可投金额小于起投金额
			if(Double.parseDouble(rp.get("remainamount").toString())<Double.parseDouble(rp.get("BEGINAMOUNT").toString())){
				//必须投足余额
				if(Double.parseDouble(TAmt)!=Double.parseDouble(rp.get("remainamount").toString())){
					result.put("CheckFlag", "NOREMAIN");
					return result;
				}
			}else{
				//投资金额小于起投金额
				if(Double.parseDouble(TAmt)<Double.parseDouble(rp.get("BEGINAMOUNT").toString())){
					result.put("CheckFlag", "NOTAmt");
					return result;
				}
				
				//投资金额必须满足 起投金额加上递增金额；
				if((Double.parseDouble(TAmt)-Double.parseDouble(rp.get("BEGINAMOUNT").toString()))%Double.parseDouble(rp.get("ADDAMOUNT").toString())==0){
					
				}else{
					result.put("CheckFlag", "AddTAmtError");
					return result;
				}
			}

			
		} else {
			result.put("CheckFlag", "NOINVEST");
			return result;
		}		
		result.put("CheckFlag", "OK");
		return result;
	}
	
	private void createSafeQuestion(int index,JSONObject request,JSONObject result)throws HandlerException{
		try{
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_account");
			String sColName = "SECURITYQUESTION";
			if(index>1){
				sColName = sColName + index;
			}
			BizObject obj = manager.createQuery("select " + sColName + " from o where o.userid=:userid").setParameter("userid", request.get("UserID").toString()).getSingleResult(false);
			if(obj==null)
				throw new HandlerException("check.security.error");
			else{
				result.put("question", obj.getAttribute(sColName).getString());
			}
		}
		catch(JBOException je){
			throw new HandlerException("default.database.error");
		}
		
	}
}
