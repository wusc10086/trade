package com.amarsoft.p2ptrade.front;

import java.util.Properties;
import com.amarsoft.p2ptrade.util.StringUtils;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 平台公告反击计数
 * 输入参数：
 * 		newsid:				INF_NEWS主见
 * 输出参数：
 * 		updateFlag          S/F
 */
public class AddCountHandler extends JSONHandler {

	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		Object obj_newsid =  request.get("newsid");
		
		if(StringUtils.isEmpty(obj_newsid))
			throw new HandlerException("newsid.empty");
			
		return addchicksum(obj_newsid + "");
	}

	
	private JSONObject addchicksum(String newsid) throws HandlerException{
		JSONObject result = new JSONObject();
		String updateFlag = "S";
		
		JBOTransaction tx= null;
		try {
			tx = JBOFactory.createJBOTransaction();
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager manager = jbo.getManager("jbo.trade.inf_news",tx);
			
			manager.createQuery("update o set clickcount=clickcount+1 where serialno=:serialno")
			.setParameter("serialno", newsid)
			.executeUpdate();
			
			tx.commit();
		} catch (JBOException e) {
			try{
				if(tx!=null)
					tx.rollback();
				e.printStackTrace();
				result.put("updateFlag", "F");
			}
			catch(JBOException e1){
				e1.printStackTrace();
			}			
			throw new HandlerException("singlesignon.error");
			
		}
		
		result.put("updateFlag", "S");
		return result;
	}
}
