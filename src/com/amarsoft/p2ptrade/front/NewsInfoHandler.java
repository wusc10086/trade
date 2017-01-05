


package com.amarsoft.p2ptrade.front;

import java.io.BufferedInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import java.sql.Blob;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 资讯详情
 * */

public class NewsInfoHandler extends JSONHandler{
	
	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		
		return selectInvest(request);
	}

	private JSONObject selectInvest(JSONObject request) throws HandlerException {
		JSONObject result = new JSONObject();
		String newsid = request.get("newsid").toString();
		try {
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.inf_news");
			//获取新闻的所有条数
            BizObjectQuery query = m.createQuery("select title,author,inputtime from o where isshow='Y' and serialno=:serialno");
            query.setParameter("serialno", newsid);
			BizObject o = query.getSingleResult(false);
			if(o!=null){
				
				result.put("title",o.getAttribute("title")==null ? "" : o.getAttribute("title").toString());
				result.put("author",o.getAttribute("author")==null ? "" : o.getAttribute("author").toString());
				result.put("source",o.getAttribute("source")==null ? "" : o.getAttribute("source").toString());
				result.put("inputtime",o.getAttribute("inputtime")==null ? "" : o.getAttribute("inputtime").toString());
				
				Connection conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					conn = ARE.getDBConnection("als");
					ps = conn.prepareStatement("select content from inf_news where serialno = ?");
					ps.setString(1, newsid);
					rs = ps.executeQuery();

					if(rs.next()){
						java.sql.Blob blob = ( java.sql.Blob)rs.getBlob(1);   
						byte[] msgContent = blobToBytes(blob); //Blob转化为字节数组						
						result.put("content",new String(msgContent,"GBK"));
					}
					
					// sb.toString());
					rs.close();
					ps.close();
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					try {
						if(rs!=null)
							rs.close();
						if(ps!=null)
							ps.close();
						if(conn!=null)
							conn.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				return result;
 			}else{
				throw new HandlerException("default.database.error");
			}
			
		} catch (JBOException e) {
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}
	}
	
	//将Blob转化为Bytes
		private byte[] blobToBytes( java.sql.Blob blob) {
			if(blob==null) return  null;
			BufferedInputStream is =null;
			byte[] bytes=null;
			try {
				is=new BufferedInputStream(blob.getBinaryStream());
				bytes=new byte[(int) blob.length()];
				int len=bytes.length;
				int offset=0;
				int read=0;
				while((read=is.read(bytes,offset,len-offset))==0){
					offset+=read;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}		
			return bytes;
		}
		private void printBytes(byte[] bytes) {
			String hexString="0123456789ABCDEF";
			StringBuilder sb=new StringBuilder(bytes.length*2);
			// 将字节数组中每个字节拆解成2位16进制整数
			for(int i=0;i<bytes.length;i++)
			{
				if(i>1 && i % 16 ==0)sb.append("\n");;
				sb.append(hexString.charAt((bytes[i]&0xf0)>>4));
				sb.append(hexString.charAt((bytes[i]&0x0f)>>0));
				sb.append(" ");
				
			}
			System.out.println( sb.toString().toLowerCase());
		}
}
