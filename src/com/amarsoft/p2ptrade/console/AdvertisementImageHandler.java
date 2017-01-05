package com.amarsoft.p2ptrade.console;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletOutputStream;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.lang.StringX;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

/**
 * 广告图片服务
 * 输入参数：
 * 		BoardNo:	广告编号
 * 输出参数：
 * 		os:			广告图片二进制字节数组
 * @author dxu
 *
 */
public class AdvertisementImageHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		String boardNo = (String) request.get("BoardNo");
		if(StringX.isEmpty(boardNo)){
			throw new HandlerException("console.emptyboardno");
		}
		ByteArrayOutputStream os = null;
		InputStream inStream = null;
		byte [] s = null;
		try{
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.sys.ti_board_list");
			BizObjectQuery query = m.createQuery("BoardNo=:BoardNo");
			query.setParameter("BoardNo", boardNo);
			BizObject board = query.getSingleResult(false);
			if(board != null){
				String fileName = board.getAttribute("FILENAME").toString();
				String fileLocation = board.getAttribute("FILELOCATION").toString();
				
				if(StringX.isEmpty(fileName)){
					throw new HandlerException("console.imagenotexists");
				}
				if(StringX.isEmpty(fileLocation)){
					throw new HandlerException("console.imagenotexists");
				}
				
				File imageFile = new File(fileLocation);
				if(!imageFile.exists()){
					throw new HandlerException("console.imagenotexists");
				}
				
				os = new ByteArrayOutputStream();
				inStream = new FileInputStream(imageFile);

				int iContentLength = (int) imageFile.length();
				//修正死循环问题 xjzhao 2010/11/24
				if (iContentLength <= 0 || iContentLength > 1024*1000*25) {
					iContentLength = 1024*1000*25;
				}
				byte abyte0[] = new byte[iContentLength];
				int k = -1;
				while ((k = inStream.read(abyte0, 0, iContentLength)) != -1) {
					System.out.println("[FileViewServlet]Read:" + k);
					os.write(abyte0, 0, k);
				}
				s = os.toByteArray();
			}else{
				throw new HandlerException("console.boardnotexists");
			}
		} catch(HandlerException e){
			throw e;
		} catch(Exception e){
			throw new HandlerException("console.error");
		} finally{
			try {
				if(inStream != null){
					inStream.close();
				}
				if(os != null){
					os.flush();
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return s;
	}

}
