package com.amarsoft.p2ptrade.loan;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 获得投标中的信息
 * @author Mbmo
 *
 */
public class MyRaisingInfoDetailHandler extends JSONHandler {
	private final String INVEST = "002";// 1代表贷款人，2代表投资人
	static {
		Parser.registerFunction("getUserName");
		Parser.registerFunction("getitemname");
	}

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		if(isCurrentUser(request)){
			return getMyRaisingDetial(request);
		}
		//如果编号与当前用户无关，则返回空
		return new JSONObject();
	}

	@SuppressWarnings("unchecked")
	private JSONObject getMyRaisingDetial(JSONObject request) {
		JSONObject result = new JSONObject();
		String applyNo = (String) request.get("serialNo");
		
		JSONObject scheduleJ = new JSONObject();// 进度
		JSONObject loanInfoJ = new JSONObject();// 贷款信息
		JSONArray investBriefInfoArray = new JSONArray();// 投资人简介
		
		BizObject loanInfoB = getLoanInfoResult(applyNo);
		String contractId ="";//合同编号
		double loanAmount=0;
		double investSum=0;
		
		if(loanInfoB!=null){
		try {
			loanInfoJ.put("loanAmount", null==loanInfoB.getAttribute("LOANAMOUNT")? "" : loanInfoB.getAttribute("LOANAMOUNT").toString());// 贷款金额
			loanInfoJ.put("loanRate", loanInfoB.getAttribute("LOANRATE") == null ? "" : loanInfoB.getAttribute("LOANRATE").toString());// 期望年利率
			loanInfoJ.put("loanTerm", loanInfoB.getAttribute("LOANTERM") == null ? 0 : loanInfoB.getAttribute("LOANTERM").getInt());// 贷款期限
			loanInfoJ.put("guarantee", loanInfoB.getAttribute("GRANANTEE") == null ? "" : loanInfoB.getAttribute("GRANANTEE").toString());// 担保方式
			loanInfoJ.put("prov", loanInfoB.getAttribute("PROV") == null ? "" : loanInfoB.getAttribute("PROV").toString());// 省
			loanInfoJ.put("city", loanInfoB.getAttribute("CITY") == null ? "" : loanInfoB.getAttribute("CITY").toString());// 市
			loanInfoJ.put("paymentMethod", loanInfoB.getAttribute("PAYMENTMETHOD") == null ? "" : loanInfoB.getAttribute("PAYMENTMETHOD").toString());// 还款方式
			loanInfoJ.put("fundSourceDesc", loanInfoB.getAttribute("fundsourcedesc") == null ? "" : loanInfoB.getAttribute("fundsourcedesc").toString());// 贷款需求说明
			
			loanAmount=loanInfoB.getAttribute("LOANAMOUNT")==null?-1:loanInfoB.getAttribute("LOANAMOUNT").getDouble();//为-1时取值错误
			contractId = loanInfoB.getAttribute("contractId") == null ?"" : loanInfoB.getAttribute("contractId").toString();
			
			List<BizObject> investBriefInfo = getInvestInfoResultList(contractId);
			for (BizObject bizO : investBriefInfo) {
				JSONObject investBriefInfoJ=new JSONObject();
				investBriefInfoJ.put("userName", handleName(bizO.getAttribute("USERNAME") == null ? "" : bizO.getAttribute("USERNAME").toString()));// 用户名
				investBriefInfoJ.put("investSum", bizO.getAttribute("investsum") == null ? "" : bizO.getAttribute("investsum").toString());// 投资金额
				investBriefInfoJ.put("inputTime", bizO.getAttribute("inputtime") == null ? "" : bizO.getAttribute("inputtime").toString());// 投资时间
				investBriefInfoJ.put("status", bizO.getAttribute("status") == null ? "" : bizO.getAttribute("status").toString());// 状态
				
				double invest=bizO.getAttribute("investsum")==null?-0.5:bizO.getAttribute("investsum").getDouble();
				investSum+=invest;
				investBriefInfoArray.add(investBriefInfoJ);
			}
			
			double ratio=-1;
			if(loanAmount!=0){
				ratio=investSum*100/loanAmount;
			}
			scheduleJ.put("loanAmount",loanAmount);// 总额
			scheduleJ.put("investSum",investSum);// 已筹款
			scheduleJ.put("difference",loanAmount-investSum);// 差额
			scheduleJ.put("ratio",ratio);// 比例
			
		} catch (JBOException e) {
			e.printStackTrace();
		}
		}
		result.put("scheduleJ", scheduleJ);
		result.put("loanInfoJ", loanInfoJ);
		result.put("investBriefInfoArray", investBriefInfoArray);
		return result;
	}

	/**
	 * 获取project_info和loan_apply的级联查询结果
	 */
	private BizObject getLoanInfoResult(String applyNo) {
		BizObject r = null;
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		try {
			m = f.getManager("jbo.trade.project_info");
			String sql = "select" + 
						" o.contractid,o.LOANAMOUNT,o.LOANRATE,o.LOANTERM,o.GRANANTEE,o.PAYMENTMETHOD, getitemname('AreaCode', la.prov) AS V.PROV,getitemname('DistrictCode', la.city) AS V.CITY,la.fundsourcedesc "+ 
						"from jbo.trade.ti_contract_info ci,o,jbo.trade.loan_apply la "+ 
						"where la.loanno=ci.loanno and o.contractid=ci.contractid and o.SERIALNO=:applyNo";
			BizObjectQuery q = m.createQuery(sql);
			q.setParameter("applyNo", applyNo);
			r = q.getSingleResult(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return r;
	}

	/**
	 * 获取投资人的查询结果
	 */
	@SuppressWarnings("unchecked")
	private List<BizObject> getInvestInfoResultList(String contractId) {
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		List<BizObject> r = null;
		try {
			m = f.getManager("jbo.trade.user_contract");
			String sql = "select " + 
									"getUserName(userid) AS V.USERNAME,inputtime,investsum,status " + 
									"from o "+ 
									"where contractId=:contractId And relativetype=:INVEST";
			BizObjectQuery q = m.createQuery(sql);
			q.setParameter("contractId", contractId);
			q.setParameter("INVEST", INVEST);
			r = q.getResultList(false);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return r;
	}
	/**
	 * 判断查询编号是否与当前用户关联
	 * @param request
	 * @return
	 */
	private boolean isCurrentUser(JSONObject request){
		boolean flag=false;
		String userId=(null==request.get("userId")?"":request.get("userId").toString());
		String serialNo=(null==request.get("serialNo")?"":request.get("serialNo").toString());
		JBOFactory f = JBOFactory.getFactory();
		BizObjectManager m;
		
		try {
			String table="jbo.trade.project_info";
			String sql = "select bc.CUSTOMERID from o,jbo.trade.business_contract bc where o.CONTRACTID=bc.SERIALNO and o.serialno=:serialNo";
			m = f.getManager(table);
			BizObjectQuery q = m.createQuery(sql);
			BizObject userInBaseB = q.setParameter("serialNo", serialNo).getSingleResult(false);
			if(userInBaseB==null){return false;}
			else if(!userId.equals(userInBaseB.getAttribute("CUSTOMERID").toString())){
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
	 * 将用户名中间用*号代替
	 */
	private  String handleName(String userName){
		String result="";
		if(userName.length()>2){
			char endS=userName.charAt(userName.length()-1);
			result=userName.charAt(0)+"**"+endS;
		}
		return result;
	}
}
