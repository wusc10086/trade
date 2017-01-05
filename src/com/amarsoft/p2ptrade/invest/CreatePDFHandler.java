package com.amarsoft.p2ptrade.invest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.amarsoft.app.accounting.util.ACCOUNT_CONSTANTS;
import com.amarsoft.app.accounting.util.NumberTools;
import com.amarsoft.are.ARE;
import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOException;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.are.util.DataConvert;
import com.amarsoft.are.util.StringFunction;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.tools.pdf.ModelParser;
import com.amarsoft.tools.pdf.imp.PDFCreator;
import com.amarsoft.tools.pdf.imp.XMLModelParser;
import com.amarsoft.tools.pdf.model.DocModel;

/**
 * 生成四方合同PDF
 * 输入参数： SubContractNo,UserID:
 * 输出参数： 
 */
public class CreatePDFHandler implements Runnable{
	
	String contractno = "";
	String userID = "";
	JBOFactory jbo = null;
	
	public CreatePDFHandler(String contractno,String userID, JBOFactory jbo){
		this.contractno = contractno;
		this.userID = userID;
		this.jbo = jbo;
	}
	
	public Object CreatePDF() throws HandlerException {
		return CreatePDFFile(contractno,userID,jbo);
	}

	/**
	 * 生成四方合同PDF
	 * 
	 * @param request
	 * @return 
	 * @throws HandlerExceptioncreateResponse
	 */
	@SuppressWarnings("unchecked")
	public String CreatePDFFile(String contractno,String userID,JBOFactory jbo) throws HandlerException {
		   
			String result="success";
			
			try{
				//aUserList :投资人信息ID，平台name，真实name，身份证证；
				ArrayList<String> aUserList = new ArrayList<String>();
				//usertmpList 投资人ID
				ArrayList<String> usertmpList = new ArrayList<String>();
				//userNametmpList:真实名字
				ArrayList<String> userNametmpList = new ArrayList<String>();
				//userCertIDtmpList:身份证号
				ArrayList<String> userCertIDtmpList = new ArrayList<String>();
				//usernametmpList:平台名字
				ArrayList<String> usernametmpList = new ArrayList<String>();
				ArrayList<String> userInvestMoneyList = new ArrayList<String>();
				//清数据
				deleteData(contractno,jbo);
				//取出用户信息
				aUserList = getUserList(contractno,jbo);
				//生成文件且插入文件
				if(aUserList==null||aUserList.size()==0){
					throw new HandlerException("business.showhtml.fail");
				}
				for(int i=0;i<aUserList.size();i++){
					String tmp[] = aUserList.get(i).split("@",-1);
					String sSubContractNo = tmp[0];
					String sUserID = tmp[1];
					String sCertID = tmp[2];
					String sRealName = tmp[3];
					String sUserName = tmp[4];
					String sMoney = tmp[5];
					usertmpList.add(sUserID);
					userNametmpList.add(sRealName);
					userCertIDtmpList.add(sCertID);
					usernametmpList.add(sUserName);
					userInvestMoneyList.add(sMoney);
					//投资人合同
					CreateContractDoc(contractno,sSubContractNo,sUserID,jbo,userID,"Invest",null,null,null,null);
				}
				//借款人合同
				CreateContractDoc(contractno,contractno,userID,jbo,userID,"Borrow",usernametmpList,userNametmpList,userCertIDtmpList,userInvestMoneyList);
			}catch(HandlerException he){
				he.printStackTrace();
				throw new HandlerException("business.showhtml.fail");
			}
			
			

		return result;
	}
	//删除数据
	private static void deleteData(String contractno,JBOFactory jbo) throws HandlerException {
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.contract_s_record");
			BizObjectQuery query = m.createQuery("delete from o where relativeno=:contractid and relativetype='002' and contracttype='002'");
			query.setParameter("contractid",contractno);
			query.executeUpdate();
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
	}
	
	
	//取得投资人信息
	private static ArrayList<String> getUserList(String contractno,JBOFactory jbo) throws HandlerException {
		ArrayList<String> aUserList = new ArrayList<String>();
		
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.user_contract");
			BizObjectQuery query = m.createQuery("select o.subcontractno,o.investsum,o.userid,ad.certid,ad.realname,ua.username from o,jbo.trade.account_detail ad" +
					",jbo.trade.user_account ua " +
					" where  o.contractid=:contractid and o.status ='1' and o.relativetype='002' and o.userid = ad.userid and ua.userid=o.userid");
			query.setParameter("contractid",contractno);
			List<BizObject> list = query.getResultList(false);
			if(list!=null&&list.size()>0){
				for (int i = 0; i < list.size(); i++) {
					BizObject o2 = list.get(i);
					String sSubContractNo = o2.getAttribute("subcontractno").getValue()==null?"":
						o2.getAttribute("subcontractno").getString();//子合同号
					String sUserID = o2.getAttribute("userid").getValue()==null?"":
						o2.getAttribute("userid").getString();//用户号
					String sCertID = o2.getAttribute("certid").getValue()==null?"":
						o2.getAttribute("certid").getString();//用户号
					String sRealName = o2.getAttribute("realname").getValue()==null?"":
						o2.getAttribute("realname").getString();//用户号
					String sUserName = o2.getAttribute("username").getValue()==null?"":
						o2.getAttribute("username").getString();//用户号
					String sInvestSum = NumberTools.numberFormat((o2.getAttribute("investsum").getValue()==null?0:
						o2.getAttribute("investsum").getDouble()), ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION);//投资金额
					aUserList.add(sSubContractNo+"@"+sUserID+"@"+sCertID+"@"+sRealName+"@"+sUserName+"@"+sInvestSum);
				}
			}else{
				throw new HandlerException("business.showhtml.fail");
			}
			
		}catch(JBOException e){
			e.printStackTrace();
			throw new HandlerException("default.database.error");
		}catch(HandlerException he){
			he.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
		
		
		return aUserList;
	}
	
