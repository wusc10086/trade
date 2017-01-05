package com.amarsoft.p2ptrade.personcenter;
/*
 * 
 * */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import javax.mail.search.FromStringTerm;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;
import com.sun.xml.fastinfoset.util.StringArray;

public class RiskReviewsHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		return getRiskReviewsInfo(request);
	}

	/**
	 * 风险评估信息查询
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getRiskReviewsInfo(JSONObject request) throws HandlerException {
		
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("userid.error");
		}

		String sUserID = request.get("UserID").toString();

		try {
			JBOFactory jboItem = JBOFactory.getFactory();
			JSONObject result = new JSONObject();

			BizObjectManager managerItem = jboItem
					.getManager("jbo.trade.inf_card_catalog");

			BizObjectQuery queryItem = managerItem
					.createQuery("select icm.modelid,icm.itemno,icm.itemname "
							+ "from jbo.trade.inf_card_catalog icc,jbo.trade.inf_card_model icm "
							+ "where icc.modelid = icm.modelid and icc.status = 'Y' and icm.status= 'Y' order by icm.itemno");
			
			List<BizObject> listItem = queryItem.getResultList(false);
			
			if (listItem != null) {
				JSONArray itemArray = new JSONArray();
				for (int i = 0; i < listItem.size(); i++) {

					BizObject oItem = listItem.get(i);

					JSONObject objItem = new JSONObject();
					objItem.put("rowid", i);
					objItem.put("ModelID", oItem.getAttribute("MODELID").getValue() == null ? ""
									: oItem.getAttribute("MODELID").toString()); //模板号
					objItem.put("ItemNo", oItem.getAttribute("ITEMNO").getValue() == null ? ""
									: oItem.getAttribute("ITEMNO").toString()); //指标号
					objItem.put("ItemName", oItem.getAttribute("ITEMNAME").getValue() == null ? ""
									: oItem.getAttribute("ITEMNAME").toString()); //指标名称
					objItem.put("optionArray", getOptionInfo(objItem.get("ModelID"), objItem.get("ItemNo"))); //分值号，分值名称
					itemArray.add(objItem);
				}

				result.put("itemArray", itemArray);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("riskreviews.error");
		}

	}

	//该方法用于获得每个指标中具体分值的内容（ValueName）及分值ID（VALUE）
	@SuppressWarnings("unchecked")
	private JSONArray getOptionInfo(Object obj1, Object obj2) throws HandlerException{

		try {
			JBOFactory jboOption = JBOFactory.getFactory();
			JSONObject resultOption = new JSONObject();
			
			BizObjectManager managerOption = jboOption
					.getManager("jbo.trade.inf_card_value");

			//通过模板号与指标号查询具体分值信息
			BizObjectQuery queryOption = managerOption
					.createQuery("select icv.valueid,icv.valuename "
							+ "from jbo.trade.inf_card_value icv "
							+ "where icv.modelid=:modelid and icv.itemno=:itemno order by icv.valueid");

			queryOption.setParameter("modelid", obj1.toString());
			queryOption.setParameter("itemno", obj2.toString());

			List<BizObject> listOption = queryOption.getResultList(false);
			JSONArray optionArray = new JSONArray();
			if (listOption != null) {
				
				for (int j = 0; j < listOption.size(); j++) {
					BizObject oOption = listOption.get(j);

					JSONObject objOption = new JSONObject();
					
					objOption.put("ValueID", oOption.getAttribute("VALUEID").getValue()==null ? ""
							: oOption.getAttribute("VALUEID").toString()); //分值ID
					objOption.put("ValueName", oOption.getAttribute("VALUENAME").getValue()==null ? ""
							: oOption.getAttribute("VALUENAME").toString()); //分值名称
					
					optionArray.add(objOption);
				}
			}
			return optionArray;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("riskreviewsOption.error");
		}
	}

}
