package com.amarsoft.p2ptrade.front;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.sun.org.apache.xml.internal.security.utils.Base64;

/**
 * app添加photo
 * 
 */
public class PhotoSaveHandler extends JSONHandler {
	

	@Override
	public Object createResponse(JSONObject request, Properties arg1) throws HandlerException{
		
		JSONObject result = new JSONObject();
		
		if(!request.containsKey("UserID")){
			throw new HandlerException("nouser.error");
		}
		String photoDatas = null;
		if (request.containsKey("PhotoDatas")) {
			photoDatas = request.get("PhotoDatas").toString();
		} else {
			throw new HandlerException("noPhotoDatas");
		}
		
		String sUserID = request.get("UserID").toString();
		
		JBOFactory jbo = JBOFactory.getFactory();
		
		String filepath = ARE.getProperty("PicPath");
		String serialnos  = "";
		try {
			BizObjectManager picm = jbo.getManager("jbo.trade.inf_picpath");
			ArrayList<byte[]> photoDatalist =  createPhotoDataList(photoDatas);
			for(byte[] b : photoDatalist){
				BizObject pico = picm.newObject();
				pico.setAttributeValue("filepath", "/photes");
				pico.setAttributeValue("inputtime", StringFunction.getTodayNow());
				pico.setAttributeValue("inputuserid", sUserID);
				picm.saveObject(pico);
				String picno = pico.getAttribute("picno").toString();
				String filename = "ind_"+picno +".jpg";
				pico.setAttributeValue("filename", filename);
				picm.saveObject(pico);
				
				File dir = new File(filepath+"/photes");
				if (!dir.exists())
					dir.mkdirs();
				
				filename = filepath+"/photes"+"/"+filename;
				//保存图片
				save(filename, b);
				
				serialnos += picno + ",";
			}


			String [] ser = serialnos.split(",");
			String spicno1 = "";
			String spicno2 = "";
			if(ser.length>1){
				spicno1 = ser[0];
				spicno2 = ser[1];
			}
			
			BizObjectManager authm = jbo.getManager("jbo.trade.user_authentication");
			BizObjectQuery authq = authm.createQuery("userid=:userid and idtype='Ind01'");
			authq.setParameter("userid", sUserID);
			BizObject autho = authq.getSingleResult(true);
			if(autho!=null){
				autho.setAttributeValue("picno1", spicno1);
				autho.setAttributeValue("picno2", spicno2);
				authm.saveObject(autho);
				result.put("sFlag", "S");
			}else{
				result.put("sFlag", "F");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// 保存文件
	public void save(String filename, byte [] b){
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			fos.write(b);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// 生成照片数据列表
	public ArrayList<byte[]> createPhotoDataList(String photodatas) throws Exception {
		String[] photoDatas = photodatas.split("\\,");
		ArrayList<byte[]> photoDatalist = new ArrayList<byte[]>();
		for (int i = 0; i < photoDatas.length; i++)
			photoDatalist.add(Base64.decode(photoDatas[i]));
		return photoDatalist;
	}
}
