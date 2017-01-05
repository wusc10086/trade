package com.amarsoft.p2ptrade.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.amarsoft.p2ptrade.tradeclient.RunTradeService;

/*
	 * @queryprojectinfo
	 * 输入：
	 *     Serialno
	 * 输出：
	 *  LoanAmount            
	 *  LoanRate          
	 *	Granantee            
	 *	PaymentMethod
	 *	LoanTerm            
	 *	TradeFee           
	 *	RateDate           
	 *	Projectdes 
	 * 
 */
public class ProjectdetailHandler extends JSONHandler{
	String sUserid = "";
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectProj(request);
	}
 
	private JSONObject selectProj(JSONObject request)throws HandlerException {	
		JSONObject result = new JSONObject();
		if(request.get("Serialno")==null){     
			throw new HandlerException("request.invalid");//项目编号
		}
		/*
		if(request.get("UserID")==null){
			throw new HandlerException("request.invalid");//用户id
		}
		*/
		String sSerialno = request.get("Serialno").toString();
		if(request.containsKey("UserID") && request.get("UserID")!=null)
		  sUserid = request.get("UserID").toString();
		String sGranantee = "";
		String sContractidString= "";
		Map<String , String>  usernameMap = new HashMap<String, String>();
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.project_info_listview");
			 BizObjectQuery query= m.createQuery(" serialno=:serialno");
			 String sTodayNow = StringFunction.getTodayNow();
			 String	sDate = sTodayNow.substring(0, 10);
		 	 String	sTime = sTodayNow.substring(11,sTodayNow.length());
			 query.setParameter("serialno",sSerialno);
			 BizObject o = query.getSingleResult(false);
			 if(o==null)
				 throw new HandlerException("common.projectnotexist");
			 /*
			 else if( (o.getAttribute("BEGINDATE").getString() + " " + o.getAttribute("begintime").getString()).compareToIgnoreCase(sTodayNow)>0){
				throw new HandlerException("project.nostart");
			 }
			 */
			 //还款计划
			//result = getpaymentschedule(o,jbo);
			result.put("RootType", "030");
			result.put("LoanAmount",GeneralTools.numberFormat(Double.parseDouble(o.getAttribute("LOANAMOUNT").toString()==null ? "0" : o.getAttribute("LOANAMOUNT").toString()), 0, 2) );
			result.put("LoanRate", o.getAttribute("LOANRATE").toString()==null ? "" : o.getAttribute("LOANRATE").toString());
			result.put("GranantorName", o.getAttribute("GranantorName").toString());
			result.put("Granantee", o.getAttribute("GRANANTEE").toString());
			result.put("ProjectName", o.getAttribute("PROJECTNAME").toString()==null ? "" :o.getAttribute("PROJECTNAME").toString());
			result.put("PaymentMethod", o.getAttribute("PAYMENTMETHOD").toString()==null ? "" :o.getAttribute("PAYMENTMETHOD").toString());
			result.put("LoanTerm", String.valueOf(o.getAttribute("LOANTERM")==null ? 0 :o.getAttribute("LOANTERM").getInt()));
			result.put("TradeFee", o.getAttribute("TRADEFEE").toString()==null ? "0" : o.getAttribute("TRADEFEE").toString());
			result.put("RateDate", o.getAttribute("RATEDATE").toString()==null ? "" : o.getAttribute("RATEDATE").toString());
			result.put("Projectdes", o.getAttribute("PROJECTDES").toString()==null ? "" : o.getAttribute("PROJECTDES").toString());
			result.put("Invalidtime", o.getAttribute("INVALIDTIME").toString()==null ? "" : o.getAttribute("INVALIDTIME").toString());
			result.put("Invaliddate", o.getAttribute("INVALIDDATE").toString()==null ? "" : o.getAttribute("INVALIDDATE").toString());
			result.put("Reciprocaltime", o.getAttribute("RECIPROCALTIME").getValue()==null ? "" : o.getAttribute("RECIPROCALTIME").toString());
			result.put("BEGINAMOUNT", o.getAttribute("BEGINAMOUNT").getValue()==null ? "0" : o.getAttribute("BEGINAMOUNT").toString());
			result.put("remainamount", o.getAttribute("remainamount").getValue()==null ? "0" : o.getAttribute("remainamount").toString());
			result.put("ADDAMOUNT", o.getAttribute("ADDAMOUNT").getValue()==null ? "0" : o.getAttribute("ADDAMOUNT").toString());
			result.put("PutOutDate", GeneralTools.getDate(1));
			sContractidString = o.getAttribute("CONTRACTID").toString();
			result.put("Contractid", sContractidString);
			//System.out.println("sContractidString@@@@@@@@@@"+sContractidString);
			
			result.put("Status", o.getAttribute("STATUS").toString()==null ? "" : o.getAttribute("STATUS").toString());
			
			
			
			result.put("serialno", sSerialno);
			result.put("BeginTime", o.getAttribute("BEGINTIME").toString());
			result.put("BeginDate", o.getAttribute("BEGINDATE").toString());
			/*if(sUserid.length()>0){
				
				usernameMap=selectuser(sContractidString);
				result.put("userid", usernameMap.get("userid"));
				result.put("tuserid", usernameMap.get("tuserid"));
				result.put("realname", usernameMap.get("realname"));
				
				result.put("username", usernameMap.get("username"));
				result.put("tusername", usernameMap.get("tusername"));
				
				Double balance =getaccountinfo( jbo, sUserid );
				
				result.put("Balance",GeneralTools.numberFormat(balance, 0, 2) );
				
			}*/
			
/*			//查询常见问题
			JSONArray helpdatas = new JSONArray();
			BizObjectManager manager2 = JBOFactory.getBizObjectManager("jbo.trade.ti_help_info");
			List<BizObject> list2 = manager2.createQuery("select title,serialno,catalogno from o where catalogno=:catalogno order by sortno asc").setParameter("catalogno", "invest").getResultList(false);
			for(BizObject obj : list2){
				JSONObject objx = new JSONObject();
				objx.put("title", obj.getAttribute("title").getString());
				objx.put("serialno", obj.getAttribute("serialno").getString());
				objx.put("catalogno", obj.getAttribute("catalogno").getString());
				helpdatas.add(objx);
			}
			result.put("helpdatas", helpdatas);*/
		}
		catch(HandlerException e){
			throw e;
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return result;
	}
	
	      
	       /**
	 * @param sContractidString
	     * @throws JBOException 
	 */
	private 	Map<String , String>  selectuser(String sContractidString) throws JBOException {
		// TODO Auto-generated method stub
		JBOFactory jbo = JBOFactory.getFactory();
		Map<String , String> usernameMap = new HashMap<String, String>();
		BizObjectManager m = jbo.getManager("jbo.trade.user_account");
		 BizObjectQuery query= m.createQuery(" select  USERID,USERNAME,ad.RealName "
				 + "from  o ,jbo.trade.user_contract uc,jbo.trade.account_detail ad"
				 + " where  uc.userid =  o.userid and o.userid=ad.userid "
				 + " and uc.RELATIVETYPE= '001' and  uc.contractid =:contractid");
		 query.setParameter("contractid",sContractidString);
		 BizObject o  = query.getSingleResult(false);
		 System.out.println("o = " + o);
	     // 借款人
		usernameMap.put("userid", o.getAttribute("USERID").toString());
		usernameMap.put("username", o.getAttribute("USERNAME").toString());
		usernameMap.put("realname", o.getAttribute("RealName").toString());
		
		 m = jbo.getManager("jbo.trade.user_account");
		 BizObjectQuery query1= m.createQuery(" userid =:userid  ");
		 query1.setParameter("userid",sUserid);
		 BizObject oo  = query1.getSingleResult(false);
	     // 投资人
		usernameMap.put("tuserid", sUserid);
		usernameMap.put("tusername", oo.getAttribute("USERNAME").toString());
		return usernameMap;
	}

		/** 
	        * @throws HandlerException 
	        * 查询本金余额 冻结金额
	        */
	       private String  getorgen(JBOFactory jbo,String orgid ) throws HandlerException {
	    	   // TODO Auto-generated method stub
	    	   BizObjectManager m;
	    	   String result = "";
	    	   try {
	    		   m = jbo.getManager("jbo.trade.org_account");
	    		   BizObjectQuery query = m.createQuery("ORGID =:ORGID ");
	    		   query.setParameter("ORGID",orgid);
	    		   query.setParameter("ORGTYPE","02");
	    		   BizObject boBizObject = query.getSingleResult(false);
	    		   result = boBizObject.getAttribute("ORGTYPE").toString();
	    	   } catch ( Exception e) {
	    		   // TODO Auto-generated catch block
	    		   e.printStackTrace();
	    		   throw new HandlerException("default.database.error");
	    	   }
	    	   return result;
	       }
		/**org_account
		 * @throws HandlerException 
		 * 查询本金余额 冻结金额
		 */
		private Double getaccountinfo(JBOFactory jbo,String userid ) throws HandlerException {
			// TODO Auto-generated method stub
			BizObjectManager m;
			try {
				m = jbo.getManager("jbo.trade.user_account");
				BizObjectQuery query = m.createQuery("userid =:userid ");
				query.setParameter("userid",userid);
				BizObject boBizObject = query.getSingleResult(false);
		
				Double Usablebalance =  Double.parseDouble(boBizObject.getAttribute("USABLEBALANCE").toString());
				//Double Frozenbalance =  Double.parseDouble(boBizObject.getAttribute("FROZENBALANCE").toString());
				//Double result = Usablebalance+Frozenbalance;
				return Usablebalance;
			} catch ( Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				throw new HandlerException("default.database.error");
			}
		}
	
	
