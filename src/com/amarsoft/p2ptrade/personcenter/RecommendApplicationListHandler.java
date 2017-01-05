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
/**
 * 推荐客户列表
 * 输入参数：
 * 		RrecommendOrgUser:推荐用户
 * 输出参数： 
 *      APPLYNO：贷款编号
 *      PROJECTNAME：贷款标题
 *      FUNDSOURCE:贷款类型
 *      BusinessSum：贷款金额
 *      RecommendTime：推荐时间 
 *      applyStatus：贷款状态
 */
public class RecommendApplicationListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfoSum(request);
	}
	
	/**
	 * 点击推荐客户
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfoSum(JSONObject request)throws HandlerException {
		if(request.get("recommenderid")==null || "".equals(request.get("recommenderid"))){
			throw new HandlerException("userid.error");
		}
		//获取用户号
		String recommenderid = request.get("recommenderid").toString();
		
		//获取pageSize每页的条数、curPage当前所在页
				if(request.containsKey("pageSize"))
					this.pageSize = Integer.parseInt(request.get("pageSize").toString());
				if(request.containsKey("pageNo"))
					this.curPage = Integer.parseInt(request.get("pageNo").toString());
		 
		
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m =jbo.getManager("jbo.trade.loan_apply");
            //查询结果集
			BizObjectQuery query = m.createQuery(" recommenderid=:recommenderid and isrecommend=:isrecommend");
			query.setParameter("recommenderid",recommenderid).setParameter("isrecommend","1");
			 //分页
            int totalAcount = query.getTotalCount();
    		int pageCount = (totalAcount + pageSize - 1) / pageSize;
    		if (curPage > pageCount)
    			curPage = pageCount;
    		if (curPage < 1)
    			curPage = 1;
    		query.setFirstResult((curPage - 1) * pageSize);
    		query.setMaxResults(pageSize);
    		
			List<BizObject> list = query.getResultList(false);
			if(list != null){
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					String applyno = o.getAttribute("applyno")==null?"":o.getAttribute("applyno").toString();//贷款编号
					String projectname = o.getAttribute("projectname")==null?"无":o.getAttribute("projectname").toString();//贷款标题
					String fundsource = o.getAttribute("fundsource")==null?"":o.getAttribute("fundsource").toString();//贷款类型
					double businesssum = Double.parseDouble(o.getAttribute("businesssum")==null?"0":o.getAttribute("businesssum").toString());//贷款金额
					String applytime = o.getAttribute("applytime")==null?"":o.getAttribute("applytime").toString();//推荐时间
					String applystatus = o.getAttribute("applystatus")==null?"":o.getAttribute("applystatus").toString();//贷款状态
	
					
					obj.put("applyno", applyno);//贷款编号
					obj.put("projectname", projectname);//贷款标题
					obj.put("fundsource", getCodeName(jbo,"LoanType",fundsource));//贷款类型
					obj.put("businesssum", businesssum);//贷款金额
					obj.put("applytime", applytime);//推荐时间
					obj.put("applystatus", applystatus);//推荐时间
					array.add(obj);
				}     
				
				result.put("TotalAcount", String.valueOf(totalAcount));
				result.put("curPage", String.valueOf(curPage));
				result.put("pagesize", String.valueOf(pageSize));
				result.put("array", array);
			}
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryapplicationlist.error");
		}
	}



/**
 * 获取字典名称
 * 
 * @param accountBo
 * @throws HandlerException
 */
private String getCodeName(JBOFactory jbo, String sCodeNo,String sItemNo)
		throws HandlerException {
		BizObjectManager m;
		try {
			m = jbo.getManager("jbo.trade.code_library");
			BizObjectQuery query = m.createQuery("CodeNo=:CodeNo and ItemNo=:ItemNo");
			query.setParameter("CodeNo", sCodeNo).setParameter("ItemNo", sItemNo);
			BizObject o = query.getSingleResult(false);
			if (o != null) {
				return o.getAttribute("ItemName").getString();
				 
			} else {
				return "";
			}
		} catch (Exception e) {
			// throw new HandlerException("quaryphonetel.error");
			return "";
		}
	}
}
