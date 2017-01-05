package com.amarsoft.p2ptrade.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;

import javax.imageio.ImageIO;

import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class DemoImageHandler extends JSONHandler {

	// 获取检验码字符串
	private String getCheckCode(int iLength) {
		char[] str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		SecureRandom random = null;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		if(random != null){
			String suiji = "";
			int temp = 0;
			for (int i = 0; i < iLength; i++) {
				temp = random.nextInt(36);
				suiji += str[temp];
			}
			return suiji;
		}else{
			return null;
		}
	}
	
	public Object createResponse(JSONObject arg0, Properties arg1)
			throws HandlerException {
		int iLength = 4;
		String code = getCheckCode(iLength);

		BufferedImage buffimg = new BufferedImage(13*iLength, 20, BufferedImage.TYPE_INT_RGB);
		Graphics g = buffimg.createGraphics();
		SecureRandom random = null;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			throw new HandlerException(e1.getMessage());
		}
		int cr, cg, cb;
		cr = random.nextInt(255);
		cg = random.nextInt(255);
		cb = random.nextInt(255);
		Color mycolor = new Color(cr, cg, cb);
		// 干扰线
		g.setColor(mycolor);
		for (int i = 0; i < 10; i++) {
			int x1 = random.nextInt(60);
			int x2 = random.nextInt(60);
			int y1 = random.nextInt(20);
			int y2 = random.nextInt(20);
			g.drawLine(x1, y1, x2, y2);
		}
		// 显示随机码
		Font myfont = new Font("times new roman", Font.PLAIN, 19);
		g.setFont(myfont);
		g.setColor(Color.WHITE);
		g.drawString(code, 5, 15);

		// 将图像输出到servlet输出流中。
		try{
			ByteArrayOutputStream sos = new ByteArrayOutputStream();
			ImageIO.write(buffimg, "jpeg", sos);
			sos.close();
			g.dispose();
			return sos.toByteArray();
		}
		catch(Exception e){
			throw new HandlerException(e.getMessage());
		}
	}

}
