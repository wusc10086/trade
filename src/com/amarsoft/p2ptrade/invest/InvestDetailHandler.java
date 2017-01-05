package com.amarsoft.p2ptrade.invest;

import java.text.DecimalFormat;
import java.util.Date;
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
import com.amarsoft.p2ptrade.account.QueryUserAccountInfoHandler;
/**
 * 投资项目详情查询
 * 输入：
 * 	SERIALNO
 *  curPage
 *  pageSize
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
 */
public class InvestDetailHandler extends JSONHandler{
	
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
		if(request.get("Serialno")==null){     
			throw new HandlerException("request.invalid");//项目编号
		}

//		if(request.get("UserID")==null){
//			throw new HandlerException("request.invalid");//用户id
//		}

		String sSerialNo = request.get("Serialno").toString();
		//String sUserID = request.get("UserID").toString();
		
		try{
			//投资用户
			if(request.get("UserID")!=null){
					result.put("userInfo_UserAuthFlag","S");
					request.put("WithBalance", "false");
					QueryUserAccountInfoHandler qu = new QueryUserAccountInfoHandler();
					JSONObject userinfo = (JSONObject)qu.createResponse(request, null);					
					result.put("userInfo", userinfo);
			}
		}catch(Exception ee){}
		
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info_listview");
			BizObjectQuery query = m.createQuery("select o.PROJECTNAME,o.SERIALNO,o.LOANAMOUNT,o.LOANTERM,"
					+ " o.GRANANTORNAME,o.LOANRATE,o.PAYMENTMETHOD," 
					+ " o.endtime,o.enddate,o.begintime,o.begindate,o.CONTRACTID,o.PROJECTDES,"
					+ " o.loanpurpose,o.RATEDATE,o.BEGINAMOUNT,o.ADDAMOUNT,o.remainamount,"
					+ " o.TRADEFEE,bc.customerid,o.status,o.guaranteeflag,o.isvip,o.reciprocaltime "
					+ " from O,jbo.trade.business_contract bc where o.contractid=bc.SERIALNO and o.SERIALNO =:SERIALNO");
			query.setParameter("SERIALNO",sSerialNo);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				JSONObject projectInfo = new JSONObject();
				String endtime = o.getAttribute("endtime").toString()==null?"":o.getAttribute("endtime").toString();
				String enddate = o.getAttribute("enddate").toString()==null?"":o.getAttribute("enddate").toString();
				String begintime = o.getAttribute("begintime").toString()==null?"":o.getAttribute("begintime").toString();
				String begindate = o.getAttribute("begindate").toString()==null?"":o.getAttribute("begindate").toString();
				
				String status = o.getAttribute("STATUS").toString()==null ? "1" : o.getAttribute("STATUS").toString();
				
				String loanamount = o.getAttribute("LOANAMOUNT").toString()==null ? "0" : o.getAttribute("LOANAMOUNT").toString();
				String remainamount = o.getAttribute("remainamount").toString()==null?"0":o.getAttribute("remainamount").toString();
				String reciprocaltime = o.getAttribute("reciprocaltime").toString()==null?"0":o.getAttribute("reciprocaltime").toString();
				
				projectInfo.put("endtime", endtime);
				projectInfo.put("enddate", enddate);
				projectInfo.put("begintime", begintime);
				projectInfo.put("begindate", begindate);
				
				projectInfo.put("LOANAMOUNT", loanamount);
				projectInfo.put("remainamount", remainamount);
				
				projectInfo.put("STATUS", status);
				String end = enddate+" "+endtime;
				String begin = begindate+" "+begintime;		
				
				projectInfo.put("begind", begin);
				String begin1 = P2pString.addDateFormat(begin, 1, -Integer.parseInt(reciprocaltime),"yyyy/MM/dd HH:mm:ss");
				System.out.println("begin===="+begin);
				//到期未满标的项目 设置显示已结束
				if(getBetwon(end)<0 && "1".equals(status)){
					projectInfo.put("end","1");
				}
				//未开始的项目 设置显示预约中
				if(getBetwon(begin)>0 && getBetwon(begin1)<0 && "1".equals(status)){
					projectInfo.put("begin","1");					
				}
				
				//统计项目进度
				double percent = 0;
				double amount = Double.parseDouble(loanamount);
				double remain = Double.parseDouble(remainamount);