//	 预计收益列表
	private JSONObject getpaymentschedule( BizObject bizObject,JBOFactory jbo) throws HandlerException {
		JSONObject result  = new JSONObject();
		RunTradeService rt=new RunTradeService();
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.acct_payment_schedule_tmp");
			BizObjectQuery query =  m.createQuery(" objectno =:objectno order by paydate");
			query.setParameter("objectno", bizObject.getAttribute("CONTRACTID").toString());
			List<BizObject> list = query.getResultList(false);
			
			if (list != null) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					Double	sPaycorpusamt = (double) 0;
					Double	sPayinteamt = (double) 0;
					Double	sdCorpusInteAmt = (double) 0;
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					obj.put("Seqid",String.valueOf(o.getAttribute("SEQID").getInt()));
				    obj.put("Paydate",o.getAttribute("PAYDATE").toString());
					 sPaycorpusamt =   o.getAttribute("PAYCORPUSAMT").getDouble();
					 sPayinteamt =   o.getAttribute("PAYINTEAMT").getDouble();
					 sdCorpusInteAmt = sPaycorpusamt+sPayinteamt;
					obj.put("dCorpusInteAmt", GeneralTools.numberFormat(sdCorpusInteAmt, 0, 2));
					obj.put("dCorpusAmt",o.getAttribute("PAYCORPUSAMT").toString());
					obj.put("dInteAmt",o.getAttribute("PAYINTEAMT").toString());
					obj.put("dCorpusBalance",o.getAttribute("CORPUSBALANCE").toString());
					array.add(obj);
				}
				result.put("array", array);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return result;
	}
        
}
