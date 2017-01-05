package com.amarsoft.p2ptrade.invest;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;

/*
 * @DrawDown  投资 交易-队列模式
 * 
 */
public class InvestTransactionWithQueeHandler  extends InvestTransactionHandler{

	protected Object runTrans(JSONObject request, JBOFactory jbo) throws HandlerException {
		if(remainamount<=0){
			throw new HandlerException("invest.amount.error");
		}
		BizObjectManager m;
		try{
			m = jbo.getManager("jbo.trade.inf_tran_queue");
			BizObject o = m.newObject();
			o.setAttributeValue("projectid", proserialno);
			o.setAttributeValue("contractid", sContractid);
			o.setAttributeValue("loanuser", loanuser);
			o.setAttributeValue("investuser", investuser);
			o.setAttributeValue("investsum", tamtString);
			o.setAttributeValue("inputdate", StringFunction.getToday());
			o.setAttributeValue("inputtime", StringFunction.getNow());
			o.setAttributeValue("status", "invest.quee.todo");
			o.setAttributeValue("trancode", "invest");
			m.saveObject(o);
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
		return request;
	}

}