package com.amarsoft.p2ptrade.forgetpassword;
/**
 * 获取绑卡信息
 * 输入参数：
 * 		Mobile 手机号
 * 输出参数：
 * 		
 */
import java.util.Properties;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.CodeManager;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class FetchBindBankInfoHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		try{
			JSONObject result = new JSONObject();
			if(request.containsKey("Mobile")==false)throw new HandlerException("缺少手机号");
			String sMobile = request.get("Mobile").toString();
			//检查手机号是否存在
			BizObjectManager manager = JBOFactory.getBizObjectManager("jbo.trade.user_account");
			BizObjectQuery query = manager.createQuery("select o.userid from o where o.phonetel=:mobile").setParameter("mobile", sMobile);
			BizObject obj = query.getSingleResult(false);
			if(obj==null){
				//没有找到手机号
				result.put("foundmobile", "0");
			}
			else{
				result.put("foundmobile", "1");
				String sUserId = obj.getAttribute("userid").getString();
				BizObjectManager manager2 = JBOFactory.getBizObjectManager("jbo.trade.account_info");
				query = manager2.createQuery("select o.status from o where o.userid=:userid and status='2'").setParameter("userid", sUserId);
				obj = query.getSingleResult(false);
				if(obj==null){//没有绑定卡
					result.put("foundbank", "0");
				}
				else{
					result.put("foundbank", "1");
				}
			}
			
			return result;
		}
		catch(JBOException je){
			je.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}

}
