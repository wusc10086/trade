package com.amarsoft.awe.common.pdf.watermark;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
/**
 * ͼƬˮӡ
 * @author flian
 *
 */
public class PictureWaterMark implements IWaterMark {
	
	private String imagePath;
	
	public PictureWaterMark(String imagePath){
		this.imagePath = imagePath;
	}

	public void run(PdfContentByte under) throws Exception{
		if(imagePath==null || imagePath.trim().length()<=0)return;
		Image image = Image.getInstance(imagePath); 
		image.setAbsolutePosition(200, 400);
		under.addImage(image);
		
	}

}
