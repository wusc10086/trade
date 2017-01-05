package com.amarsoft.biz.rs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.amarsoft.app.accounting.sysconfig.SystemConfig;
import com.amarsoft.app.accounting.util.ACCOUNT_CONSTANTS;
import com.amarsoft.are.ARE;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.awe.util.Transaction;

public class InitSystemConfig extends HttpServlet implements Servlet{
	private static final long serialVersionUID = -931346619205476616L;

	public void init() throws ServletException {
		super.init();
		Transaction Sqlca = null;
		
		ARE.getLog().info("**********************************InitDataServlet Start*********************************");
		try {
//			Configure asc = Configure.getInstance(this.getServletContext());
			
			String sDataSource = ARE.getProperty(ACCOUNT_CONSTANTS.DATABASE_ALS);
			Sqlca = new Transaction(sDataSource);
			
			try {
				Properties setting=null;
				InputStream in = null;
				try {
					in = this.getClass().getClassLoader().getResourceAsStream("SysConfig.properties");
					setting = new Properties();
		            setting.load(in);
		        } catch (IOException e) {
		        	ARE.getLog().info("Error: Read SysConfig.properties file failed!!!");
		            e.printStackTrace();
		        } finally{
		        	if(in != null){
		        		try {
							in.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		        	}
		        }
		        for (Iterator iter = setting.keySet().iterator(); iter.hasNext();) {
		        	String key = (String)iter.next();
					String parameterValue = (String)setting.get(key);
					ARE.getLog().info("Now Loading "+key+"-"+parameterValue+"!!!");
//		        	asc.getConfigure(key);
				}
		        
		        ARE.getLog().info("Initial AccountingConfig            .......... Starting" + StringFunction.getNow());
				//核算参数加载部分
				//获取配置文件设置
		        com.amarsoft.app.accounting.persistence.SerialnoGenerator.dstype = "Web";
				String ConfigFile = getInitParameter("AccountingConfigFile");
				SystemConfig.loadSystemConfig(Sqlca.getConnection(),ConfigFile);
				ARE.getLog().info("Initial AccountingConfig            .......... Success!"+ StringFunction.getNow());
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("构造系统配置时出错：" + e);
			} finally {
				try {
					if (Sqlca != null) {
		                Sqlca.commit();
						Sqlca.disConnect();
						Sqlca = null;
					}
				} catch (Exception e1) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ARE.getLog().info("**********************************InitDataServlet Success*********************************");
	}

	public void destroy() {
		super.destroy();
	}
}
