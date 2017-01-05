package com.amarsoft.p2ptrade.mobileapp;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * APP版本信息（只获取最新版本的信息）
 * 
 * 输入：DeviceType:Android(默认);iOS
 * 
 * @author hhCai 2015-3-16
 */
public class APPEditionHandler extends JSONHandler {

	@SuppressWarnings("unchecked")
	@Override
	public Object createResponse(JSONObject arg0, Properties arg1)
			throws HandlerException {
		// TODO Auto-generated method stub
		String device = arg0.containsKey("DeviceType") ? "" : arg0.get(
				"DeviceType").toString();
		if (device.toLowerCase().equals("android")) {
			device = "01";
		} else if (device.toLowerCase().equals("ios")) {
			device = "02";
		} else {
			device = "01";
		}

		String appsql = "select o.* from o where o.EditionType='" + device
				+ "' Order By o.InputTime Desc";// 查询语句
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.app_edition");
			// APP广告位的记录
			BizObjectQuery query = m.createQuery(appsql);
			// 将申请贷款中获取的记录放到json对象中
			List<BizObject> list = query.getResultList(false);
			if (list != null) {
				JSONObject jsonObject = new JSONObject();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					if (o != null) {
						jsonObject
								.put("editionno", o.getAttribute("editionno")
										.toString() == null ? "" : o
										.getAttribute("editionno").toString());// 最新版本号
						jsonObject
								.put("editioninfo",
										o.getAttribute("editioninfo")
												.toString() == null ? "有新版哦，是否更新？"
												: o.getAttribute("editioninfo")
														.toString());// 新版本信息
						String update = o.getAttribute("isupdate").toString() == null ? "NO"
								: o.getAttribute("isupdate").toString();
						if (update.equals("1")) {
							update = "YES";
						} else
							update = "NO";
						jsonObject.put("isupdate", update);// 是否必须更新，NO-不需要，YES-需要
						break;
					}

				}
				return jsonObject;

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return new HandlerException("NoNew");
	}
}
