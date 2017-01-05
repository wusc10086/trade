package com.amarsoft.p2ptrade.front;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/***
 * 
 * ¸ù¾ÝÍ¼Æ¬±àºÅ »ñÈ¡Í¼Æ¬´æ´¢Â·¾¶ inf_picpath
 * */
public class PicViewHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return getPicView(request);
	}

	private JSONObject getPicView(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();

		String picno = "";
		if(request.containsKey("picno"))
			picno = request.get("picno").toString();
		else{
			throw new HandlerException("request.error");
		}
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_picpath");
			//»ñÈ¡Í¼Æ¬
            BizObjectQuery query = m.createQuery("picno=:picno");
            query.setParameter("picno", picno);
            BizObject o = query.getSingleResult(false);

			if(o!=null){
				String filepath = o.getAttribute("filepath")==null?"":o.getAttribute("filepath").toString();
				String filename = o.getAttribute("filename")==null?"":o.getAttribute("filename").toString();
				result.put("filepath", filepath);
				result.put("filename", filename);
				return result;
 			}else{
				throw new HandlerException("pic.notexist.error");
			}
			
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
}
