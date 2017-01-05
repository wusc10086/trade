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
/**
 * 投资成功详情
 * 输入：
 *  userid
 * 输出：
 * @author mgao1
 */
public class InvestSuccessHandler extends JSONHandler{
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
	
		String userid = request.get("userid").toString();

		try {
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query = m.createQuery("select CONTRACTID,investsum,PROJECTID,LastInvestSum from o where  userid=:userid order by INPUTTIME desc");
			query.setParameter("userid",userid);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				String sCONTRACTID = o.getAttribute("CONTRACTID").toString();
				//String investsum = o.getAttribute("investsum").toString();
				String PROJECTID = o.getAttribute("PROJECTID").toString();
				String LastInvestSum = o.getAttribute("LastInvestSum").toString();

				result.put("PROJECTID", PROJECTID);
				result.put("CONTRACTID",sCONTRACTID );
				result.put("investsum", LastInvestSum);
				BizObjectManager mm = jbo.getManager("jbo.trade.project_info");
				BizObjectQuery q = mm.createQuery("select projectname,LOANTERM,LOANRATE,PAYMENTMETHOD from o where serialno=:SERIALNO");
				q.setParameter("SERIALNO",PROJECTID);
				BizObject oo = q.getSingleResult(false);
				if(oo!=null){
					String projectname = oo.getAttribute("projectname").toString();
					String LOANTERM = oo.getAttribute("LOANTERM").toString();
					String LOANRATE = oo.getAttribute("LOANRATE").toString();
					String PAYMENTMETHOD = oo.getAttribute("PAYMENTMETHOD").toString();
					result.put("LOANTERM", LOANTERM);
					result.put("LOANRATE", LOANRATE);
					result.put("PAYMENTMETHOD", PAYMENTMETHOD);
					result.put("projectname", projectname);
				}
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
