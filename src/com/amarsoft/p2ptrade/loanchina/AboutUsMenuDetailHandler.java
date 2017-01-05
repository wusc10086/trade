package com.amarsoft.p2ptrade.loanchina;

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

/*
 * @aboutUsMenuDetail 帮助中心、关于我们
 * 输入：
 * classify 模块编号
 * 
 * 
 * 输出：
 * menu_array 存放栏目信息 包含栏目编号itemno、栏目名称itemname以及对应栏目的页面内容leftpagelist  JSONArray
 * culumn_array 存放类别classify、页面内容helplist  JSONArray
 * content_array 存放标题title、内容content1  JSONArray
 * result 返回最终的JSON对象
 * 
 */

public class AboutUsMenuDetailHandler extends JSONHandler{
	private String classify;
	//注册sql中用到的函数
		static{
			Parser.registerFunction("substr");
		}
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return selectGuideDetail(request);
	}

	private JSONObject selectGuideDetail(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		//获取模块编号
		if(request.containsKey("classify")){
			if (request.get("classify")!=null) {
				this.classify = request.get("classify").toString();	
			}
		}	
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_catalog");
			//取栏目信息
			BizObjectQuery menu_query = m.createQuery("length(itemno)=6 and substr(itemno,0,3)=:itemno and isinuse='1' order by sortno");
			menu_query.setParameter("itemno", classify);
			List<BizObject> list = menu_query.getResultList(false);
			JSONArray menu_array = new JSONArray();//放栏目信息
			for(BizObject o : list){
				JSONObject obj = new JSONObject();
				//获取menu
				String menu_itemno = o.getAttribute("itemno").toString()==null ? "" : o.getAttribute("itemno").toString();//栏目编号
				obj.put("itemno",menu_itemno);
				obj.put("itemname",o.getAttribute("itemname").toString()==null ? "" : o.getAttribute("itemname").toString());//栏目名称						
				//获取分类
				BizObjectQuery culumn_query = m.createQuery("length(itemno)=9 and substr(itemno,0,6)=:itemno and isinuse='1' order by sortno");
				culumn_query.setParameter("itemno", menu_itemno);
				List<BizObject> culumn_list =culumn_query.getResultList(false);
				JSONArray culumn_array = new JSONArray();//放分类信息
				if(culumn_list.size()>0){//有分类	
					for(BizObject culumn_o : culumn_list){
						//保存分类
						String classifyno = culumn_o.getAttribute("itemno").toString()==null ? "" : culumn_o.getAttribute("itemno").toString();//分类编号
						String classifyname = culumn_o.getAttribute("itemname").toString()==null ? "" : culumn_o.getAttribute("itemname").toString();//分类名称	
						
						//根据分类查询帮助信息
						JSONObject helplist = getHelpList(classifyno);
						helplist.put("classify", classifyname);
						culumn_array.add(helplist);
					}
				}else{//无分类
					//根据栏目查询帮助信息
					JSONObject obj1 = getHelpList(menu_itemno);	
					obj1.put("classify", null);
					culumn_array.add(obj1);
				}
				//记录页面信息
				obj.put("leftpagelist", culumn_array);
				//记录菜单信息
				menu_array.add(obj);
			 }
			 result.put("menulist", menu_array);//返回最终的JSON对象
			return result;
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	/**
	 * 查询帮助信息列表
	 * */
	private JSONObject  getHelpList (String classify) throws JBOException{
		JSONObject object = new JSONObject();
		JBOFactory f = JBOFactory.getFactory();
		List<BizObject> list = f.getManager("jbo.trade.inf_help").createQuery("classify=:classify and status='Y'").setParameter("classify",classify).getResultList(false);
		JSONArray content_array = new JSONArray();//放内容信息
		for(BizObject o : list){
			JSONObject help = new JSONObject();
			help.put("title", o.getAttribute("title").toString());
			help.put("content1", o.getAttribute("content1").toString());
			content_array.add(help);
		}
		object.put("helplist", content_array);
		return object;
	}
}
