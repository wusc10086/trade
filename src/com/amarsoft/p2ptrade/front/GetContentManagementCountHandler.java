package com.amarsoft.p2ptrade.front;

import java.util.Properties;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 推荐的项目列表
 * **/
public class GetContentManagementCountHandler extends JSONHandler{
    static{
        Parser.registerFunction("count");
    }
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return proList(request);
	}
	
	public JSONArray proList(JSONObject request) throws HandlerException{
		if(request.containsKey("terms")==false){
			throw new HandlerException("request.error");
		}
		JSONArray arry = new JSONArray();
		String[] terms = request.get("terms").toString().split("\\,");
		for(int i=0;i<terms.length;i++){
			arry.add(getProNumByLoanterm(JBOFactory.getFactory(),terms[i]));
		}
		return arry;
	}
	//获取相应贷款其次的项目数量
	private int getProNumByLoanterm(JBOFactory jbo,String loanterm){
	    int proNum=0;
	    try {
	        //BizObjectQuery q = jbo.getManager("jbo.trade.project_info_listview").createQuery("select count(*) AS V.pronum from o ,jbo.trade.business_contract bc where o.contractid=bc.SERIALNO and loanterm=:loanterm and status in ('1','104','105','106')").setParameter("loanterm", loanterm);
	    	BizObjectQuery q = jbo.getManager("jbo.trade.project_info_listview").createQuery("select count(*) AS V.pronum from o  where o.remainamount>0 and loanterm=:loanterm and status in ('1','104','105','106')").setParameter("loanterm", loanterm);
	        
	    	BizObject r = q.getSingleResult(false);
	        proNum = r.getAttribute("pronum").getInt();
        } catch (JBOException e) {
            e.printStackTrace();
        }
	    return proNum;
	}
}
