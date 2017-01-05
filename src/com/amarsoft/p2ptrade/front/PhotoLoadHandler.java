package com.amarsoft.p2ptrade.front;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONArray;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.ServiceFactory;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;
import com.amarsoft.mobile.webservice.security.Base64;

/**
 * 图片保存处理类
 * 
 * 输入参数： BizType:业务类型（客户管理模块-CustomerManagement，业务申请—BusinessApply，类推），必输；
 * ObjectNo-对象编号（若是客户管理则为客户编号；若为业务申请则为业务编号；类推），必输;
 * ObjectType-对象类型，若为客户管理可为客户类型，默认同BizType,非必输；
 * 
 */
public class PhotoLoadHandler extends JSONHandler {

	/**
	 * 业务类型
	 */
	private String bizType;
	/**
	 * 对象编号
	 */
	private String objectNo;
	/**
	 * 对象类型（默认与bizType一致）
	 */
	private String objectType;
	private String sUserID;

	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException{

		if (request.containsKey("UserID")) {
			sUserID = request.get("UserID").toString();
		} else {
			throw new HandlerException("缺少参数UserID");
		}
		if (request.containsKey("BizType")) {
			bizType = request.get("BizType").toString();
		} else {
			throw new HandlerException("缺少参数BizType");
		}

		if (request.containsKey("ObjectNo")) {
			objectNo = request.get("ObjectNo").toString();
		} else {
			throw new HandlerException("缺少参数ObjectNo");
		}

		if (bizType == null || bizType.trim().length() == 0)
			throw new HandlerException("参数BizType错误");

		if (objectNo == null || objectNo.trim().length() == 0)
			throw new HandlerException("参数ObjectNo错误");

		if (request.containsKey("ObjectType")) {
			objectType = request.get("ObjectType").toString();
		} else {
			objectType = bizType;
		}

		JSONObject result = new JSONObject();
		try {
//			String sSqlPhoto = "select PI.photodesc as PhotoDesc,address as Address,LATITUDE as Latitude,LONGITUDE as Longitude,PI.PhotoPath as PhotoPath"
//					+ " from photo_info PI "
//					+ " WHERE PI.ObjectNo ='"
//					+ objectNo
//					+ "' and PI.ObjectType = '"
//					+ objectType
//					+ "' and PI.BizType = '"
//					+ bizType
//					+ "' and INPUTUSER='"
//					+ userId + "'";
//			PreparedStatement psPhoto = conn.prepareStatement(sSqlPhoto);
//			ARE.getLog().debug(sSqlPhoto);
//			ResultSet rsPhoto = psPhoto.executeQuery();
			
			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.photo_info");
			BizObjectQuery q = m.createQuery("ObjectNo=:ObjectNo and ObjectType=:ObjectType and BizType=:BizType and INPUTUSER=:INPUTUSER");
			q.setParameter("ObjectNo", objectNo).setParameter("ObjectType", objectType)
			.setParameter("BizType", bizType).setParameter("INPUTUSER", sUserID);
			String sPhotoRootPath = ServiceFactory.getFactory().getUploadRootPath();// 根路径
			String sReturnPhotoData = "";
			JSONArray photoDescs = new JSONArray();
			
			List<BizObject> list = q.getResultList(false);
			for(BizObject o : list){
				String sPhotoPath = o.getAttribute("PhotoPath").toString();// 相对路径
				sPhotoPath = sPhotoRootPath + sPhotoPath;// 物理路径
				ARE.getLog().info(sPhotoPath);
				try {
					FileInputStream fis = new FileInputStream(sPhotoPath);// sPhotpPath是图片物理路径
					byte[] bPhotoData = getBytes(fis);// 读取取文件转换成二进制
					fis.close();
					// 二进制经过base64编码将二进制转换成字符串
					String sReturnPhotoData1 = Base64.encode(bPhotoData);
					sReturnPhotoData = sReturnPhotoData + sReturnPhotoData1 + ",";
					JSONObject photoDescObject = new JSONObject();
					photoDescObject.put("PhotoDesc",o.getAttribute("PhotoDesc").toString());//图片描述
					photoDescObject.put("Address",o.getAttribute("Address").toString());//地址
					photoDescObject.put("Longitude",o.getAttribute("Longitude").toString());//经度
					photoDescObject.put("Latitude",o.getAttribute("Latitude").toString());//纬度
					photoDescs.add(photoDescObject);
				} catch (FileNotFoundException exception) {
					exception.printStackTrace();
				}
			}
			ARE.getLog().info(sReturnPhotoData + "  ");
			ARE.getLog().info(photoDescs.toString());
			result.put("PhotoData", sReturnPhotoData);// 相片数据
			result.put("PhotoDescs", photoDescs);// 相片描述
			return result;
		} catch (Exception e) {
			throw new HandlerException(e.getMessage());
		}
	}

	private byte[] getBytes(InputStream is) throws IOException {
		ByteArrayOutputStream ois = new ByteArrayOutputStream();
		byte[] buffer = new byte[10240];
		int b = is.read(buffer);
		while (b > -1) {
			ois.write(buffer, 0, b);
			b = is.read(buffer);
		}
		return ois.toByteArray();
	}
}
