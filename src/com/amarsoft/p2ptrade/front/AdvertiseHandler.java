package com.amarsoft.p2ptrade.front;

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
 * 根据广告位置获取广告内容
 * */
public class AdvertiseHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties property)
			throws HandlerException {
		return getAdvertiseInfo(request);
		}
		
		private JSONObject getAdvertiseInfo(JSONObject request) throws HandlerException{
			JSONObject result = new JSONObject();
			try{
				String positionid = (String) request.get("positionid");
				JBOFactory jbo = JBOFactory.getFactory();
				BizObjectManager m =jbo.getManager("jbo.trade.inf_advertise");
				
				BizObjectQuery query = m.createQuery("select o.title,o.remark,o.linkurl,o.picno,c.adtype,c.adcount,c.adwidth,c.adheight from o,jbo.trade.inf_ad_position c where o.positionid=c.positionid and o.positionid=:positionid and o.isshow='Y' and o.chkstatus='1' order by picno desc");
				query.setParameter("positionid", positionid);
				List<BizObject> objectlist = query.getResultList(false);
				
				
				JSONArray array = new JSONArray();
				int count = 0;
				int index_s =100;
				for(BizObject o : objectlist){
					index_s--;
					count ++;
					//广告数量超过需求 则推出循环...
					int adcount = o.getAttribute("adcount").getInt();
					if(count>adcount) break;
					JSONObject object = new JSONObject();
					
					String picno = o.getAttribute("picno")==null?"":o.getAttribute("picno").toString();
					picno = getFileName(picno);
					object.put("positionid", positionid);
					object.put("title", o.getAttribute("title")==null?"":o.getAttribute("title").toString());
					object.put("remark", o.getAttribute("remark")==null?"":o.getAttribute("remark").toString());
					object.put("linkurl", o.getAttribute("linkurl")==null?"":o.getAttribute("linkurl").toString());
					object.put("picno", picno);
					result.put("adtype", o.getAttribute("adtype")==null?"":o.getAttribute("adtype").toString());
					result.put("width", o.getAttribute("adwidth")==null?"":o.getAttribute("adwidth").toString());
					result.put("height", o.getAttribute("adheight")==null?"":o.getAttribute("adheight").toString());
					object.put("index_s", index_s);
					object.put("count", count);
					array.add(object);
				}

				result.put("result", array);
			}catch(Exception e){
				e.printStackTrace();
				throw new HandlerException("");
			}
			return result;
		}

		private String getFileName(String picno){
			String filename = "";
			JBOFactory jbo = JBOFactory.getFactory();
			try {

				BizObjectManager m = jbo.getManager("jbo.trade.inf_picpath");
				//获取图片
	            BizObjectQuery query = m.createQuery("picno=:picno");
	            query.setParameter("picno", picno);
	            BizObject o = query.getSingleResult(false);

				if(o!=null){
					String filepath = o.getAttribute("filepath")==null?"":o.getAttribute("filepath").toString();
					filename = o.getAttribute("filename")==null?"":o.getAttribute("filename").toString();
					if(filename.startsWith("/"))
						filename = filepath + filename;
					else
						filename = filepath + "/" + filename;
	 			}
			} catch (JBOException e) {
				e.printStackTrace();
			}
			return filename;
		}
}
