package com.amarsoft.p2ptrade.loan;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 获取用户的贷款申请信息
 * @author Mbmo
 *
 */
public class GetLoanApplyInfoHandler extends JSONHandler {
	static {
		Parser.registerFunction("getitemname");
	}

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		if(isCurrentUser(request)){
			return getApplyInfo(request);
		}
		return new JSONObject();
	}

	/**
	 * 判断查询编号是否与当前用户关联
	 * @param request
	 * @return
	 */
	private boolean isCurrentUser(JSONObject request){
		boolean flag=false;
		String userId=(null==request.get("userId")?"":request.get("userId").toString());
		String applyNo=(null==request.get("applyNo")?"":request.get("applyNo").toString());
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		
		try {
			String table="jbo.trade.loan_apply";
			String sql = "SELECT USERID FROM O WHERE O.APPLYNO=:applyNo";
			m = f.getManager(table);
			BizObjectQuery q = m.createQuery(sql);
			BizObject userInBaseB = q.setParameter("applyNo", applyNo).getSingleResult(false);
			if(userInBaseB==null){return false;}
			else if(!userId.equals(userInBaseB.getAttribute("userid").toString())){
				return false;
			}else{
				flag=true;
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return flag;
	}
	/**
	 * 获取贷款申请的信息
	 * @param request
	 * @return
	 * @throws HandlerException 
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getApplyInfo(JSONObject request) throws HandlerException {
		String applyNo = (String) request.get("applyNo");
		String quitApply=(String)request.get("quitApply");//是否不贷了
		String quitReason=(String)request.get("quitReason");
		
		JSONObject result = new JSONObject();
		JSONObject applyInfo = new JSONObject();
		boolean quitSuc=quitApply(quitApply,applyNo,quitReason);
		if(quitSuc){
			result.put("flag", "success");
		}
		BizObject applyResult = getApplyResult(applyNo);
		try {
			applyInfo=putApplyInfo(applyResult);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		result.put("applyInfo", applyInfo);
		return result;
	}

	/**
	 * 保存贷款申请信息
	 * @param applyResult
	 * @return
	 * @throws JBOException 
	 */
	@SuppressWarnings("unchecked")
	private JSONObject putApplyInfo(BizObject applyResult) throws JBOException {
		JSONObject result=new JSONObject();
		//贷款金额，期限，地区，金额，说明
		if(null!=applyResult){
		result.put("applyNo",applyResult.getAttribute("applyno")==null?"":applyResult.getAttribute("applyno").toString());
		result.put("businessSum",applyResult.getAttribute("businesssum")==null?"":applyResult.getAttribute("businesssum").getDouble()/10000);
		result.put("loanAmount",applyResult.getAttribute("businesssum")==null?"":applyResult.getAttribute("businesssum").getDouble());
		result.put("projectName",applyResult.getAttribute("projectname")==null?"":applyResult.getAttribute("projectname").toString());
		result.put("fundsourceDesc",applyResult.getAttribute("fundsourcedesc")==null?"":applyResult.getAttribute("fundsourcedesc").toString());
		result.put("prov",applyResult.getAttribute("PROV")==null?"":applyResult.getAttribute("PROV").toString());
		result.put("city",applyResult.getAttribute("CITY")==null?"":applyResult.getAttribute("CITY").toString());
		result.put("statusName",applyResult.getAttribute("statusName")==null?"":applyResult.getAttribute("statusName").toString());
		int repayTimes=applyResult.getAttribute("repaytimes")==null?0:applyResult.getAttribute("repaytimes").getInt();
		result.put("repaytimes",repayTimes);
		result.put("applystatus",applyResult.getAttribute("applystatus")==null?"":applyResult.getAttribute("applystatus").toString());
		}
		return result;
	}

	/**
	 * 获取loan_apply页面返回的结果集
	 * @param applyNo
	 * @return
	 * @throws HandlerException 
	 */
	private BizObject getApplyResult(String applyNo) throws HandlerException {
		BizObject r = null;
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			m = f.getManager("jbo.trade.loan_apply");
			String sql = "select o.applyno,o.businesssum,o.projectname,o.repaytimes,o.fundsourcedesc,o.applystatus,getitemname('DistrictCode', o.prov) AS V.PROV,getitemname('DistrictCode', o.city) AS V.CITY,getitemname('LoanApplyStatus', o.applystatus) AS V.statusName from o where o.applyno=:applyNo";
			BizObjectQuery q = m.createQuery(sql);
			q.setParameter("applyNo", applyNo);
			r = q.getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("undefined.error");
		}
		return r;
	}
	/**
	 * 我不想贷了
	 * @param wantQuit 判断是否不想贷了
	 * @param applyNo	将此贷款编号的状态设置为090
	 * @return	返回是否成功
	 * @throws HandlerException 
	 */
	private boolean quitApply(String wantQuit,String applyNo,String quitReason) throws HandlerException{
		if("t".equals(wantQuit)){
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
			String table="jbo.trade.loan_apply";
			String sql = "SELECT applystatus,applyNo,quitreason FROM O WHERE applyno=:applyNo";
			try {
				m = f.getManager(table);
				BizObjectQuery q = m.createQuery(sql);
				BizObject result = q.setParameter("applyNo",applyNo).getSingleResult(true);
				if(result!=null){
					result.setAttributeValue("applystatus", "090");
					result.setAttributeValue("quitreason", quitReason);
					m.saveObject(result);
				}
				return true;
			} catch (JBOException e) {
				e.printStackTrace();
				throw new HandlerException("save.error");
			}
		}
	return false;
	}
	
}
