package com.amarsoft.p2ptrade.console;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.lang.StringX;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 查询公告信息
 * 输入参数：
 * 		BoardType:		公告类型
 * 		AdvertLocation:	广告位
 *  	CurPage   0开始
 *		PageSize
 * 输出参数：
 * 		BoardArray:		公告信息列表
 * @author dxu
 *
 */
public class QueryBoardHandler extends JSONHandler {
	

	private int pageSize =-1;
	private int curPage = 0;
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String boardType = (String) request.get("BoardType");
		String advertLocation = (String) request.get("AdvertLocation");
		
		if(StringX.isEmpty(boardType)){
			throw new HandlerException("console.emptyboardtype");
		}
		
		if("3".equals(boardType) && StringX.isEmpty(advertLocation)){
			throw new HandlerException("console.emptylocation");
		}
		
		if (request.containsKey("CurPage") && request.get("CurPage")!=null) {
			curPage = ((Long)request.get("CurPage")).intValue();
		}
		if (request.containsKey("PageSize") && request.get("PageSize")!=null) {
			pageSize = ((Long)request.get("PageSize")).intValue();
		}
		
		
		JSONObject result  = new JSONObject();
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.sys.ti_board_list");
			BizObjectQuery query = null;
			if(!"3".equals(boardType)){
				if(boardType.equals("1,2")){
					query = m.createQuery("BoardType in (:b1,:b2) and IsPublish='1' and Status='1' and PublishDate<=:SysDate order by PublishDate desc");
					query.setParameter("b1", "1");
					query.setParameter("b2", "2");
					
				}
				else{
					query = m.createQuery("BoardType=:BoardType and IsPublish='1' and Status='1' and PublishDate<=:SysDate order by PublishDate desc");
					query.setParameter("BoardType", boardType);
				}
				
				query.setParameter("SysDate", StringFunction.getToday());
			}else{
				query = m.createQuery("BoardType=:BoardType and ADLocation=:ADLocation and IsPublish='1' and Status='1' and PublishDate<=:SysDate order by PublishDate desc");
				query.setParameter("BoardType", boardType);
				query.setParameter("ADLocation", advertLocation);
				query.setParameter("SysDate", StringFunction.getToday());
			}
			
			int firstRow = curPage * pageSize;
			if(firstRow < 0){
				firstRow = 0;
			}
			
			
			query.setFirstResult(firstRow);
			if(pageSize>0)
				query.setMaxResults(pageSize);
			result.put("TotalCount", query.getTotalCount());
			List boards = query.getResultList(false);
			if(boards == null){
				throw new HandlerException("console.boardnotfound");
			}
			JSONArray array = new JSONArray();
			for(Iterator it = boards.iterator();it.hasNext();){
				BizObject board = (BizObject) it.next();
				if(!"1".equals(board.getAttribute("STATUS").getString())){
					continue;
				}
				JSONObject boardJSON = new JSONObject();
				boardJSON.put("BoardNo", toString(board.getAttribute("BOARDNO").getString()));
				boardJSON.put("BoardType", toString(board.getAttribute("BoardType").getString()));
				boardJSON.put("BoardName", toString(board.getAttribute("BOARDNAME").getString()));
				boardJSON.put("BoardTitle", toString(board.getAttribute("BOARDTITLE").getString()));
				boardJSON.put("BoardDesc", toString(board.getAttribute("BOARDDESC").getString()));
				boardJSON.put("IsNew", toString(board.getAttribute("ISNEW").getString()));
				boardJSON.put("IsEject", toString(board.getAttribute("ISEJECT").getString()));
				boardJSON.put("ADLink", toString(board.getAttribute("ADLINK").getString()));
				boardJSON.put("PublishDate", toString(board.getAttribute("PUBLISHDATE").getString()));
				boardJSON.put("IsPublish", toString(board.getAttribute("ISPUBLISH").getString()));
				array.add(boardJSON);
			}
			result.put("BoardArray", array);
		} catch(HandlerException e){
			e.printStackTrace();
			throw e;
		} catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("console.error");
		}
		return result;
	}
	
	private static String toString(String s){
		if(s == null){
			return "";
		}else{
			return s;
		}
	}

}
