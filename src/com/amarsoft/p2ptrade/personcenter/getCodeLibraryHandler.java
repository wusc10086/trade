package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.p2ptrade.tools.GeneralTools;

/**
 * 初始化银行卡详细信息
 * 输入参数： CodeNo:数据字典号
 * 输出参数： ItemNo:编号
 *        ItemName:名称
 */
public class getCodeLibraryHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return getCodeLibraryList(request);
	}

	/**
	 * 查询数据字典
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getCodeLibraryList(JSONObject request)
			throws HandlerException {
			if (request.get("codeNo") == null || "".equals(request.get("codeNo"))) {
				throw new HandlerException("common.emptycodeno");
			}
			String sCodeNo = request.get("codeNo").toString();
			String sRemark = request.get("Remark").toString();
			String sSql="";

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
			BizObjectManager m = jbo.getManager("jbo.trade.code_library");
			sSql="codeno=:codeno and isinuse=:isinuse ";
			if(sRemark!=null&&!sRemark.equals("")){
				sSql+=" and remark like '%"+sRemark+"%' ";
			}	
			sSql+=" order by bankno,itemno  ";
			BizObjectQuery query = m
					.createQuery(sSql);
			query.setParameter("codeno", sCodeNo);
			query.setParameter("isinuse", "1");
			
			List<BizObject> list = query.getResultList(true);

			if (list != null && list.size() != 0) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					String sItemNo = o.getAttribute("ItemNo")==null?"":o.getAttribute("ItemNo").toString();
					String sItemName = o.getAttribute("ItemName")==null?"":o.getAttribute("ItemName").toString();
					obj.put("itemNo", sItemNo);// 编号
					obj.put("itemName", sItemName);// 名称
					array.add(obj);	
					
				}
				result.put("codelist", array);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("codelibrary.error");
		}
	}
}