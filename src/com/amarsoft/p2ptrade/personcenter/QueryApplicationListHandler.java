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
 * 点击申请借款
 * 输入参数：
 * 		UserID:用户 名
 * 输出参数： 
 *      PROJECTNAME借款标题
 *      LOANAMOUNT借款金额
 *      LOANRATE:年利率
 *      LOANTERM借款期限
 *      STATUS状态  
 */
public class QueryApplicationListHandler extends JSONHandler {
	private int pageSize = 10;
	private int curPage = 0;
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
	
		return getLoanInfoSum(request);
	}
	
	/**
	 * 点击申请借款
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getLoanInfoSum(JSONObject request)throws HandlerException {
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("userid.error");
		}
		
		if(request.containsKey("PageSize"))
			this.pageSize = Integer.parseInt(request.get("PageSize").toString());
		if(request.containsKey("CurPage"))
			this.curPage = Integer.parseInt(request.get("CurPage").toString());
		 
		String sUserID = request.get("UserID").toString();
		
		try{ 
			JBOFactory jbo = JBOFactory.getFactory();
			JSONObject result = new JSONObject();
            BizObjectManager m =jbo.getManager("jbo.trade.ti_business_apply");
            
			BizObjectQuery query = m.createQuery(" userid=:userid and status in ('1','2','3') ");
			query.setParameter("userid",sUserID);
			int firstRow = curPage * pageSize;
			if(firstRow < 0){
				firstRow = 0;
			}
			int maxRow = pageSize;
			if(maxRow <= 0){
				maxRow = 10;
			}
			query.setFirstResult(firstRow);
			if(request.containsKey("PageSize"))
				query.setMaxResults(maxRow);
			
			int totalAcount = query.getTotalCount();
			int temp = totalAcount % pageSize;
			int pageCount = totalAcount / pageSize;
			if(temp != 0){
				pageCount += 1;
			}
			List<BizObject> list = query.getResultList(false);
			if(list != null){
				JSONArray array = new JSONArray();
				for (int i = 0; i < list.size(); i++) {
					BizObject o = list.get(i);
					JSONObject obj = new JSONObject();
					String sProjectName = o.getAttribute("PROJECTNAME").toString()==null?
									"0":o.getAttribute("PROJECTNAME").toString();
					double sLoanAmount = Double.parseDouble(
							o.getAttribute("LOANAMOUNT").toString()==null?
									"0":o.getAttribute("LOANAMOUNT").toString());
					double sLoanRate = Double.parseDouble(
							o.getAttribute("LOANRATE").toString()==null?
									"0":o.getAttribute("LOANRATE").toString());
					int sLoanTerm = o.getAttribute("LoanTerm")==null?
									0:o.getAttribute("LoanTerm").getInt();
					String sStatusCode = o.getAttribute("STATUS").toString()==null?
							"":o.getAttribute("STATUS").toString();
					String sStatus = "";
					if("1".equals(sStatusCode)){
						sStatus = "申请中";
					}else if("2".equals(sStatusCode)){
						sStatus = "审核未通过";
					}else if("3".equals(sStatusCode)){
						sStatus = "审核通过";
					}
					obj.put("ProjectName", sProjectName);//借款标题
					obj.put("LoanAmount", sLoanAmount);//借款金额
					obj.put("LoanRate", sLoanRate);//年利率
					obj.put("LoanTerm", sLoanTerm);//借款期限
					obj.put("Status", sStatus);//状态
					array.add(obj);
				}     
				result.put("RootType", "030");
				result.put("TotalAcount", String.valueOf(totalAcount));// 该条件下从数量
				result.put("PageCount", String.valueOf(pageCount));
				result.put("array", array);
			}
			return result;
		} 
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("queryapplicationlist.error");
		}
	}
}
