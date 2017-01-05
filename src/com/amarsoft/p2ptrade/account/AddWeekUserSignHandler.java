package com.amarsoft.p2ptrade.account;

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.invest.P2pString;

/**
 * 用户一周免签签到交易
 *
 */
public class AddWeekUserSignHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		//获取参数值
		String userid = (String) request.get("UserID");
		String signtype = (String) request.get("signtype");
		

		if(userid == null || userid.length() == 0){
			throw new HandlerException("sign.nouserid");
		}
		if(signtype == null || signtype.length() == 0){
			throw new HandlerException("sign.signtype");
		}		
		JSONObject result = new JSONObject();
		double weekrestoresum = 0;
		JBOTransaction tx = null;
		boolean flag = false;
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			tx = JBOFactory.createJBOTransaction();
			BizObjectManager m =jbo.getManager("jbo.trade.user_sign",tx);
			String inputdate = StringFunction.getToday();
			BizObjectQuery query = m.createQuery(" UserID=:UserID and inputdate=:inputdate");
			query.setParameter("UserID", userid).setParameter("inputdate", inputdate);
    
			BizObject o = query.getSingleResult(true);
			if(o!=null){
				result.put("SuccessFlag", "E");
				tx.commit();
			}else{
				String day = StringFunction.getToday() + " " + StringFunction.getNow();
				for(int i=0;i<7;i++){
					
					String inputd = P2pString.addDateFormat(day, 3, i, "yyyy/MM/dd");
					String inputt = P2pString.addDateFormat(day, 3, i, "HH:mm:ss");
					//插入签到流水
					o = m.newObject();
					o.setAttributeValue("userid", userid);
					o.setAttributeValue("signtype", signtype);
					o.setAttributeValue("inputdate", inputd);
					o.setAttributeValue("inputtime", inputt);
					o.setAttributeValue("remark", "一周免签");
					m.saveObject(o);
					String serialno = o.getAttribute("serialno").toString();
					//持有中的项目金额
					BizObjectQuery ucq = m.createQuery("select sum(uc.investsum) as v.sum from jbo.trade.user_contract uc where uc.userid=:userid and uc.status='1'");
					ucq.setParameter("userid", userid);
					BizObject uco = ucq.getSingleResult(false);
					double ucsum = 0;
					if(uco!=null){
						ucsum = uco.getAttribute("sum").getDouble();
					} else{
						result.put("SuccessFlag", "R");
						return result;
					}
					//从签到返利规则查询当前可返利金额
					BizObjectQuery ruleq = m.createQuery(" select rule.restoresum,rule.minimum from jbo.trade.restore_rule rule where rule.rulecode='sign' and rule.minimum <=:ucsum order by rule.minimum desc");
					ruleq.setParameter("ucsum", ucsum);
					BizObject ruleo = ruleq.getSingleResult(false);
					double restoresum = 0;
					if(ruleo!=null){
						restoresum = ruleo.getAttribute("restoresum").getDouble();
					}
					
					weekrestoresum += restoresum;
					if(restoresum>0){
						BizObjectManager recordm =jbo.getManager("jbo.trade.transaction_record",tx);

						//插入交易记录
						BizObject recordo1 = recordm.newObject();
						recordo1.setAttributeValue("USERID", ARE.getProperty("HouBankSerialNo"));
						recordo1.setAttributeValue("transactionserialno", "2050@"+serialno);
						recordo1.setAttributeValue("DIRECTION", "P");
						recordo1.setAttributeValue("AMOUNT", restoresum);
						recordo1.setAttributeValue("BALANCE", 0);
						recordo1.setAttributeValue("TRANSTYPE", "2050");
						recordo1.setAttributeValue("TRANSDATE", inputd);
						recordo1.setAttributeValue("TRANSTIME", inputt);
						recordo1.setAttributeValue("STATUS", "01");
						recordo1.setAttributeValue("USERACCOUNTTYPE", "003");
						recordm.saveObject(recordo1);
						
						BizObject recordo = recordm.newObject();
						recordo.setAttributeValue("USERID", userid);
						recordo.setAttributeValue("transactionserialno", "2050@"+serialno);
						recordo.setAttributeValue("DIRECTION", "R");
						recordo.setAttributeValue("AMOUNT", restoresum);
						recordo.setAttributeValue("BALANCE", 0);
						recordo.setAttributeValue("TRANSTYPE", "2050");
						recordo.setAttributeValue("TRANSDATE", inputd);
						recordo.setAttributeValue("TRANSTIME", inputt);
						recordo.setAttributeValue("STATUS", "01");
						recordo.setAttributeValue("USERACCOUNTTYPE", "001");
						recordm.saveObject(recordo);
						
						//插入打款交易
						BizObjectManager mm =jbo.getManager("jbo.trade.acct_transfer",tx);
						
						//平台营销账户
						BizObject transfer1 = mm.newObject();
						transfer1.setAttributeValue("objectno", serialno);
						transfer1.setAttributeValue("objecttype", "050");
						transfer1.setAttributeValue("seqid", "1");
						transfer1.setAttributeValue("userid", ARE.getProperty("HouBankSerialNo"));
						transfer1.setAttributeValue("direction", "R");
						transfer1.setAttributeValue("amount", restoresum);
						transfer1.setAttributeValue("status", "01");
						transfer1.setAttributeValue("inputdate", inputd);
						transfer1.setAttributeValue("inputtime", inputt);
						transfer1.setAttributeValue("transserialno", recordo1.getAttribute("SERIALNO"));
						transfer1.setAttributeValue("remark", "代收签到收益");
						transfer1.setAttributeValue("transcode", "1001");
						transfer1.setAttributeValue("useraccounttype", "003");
						mm.saveObject(transfer1);
						
						//签到人
						BizObject transfer = mm.newObject();						
						transfer.setAttributeValue("objectno", serialno);
						transfer.setAttributeValue("objecttype", "050");
						transfer.setAttributeValue("seqid", "2");
						transfer.setAttributeValue("userid", userid);
						transfer.setAttributeValue("direction", "P");
						transfer.setAttributeValue("amount", restoresum);
						transfer.setAttributeValue("status", "01");
						transfer.setAttributeValue("inputdate", inputd);
						transfer.setAttributeValue("inputtime", inputt);
						transfer.setAttributeValue("transserialno", recordo.getAttribute("SERIALNO"));
						transfer.setAttributeValue("remark", "代付签到收益");
						transfer.setAttributeValue("transcode", "2001");
						transfer.setAttributeValue("useraccounttype", "001");
						mm.saveObject(transfer);
					}
					
					result.put("SuccessFlag", "S");
					result.put("weekrestoresum", weekrestoresum);
				}
			}
			flag = true;
		} catch(JBOException e){
			flag = false;
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}finally{
			try {
				if(flag) 
					tx.commit();
				else
					tx.rollback();
			} catch (JBOException e) {
				e.printStackTrace();
			}
		}	
		return result;
	}
}