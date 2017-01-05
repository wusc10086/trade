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
import com.amarsoft.p2ptrade.account.RegisterHandler;

/**
 * 贷款需求发布
 * 输入参数：
 *      UserID:用户ID
 *      loantype:贷款类型
 *      ocupation:职业
 *      
 * 输出参数： 
 * 	    flag:成功标识
 *      错误代码
 */
public class LoanAddHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return loanJoin(request);
	}

	//添加贷款信息
	public JSONObject loanJoin(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		String prov = (String)request.get("prov");//省份
		String city = (String)request.get("city");//城市
		String fundsource = (String)request.get("fundsource");//贷款用途
		String jobtype = (String)request.get("jobtype");//职业类型
		String businesssum = (String)request.get("businesssum");//贷款金额
		String sex = (String)request.get("sex");//性别
		String username = (String)request.get("username");//姓名
		String userid = (String)request.get("userid");//用户ID
		String phone = (String)request.get("phone");//手机号码
		String applyway = (String)request.get("applyway");//渠道
		String usertype = (String)request.get("usertype");//用户类型
		String monthincome = (String)request.get("monthincome");//月收入
		String recommenderid = (String)request.get("recommenderid");//推荐用户编号
		String isrecommend = (String)request.get("isrecommend");//是否推荐
		String incometype = (String)request.get("incometype");//工资发放形式
		String projectname = (String)request.get("projectname")==null?"":(String)request.get("projectname");//贷款标题
		String fundsourcedesc = (String)request.get("fundsourcedesc")==null?"":(String)request.get("fundsourcedesc");//贷款说明
		try {
			//注册
			if("1".equals(isrecommend)){//推荐的判断用户存在提醒
				
			}else if(userid==null || userid.length()<1){
				JSONObject register = (JSONObject)new RegisterHandler().createResponse(request, null);
				userid = (String)register.get("userid");
			}
			
			
			
			//获取用户编号
			
			//申请贷款
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager loanManger = jbo.getManager("jbo.trade.loan_apply");
			BizObject o = loanManger.newObject();
			o.setAttributeValue("fundsource", fundsource);
			o.setAttributeValue("businesssum", businesssum);
			o.setAttributeValue("prov", prov);
			o.setAttributeValue("city", city);
			o.setAttributeValue("applyway", applyway);
			o.setAttributeValue("userid", userid);
			o.setAttributeValue("username", username);
			o.setAttributeValue("phone", phone);
			o.setAttributeValue("sex", sex);
			o.setAttributeValue("jobtype", jobtype);
			o.setAttributeValue("monthincome", monthincome);
			o.setAttributeValue("recommenderid", recommenderid);
			o.setAttributeValue("isrecommend", isrecommend);
			o.setAttributeValue("usertype", usertype);
			o.setAttributeValue("incometype", incometype);
			o.setAttributeValue("projectname", projectname);
			o.setAttributeValue("fundsourcedesc", fundsourcedesc);
			o.setAttributeValue("applystatus", "010");
			o.setAttributeValue("applytime", StringFunction.getTodayNow());
			loanManger.saveObject(o);
			o.setAttributeValue("loanno", o.getAttribute("applyno"));
			loanManger.saveObject(o);
			result.put("flag", "success");
			result.put("userid", userid);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return result;
	}
}
