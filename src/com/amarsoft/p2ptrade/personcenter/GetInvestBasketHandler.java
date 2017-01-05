package com.amarsoft.p2ptrade.personcenter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 投资篮
 * 输入参数： UserID:账户编号 
 * 输出参数： ProjectNo:项目号 ProjectName:项目名 InputTime:操作时间PStatus:投资状态
 */
public class GetInvestBasketHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return getBindingBankCard(request);
	}

	/**
	 * 查询投资篮
	 * 
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getBindingBankCard(JSONObject request)
			throws HandlerException {
		if (request.get("UserID") == null || "".equals(request.get("UserID"))) {
			throw new HandlerException("common.emptyuserid");
		}
		String sUserID = request.get("UserID").toString();

		try {
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
			BizObjectManager m = jbo.getManager("jbo.trade.invest_hisrecord");

			BizObjectQuery query = m
					.createQuery("select pi.serialno,pi.projectname,pi.contractid,o.inputtime,pi.status from o,jbo.trade.project_info pi "+
			                     " where pi.serialno=o.projectid "+
							     " and o.userid=:userid order by o.inputtime desc");
			query.setParameter("userid", sUserID);
			List<BizObject> list = query.getResultList(true);

			if (list != null && list.size() != 0) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					String sProjectNo = o.getAttribute("serialno").toString();
					String sProjectName = o.getAttribute("projectname").toString();
					String sInputTime = o.getAttribute("inputtime").toString();
					String sStatus = o.getAttribute("status").toString();
					String sContractid = o.getAttribute("contractid").toString();


                    SimpleDateFormat myFmt3=new SimpleDateFormat("yyyy年MM月dd日 E HH时mm分ss秒 ");  

					sInputTime=myFmt3.format(new Date(sInputTime));

					sStatus = getUserContractStatus(jbo,sUserID,sContractid);
					obj.put("ProjectNo", sProjectNo);// 项目号
					obj.put("ProjectName", sProjectName);// 项目名称
					obj.put("Status", sStatus);// 项目投资状态
					obj.put("Inputtime", sInputTime);// 操作时间
					array.add(obj);	
				}
				result.put("investbasket", array);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("querybindingbankcard.error");
		}
	}
	//获取该项目用户投资状态
	private String getUserContractStatus(JBOFactory jbo, String sUserID,String sContractid){
		String sStatus = "";
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery q = m.createQuery("select status from o where userid=:userid and contractid=:contractid")
					.setParameter("userid", sUserID).setParameter("contractid", sContractid);
			BizObject o = q.getSingleResult(false);
			if(o!=null){
				sStatus = o.getAttribute("status")==null?"":o.getAttribute("status").toString(); 
			}else{
				sStatus = "6";
			}
			sStatus = getStatus(sStatus);
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return sStatus;
	}
	
	private String getStatus(String sCode){
		String sStatus = "";
		if("0".equalsIgnoreCase(sCode)){
			sStatus = "申请中";
		}else if("1".equalsIgnoreCase(sCode)){
			sStatus = "持有中";
		}else if("2".equalsIgnoreCase(sCode)){
			sStatus = "申请失败";
		}else if("3".equalsIgnoreCase(sCode)){
			sStatus = "已结束";
		}else{
			sStatus = "未支付";
		}
		return sStatus;
	}
}