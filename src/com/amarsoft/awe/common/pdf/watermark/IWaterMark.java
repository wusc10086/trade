package com.amarsoft.awe.common.pdf.watermark;
/**
 * 水印顶级接口
 */
import com.lowagie.text.pdf.PdfContentByte;

public interface IWaterMark {
	void run(PdfContentByte under)throws Exception;
}
