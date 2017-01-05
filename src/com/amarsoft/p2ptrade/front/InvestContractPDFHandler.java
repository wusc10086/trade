package com.amarsoft.p2ptrade.front;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import com.amarsoft.p2ptrade.util.StringUtils;
import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.JBOTransaction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 平台公告反击计数
 * 输入参数：
 * 		newsid:				INF_NEWS主见
 * 输出参数：
 * 		updateFlag          S/F
 */
public class InvestContractPDFHandler extends JSONHandler {

	
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
/*
		Object obj_newsid =  request.get("newsid");
		
		if(StringUtils.isEmpty(obj_newsid))
			throw new HandlerException("newsid.empty");
			*/
		return addchicksum(request);
	}

	
	private JSONObject addchicksum(JSONObject request) throws HandlerException{
		JSONObject result = new JSONObject();
		String sSubContractNo = (String)request.get("SubContractNo");
		//String sSubContractNo = (String)result.get("SubContractNo");
		if(sSubContractNo==null)
			throw new HandlerException("no.SubContractNo.error");
		Connection conn = null;
		try {// ARE.getDBConnection("als")
			//conn = ARE.getDBConnection(ARE.getProperty("dbname"));

			String filePath2 = ARE.getProperty("PDFPath");
			conn = ARE.getDBConnection("als");
		/*	PreparedStatement ps = conn.prepareStatement("select SAVEFILE from Contract_s_Record where contractno in" +
	 			 "(select contractid  from user_contract where SubContractNo=?) and contracttype='002'");*/
			PreparedStatement ps = conn.prepareStatement("select SAVEFILE from Contract_s_Record where contractno=?" +
		 			 " and contracttype='002'");
			ps.setString(1, sSubContractNo);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				result.put("filepath", rs.getString("SAVEFILE")==null?"":rs.getString("SAVEFILE"));
			}
			result.put("filePath2", filePath2);
			
		} catch (Exception e) {
			try{
				if(conn!=null)
					conn.close();
				e.printStackTrace();
			}
			catch(Exception e1){
				e1.printStackTrace();
			}			
			throw new HandlerException("singlesignon.error");
			
		}		
		return result;
	}
}
