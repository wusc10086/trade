package com.amarsoft.p2ptrade.personcenter;

import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.jbo.ql.Parser;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
/** 
 * 个人邀请统计
 * 输入参数：
 * 		UserID:账户编号
 *
 */
public class UserInviteHandler extends JSONHandler {

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {

		Parser.registerFunction("sum");
		Parser.registerFunction("count");
		return getAcount(request);		
	}
	  
	/**
	 * @param request
	 * @return
	 * @throws HandlerException
	 */
	private JSONObject getAcount(JSONObject request)throws HandlerException {
		
		JSONObject result = new JSONObject();
		//参数校验
		if(request.get("UserID")==null || "".equals(request.get("UserID"))){
			throw new HandlerException("common.emptyuserid");
		}

		String sUserID = request.get("UserID").toString();
		
		JBOFactory jbo = JBOFactory.getFactory();
		try {
			BizObjectManager m = jbo.getManager("jbo.trade.user_account");
			BizObjectQuery q = m.createQuery(" select username,phonetel,invitecode from o where UserID=:UserID");
			q.setParameter("UserID", sUserID);
			BizObject o = q.getSingleResult(false);
			String username = "";
			String phonetel = "";
			String invitecode = "";
			if(o!=null){
				username = o.getAttribute("username").toString();
				phonetel = o.getAttribute("phonetel").toString();
				invitecode = o.getAttribute("invitecode").toString()==null?"null":o.getAttribute("invitecode").toString();
			}
			if(invitecode.length()<1)
				invitecode = "null";
			//邀请码
			result.put("invitecode", invitecode);
			BizObjectQuery qq = m.createQuery("select o.userid,o.username,a.inputtime,o.phonetel from o,jbo.trade.account_info a where o.userid=a.userid and o.usercode in (:username,:phonetel,:invitecode) and a.status='2'");
						   qq.setParameter("username", username).setParameter("phonetel", phonetel).setParameter("invitecode", invitecode);
			List <BizObject> list = qq.getResultList(false);
			int count = list.size();
			//邀请人数
			result.put("count", count);
//			BizObject objc = m.createQuery("select count(*) as v.vcount from o where o.usercode in (:username,:phonetel,:invitecode) and UserAuthFlag='2' and userid in ( select userid from account_info where status='2')")
//				.setParameter("username", username).setParameter("phonetel", phonetel).setParameter("invitecode", invitecode)
//				.getSingleResult(false);
//			if(objc==null)
//				result.put("count", 0);
//			else
//				result.put("count", objc.getAttribute("vcount").getInt());
			JSONArray arry = new JSONArray();
			for(BizObject b : list){
				JSONObject json = new JSONObject();
				json.put("userid", b.getAttribute("userid").toString());
				json.put("username", b.getAttribute("username").toString());
				String phone = b.getAttribute("phonetel").toString();
				json.put("phonetel", phone.substring(0, 3)+"****"+phone.substring(7, 10));
				json.put("inputtime", b.getAttribute("inputtime").toString());
				arry.add(json);
			}
			//邀请列表
			result.put("userlist", arry);
			
			//邀请收益
			BizObjectManager mm = jbo.getManager("jbo.trade.acct_transfer");
			//已经收益
			BizObjectQuery q1 = mm.createQuery("select sum(amount) as v.count from o where objecttype in ('030','040') and status='10' and userid=:userid");
			q1.setParameter("userid", sUserID);
			BizObject o1 = q1.getSingleResult(false);
			if(o1!=null){
				result.put("sum",o1.getAttribute("count").getDouble());
			}else 
				result.put("sum",0);
			//待收益
			BizObjectQuery q2 = mm.createQuery("select sum(amount) as v.count from o where objecttype in ('030','040') and status in ('01','99') and userid=:userid");
			q2.setParameter("userid", sUserID);
			BizObject o2 = q2.getSingleResult(false);
			if(o1!=null){
				result.put("acctsum",o2.getAttribute("count").getDouble());
			}else
				result.put("acctsum",0);
			
			
			//成功投资笔数和收益
			BizObjectQuery q4 = mm.createQuery("select count(userid) as v.count,sum(amount) as v.sum from o,jbo.trade.user_account uc where o.objecttype in ('040') and o.status='10' and o.userid=:userid and uc.userid=o.userid and uc.UserAuthFlag='2'");
			q4.setParameter("userid", sUserID);
			BizObject o4 = q4.getSingleResult(false);
			if(o1!=null){
				result.put("investsum",o4.getAttribute("sum").getDouble());
				result.put("investcount",o4.getAttribute("count").getInt());
			}else{
				result.put("investsum",0);
				result.put("investcount",0);				
			}
		} catch (JBOException e) {
			e.printStackTrace();
		}
		return result;
	}
}