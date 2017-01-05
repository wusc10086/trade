package com.amarsoft.p2ptrade.front;

import java.util.List;
import java.util.Properties;



import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.util.StringUtils;

/**
 * 投资达人
 * 
 * @author wfle<wfle@amarsoft.com>
 *  * 输入参数：
 * 		shownum: 展示的数量
 * 
 */
public class CurContractHandler extends JSONHandler {

	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		Parser.registerFunction("sum");
		return getCurContractList(request);
	}

	private JSONObject getCurContractList(JSONObject request)throws HandlerException {
		
		try{
			String shownum = StringUtils.isEmpty(request.get("shownum"))?"50" : request.get("shownum") + "";
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			
			BizObjectQuery query = m.createQuery("select t2.USERID,t2.USERNAME, sum(o.INVESTSUM) as v.sum"
					+" from o,jbo.trade.user_account t2"
					+" where o.USERID = t2.USERID and o.status='1' group by t2.USERID,t2.USERNAME order by v.sum desc");
			//query.setMaxResults(Integer.parseInt(shownum));
			
			List<BizObject> list = query.getResultList(false);
			
			JSONObject result = new JSONObject();
			JSONArray array = new JSONArray();
			Object lo_userId,lo_userName,lo_investsum;
			int userNameLen = 0; String hidUserName;
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					
					lo_userId = o.getAttribute("USERID").getValue();
					lo_userName = o.getAttribute("USERNAME").getValue();
					lo_investsum = o.getAttribute("sum").getValue();
					
					hidUserName = lo_userName+"";
					userNameLen = StringUtils.length(hidUserName);
					
					hidUserName = StringUtils.substring(hidUserName, 0, 1) + "***" + StringUtils.substring(hidUserName, userNameLen - 1, userNameLen);					
				
					obj.put("hidUserName", hidUserName);
					obj.put("investsum", lo_investsum);
					
					array.add(obj);
				}
				
				result.put("array", array);
			}
			
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryinvestmentlist.error");
		}
		}
	
}
