package com.amarsoft.p2ptrade.invest;

import java.util.Properties;

import com.amarsoft.account.common.ObjectBalanceUtils;
import com.amarsoft.account.common.ObjectConstants;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/**
 * 投资确认详情
 * 输入：
 * 	serialno
 *  userid
 * 输出：
 * 	LOANAMOUNT
 * 	LOANTERM
 * 	GRANANTORNAME
 * 	LOANRATE
 * 	PAYMENTMETHOD
 * 	GRANANTEE
 * 	PROJECTDES
 * 	LOANPURPOSE
 * 	RATEDATE
 * 	INVESTSUM
 * 	USERID
 * 	INVESTSUM
 * 	INPUTTIME
 * @author mgao1
 */
public class InvestAffirmInfoHandler extends JSONHandler{
	
	static{
		Parser.registerFunction("sum");
		Parser.registerFunction("getitemname");
	}
	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
	
		String sSerialNo = request.get("serialno").toString();
		String userid = request.get("userid").toString();
		String investAmount = request.get("investamount").toString();
		String submitToken = request.get("submitToken").toString();
		double amout = 0;
		try{
			amout = Double.parseDouble(investAmount);
		}catch(Exception e){
			throw new HandlerException("invest.amount.error");
		}
		double usablebalance = 0;
		//记录流水
		if("1".equals(submitToken)){
			saveInvestRecord(request,result);
		}
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info");
			BizObjectQuery query = m.createQuery("select projectname,loanamount,loanrate,loanterm,paymentmethod,getitemname('PACBReturnMethod',paymentmethod) as v.paymentmethodN from O where serialno=:serialno ");//and status='1'");
			query.setParameter("serialno",sSerialNo);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				JSONObject projectInfo = new JSONObject();
				projectInfo.put("projectname", o.getAttribute("projectname").toString()==null?"":o.getAttribute("projectname").toString());
				projectInfo.put("loanamount", o.getAttribute("loanamount").getDouble());
				projectInfo.put("loanrate", o.getAttribute("loanrate").getDouble());
				projectInfo.put("loanterm", o.getAttribute("loanterm").getInt());
				projectInfo.put("paymentmethod", o.getAttribute("paymentmethod").toString()==null?"":o.getAttribute("paymentmethod").toString());
				projectInfo.put("paymentmethodN", o.getAttribute("paymentmethodN").toString()==null?"":o.getAttribute("paymentmethodN").toString());
				gettermname(projectInfo,jbo,projectInfo.get("paymentmethod").toString());
				double loanamout = o.getAttribute("loanamount")==null?0:o.getAttribute("loanamount").getDouble();
				BizObjectManager m1 = jbo.getManager("jbo.trade.user_contract");
				BizObjectQuery query1 = m1.createQuery("SELECT SUM(O.INVESTSUM) AS V.SUM FROM O WHERE PROJECTID = :PROJECTID and status <>'2'");
				query1.setParameter("PROJECTID",sSerialNo);
				BizObject object = query1.getSingleResult(false);
				double sum =  object.getAttribute("SUM")==null?0:object.getAttribute("SUM").getDouble();
				if(sum>=loanamout)
					throw new HandlerException("project.noamout");
				result.put("investDetail", projectInfo);
			}else{
				throw new HandlerException("default.database.error");
			}
			//查询可用余额
			try
			{
				usablebalance = ObjectBalanceUtils.queryObjectBalance(userid, ObjectConstants.OBJECT_TYPE_001, ObjectConstants.ACCOUNT_TYPE_001);
				if(usablebalance<amout)
					throw new HandlerException("invets.user.nomoney");
				
				JSONObject user = new JSONObject();
				result.put("investUser", user);
			}
			catch(Exception ex)
			{
				throw new JBOException(ex.getMessage());
			}
			/*BizObjectManager userm = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery userquery = userm.createQuery("select usablebalance from O where userid=:userid ");
			userquery.setParameter("userid",userid); 
			BizObject usero = userquery.getSingleResult(false);
			if(usero!=null){
				usablebalance = usero.getAttribute("usablebalance").getDouble();
				if(usablebalance<amout)
					throw new HandlerException("invets.user.nomoney");
				JSONObject user = new JSONObject();
				
				//user.put("usablebalance", usablebalance);
				result.put("investUser", user);
			}
			*/			
			return result;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}		
	}
	
	//获取还款方式
	private JSONObject gettermname (JSONObject result,JBOFactory jbo,String termid) throws HandlerException{

		try{
			String termname = "";
			BizObjectManager m = jbo.getManager("jbo.trade.acct_term_library");
			BizObjectQuery q = m.createQuery("Select termname From o Where Termtype='RPT' And Status = '1' and termid=:termid");
			q.setParameter("termid",termid);
			BizObject o = q.getSingleResult(false);
			if(o!=null){
				termname = o.getAttribute("termname")==null?"":o.getAttribute("termname").toString();
				result.put("termname", termname);
			}
		}catch(JBOException e){
			e.printStackTrace();
		}
		return result;
	}
	//投资蓝流水记录
	private void saveInvestRecord(JSONObject request,JSONObject result)throws HandlerException{
		try{
			String projectid = (String)request.get("serialno");
			String sUserID = (String)request.get("userid");
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.invest_hisrecord");
			BizObject o = m.newObject();
			o.setAttributeValue("userid",sUserID);
			o.setAttributeValue("projectid",projectid);
			o.setAttributeValue("inputtime", StringFunction.getTodayNow());			
			m.saveObject(o);
			result.put("investRecord","ok");
		}
		catch(JBOException je){
			throw new HandlerException("default.database.error");
		}
		
	}
}