				percent = (amount-remain)/amount-0.00001<0?0:(amount-remain)/amount-0.00001;

				projectInfo.put("INVESTSUM", amount-remain);
				projectInfo.put("percent", percent);
				
				projectInfo.put("isvip", o.getAttribute("isvip").toString()==null?"":o.getAttribute("isvip").toString());
				projectInfo.put("SERIALNO", o.getAttribute("SERIALNO").toString()==null?"":o.getAttribute("SERIALNO").toString());
				projectInfo.put("PROJECTNAME", o.getAttribute("PROJECTNAME").toString()==null?"0":o.getAttribute("PROJECTNAME").toString());
				projectInfo.put("LOANTERM", o.getAttribute("LOANTERM").toString()==null?"0":o.getAttribute("LOANTERM").toString());
				projectInfo.put("GRANANTORNAME", o.getAttribute("GRANANTORNAME").toString()==null?"":o.getAttribute("GRANANTORNAME").toString());
				projectInfo.put("LOANRATE", o.getAttribute("LOANRATE").toString()==null?"0":o.getAttribute("LOANRATE").toString());
				projectInfo.put("PAYMENTMETHOD", o.getAttribute("PAYMENTMETHOD").toString()==null?"":o.getAttribute("PAYMENTMETHOD").toString());
				projectInfo.put("CONTRACTID", o.getAttribute("CONTRACTID").toString()==null?"":o.getAttribute("CONTRACTID").toString());
				projectInfo.put("PROJECTDES", o.getAttribute("PROJECTDES").toString()==null?"":o.getAttribute("PROJECTDES").toString());
				//projectInfo.put("LOANPURPOSE", o.getAttribute("loanpurpose").toString()==null?"":o.getAttribute("loanpurpose").toString());
				projectInfo.put("RATEDATE", o.getAttribute("RATEDATE").toString()==null?"":o.getAttribute("RATEDATE").toString());
				projectInfo.put("BEGINAMOUNT", o.getAttribute("BEGINAMOUNT").toString()==null?"0":o.getAttribute("BEGINAMOUNT").toString());
				projectInfo.put("ADDAMOUNT", o.getAttribute("ADDAMOUNT").toString()==null?"0":o.getAttribute("ADDAMOUNT").toString());
				projectInfo.put("TRADEFEE", o.getAttribute("TRADEFEE").toString()==null?"0":o.getAttribute("TRADEFEE").toString());
				projectInfo.put("guaranteeflag", o.getAttribute("guaranteeflag").toString()==null?"0":o.getAttribute("guaranteeflag").toString());
				gettermname(projectInfo,jbo,projectInfo.get("PAYMENTMETHOD").toString());
				//借款用户
				String customerid = o.getAttribute("customerid").toString()==null?"":o.getAttribute("customerid").toString();
				request.put("UserID", customerid);
				QueryUserAccountInfoHandler qd = new QueryUserAccountInfoHandler();
				JSONObject userInfo = (JSONObject)qd.createResponse(request, null);
				String sRealName = (String)userInfo.get("RealName");
				if(sRealName!=null && sRealName.length()>1){
					sRealName = sRealName.substring(0,1) + "**";
				}
				userInfo.put("RealName", sRealName);
				result.put("LoanUser",userInfo);
				
				String CONTRACTID = o.getAttribute("CONTRACTID").toString()==null?"":o.getAttribute("CONTRACTID").toString();
				//借款描述
				selectLoanApply(projectInfo,jbo,CONTRACTID);
				//产品名称
				selectBusiness(projectInfo,jbo,CONTRACTID);
				//总投资额
				BizObjectManager m1 = jbo.getManager("jbo.trade.user_contract");
				//BizObjectQuery query1 = m1.createQuery("SELECT SUM(O.INVESTSUM) AS V.SUM FROM O WHERE PROJECTID = :PROJECTID and status not in ('2')");
				BizObjectQuery query1 = m1.createQuery("SELECT SUM(O.INVESTSUM) AS V.SUM,count(O.INVESTSUM) AS V.COUNT FROM O WHERE PROJECTID = :PROJECTID and (status > '2' or status <'2')");
				query1.setParameter("PROJECTID",sSerialNo);
				BizObject object = query1.getSingleResult(false);
				projectInfo.put("INVESTSUM", object.getAttribute("SUM").toString()==null?"0":object.getAttribute("SUM").toString());
				
