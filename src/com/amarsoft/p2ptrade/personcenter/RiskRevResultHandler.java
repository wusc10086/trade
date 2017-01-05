package com.amarsoft.p2ptrade.personcenter;
/*
 * 
 * */

import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.TimeTool;

public class RiskRevResultHandler extends JSONHandler {
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getRiskRevResult(request);
	}

	/**
	 * 风险评估结果查询
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getRiskRevResult(JSONObject request)
			throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}

		String sUserID = request.get("UserID").toString();

		if (request.get("reviewsArray") == null
				|| "".equals(request.get("reviewsArray"))) {
			throw new HandlerException("reviewsArray.error");
		}
		
		//用户风险评估指标字符串
		String itemString = request.get("reviewsArray").toString();
		
		try {
			JBOFactory jboItem = JBOFactory.getFactory();
			JSONObject result = new JSONObject();

			BizObjectManager managerItem = jboItem
					.getManager("jbo.trade.inf_card_value");

			/*
			 * 用户风险评估指标选项数组
			 * 拆分函数用于将前台传递的用“#”连接的每项指标的选项拆分
			 * */
			String[] itemArray = itemString.split("#");
			
			//JSONArray riskArray = new JSONArray();
			
			String modelID = ""; //模板号
			int sum = 0; //评估结果总分
			String sDate = StringFunction.getTodayNow();
			System.out.println("测试时间："+sDate);
			for (int i = 0; i < itemArray.length; i++) {
				//拆分函数，将前台传递的用“%”连接的模板号，指标号，分值号分别拆分
				String[] optionArray = itemArray[i].split("%");
				
				//通过模板号，指标号，分值号查询用户选择的每项的分值
				BizObjectQuery queryItem = managerItem
						.createQuery("select icv.value "
								+ "from jbo.trade.inf_card_value icv "
								+ "where icv.modelid = :modelid and icv.itemno = :itemno and icv.valueid= :valueid");
				queryItem.setParameter("modelid", optionArray[0]); //模板号
				queryItem.setParameter("itemno", optionArray[1]); //指标号
				queryItem.setParameter("valueid", optionArray[2]); //分值号

				modelID = optionArray[0];
				BizObject mScore = queryItem.getSingleResult(false); //用户选择的项的分值

				if (mScore != null) {

						JSONObject objOption = new JSONObject();
						objOption.put("Value", mScore.getAttribute("VALUE")
								.getValue() == null ? 0 : mScore
								.getAttribute("VALUE").getInt());
						sum += mScore.getAttribute("VALUE").getInt(); //分值叠加得出总分
						
				}
			}
			
			//保存评估记录
			BizObjectManager mRecordManager = jboItem.getManager("jbo.trade.inf_card_record");
			BizObjectQuery query = mRecordManager.createQuery("status=:status and userid=:userid");
            query.setParameter("status", "Y");
			query.setParameter("userid", sUserID);
			BizObject recordAccount = query.getSingleResult(true);
			//用户记录存在时，将历史记录状态置为“N”
			if(recordAccount !=null){
				recordAccount.setAttributeValue("Status", "N");
				mRecordManager.saveObject(recordAccount);
			}
			
			//记录用户最新评估记录
            BizObject bo = mRecordManager.newObject();
            bo.setAttributeValue("ModelID", modelID); //模板号
            bo.setAttributeValue("UserID", sUserID); //用户ID
            bo.setAttributeValue("Score", sum); //总分
            bo.setAttributeValue("TestTime", sDate); //测试时间
            bo.setAttributeValue("Status", "Y"); //状态
            mRecordManager.saveObject(bo);	
            
            
            
            
//          //根据模板号和用户ID查询测试ID
//            BizObjectQuery queryRecord = mRecordManager
//					.createQuery("select icr.testid "
//							+ "from jbo.trade.inf_card_record icr "
//							+ "where icr.modelid = :modelid and icr.userid= :userid and icr.status='Y'");
//            queryRecord.setParameter("Modelid", modelID); //模板号
//            queryRecord.setParameter("UserID", sUserID); //用户ID
//            
//           
//           BizObject testIDObj = queryRecord.getSingleResult(false);
//           String testID = testIDObj.getAttribute("TESTID").toString();
//           //System.out.println("测试号："+testID);
//           
//           BizObjectManager mDetailManager = jboItem.getManager("jbo.trade.inf_card_record_detail");
//	       BizObject boDetail = mDetailManager.newObject();
//           for(int j = 0; j < itemArray.length; j++){
//              	//拆分函数，将前台传递的用“%”连接的模板号，指标号，分值号分别拆分
//              	String[] optinArray = itemArray[j].split("%");
//              	
//              	//通过模板号，指标号，分值号查询用户选择的每项的分值
//   				BizObjectQuery querywItem = managerItem
//   						.createQuery("select icv.value "
//   								+ "from jbo.trade.inf_card_value icv "
//   								+ "where icv.modelid = :modelid and icv.itemno = :itemno and icv.valueid= :valueid");
//   				querywItem.setParameter("modelid", optinArray[0]); //模板号
//   				querywItem.setParameter("itemno", optinArray[1]); //指标号
//   				querywItem.setParameter("valueid", optinArray[2]); //分值号
//
//   				BizObject mScore = querywItem.getSingleResult(false); //用户选择的项的分值
//
//   				//记录用户风险评估的历史记录
//   				boDetail.setAttributeValue("TestID", testID); //测试号
//              	boDetail.setAttributeValue("ModelID", optinArray[0]); //模板号
//              	boDetail.setAttributeValue("ItemNo", optinArray[1]); //指标号
//              	boDetail.setAttributeValue("ValueID", optinArray[2]); //分值号
//              	boDetail.setAttributeValue("UserID", sUserID); //用户ID
//              	boDetail.setAttributeValue("Value", mScore.getAttribute("VALUE").getInt()); //分值
//              	//System.out.println("阿达缩放离开撒旦发三："+testID+"&&&&&&&" +molID+"&&&&&&&"+optinArray[1]+"&&&&&&&"+optinArray[2]+"&&&&&&&"+sUserID+"京东方"+mScore.getAttribute("VALUE").getInt());
//              	mDetailManager.saveObject(boDetail);              	
//              }
           
			result.put("sum", sum);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("riskreviews.error");
		}

	}
}
