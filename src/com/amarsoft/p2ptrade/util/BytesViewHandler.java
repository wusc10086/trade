package com.amarsoft.p2ptrade.util;

import java.io.*;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class BytesViewHandler extends JSONHandler {

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		if(request.containsKey("filePath")==false){
			throw new HandlerException("request.invalid");
		}
		byte[] result = null;
		String filePath = ((String)request.get("filePath"));
		ARE.getLog().info("filePath[BytesViewHandler]="+filePath);
		BufferedInputStream bis = null;
		java.io.ByteArrayOutputStream bos = null;
		try{
			bis = new BufferedInputStream(new FileInputStream(filePath));
			bos = new ByteArrayOutputStream();
			byte[] buff = new byte[2048];
			int bytesread;
			while (-1 != (bytesread = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesread);
			}
			result = bos.toByteArray();
			ARE.getLog().info("read file:" + filePath + "(size="+result.length+")");
		}
		catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("文件获取失败");
		}
		finally{
			try{
				if(bis!=null)bis.close();
				if(bos!=null)bis.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return result;
	}

}
