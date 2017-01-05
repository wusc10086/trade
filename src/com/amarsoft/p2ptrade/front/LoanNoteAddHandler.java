package com.amarsoft.p2ptrade.front;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 贷款留言发布
 * 输入参数：
 *      UserName:用户姓名
 *      telphone:手机号码
 *      
 * 输出参数： 
 * 	    flag:成功标识
 *      错误代码
 */
public class LoanNoteAddHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return loanJoin(request);
	}

	//添加贷款留言信息
	public JSONObject loanJoin(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		String jobtype = (String)request.get("jobtype");//职业类型
		String monthincoming = (String)request.get("monthincoming");//月收入
		String businesssum = (String)request.get("businesssum");//贷款金额
		String sex = (String)request.get("sex");//性别
		String username = (String)request.get("username");//姓名
		String telphone = (String)request.get("telphone");//手机号码
		if(jobtype==null||jobtype.length()<1)
			throw new HandlerException("loannote.nojobtype");
		if(businesssum==null||businesssum.length()<1)
			throw new HandlerException("loannote.nobusinessum");
		if(sex==null||sex.length()<1)
			throw new HandlerException("loannote.nosex");
		if(username==null||username.length()<1)
			throw new HandlerException("loannote.nousername");
		if(telphone==null||telphone.length()<1)
			throw new HandlerException("loannote.notelphone");
		if(monthincoming==null||monthincoming.length()<1)
			throw new HandlerException("loannote.nomonthincoming");
		try {
			//申请贷款
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager loanManger = jbo.getManager("jbo.trade.loan_note");
			BizObject o = loanManger.newObject();
			o.setAttributeValue("loansum", businesssum);
			o.setAttributeValue("username", username);
			o.setAttributeValue("telphone", telphone);
			o.setAttributeValue("sex", sex);
			o.setAttributeValue("jobtype", jobtype);
			o.setAttributeValue("monthincoming", monthincoming);
			o.setAttributeValue("status", "0");
			o.setAttributeValue("inputtime", StringFunction.getTodayNow());
			loanManger.saveObject(o);
			result.put("flag", "success");
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return result;
	}
}
