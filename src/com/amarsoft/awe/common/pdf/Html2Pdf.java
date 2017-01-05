package com.amarsoft.awe.common.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFEncryption;

import com.amarsoft.are.ARE;
import com.amarsoft.awe.common.pdf.css.CssParserFactory;
import com.amarsoft.awe.common.pdf.util.StringConvertor;
import com.lowagie.text.pdf.BaseFont;
/**
 * html模板生成pdf
 * @author flian
 *
 */
public class Html2Pdf {
	
	/** 
	 * 字体文件所在目录
	 */
	public static String FONTS_DIR = "";//ARE.getProperty("APP_HOME")+"/etc/fonts/";
	
	/**
	 * 文档所有权密码
	 */
	public static String OWNER_PWD = "123456";
	
	/**
	 * 文档可读权限密码
	 */
	public static String READER_PWD = "";
	
	/**
	 * html流转pdf流
	 * @param htmlStream html输入流
	 * @param outputStream 输出流
	 * @param sourceDir 资源路径
	 * @throws Exception 
	 */
	public static void generatePdf(InputStream htmlStream,OutputStream outputStream,String sourceDir)throws Exception{
		//(new com.lowagie.text.pdf.BaseFont()).getc
		//检查字体路径是否整理
		if(checkFontPath()==false){
			throw new Exception("无效的字体路径:"+FONTS_DIR);
		}
		ByteArrayOutputStream tidyOut = null;
		Reader inputReader = null;
		InputStream tidyIn = null;
		
		try {
			tidyOut = new ByteArrayOutputStream();
			Tidy tidy = new Tidy();
			tidy.setXHTML(true);
			tidy.setInputEncoding("gbk"); 
			tidy.setOutputEncoding("gbk"); //为了避免部分特殊字符导致文档无法解析，这里最好设置为gbk
			//tidy.parse(input, tidyOut);改为下两行代码，做css解析
			String sSource = StringConvertor.inputStream2String(htmlStream, "gbk");
			//css替换
			sSource =  CssParserFactory.getCssParser().parse(sSource, sourceDir);
			//替换<o:p>标签
			sSource = sSource.replaceAll("<o:p>", "");
			sSource = sSource.replaceAll("</o:p>", "");
			//替换st1:chsdate标签
			sSource = sSource.replaceAll("<st1:chsdate[\\w\\W]+?>", "");
			//为table标签增加bordercolor
			sSource = sSource.replaceAll("<\\s*[Tt][Aa][Bb][Ll][Ee]", "<table bordercolor=''");
			//字体替换
			sSource = CssParserFactory.getCssParser("com.amarsoft.awe.common.pdf.css.FontParser").parse(sSource, null);
			//htmlsource -> tidy
			inputReader = new StringReader(sSource);
			tidy.parse(inputReader, tidyOut);
			tidyIn = new ByteArrayInputStream(tidyOut.toByteArray());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(tidyIn);
			
			//tidy->pdf
			Element style = doc.createElement("style");
			//如果文档中没有设置字体，那么这里设置默认字体：simsun
			style.setTextContent("body {font-family:SimSun;}\n@page {size: 8.5in 11in; }");
			Element root = doc.getDocumentElement();
			//root.getElementsByTagName("head").item(0).appendChild(style);
			ITextRenderer renderer = new ITextRenderer();
			//加密
			//encrypt(renderer);
			// 解决中文支持问题
			ITextFontResolver fr = renderer.getFontResolver();	
			//增加默认字体simsun的支持
			fr.addFont(FONTS_DIR + "simsun.ttc",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
			//根据html文档来增加额外字体支持
			fr.addFont(FONTS_DIR + "simkai.ttf",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
			fr.addFont(FONTS_DIR + "simhei.ttf",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
			renderer.setDocument(doc,"http://www.amarsoft.com");
			//设置资源根路径
			renderer.getSharedContext().setBaseURL("file://" + sourceDir );   
			//renderer.getSharedContext().setBaseURL("http://localhost:8081/P2P_Loan/investment/agreement/");   
			renderer.layout();
			//输出流
			renderer.createPDF(outputStream);
		} finally{
			if(tidyOut != null){
				try {
					tidyOut.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(tidyIn != null){
				try {
					tidyIn.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(inputReader != null){
				try {
					inputReader.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	//加密
	private static void encrypt(ITextRenderer renderer){
		PDFEncryption enc = new PDFEncryption();
		enc.setOwnerPassword(OWNER_PWD.getBytes());
		enc.setUserPassword(READER_PWD.getBytes());
		renderer.setPDFEncryption(enc);
	}
	
	private static boolean checkFontPath() {
		File file = new File(FONTS_DIR);
		return file.exists();
	}

	/**
	 * html文件转pdf文件
	 * @param htmlPath html文件全物理路径
	 * @param outputPath 输出文件全物理路径
	 * @param sourceDir 资源文件目录
	 * @throws Exception
	 */
	public static void generatePdf(String htmlPath,String outputPath,String sourceDir)throws Exception{
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(htmlPath);
			output = new FileOutputStream(outputPath); 
			generatePdf(input,output,sourceDir);
			output.flush();
		} finally {
			if(output != null){
				output.close();
			}
			if(input != null){
				input.close();
			}
		}
	}
	
	public static void main(String[] args)throws Exception{
		System.out.println("start");
		String sSerialno = "20140701000013";
		String htmlPath = "/home/yhxu/workspace/P2P_Service/WebRoot/Contract/recharge.html";
		String outputPath = "/home/yhxu/pdf/"+sSerialno+".pdf";
		String sFonts="/home/yhxu/workspace/P2P_Service/WebRoot/WEB-INF/etc/fonts/";
		Html2Pdf.FONTS_DIR = sFonts;
		Html2Pdf.generatePdf(htmlPath, outputPath, "/home/yhxu/workspace/P2P_Service/WebRoot/Contract/");
		System.out.println("end");		
	}
}
