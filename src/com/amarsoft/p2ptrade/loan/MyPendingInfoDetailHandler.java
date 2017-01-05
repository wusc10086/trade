package com.amarsoft.p2ptrade.loan;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 获取帐号详情和工作信息详情
 * @author Mbmo
 *
 */
public class MyPendingInfoDetailHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		
		return getResult(request);
	}

	@SuppressWarnings("unchecked")
	private JSONObject getResult(JSONObject request) {
		JSONObject result=new JSONObject();
		String userId = (String) request.get("userId");
		
		BizObject accountDetailB=getAccountDetailResult(userId);//帐号详情结果集
		BizObject workDetailB=getWorkDetailResult(userId);//工作信息详情结果集
		JSONObject accountDetailJ=new JSONObject();
		JSONObject workDetailJ=new JSONObject();
		try {
			accountDetailJ.put("realName", accountDetailB.getAttribute("realname")==null?"":accountDetailB.getAttribute("realname").toString());//真实姓名
			accountDetailJ.put("certId", accountDetailB.getAttribute("certid")==null?"":accountDetailB.getAttribute("certid").toString());//证件号码
			accountDetailJ.put("sexual", accountDetailB.getAttribute("sexual")==null?"":accountDetailB.getAttribute("sexual").toString());//性别
			accountDetailJ.put("birthday", accountDetailB.getAttribute("borndate")==null?"":accountDetailB.getAttribute("borndate").toString());//出生日期
			accountDetailJ.put("marriage", accountDetailB.getAttribute("marriage")==null?"":accountDetailB.getAttribute("marriage").toString());//婚姻状况
			accountDetailJ.put("afterworld", accountDetailB.getAttribute("afterworld")==null?"":accountDetailB.getAttribute("afterworld").toString());//有无子女
			accountDetailJ.put("education", accountDetailB.getAttribute("education")==null?"":accountDetailB.getAttribute("education").toString());//最高学历
			accountDetailJ.put("familyAdd", accountDetailB.getAttribute("FAMILYADD")==null?"":accountDetailB.getAttribute("FAMILYADD").toString());//居住地址
			accountDetailJ.put("familyTel", accountDetailB.getAttribute("FAMILYTEL")==null?"":accountDetailB.getAttribute("FAMILYTEL").toString());//居住地电话
			
			workDetailJ.put("employeeType", workDetailB.getAttribute("employeetype")==null?"":workDetailB.getAttribute("employeetype").toString());//职业状态
			workDetailJ.put("workCorp", workDetailB.getAttribute("workcorp")==null?"":workDetailB.getAttribute("workcorp").toString());//单位名称
			workDetailJ.put("department", workDetailB.getAttribute("department")==null?"":workDetailB.getAttribute("department").toString());//职位
			workDetailJ.put("salary", workDetailB.getAttribute("salary")==null?"":workDetailB.getAttribute("salary").toString());//月收入
			workDetailJ.put("paymentType", workDetailB.getAttribute("payment_type")==null?"":workDetailB.getAttribute("payment_type").toString());//工作发放形式
			workDetailJ.put("workNature", workDetailB.getAttribute("worknature")==null?"":workDetailB.getAttribute("worknature").toString());//公司类别
			workDetailJ.put("unitkind", workDetailB.getAttribute("unitkind")==null?"":workDetailB.getAttribute("unitkind").toString());//公司行业
			workDetailJ.put("corpScope", workDetailB.getAttribute("corp_scope")==null?"":workDetailB.getAttribute("corp_scope").toString());//公司规模
			workDetailJ.put("startDate", workDetailB.getAttribute("start_date")==null?"":workDetailB.getAttribute("start_date").toString());//在现单位工作年限
			workDetailJ.put("prov", workDetailB.getAttribute("prov")==null?"":workDetailB.getAttribute("prov").toString());//省
			workDetailJ.put("city", workDetailB.getAttribute("city")==null?"":workDetailB.getAttribute("city").toString());//市
			
		} catch (JBOException e) {
			e.printStackTrace();
		}
		result.put("accountDetail", accountDetailJ);
		result.put("workDetail", workDetailJ);
		return result;
	}

	private BizObject getAccountDetailResult(String userId) {
		BizObject r = null;
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			m=f.getManager("jbo.trade.account_detail");
			r=m.createQuery("userId=:userId").setParameter("userId", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return r;
	}

	private BizObject getWorkDetailResult(String userId) {
		BizObject r = null;
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			m=f.getManager("jbo.trade.customer_work");
			r=m.createQuery("customerid=:userId").setParameter("userId", userId).getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return r;
	}
	

}