				//查询投资人数
				//BizObjectQuery query2 = m1.createQuery("SELECT count(O.INVESTSUM) AS V.SUM FROM O WHERE PROJECTID = :PROJECTID and status not in('2')");
				//query2.setParameter("PROJECTID",sSerialNo);
				//BizObject object2 = query2.getSingleResult(false);
				projectInfo.put("count", object.getAttribute("COUNT").toString()==null?"0":object.getAttribute("COUNT").toString());
				result.put("projectInfo", projectInfo);
				
				query1 = JBOFactory.getBizObjectManager("jbo.trade.user_account")
						.createQuery("select o.USERID,o.UserName,uc.INVESTSUM,uc.UPDATETIME from o,jbo.trade.user_contract uc where o.userid= uc.userid and uc.PROJECTID = :PROJECTID and (uc.status>'2' or uc.status<'2') order by uc.UPDATETIME desc")
						.setParameter("PROJECTID", sSerialNo);
				List<BizObject> list = query1.getResultList(false);
				JSONArray projectList = new JSONArray();
				for(int i=0;i<list.size();i++){
					BizObject bizObj = list.get(i);
					JSONObject obj = new JSONObject();
					String userId = bizObj.getAttribute("USERID").toString()==null?"0000":bizObj.getAttribute("USERID").toString();
					
					String str = "";
					str = bizObj.getAttribute("UserName").getString();
					str = str.substring(0,1) + "**" + str.substring(str.length()-1,str.length());
					obj.put("SERIALNO", i+1);
					obj.put("USERID",str);
					obj.put("INVESTSUM", bizObj.getAttribute("INVESTSUM").toString()==null?"0":bizObj.getAttribute("INVESTSUM").toString());
					obj.put("INPUTTIME", bizObj.getAttribute("UPDATETIME").toString()==null?"":bizObj.getAttribute("UPDATETIME").toString());
					
					projectList.add(obj);
					
				}
				result.put("projectList", projectList);				
			}else{
				throw new HandlerException("invest.noproject.error");
			}
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
	
	//获取借款信息
	private JSONObject selectLoanApply (JSONObject result,JBOFactory jbo,String sContractID) throws HandlerException{

		try{
			String fundsourcedesc = "";
			BizObjectManager m = jbo.getManager("jbo.trade.loan_apply");
			BizObjectQuery q = m.createQuery("select fundsourcedesc from O where precontractno = :precontractno");
			q.setParameter("precontractno",sContractID);
			BizObject o = q.getSingleResult(false);
			if(o!=null){
				fundsourcedesc = o.getAttribute("fundsourcedesc")==null?"":o.getAttribute("fundsourcedesc").toString();
			}
			result.put("fundsourcedesc", fundsourcedesc);
		}catch(JBOException e){
			
		}
		return result;
	}
	
	//获取产品信息
	private JSONObject selectBusiness (JSONObject result,JBOFactory jbo,String sContractID) throws HandlerException{

		try{
			String fundsourcedesc = "";
			BizObjectManager m = jbo.getManager("jbo.trade.CORE_BUSINESS_TYPE");
			BizObjectQuery q = m.createQuery("select TYPENAME from O,jbo.trade.loan_apply la  where o.TYPENO = la.productid and la.precontractno = :precontractno ");
			q.setParameter("precontractno",sContractID);
			BizObject o = q.getSingleResult(false);
			if(o!=null){
				fundsourcedesc = o.getAttribute("TYPENAME")==null?"":o.getAttribute("TYPENAME").toString();
			}
			result.put("ProName", fundsourcedesc);
		}catch(JBOException e){
			
		}
		return result;
	}
	
	//时间差
	private int getBetwon(String str){
   
		java.text.DateFormat df=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		java.util.Calendar c1=java.util.Calendar.getInstance();   
		java.util.Calendar c2=java.util.Calendar.getInstance();   
		str = str.replaceAll("/", "-");
		try {
			c1.setTime(df.parse(str));   
			c2.setTime(new Date());   
		}catch(java.text.ParseException e){   
			System.err.println("格式不正确");   
		}
		
		int result=c1.compareTo(c2);
		return result;
	}
}