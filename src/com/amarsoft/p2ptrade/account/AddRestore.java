package com.amarsoft.p2ptrade.account;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.are.util.StringFunction;

public class AddRestore {

	/**
	 * 
	 * @ sUserID  当前用户编号
	 * @ restoreType  当前用户编号
	 * @ tx
	 * */
	public static void setRestore(String sUserID,String restoreType,JBOTransaction tx){
		
		JBOFactory jbo = JBOFactory.getFactory();
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.restore_rule");
			//从签到返利规则查询当前可返利金额
			BizObjectQuery ruleq = m.createQuery(" select restoresum from o where rule.rulecode=:restoreType");
			ruleq.setParameter("restoreType", restoreType);
			
			BizObject ruleo = ruleq.getSingleResult(false);
			double restoresum = 0;
			if(ruleo!=null){
				restoresum = ruleo.getAttribute("restoresum").getDouble();
			}
			
			if(restoresum>0){
				//插入交易记录
				BizObjectManager recordm = jbo.getManager("jbo.trade.transaction_record",tx);
				
				//插入交易记录
				BizObject recordo1 = recordm.newObject();
				recordo1.setAttributeValue("USERID", ARE.getProperty("HouBankSerialNo"));
				recordo1.setAttributeValue("relaaccount", "");
				recordo1.setAttributeValue("DIRECTION", "P");
				recordo1.setAttributeValue("AMOUNT", restoresum);
				recordo1.setAttributeValue("BALANCE", 0);
				recordo1.setAttributeValue("TRANSTYPE", "2030");
				recordo1.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo1.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo1.setAttributeValue("STATUS", "01");
				recordo1.setAttributeValue("USERACCOUNTTYPE", "003");
				recordm.saveObject(recordo1);
				
				BizObject recordo = recordm.newObject();
				recordo.setAttributeValue("USERID", sUserID);
				recordo.setAttributeValue("relaaccount", "");
				recordo.setAttributeValue("DIRECTION", "R");
				recordo.setAttributeValue("AMOUNT", restoresum);
				recordo.setAttributeValue("BALANCE", 0);
				recordo.setAttributeValue("TRANSTYPE", "2030");
				recordo.setAttributeValue("TRANSDATE", StringFunction.getToday());
				recordo.setAttributeValue("TRANSTIME", StringFunction.getNow());
				recordo1.setAttributeValue("inputTIME", StringFunction.getToday() +" "+ StringFunction.getNow());
				recordo.setAttributeValue("STATUS", "01");
				recordo.setAttributeValue("USERACCOUNTTYPE", "001");
				recordm.saveObject(recordo);
				
				//插入打款交易
				BizObjectManager mm =jbo.getManager("jbo.trade.acct_transfer",tx);
				
				//平台营销账户
				BizObject transfer1 = mm.newObject();
				transfer1.setAttributeValue("objectno", "");
				transfer1.setAttributeValue("objecttype", "030");
				transfer1.setAttributeValue("seqid", "1");
				transfer1.setAttributeValue("userid", ARE.getProperty("HouBankSerialNo"));
				transfer1.setAttributeValue("direction", "R");
				transfer1.setAttributeValue("amount", restoresum);
				transfer1.setAttributeValue("status", "01");
				transfer1.setAttributeValue("inputdate", StringFunction.getToday());
				transfer1.setAttributeValue("inputtime", StringFunction.getNow());
				transfer1.setAttributeValue("transserialno", recordo1.getAttribute("serialno"));
				transfer1.setAttributeValue("remark", "代收邀请注册收益");
				transfer1.setAttributeValue("transcode", "1001");
				transfer1.setAttributeValue("useraccounttype", "003");
				mm.saveObject(transfer1);
				
				//邀请人
				BizObject transfer = mm.newObject();						
				transfer.setAttributeValue("objectno", "");
				transfer.setAttributeValue("objecttype", "030");
				transfer.setAttributeValue("seqid", "2");
				transfer.setAttributeValue("userid", sUserID);
				transfer.setAttributeValue("direction", "P");
				transfer.setAttributeValue("amount", restoresum);
				transfer.setAttributeValue("status", "01");
				transfer.setAttributeValue("inputdate", StringFunction.getToday());
				transfer.setAttributeValue("inputtime", StringFunction.getNow());
				transfer.setAttributeValue("transserialno", recordo.getAttribute("SERIALNO"));
				transfer.setAttributeValue("remark", "代付邀请注册收益");
				transfer.setAttributeValue("transcode", "2001");
				transfer.setAttributeValue("useraccounttype", "001");
				mm.saveObject(transfer);
			}
		}catch(Exception e){}
	}
}
