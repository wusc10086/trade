package com.amarsoft.p2ptrade.invest;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.account.MobileMsgHandler;
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
/**
 * 投资短信验证详情
 * 输入：
 * 	serialno
 *  userid
 *  submitToken
 * 输出：
 * @author mgao1
 */
public class InvestPhoneHandler extends JSONHandler{
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
	
		String sSerialNo = request.get("serialno").toString();
		String userid = request.get("userid").toString();
		String investAmount = request.get("investamount").toString();
		
		double amout = 0;
		try{
			amout = Double.parseDouble(investAmount);
		}catch(Exception e){
			throw new HandlerException("invest.amount.error");
		}

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info_listview");
			BizObjectQuery query = m.createQuery("select o.projectname,o.loanamount,o.loanrate,o.loanterm,getitemname('PACBReturnMethod',o.paymentmethod) as v.paymentmethod ,bc.customerid from O,jbo.trade.business_contract bc where o.contractid=bc.SERIALNO and o.serialno=:serialno ");//and status='1'");
			query.setParameter("serialno",sSerialNo);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				double loanamout = o.getAttribute("loanamount")==null?0:o.getAttribute("loanamount").getDouble();
				BizObjectManager m1 = jbo.getManager("jbo.trade.user_contract");
				BizObjectQuery query1 = m1.createQuery("SELECT SUM(O.INVESTSUM) AS V.SUM FROM O WHERE PROJECTID = :PROJECTID and status <>'2'");
				query1.setParameter("PROJECTID",sSerialNo);
				BizObject object = query1.getSingleResult(false);
				double sum =  object.getAttribute("SUM")==null?0:object.getAttribute("SUM").getDouble();
				if(sum>=loanamout)
					throw new HandlerException("project.noamout");

				request.put("UserID", userid);
				QueryUserAccountInfoHandler qd = new QueryUserAccountInfoHandler();
				JSONObject userInfo = (JSONObject)qd.createResponse(request, null);
				String sPhoneTel = userInfo.get("PhoneTel").toString();
				request.put("mobile", sPhoneTel);
				request.put("valid", "P2P_TZQRYZ");
				request.put("ProjectName", o.getAttribute("projectname"));
				result.put("ProjectName", o.getAttribute("projectname"));
				request.put("Balance", amout);
				MobileMsgHandler msg = new MobileMsgHandler();
				result = (JSONObject)msg.createResponse(request, null);
				result.put("phone", sPhoneTel);
			}else{
				throw new HandlerException("default.database.error");
			}			
			return result;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}		
	}
}