	private static String CreateContractDoc(String contractno, String subcontractno,String sUserID,JBOFactory jbo,String userID,String contractType,
			ArrayList<String> usertmpList,ArrayList<String> userNametmpList,ArrayList<String> userCertIDtmpList,ArrayList<String> userInvestMoneyList) throws HandlerException{
		String result = "success";
		String sSerialnoForContract="";
		try {
			sSerialnoForContract = insertContractRecord( contractno,  subcontractno, sUserID, jbo, userID);
			HashMap<String,Object> responseParams = getResponseParams(sUserID,jbo,userID,subcontractno,contractno,contractType,usertmpList,userNametmpList,userCertIDtmpList);
			ArrayList<HashMap<String,String>> arrayMap = getPaymentSch(jbo,subcontractno,contractType,contractno);
			ArrayList<HashMap<String,String>> investMap = ((ArrayList<HashMap<String, String>>)responseParams.get("investMap"));
			//创建模型解析类
			ModelParser modelParser = new XMLModelParser();
			//存放要替换的标签值
			String str = getStr(responseParams,arrayMap,investMap,userInvestMoneyList,contractType);
			HashMap hm = new HashMap();
			//hm.put("test1", "测试1");
			//即系模版，生成文档模型对象
			DocModel docModel = modelParser.parse(str,  hm);
			//创建文档生成器
			PDFCreator pc = new PDFCreator();
			//文档路径
			String subfolderPath = ARE.getProperty("subFolder").toString();
			String folderPath = ARE.getProperty("PDFPath").toString();
			
			if(subfolderPath.endsWith("/")){
				subfolderPath = subfolderPath.substring(0,subfolderPath.length()-1);
			}
			
			if(!subfolderPath.startsWith("/")){
				subfolderPath = "/"+subfolderPath;
			}
			
			if(folderPath.endsWith("/")){
				folderPath =folderPath.substring(0,folderPath.length()-1);;
			}
			
			File file = new File(folderPath);
			if(!file.exists()){
				file.mkdir(); 
			}
			
			String filePath = folderPath+subfolderPath+"/"+subcontractno+".pdf";
			
			//生成pdf文档
			pc.create(docModel, new java.io.FileOutputStream(filePath));
			
		   //保存记录
			updateContractRecord( sSerialnoForContract,"3", subfolderPath+"/"+subcontractno+".pdf","",jbo);
			
		} catch (Exception e) {
			e.printStackTrace();
			try{
				updateContractRecord( sSerialnoForContract,"1", "",e.toString(),jbo);
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			throw new HandlerException("business.showhtml.fail");
		}
		return result;
	}
	
	private static String  insertContractRecord(String contractno, String subcontractno,String sUserID,JBOFactory jbo,String userID)throws HandlerException{
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.contract_s_record");
			BizObject o = m.newObject();
			o.setAttributeValue("CONTRACTNO", subcontractno);//合同编号
			o.setAttributeValue("CONTRACTTYPE", "002");//合同类型
			o.setAttributeValue("RELATIVENO", contractno);//关联编号
			o.setAttributeValue("RELATIVETYPE", "002");//关联编号类型
			o.setAttributeValue("LOANUSERID", userID);//借款人编号
			o.setAttributeValue("INVESTUSERID", sUserID);//投资人编号
			o.setAttributeValue("SIGNTIME", StringFunction.getToday()+" "+StringFunction.getNow());//签署时间
			o.setAttributeValue("SIGNUSERID", sUserID);//签署人
			o.setAttributeValue("STATUS", "0");//合同状态
			
			m.saveObject(o);
			return o.getAttribute("SERIALNO").getString();
			
		}catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
	}
	//保存记录
	private static void updateContractRecord(String serialno,String status,String sURL,String remark,JBOFactory jbo)throws HandlerException{
		try{
			BizObjectManager m = jbo.getManager("jbo.trade.contract_s_record");
			BizObject o = m.createQuery("serialno=:serialno").setParameter("serialno", serialno).getSingleResult(true);
			if(o!=null){
				o.setAttributeValue("STATUS", status);
				o.setAttributeValue("remark", remark);
				o.setAttributeValue("SAVEFILE", sURL);//保存路径
				m.saveObject(o);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
	}
	
	//查询生产PDF需要数据
	/**
	 * @param sUserID 投资人 userID 借款人
	 * **/
	private static HashMap<String,Object> getResponseParams(String sUserID,JBOFactory jbo,String userID,String subcontractno,String contractno,String contractType
			,ArrayList<String> usertmpList,ArrayList<String> userNametmpList,ArrayList<String> userCertIDtmpList)throws HandlerException{
		 HashMap<String,Object> responseParams = new  HashMap<String,Object>();
		 //获取出借人表格展示信息
		 ArrayList<HashMap<String,String>> investMap = new ArrayList<HashMap<String,String>>();
		 String sLenderName = "";
		 String sLenderCard = "";
		 String sBorrowName ="" ;
		 String sBorrowCard = "";
		// String sFundSource = "";
		 String sFundSourceDesc = "";
		 String sLoanRate ="";
		 String sLoanTerm = "";
		 String sRepaymentMethod = "";
		 String sPutoutDate = "";
		 String sMaturityDate = "";
		 String sGuaranorPerson ="";
		 String sGuaranorIDCard ="";
		 String sInvestsum2 = "";
		 String sInvestsum3 = "";
		 String sInvestsum = "";
		 String sBusinesssum2 = "";
		 String sBusinesssum = "";
		 String sBorrowUserName = "";
		 String sLenderUserName = "";
		 String mortgageperson = "";
		 String MORTGAGEIDCARD = "";
		 String MORTGAGEORGID ="";
		 String GUARANTORORGID = "";
		 String feerate = "";
		 try{
			 //借款人
			 	BizObjectManager m = jbo.getManager("jbo.trade.account_detail");
				BizObjectQuery query = m.createQuery("select o.certid,o.realname,li.loanrate,li.loanterm,li.putoutdate," +
						"li.maturitydate,li.repaymentmethod,la.GUARANTORPERSON,la.GUARANTORIDCARD,la.FUNDSOURCEDESC,li.businesssum,ua.username, " +
						" la.mortgageperson,la.GUARANTORORGID,la.MORTGAGEIDCARD,la.MORTGAGEORGID,afli.feerate from o,jbo.trade.acct_loan li,jbo.trade.loan_apply la,jbo.trade.user_account ua,jbo.trade.acct_fee_loan_info afli" +
						"  where  o.userid=:userID  and o.userid=li.customerid and o.userid = ua.userid " +
						"and li.contractserialno=:contractno " +
						"and afli.feecode = '0001'  and afli.ISINUSE = '1' " +
						"and afli.loanserialno = li.baserialno " +
						"and la.precontractno = li.contractserialno");
				query.setParameter("userID",userID);
				query.setParameter("contractno",contractno);
				BizObject o = query.getSingleResult(false);
				if(o!=null){
					sBorrowName = o.getAttribute("realname").getValue()==null?"":o.getAttribute("realname").getString();
					sBorrowCard = o.getAttribute("certid").getValue()==null?"":o.getAttribute("certid").getString();
					sLoanTerm = Integer.toString(o.getAttribute("loanterm").getValue()==null?0:o.getAttribute("loanterm").getInt());
					sLoanRate = NumberTools.numberFormat((o.getAttribute("loanrate").getValue()==null?0:o.getAttribute("loanrate").getDouble()), ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION);
					sPutoutDate = o.getAttribute("putoutdate").getValue()==null?"":o.getAttribute("putoutdate").getString();
					sMaturityDate = o.getAttribute("maturitydate").getValue()==null?"":o.getAttribute("maturitydate").getString();
					sBorrowUserName = o.getAttribute("username").getValue()==null?"":o.getAttribute("username").getString();
					feerate = o.getAttribute("feerate").getValue()==null?"":o.getAttribute("feerate").getString();
					
					sRepaymentMethod = o.getAttribute("repaymentmethod").getValue()==null?"":o.getAttribute("repaymentmethod").getString();
					if("RPT000010".equals(sRepaymentMethod)){
						sRepaymentMethod = "等额本息";
					}else if("RPT000040".equals(sRepaymentMethod)){
						sRepaymentMethod = "按月还息到期还本";
					}else if("RPT000020".equals(sRepaymentMethod)){
						sRepaymentMethod = "等额本金";
					}else if("RPT000045".equals(sRepaymentMethod)){
						sRepaymentMethod = "按季付息到期还本";
					}else if("RPT000050".equals(sRepaymentMethod)){
						sRepaymentMethod = "一次性还本付息条款";
					}
					
					sFundSourceDesc = o.getAttribute("FUNDSOURCEDESC").getValue()==null?"":o.getAttribute("FUNDSOURCEDESC").getString(); 
					/*if("personal".equals(sFundSource)){
						sFundSource = "个人贷款";
					}else if("company".equals(sFundSource)){
						sFundSource = "企业贷款";
					}*/
					
					MORTGAGEIDCARD = o.getAttribute("MORTGAGEIDCARD").getValue()==null?"":o.getAttribute("MORTGAGEIDCARD").getString();
					MORTGAGEORGID = o.getAttribute("MORTGAGEORGID").getValue()==null?"":o.getAttribute("MORTGAGEORGID").getString();
					mortgageperson = o.getAttribute("mortgageperson").getValue()==null?"":o.getAttribute("mortgageperson").getString(); 
					GUARANTORORGID = o.getAttribute("GUARANTORORGID").getValue()==null?"":o.getAttribute("GUARANTORORGID").getString(); 
					sGuaranorPerson = o.getAttribute("GUARANTORPERSON").getValue()==null?"":o.getAttribute("GUARANTORPERSON").getString(); 
					sGuaranorIDCard = o.getAttribute("GUARANTORIDCARD").getValue()==null?"":o.getAttribute("GUARANTORIDCARD").getString(); 
					sBusinesssum = StringFunction.numberToChinese(NumberTools.round((o.getAttribute("businesssum").getValue()==null?0:o.getAttribute("businesssum").getDouble()), ACCOUNT_CONSTANTS.MONEY_PRECISION));
					sBusinesssum2 = DataConvert.toMoney(NumberTools.round((o.getAttribute("businesssum").getValue()==null?0:o.getAttribute("businesssum").getDouble()), ACCOUNT_CONSTANTS.MONEY_PRECISION));
				
				}
				
				//投资人
				if("Invest".equals(contractType)){
					BizObjectManager m1 = jbo.getManager("jbo.trade.account_detail");
					BizObjectQuery query1 = m1.createQuery("select o.certid,o.realname,uc.investsum,ua.username from o,jbo.trade.user_contract uc,jbo.trade.user_account ua" +
								" where  o.userid=:userID  and o.userid=uc.userid and uc.contractid=:contractno and uc.subcontractno=:subcontractno and uc.status='1' and ua.userid=o.userid");
					query1.setParameter("userID",sUserID);
					query1.setParameter("contractno",contractno);
					query1.setParameter("subcontractno",subcontractno);
					BizObject o1 = query1.getSingleResult(false);
					if(o1!=null){
						sLenderUserName = o1.getAttribute("username").getValue()==null?"":o1.getAttribute("username").getString();
						sLenderName = o1.getAttribute("realname").getValue()==null?"":o1.getAttribute("realname").getString();
						sLenderCard = o1.getAttribute("certid").getValue()==null?"":o1.getAttribute("certid").getString();
						sInvestsum = StringFunction.numberToChinese(NumberTools.round((o1.getAttribute("investsum").getValue()==null?0:o1.getAttribute("investsum").getDouble()), ACCOUNT_CONSTANTS.MONEY_PRECISION));
						sInvestsum2 = DataConvert.toMoney(NumberTools.round((o1.getAttribute("investsum").getValue()==null?0:o1.getAttribute("investsum").getDouble()), ACCOUNT_CONSTANTS.MONEY_PRECISION));
						HashMap<String,String> lendMap = new HashMap<String,String>();
						lendMap.put("username", sLenderUserName);
						lendMap.put("realname", sLenderName);
						lendMap.put("certid", sLenderCard);
						investMap.add(lendMap);
					}
					
				}else if("Borrow".equals(contractType)){
					
					sInvestsum = sBusinesssum;
					sInvestsum2 = sBusinesssum2;
					subcontractno = contractno;
					
					for(int i=0;i<usertmpList.size();i++){
						
						if(i==usertmpList.size()-1){
							sLenderUserName = sLenderUserName + usertmpList.get(i);
							sLenderName = sLenderName + userNametmpList.get(i);
							sLenderCard = sLenderCard + userCertIDtmpList.get(i);
						}else{
							sLenderUserName = sLenderUserName + usertmpList.get(i) + ",";
							sLenderName = sLenderName + userNametmpList.get(i) + ",";
							sLenderCard = sLenderCard + userCertIDtmpList.get(i) + ",";
						}
						
					}
					HashMap<String,String> lendMap = new HashMap<String,String>();
					lendMap.put("username", sLenderUserName);
					lendMap.put("realname", sLenderName);
					lendMap.put("certid", sLenderCard);
					investMap.add(lendMap);
					
				}
		    responseParams.put("investMap", investMap);//出借人集合
			 responseParams.put("mortgageperson", mortgageperson);//担保抵押人
			 responseParams.put("lendername", sLenderName);//投资人姓名
			 responseParams.put("lenderid", sLenderUserName);//投资人ID
			 responseParams.put("lendercard", sLenderCard);//投资人身份证ID
			 responseParams.put("borrowname", sBorrowName);//借款人姓名
			 responseParams.put("borrowid", sBorrowUserName);//借款人ID
			 responseParams.put("borrowcard", sBorrowCard);//借款人身份证ID
			 responseParams.put("fundsourcedesc", sFundSourceDesc);//借款用途
			 responseParams.put("investsum", sInvestsum);//借款本金数额（大写）
			 responseParams.put("investsum2", sInvestsum2);//借款本金数额（小写）
			 responseParams.put("investsum3", sInvestsum3);//借款本金数额（小写）
			 responseParams.put("loanrate", sLoanRate);//借款年利率
			 responseParams.put("loanterm", sLoanTerm);//借款期限
			 responseParams.put("RepaymentMethod", sRepaymentMethod);//还款方式
			 responseParams.put("putoutdate", sPutoutDate);//起息日
			 responseParams.put("maturitydate", sMaturityDate);//到期日
			 responseParams.put("SubContractNo", subcontractno);//投资人子合同
			 responseParams.put("guarantorperson", sGuaranorPerson);//担保人姓  名
			 responseParams.put("guarantoridcard", sGuaranorIDCard);//担保人身份证
			 responseParams.put("guarantororgid", GUARANTORORGID);//担保人营业执照
			 responseParams.put("mortgageidcard", MORTGAGEIDCARD);//抵押人身份证
			 responseParams.put("mortgageorgid", MORTGAGEORGID);//抵押人营业执照
			 responseParams.put("feerate", feerate);//风险金费率
		 }catch(Exception e){
			 e.printStackTrace();
				throw new HandlerException("business.showhtml.fail");
		 }
		 return responseParams;
	}
	
	//取收益计划
	private static ArrayList<HashMap<String,String>> getPaymentSch(JBOFactory jbo,String subcontractno,String contractType,String contractno) throws HandlerException{
		ArrayList<HashMap<String,String>> arrayMap = new ArrayList<HashMap<String,String>>();
		try{
			if("Invest".equals(contractType)){
				BizObjectManager m = jbo.getManager("jbo.trade.income_schedule");
				BizObjectQuery query = m.createQuery("select o.SeqId,o.PayDate,o.PayCorpusAmt,o.PayInteAmt from o  where o.subcontractno=:subcontractno order by seqid ");
				query.setParameter("subcontractno",subcontractno);
				List<BizObject> list = query.getResultList(false);
				if(list!=null&&list.size()>0){
					for (int i = 0; i < list.size(); i++) {
						BizObject o = list.get(i);
						int SeqId = o.getAttribute("SeqId").getValue()==null?0:o.getAttribute("SeqId").getInt();
						String PayDate = o.getAttribute("PayDate").getValue()==null?"":o.getAttribute("PayDate").getString();
						double dPayCorpusAmt = NumberTools.round(o.getAttribute("PayCorpusAmt").getValue()==null?0:o.getAttribute("PayCorpusAmt").getDouble(), ACCOUNT_CONSTANTS.MONEY_PRECISION);
						double dPayInteAmt = NumberTools.round(o.getAttribute("PayInteAmt").getValue()==null?0:o.getAttribute("PayInteAmt").getDouble(), ACCOUNT_CONSTANTS.MONEY_PRECISION);
						double dActualSum = NumberTools.round( dPayCorpusAmt+dPayInteAmt ,ACCOUNT_CONSTANTS.MONEY_PRECISION);
						HashMap<String,String> contMap = new HashMap<String,String>();
						contMap.put("SeqId", String.valueOf(SeqId));
						contMap.put("PayDate", PayDate);
						contMap.put("PayCorpusAmt", NumberTools.numberFormat(dPayCorpusAmt,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						contMap.put("PayInteAmt", NumberTools.numberFormat(dPayInteAmt,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						contMap.put("ActualSum", NumberTools.numberFormat(dActualSum,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						arrayMap.add(contMap);
					}
				}
			}else if("Borrow".equals(contractType)){

				BizObjectManager m = jbo.getManager("jbo.trade.acct_payment_schedule");
				BizObjectQuery query = m.createQuery("select o.SeqId,o.PayDate,o.PayCorpusAmt,o.PayInteAmt from o  where o.objectno=:subcontractno order by seqid ");
				query.setParameter("subcontractno",contractno);
				List<BizObject> list = query.getResultList(false);
				if(list!=null&&list.size()>0){
					for (int i = 0; i < list.size(); i++) {
						BizObject o = list.get(i);
						int SeqId = o.getAttribute("SeqId").getValue()==null?0:o.getAttribute("SeqId").getInt();
						String PayDate = o.getAttribute("PayDate").getValue()==null?"":o.getAttribute("PayDate").getString();
						double dPayCorpusAmt = NumberTools.round(o.getAttribute("PayCorpusAmt").getValue()==null?0:o.getAttribute("PayCorpusAmt").getDouble(), ACCOUNT_CONSTANTS.MONEY_PRECISION);
						double dPayInteAmt = NumberTools.round(o.getAttribute("PayInteAmt").getValue()==null?0:o.getAttribute("PayInteAmt").getDouble(), ACCOUNT_CONSTANTS.MONEY_PRECISION);
						double dActualSum = NumberTools.round( dPayCorpusAmt+dPayInteAmt ,ACCOUNT_CONSTANTS.MONEY_PRECISION);
						HashMap<String,String> contMap = new HashMap<String,String>();
						contMap.put("SeqId", String.valueOf(SeqId));
						contMap.put("PayDate", PayDate);
						contMap.put("PayCorpusAmt", NumberTools.numberFormat(dPayCorpusAmt,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						contMap.put("PayInteAmt", NumberTools.numberFormat(dPayInteAmt,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						contMap.put("ActualSum", NumberTools.numberFormat(dActualSum,ACCOUNT_CONSTANTS.SPACE_FILL,ACCOUNT_CONSTANTS.MONEY_PRECISION));
						arrayMap.add(contMap);
					}
				}
			
			}
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new HandlerException("business.showhtml.fail");
		}
		return arrayMap;
	}
	
	//生成PDF数据
	private static String getStr(HashMap<String, Object> responseParams,ArrayList<HashMap<String,String>> arrayMap,ArrayList<HashMap<String, String>> investMap,
			ArrayList<String> userInvestMoneyList,String contractType) {
		int lengths = responseParams.get("guarantorperson").toString().length();
		String sss = lengths==0?"":"√";
		String str1 = lengths==0?"":"附件（一）";
		int length= responseParams.get("mortgageperson").toString().length();
		String ss = length==0?"":"√";
		String str2 = length==0?"":"附件（二 ）";
		String str3 = (str1.length()==0&&str2.length()==0)?"":"、";
		String str = "";
		str+="<?xml version=\"1.0\" encoding=\"GBK\"?>";
		str+="<doc>";
		str+="	<global>";
		str+="		<font id=\"defaultFont\" family=\"STSongStd-Light\" charset=\"UniGB-UCS2-H\" size=\"10\"></font>";
		str+="		<font id=\"headerfooter\" family=\"STSongStd-Light\" charset=\"UniGB-UCS2-H\" size=\"7\" color=\"#c9c9c9\"";
		str+="></font>";
		str+="		<headerfooter fontid=\"headerfooter\" border=\"\" align=\"\" content1=\"                                         \" content2=\"\"></headerfooter>";
		str+="		<page marginLeft=\"-1\" marginRight=\"-1\" marginTop=\"-1\" marginBottom=\"-1\" lineHeight=\"16\"></page>";
		str+="	</global>";
		str+="	<body>";


		str+="		<p fontSize=\"30\" fontColor=\"#000000\" align=\"1\" >借款协议书  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"2\" idleft=\"10\" > 协议编号： "+responseParams.get("SubContractNo")+"</p>";
		/*str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >甲方（出借人）： </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >证件号码（身份证号码）： "+responseParams.get("lendercard")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >姓      名："+responseParams.get("lendername")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >齐乐融融投融资E平台用户名："+responseParams.get("lenderid")+" </p>";
		str+="		<br/>";*/
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >甲方（出借人）： </p>";
		str+="		<br/>";
		str+="		<table colWidths=\"4,4,4,4,4\" borderWidth=\"1\">";
		str+="          <cell><p >平台用户名</p></cell>";
		str+="          <cell><p >真实姓名</p></cell>";
		str+="          <cell><p >身份证号码</p></cell>";
		str+="          <cell><p >借出金额</p></cell>";
		str+="          <cell><p >借款期限</p></cell>";
		
        for (int i = 0; i < investMap.size(); i++) { 
        	HashMap<String,String> jsonString1 = investMap.get(i);
        	String[] uns = jsonString1.get("username").toString().split("\\,",-1);
        	String[] rns = jsonString1.get("realname").toString().split("\\,",-1);
        	String[] cis = jsonString1.get("certid").toString().split("\\,",-1);
        	//String[] its = jsonString1.get("investsum").toString().split("!");
        	//String[] lts = jsonString1.get("loanterm").toString().split("\\,");
        	for(int kk=0;kk<uns.length;kk++){
        		str+="          <cell><p >"+uns[kk]+"</p></cell>";
                str+="          <cell><p >"+rns[kk]+"</p></cell>";
             	str+="          <cell><p >"+cis[kk]+"</p></cell>";
             	
             	if("Borrow".equals(contractType)){
             		str+="          <cell><p >"+DataConvert.toMoney(userInvestMoneyList.get(kk))+"元</p></cell>";
             	}else{
             		str+="          <cell><p >"+responseParams.get("investsum2")+"元</p></cell>";
             	}
             	
             	str+="          <cell><p >"+responseParams.get("loanterm")+"个月</p></cell>";
        	}
        	
        }
       
        
		str+="		</table>";
		str+="		<br/>";
		///
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >乙方（借款人）： </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	证件号码（身份证号码）： "+responseParams.get("borrowcard")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >姓      名："+responseParams.get("borrowname")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >厚本金融用户名： "+responseParams.get("borrowid")+"</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	丙方（平台网站）：  齐乐融融投融资E平台    </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >网址：     http://www.houbank.cn        </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		客服电话：400-090-6588  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >邮箱：csh001@qilerongrong.com.cn </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	丁方：风险管理人  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	名称：北京博融天下信息技术有限公司 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >邮箱：   bjbrtxyxgs@163.com</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >			鉴于：  </p>";
		
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	1、甲方是指《借款协议书》项下的出借人，已在平台网站注册。出借人为符合中华人民共和国法律规定的具有完全民事权利能力和民事行为能力，能独立行使和承担本协议项下的权利和义务的自然人，承诺对本协议涉及的借款具有完全的支配和处分权利，是其自有资金，为其合法所得；承诺给丙方提供的是真实、合法、有效、完整的信息。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	2、乙方是指《借款协议书》项下的借款人，已在平台网站注册。借款人为符合中华人民共和国法律规定的具有完全民事权利能力和民事行为能力，能独立行使和承担本协议项下的权利和义务的自然人，承诺给丙方提供的是真实、合法、有效、完整的信息。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	3、丙方是指齐乐融融投融资E平台，是外滩金融的践行者股份有限公司旗下互联网投融资平台，外滩金融的践行者股份有限公司拥有http://www.houbank.cn（以下简称“平台网站”）的合法经营权，为互联网金融业务搭建安全有效的交易平台并为交易提供信息咨询及信息服务。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	4、丁方是指依据中华人民共和国法律依法注册成立的有限责任公司，为本协议项下的风险管理人。丁方负责以受托管理的风险金为经丙方平台网站推荐在丙方平台融资的所有借款人的债务按照本协议约定的风险保障制度提供最高额质押担保。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	5、乙方有借款需求，甲方亦同意提供借款，乙方同意通过丙方平台就借款事项与甲方建立借贷关系。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	6、为乙方履行本协议项下的全部义务，担保人向甲方提供连带责任保证。本协议第一条借款信息中若没有保证或抵押担保，则本协议第七条第一款不对甲方生效。本款项下的担保文件见本协议"+str1+str3+str2+"。  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	各方经协商一致签订如下协议，共同遵照履行：  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		1．借款基本信息  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		1.1 借款信息 </p>";
		str+="		<br/>";

		str+="		<table colWidths=\"6,6\" borderWidth=\"1\">";
		str+="          <cell><p >借款用途</p></cell>";
		str+="          <cell><p>"+responseParams.get("fundsourcedesc")+"</p></cell>";
		str+="          <cell><p>借款本金数额（大写）</p></cell>";
		str+="          <cell><p>"+responseParams.get("investsum")+"</p></cell>";
		str+="          <cell><p>借款本金数额（小写）</p></cell>";
		str+="          <cell><p>"+responseParams.get("investsum2")+"元</p></cell>";
		str+="          <cell><p>借款年利率</p></cell>";
		str+="          <cell><p>"+responseParams.get("loanrate")+"%</p></cell>";
		str+="          <cell><p>借款期限</p></cell>";
		str+="          <cell><p>"+responseParams.get("loanterm")+"个月</p></cell>";
/*		str+="          <cell><p>每期偿还利息数额</p></cell>";
		str+="          <cell><p>"+arrayMap.get(0).get("InteAmt")+"</p></cell>";*/
		str+="          <cell><p>还款期数</p></cell>";
		str+="          <cell><p>"+responseParams.get("loanterm")+"期</p></cell>";
		str+="          <cell><p>还款方式</p></cell>";
		str+="          <cell><p>"+responseParams.get("RepaymentMethod")+"</p></cell>";
		str+="          <cell><p>起息日</p></cell>";
		str+="          <cell><p>"+responseParams.get("putoutdate")+"</p></cell>";
		str+="          <cell><p>到期日</p></cell>";
		str+="          <cell><p>"+responseParams.get("maturitydate")+"</p></cell>";
		str+="          <cell><p>担保人</p></cell>";
		str+="          <cell><p> 	(  "+sss+"   )1、保证人：保证承诺函见本协议附件一     (  "+ss+"  )2、抵押人：抵押担保函见本协议附件二</p></cell>";
		str+="		</table>";
		
		
		
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		1.2 还款计划表 </p>";
		str+="		<br/>";
		
		str+="		<table colWidths=\"4,4,4,4,4\" borderWidth=\"1\">";
		str+="          <cell><p >还款期数</p></cell>";
		str+="          <cell><p >还款日</p></cell>";
		str+="          <cell><p >当期本金</p></cell>";
		str+="          <cell><p >当期利息</p></cell>";
		str+="          <cell><p >合计</p></cell>";
		
        for (int i = 0; i < arrayMap.size(); i++) { 
        	HashMap<String,String> jsonString = arrayMap.get(i);
        	str+="          <cell><p >"+jsonString.get("SeqId").toString()+"</p></cell>";
            str+="          <cell><p >"+jsonString.get("PayDate").toString()+"</p></cell>";
         	str+="          <cell><p >"+jsonString.get("PayCorpusAmt").toString()+"</p></cell>";
         	str+="          <cell><p >"+jsonString.get("PayInteAmt").toString()+"</p></cell>";
         	str+="          <cell><p >"+jsonString.get("ActualSum").toString()+"</p></cell>";
        }
       
        
		str+="		</table>";
		
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2．各方权利和义务</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1 甲方的权利和义务</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.1 甲方应根据丙方的交易规则，在线点击确认借款（包含借款用途、借款本金、借款年利率、借款期限等事项）。丙方的交易规则在此是指：乙方经丙方推荐并在丙方平台上发布借款需求，由甲方在线点击确认借款，即视为甲方乙方合意借款并经丙方确认、丁方签署后，借款协议即成立并立即生效。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.2 甲方保证其所用于出借的资金来源合法，甲方是该资金的合法所有人，如果第三人对资金归属、合法性问题发生争议，由甲方负责解决并自行承担责任。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.3 甲方通过丙方平台在线接收本协议后，即视为不可撤销的授权丙方将等同于借款明细中列明的借款本金金额的资金由甲方在丙方开立的账户划转至乙方在丙方开立的账户中。划转完毕就视为放款成功，当日即为放款日。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.4 甲方享有其借款所带来约定的利息收益。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.5 如乙方违约，甲方有权要求丙方提供其已获得的乙方相关信息。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.6 无须征得协议其他方的同意，甲方可以根据自己的意愿进行本协议项下其对乙方债权的转让，但须在转让后及时通知其他方。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.7 甲方应自行缴纳由所获收益所得带来的可能的税款，丙方不提供代扣代缴服务。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.8 如乙方在平台上涉及多笔借款时，且每期还款金额不足以偿还应偿还的本金、利息及逾期罚息的，甲方同意按照其出借金额占乙方在平台全部借款金额的款项比例收取还款。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	    2.1.9 甲方在此特委托丙方收取乙方应还甲方的本息款项并按本协议约定进行分配，即：乙方每笔还款应归还至在丙方平台开立的账户中，由丙方按照本协议约定比例支付至各出借人的收款账户。借款本息足额清偿前，不得以任何方式撤销或变更本委托。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.10 甲方可以通过平台与乙方达成还款意向或协议，但不得接受乙方的任何直接还款。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.11 甲方同意按照本协议中的风险金担保条款获得代偿后次日起，将本协议项下的逾期罚息转让给丁方。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.1.12 乙方按照本协议约定提前还款的，甲方有权收取提前还款违约金（违约金计算方法见本协议4.3）。</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2 乙方的权利和义务  </p>";
		str+="		<br/>";
		

		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1 乙方应承担如下付款义务，直至其清偿债务为止：   </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1.1 按期足额向甲方偿还本金和利息； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1.2 按期足额向丙方支付平台服务费；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1.3 按期足额向丁方支付风险管理费； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1.4 如发生逾期还款，乙方须支付逾期罚息（罚息计算方式见本协议5.1）； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.1.5 如发生提前还款，乙方需按本协议约定向甲方支付提前还款违约金（违约金计算方式见本协议4.3）。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.2 乙方应根据本协议的约定按时足额向风险管理人支付风险金，风险金为借款金额的5%。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.3 乙方应按本协议约定用途使用借款，不得挪用。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.4 乙方应确保其提供的信息和资料的真实、准确、合法、有效、完整，不得提供虚假信息或隐瞒重要事实。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.5 乙方不得转让本协议项下的任何权利义务。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.6 在甲方的债权转让后，乙方仍应按照本协议的约定向债权受让人支付每期应还借款本息，不得以未接到债权转让通知为由拒绝履行还款义务。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.7 乙方同意：乙方违约，丙方有权向甲方提供其已获得的乙方及担保人信息，无需另行授权。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.8 乙方应按期足额支付平台服务费：  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.8.1 “平台服务费”是指因丙方为乙方借款提供平台推介、借款管理、提前还款管理、还款特殊情况沟通等系列服务而由乙方支付给丙方的报酬； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.8.2 本协议项下乙方需向丙方支付平台服务费为借款金额的1％，平台服务费自起息日开始收取，至乙方履行完毕本协议项下权利义务或风险管理人按照风险保障制度向丙方赔付完毕平台服务费之日止。支付方式为（ 2   ）：1、借款成功时一次性缴纳；2、按照本协议还款进度在还款日分期支付；3、由乙方和丙方另行协商确定； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.2.8.3 如乙方和丙方协商一致调整平台服务费时，无需经过本协议任何其他方同意。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3 丙方的权利和义务     </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3.1 甲方提供借款后，丙方应将该笔借款直接划付至乙方指定账户。如因甲方资金来源合法性问题而致该借款行为被取消因此给乙方或丙方造成损失的，甲方应当承担违约责任。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3.2 除本协议另有约定外，丙方应对甲方和乙方的信息及本协议内容保密；如任何一方违约，或因相关权力部门要求（包括但不限于公、检、法、海关、税务、金融监管机构等），丙方有权披露。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3.3 丙方有权依据本协议收取相应的平台服务费（收费标准见2.2.8.2）。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3.4 本协议各方确认并同意丙方保留与本协议有关的所有书面文件和电子信息。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		2.3.5 丙方仅为乙方借款、甲方提供借款承担居间平台服务，丙方不对乙方和保证人的个人信用、乙方的还款能力等做出任何形式的承诺，也不对甲方的资金来源的合法性、安全性做出任何形式的承诺。  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3．还款   </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3.1 乙方应于本协议约定的还款日和还款金额按期足额向甲方归还借款。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3.2 乙方的每期还款应按照如下顺序清偿：     </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		（1）违约金；（2）逾期罚息；（3）利息；（4）本金；（5）风险管理费；（6）平台服务费。   </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3.3 还款时间定义如下：起息日为乙方借款到账日起。还款日为“借款明细”中列明的乙方应偿还借款的日期。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3.4 乙方授权丙方于还款日从乙方在丙方开立账户中将资金划转至甲方在丙方开立的账户及丙方自身账户及丁方账户。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		3.5 当乙方在丙方开立账户中的资金余额不足支付当期应付款项时，乙方授权丙方从与乙方在丙方账户绑定的银行账户中代扣差额。   </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4．提前还款   </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.1 本协议项下借款不得部分提前还款，若提前还款应当归还全部借款本息。乙方如需提前还款，应向丙方提出，由丙方代为处理提前结清的相关事项。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.2 乙方申请提前还款的，应事先在丙方平台开立的账户中存入足额资金，并按下列顺序足额清偿应付款项：（1）提前还款违约金（2）应付利息；（3）剩余本金；（4）风险管理费；（5）平台服务费。丙方账户于上述时间点未收到上述金额的，视为乙方未提出提前还款申请，乙方仍应当按照原约定还款。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.3 提前还款违约金的计费方式为：违约金＝剩余本金×借款年利率/360×剩余天数×50%。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.4 向丙方支付按本协议约定计算的平台服务费。平台服务费的计算方式为：平台服务费＝剩余本金×平台服务费费率(1%)。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.5 向丁方支付按本协议约定计算的风险管理费。风险管理费的计算方式为：风险管理费＝剩余本金×风险管理费费率(2%)。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		4.6 若借款人申请提前还款时，尚未还清逾期应付款项的，则应先结清所有逾期款项后方可提前还款。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		5．逾期还款</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		5.1 乙方逾期还款的，应向甲方支付逾期罚息，直至清偿完毕之日。逾期罚息计算方式为：逾期罚息=逾期款项中的借款本金×借款年利率×1.5／360×实际逾期天数，逾期罚息不计复利。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		5.2 如借款人逾期还款时，其还款金额不足以足额清偿全部到期应付款项的，借款人应按如下顺序支付应付款项：（1）应付未付的风险管理费总和；（2）应付未付的平台服务费总和；（3）逾期罚息；（4）利息；（5）本金。其中，逾期罚息、利息或本金的还款顺序应根据拖欠的时间按次／期偿还。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		5.3 乙方逾期还款的，甲方根据其与丁方签订的《催收授权委托书》，授权丁方对乙方进行欠款催收、提示和追索。丁方有权将催收事宜转委托至第三方并无需再获取甲方、乙方之认可。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		6．借款提前到期 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		当乙方出现逾期支付任何一期还款超过90天、连续逾期三期以上（含三期）或累计逾期达五期以上（含五期），或在借款成功后出现逃避、失联或拒绝承认欠款事实、经营状况出现严重恶化、故意转移财产等恶意行为时，甲方授权丁方宣布借款提前到期并行使收款权利。借款到期后，乙方应立即清偿本协议项下的逾期罚息、利息、应付未付的全部本金、风险管理费、平台服务费以及根据本协议产生的其他费用。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7．风险保障</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.1 为保证乙方履行本协议项下的权利义务，担保人向甲方提供连带责任保证，具体以担保人提供的保证承诺（见协议"+str1+str3+str2+"）约定为准。   </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.2 乙方应按借款金额的  "+responseParams.get("feerate")+"%向风险管理人缴纳风险金，风险管理人开立的风险金专用账户（以下称“风险金账户”）为：  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		         账户名称：北京博融天下信息技术有限公司风险准备金 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		         账号：801110801421014798</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		        开户行：  外滩金融的践行者西安分行</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.3 借款人在任意一期还款日未能按期足额偿还借款本息到第10个工作日时，风险管理人应以所管理的风险金进行代偿。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.4 风险管理人代为偿还债务后，自行取得因代为偿还债务的追偿权。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.5 风险金独立于风险管理人的自有资金，风险管理人对风险金仅拥有管理、使用权利，不得用于自身经营、清偿自身债务或用于风险金本身目的以外的其他任何用途。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		7.6 借款人需向风险管理人支付的风险管理费为借款金额的2％，风险管理费自起息日开始收取，至乙方履行完毕本协议项下权利义务之日止，管理费按[月]计提。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		          户名：北京博融天下信息技术有限公司           </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		          账号 : 801100701421013360           </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		         开户银行:外滩金融的践行者营业部          </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		8．违约责任  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.1 各方均应严格履行本协议，非经各方协商一致或依照本协议约定，任何一方不得变更或解除本协议。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.2 乙方不得将所借款项用于生产经营和消费以外的范畴（包括但不限于投资，购买彩票，购买股票、基金、期货等金融产品，转贷等），如有上述行为，甲方可提前收回借款。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.3 发生下列任何一项情形的，即构成乙方违约：</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.3.1 乙方违反其在本协议所做的任何承诺和保证的； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.3.2 乙方的任何财产遭受没收、征用、查封、扣押、冻结等可能影响其履约能力的不利事件，且不能及时提供有效补救措施的；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.3.3 乙方的财务状况出现影响其履约能力的不利变化，且不能及时提供有效补救措施的。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.4 若因本条第8.3款所述情形而导致乙方违约的，或根据甲方、丁方合理判断乙方可能发生违约事件的，经丁方同意，甲方有权委托丙方采取下列任何一项或几项救济措施：  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.4.1 立即暂缓、取消发放全部或部分借款； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.4.2 宣布已发放借款全部提前到期，借款人应立即偿还所有应付款项； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.4.3 解除本协议；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.4.4 采取法律、法规以及本协议约定的其他救济措施。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.5 任何一方违约，违约方应承担因违约而给其他各方造成的损失及费用，包括诉讼费、风险管理费、平台服务费、以及合理的律师费等。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 8.6 乙方同意并授权丙方将乙方的逾期记录录入征信系统。</p>";
		
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		9．适用法律及争议解决 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >      本协议的签订、履行、终止、解释均适用中华人民共和国法律，并约定由丙方所在地的人民法院管辖。   </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		10．通知</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >  10.1 自本协议订立之日起，本协议项下其他各方有义务在下列信息变更的三日内书面通知丙方：包括但不限于联系人的有效身份信息、居住地、手机号码、电子邮箱、银行账户等。若因任何一方不及时提供上述变更信息而带来的损失或额外费用应由该方自行承担。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >  10.2 本协议项下任何一方根据本协议约定作出的通知、变更和文件均应以书面形式作出，并委托丙方通过指定渠道（包括但不限于公告或以电子数据方式）发送至其他各方。</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		11．保密条款  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 各方应将其在本协议及其附属合同、文件的签订和履行过程中获得的有关本协议项下的事宜以及与此等事宜有关的任何文件、资料或信息视为保密信息，不得披露或使用，除非事先得到另外三方的书面同意。 </p>";
		
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		12．附则  </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		 12.1 本协议采用电子文本形式制成，并委托丙方保管所有与本协议有关书面文件或电子信息，各方均认可该形式的协议效力。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		 12.2 本协议项下的附件和补充协议构成本协议不可分割的一部分。若本协议的任何一项的部分或全部被认定为无效或者不具有执行力，并不损害协议其他任何条款的有效性或执行力。于前置规定的情形，各方应及时进行协商，达成必要的协议，以将被认定为无效或无执行力的该条款置换为最大限度地依照合同目的和精神而规定的其他条款。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		声明：本签署栏中，甲乙双方信息由丙方系统自动完成操作，甲乙双方通过在线点击方式确认本协议的订立，丁方的电子签名与手写签名或者盖章具有同等的法律效力。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		 甲方："+responseParams.get("lendername")+"  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		 乙方："+responseParams.get("borrowname")+"  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		 丙方：  齐乐融融投融资E平台  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		丁方：北京博融天下信息技术有限公司</p>";
		str+="		<br/>";
		//附件一：保证承诺函
		if(((String) responseParams.get("guarantorperson")).length()!=0){
		str+="		<br/>";
		str+="		<br/>";	
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		附件一： </p>";
		str+="		<br/>";	
		str+="		<br/>";	
		str+="		<p fontSize=\"30\" fontColor=\"#000000\" align=\"1\" >保证承诺函 </p>";
		str+="		<br/>";	
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >"+responseParams.get("guarantorperson")+"（下称“保证人”）知悉丙方平台网站编号为 "+responseParams.get("SubContractNo")+"《借款协议书》（以下简称借款协议）的债权人（即出借人，明细详见借款协议） 与"+responseParams.get("borrowname")+"(以下称“借款人”）签订的编号为  "+responseParams.get("SubContractNo")+"的《借款协议书》，并愿为该借款协议项下借款人之债务提供连带责任保证。</p>";
		str+="	    <p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 1．保证范围为《借款协议书》项下的全部债务，包括但不限于违约金、逾期罚息、利息、本金、风险管理费、平台服务费、实现债权的费用。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 2．保证人确认，当债务人违约时，即按本保证承诺函规定的保证范围承担连带担保责任。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 3．本保证承诺函不因主协议因为任何原因而发生的不成立、不生效、无效、部分无效或被撤销、被解除而影响保证函的约定。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 4．本保证承诺函有效期间为借款协议项下债务履行期限届满之日起两年。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5．保证人声明和保证如下： </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.1 保证人为依法注册并合法存续的法人企业、其他组织或自然人，具备签订和履行本保证函所需的完全民事权利能力和行为能力； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.2 保证人阅读并完全了解借款协议及本保证承诺函的所有内容，签署和履行保证承诺函系基于保证人的真实意思表示，已经按照章程及其他内部管理文件要求取得合法、有效的授权，且不会违反对保证人有约束力的任何协议、合同和其他法律文件； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.3 保证人已经取得签订和履行本保证承诺函所需的一切有关批准文件、许可、备案或者登记；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.4 代表保证人签署本保证承诺函的授权签字人经过合法、有效的公司授权；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.5 保证人为公司的，提供该保证已经按照公司章程的规定由董事会或者股东会、股东大会决议通过；其公司章程对担保的总额及单项担保的数额有限额规定的，本保证承诺函项下担保未超过规定的限额；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.6 保证人向丙方平台网站提供的有关文件、资料是准确、真实、完整、有效的；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.7 保证人接受丁方风险管理人对有关生产经营、财务活动的监督检查，并给予足够的协助和配合； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.8 若保证人发生可能影响保证人财务状况和履约能力的情况，保证人应立即书面通知丁方风险管理人； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.9 保证人发生名称变更、法定代表人（负责人）、住所、经营范围、注册资本金或公司章程等工商登记事项变更的、应当在变更后7个工作日内书面通知丙方平台网站； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.10 保证人承诺如借款人不能按借款协议约定足额归还本息，保证人将代其足额偿还借款本息； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.11 本保证承诺函项下的声明和保证是连续和有效的，其保证责任不因债权人给予借款人任何宽容、宽限或优惠或延缓行使权利而受影响，也不因《借款协议书》的修改、补充、变更时视为保证人重复作出。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	6 该保证承诺函是《借款协议书》的组成部分，具有同等法律效力。</p>";
		str+="		<br/>";	
		str+="		<br/>";	
		str+="		<br/>";
		if(((String) responseParams.get("guarantororgid")).length()!=0){
		str+="		<table colWidths=\"50\"  borderWidth=\"1\">";
		str+="          <cell>";
		str+="                <table colWidths=\"1,4,5\" borderWidth=\"1\" padding=\"0\">";
		str+="                      <cell><p align=\"1\">\n\n　保\n\n　证\n\n　人</p></cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\" height=\"100\">";
		str+="                      <cell><p align=\"1\">自然人</p></cell>";
		str+="                      <cell><p>（签章）：\n\n\n　</p></cell>";
		str+="                      <cell><p>（签章）：\n\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\">";
		str+="                      <cell><p align=\"1\">法人或其他组织：</p></cell>";
		str+="                      <cell><p>单位（公章）：\n\n\n法定代表人（负责人）/授权代理人\n（签章）："+responseParams.get("guarantorperson")+"\n"+responseParams.get("guarantoridcard")+"\n"+responseParams.get("guarantororgid")+"\n"+responseParams.get("putoutdate")+"　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                </table>";
		str+="          </cell>";
		//str+="			<cell colspan=\"3\"><p align=\"1\">签订日期： "+responseParams.get("putoutdate")+"</p></cell>";
		str+="		</table>";
		  }else{
	    str+="		<table colWidths=\"50\"  borderWidth=\"1\">";
		str+="          <cell>";
		str+="                <table colWidths=\"1,4,5\" borderWidth=\"1\" padding=\"0\">";
		str+="                      <cell><p align=\"1\">\n\n　保\n\n　证\n\n　人</p></cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\" height=\"100\">";
		str+="                      <cell><p align=\"1\">自然人</p></cell>";
		str+="                      <cell><p>（签章）："+responseParams.get("guarantorperson")+"\n"+responseParams.get("guarantoridcard")+"\n"+responseParams.get("putoutdate")+"\n　</p></cell>";
		str+="                      <cell><p>（签章）：\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\">";
		str+="                      <cell><p align=\"1\">法人或其他组织：</p></cell>";
		str+="                      <cell><p>单位（公章）：\n\n\n法定代表人（负责人）/授权代理人\n（签章）：\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                </table>";
		str+="          </cell>";
		//str+="			<cell colspan=\"3\"><p align=\"1\">签订日期： "+responseParams.get("putoutdate")+"</p></cell>";
		str+="		</table>"; 
		  }
		}
		
		//附件二：抵押担保函
		if(((String) responseParams.get("mortgageperson")).length()!=0){
		str+="		<br/>";
		str+="		<br/>";	
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		附件二：</p>";
		str+="		<br/>";	
		str+="		<br/>";	
		str+="		<p fontSize=\"30\" fontColor=\"#000000\" align=\"1\" >抵 押 担 保 函 </p>";
		str+="		<br/>";	
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >   "+responseParams.get("mortgageperson")+" （下称“抵押担保人”）知悉丙方平台网站编号为 "+responseParams.get("SubContractNo")+"《借款协议书》（以下简称借款协议）的债权人（即出借人，明细详见借款协议书） 与 "+responseParams.get("borrowname")+"(以下称“借款人”）签订的编号为  "+responseParams.get("SubContractNo")+" 的《借款协议书》，并愿为该借款协议项下借款人之债务提供抵押担保。</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >   1．抵押担保范围为《借款协议书》项下的全部债务，包括但不限于违约金、逾期罚息、利息、本金、风险管理费、平台服务费、实现债权的费用。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >   2．抵押担保人确认，当债务人违约时，即按本抵押担保函规定的担保范围以抵押物承担担保责任。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >   3．抵押担保函不因借款协议因为任何原因而发生的不成立、不生效、无效、部分无效或被撤销、被解除而影响抵押担保函的约定。  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >   4．抵押担保函有效期间为借款协议项下债务履行期限届满之日起两年。 </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5．抵押担保人在抵押担保函有效期内声明和担保如下：  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.1 抵押担保人为依法注册并合法存续的法人企业、其他组织或自然人，具备签订和履行抵押担保函所需的完全民事权利能力和行为能力；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.2 抵押担保人阅读并完全了解借款协议及抵押担保函的所有内容，签署和履行抵押担保函系基于抵押担保人的真实意思表示，已经按照章程及其他内部管理文件要求取得合法、有效的授权，且不会违反对本公司有约束力的任何协议、合同和其他法律文件； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.3 抵押担保人已经取得签订和履行抵押担保函所需的一切有关批准文件、许可、备案或者登记；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.4 代表抵押担保人签署抵押担保函的授权签字人经过合法、有效的公司授权； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.5 抵押担保人为公司的，提供该抵押担保已经按照公司章程的规定由董事会或者股东会、股东大会决议通过；其公司章程对抵押担保的总额及单项抵押担保的数额有限额规定的，抵押担保函项下抵押担保未超过规定的限额；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.6 抵押担保人向丙方平台网站提供的有关文件、资料是准确、真实、完整、有效的；      </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.7 抵押担保人接受丁方风险管理人对有关生产经营、财务活动的监督检查，并给予足够的协助和配合；</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.8 若抵押担保人发生可能影响担保人财务状况和履约能力的情况，抵押担保人应立即书面通知丁方风险管理人； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.9 抵押担保人发生名称变更、法定代表人（负责人）、住所、经营范围、注册资本金或公司章程等工商登记事项变更的，应当在变更后7个工作日内书面通知丙方平台网站； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.10 抵押担保人承诺如借款人不能按借款协议约定足额归还本息，抵押担保人将代其足额偿还借款本息。若抵押担保人未能履行约定，丁方风险管理人有权处置抵押物用以清偿借款本息及相关费用； </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >5.11 抵押担保函项下的声明和担保是连续和有效的，其担保责任不因债权人给予借款人任何宽容、宽限或优惠或延缓行使权利而受影响，也不因借款协议书的修改、补充、变更时视为抵押担保人重复作出。</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	6.该抵押担保函是借款协议书的组成部分，具有同等法律效力。</p>";
		str+="		<br/>";	
		str+="		<br/>";	
		if(((String) responseParams.get("mortgageorgid")).length()!=0){
		str+="		<table colWidths=\"50\"  borderWidth=\"1\">";
		str+="          <cell>";
		str+="                <table colWidths=\"1,4,5\" borderWidth=\"1\" padding=\"0\">";
		str+="                      <cell><p align=\"1\">\n\n　抵\n\n　押\n\n　人</p></cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\" height=\"100\">";
		str+="                      <cell><p align=\"1\">自然人</p></cell>";
		str+="                      <cell><p>（签章）：\n\n\n　</p></cell>";
		str+="                      <cell><p>（签章）：\n\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\">";
		str+="                      <cell><p align=\"1\">法人或其他组织：</p></cell>";
		str+="                      <cell><p>单位（公章）：\n\n\n法定代表人（负责人）/授权代理人\n（签章）："+responseParams.get("mortgageperson")+"\n"+responseParams.get("mortgageidcard")+"\n"+responseParams.get("mortgageorgid")+"\n"+responseParams.get("putoutdate")+"\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                </table>";
		str+="          </cell>";
		//str+="			<cell colspan=\"3\"><p align=\"1\">签订日期："+responseParams.get("putoutdate")+"</p></cell>";
		str+="		</table>";
		 }else{
		str+="		<table colWidths=\"50\"  borderWidth=\"1\">";
		str+="          <cell>";
		str+="                <table colWidths=\"1,4,5\" borderWidth=\"1\" padding=\"0\">";
		str+="                      <cell><p align=\"1\">\n\n　抵\n\n　押\n\n　人</p></cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\" height=\"100\">";
		str+="                      <cell><p align=\"1\">自然人</p></cell>";
		str+="                      <cell><p>（签章）："+responseParams.get("mortgageperson")+"\n"+responseParams.get("mortgageidcard")+"\n"+responseParams.get("putoutdate")+"\n　</p></cell>";
		str+="                      <cell><p>（签章）：\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                      <cell>";
		str+="		<table colWidths=\"10\">";
		str+="                      <cell><p align=\"1\">法人或其他组织：</p></cell>";
		str+="                      <cell><p>单位（公章）：\n\n\n法定代表人（负责人）/授权代理人\n（签章）：\n\n　</p></cell>";
		str+="                </table>";
		str+="                      </cell>";
		str+="                </table>";
		str+="          </cell>";
		//str+="			<cell colspan=\"3\"><p align=\"1\">签订日期："+responseParams.get("putoutdate")+"</p></cell>";
		str+="		</table>";
		 }
		}
		//协议附件（三）、催收授权委托书
		if(!"Borrow".equals(contractType)){
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >		协议附件（三）</p>";
		str+="		<br/>";	
		str+="		<br/>";	
		str+="		<p fontSize=\"30\" fontColor=\"#000000\" align=\"1\" >催收授权委托书  </p>";
		str+="		<br/>";	
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >授权人：</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	姓名："+responseParams.get("lendername")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >证件号码（身份证号码）："+responseParams.get("lendercard")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >齐乐融融投融资E平台用户名："+responseParams.get("lenderid")+"</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	被授权人：北京博融天下信息技术有限公司  </p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" > 鉴于授权人在齐乐融融投融资E平台签署电子版《借款协议书》（协议书编号："+responseParams.get("SubContractNo")+"），待《借款协议书》中的借款人（以下简称“借款人”）获得授权人的全部资金后，授权人授权被授权人可随时以手机短信、电话、信函、电子邮件或其他合法方式提示或催告借款人履行还款义务。授权人同意并确认被授权人在授权期内，可将该提示并／或催告的权利转委托予被授权人确认的第三方受托人。</p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	授权人知晓并同意，本授权书自授权人在平台在线点击“确认”时生效。 </p>";
		str+="		<br/>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	授权人：</p>";
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	姓   名："+responseParams.get("lendername")+"</p>";
		str+="		<br/>";	
		str+="		<p fontSize=\"10\" fontColor=\"#000000\" firstLineIndent=\"-10\" align=\"0\" idleft=\"10\" >	日期： "+responseParams.get("putoutdate")+"</p>";
		str+="		<br/>";	
		}
		
	
		str+="	</body>";
		str+="</doc>";
		
		return str;
	}

	@Override
	public void run() {		
		try {
			CreatePDF();
		} catch (HandlerException e) {
			e.printStackTrace();
		}
	}	
}