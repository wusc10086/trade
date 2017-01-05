package com.amarsoft.p2ptrade.loan;

import java.util.Properties;

import com.amarsoft.are.jbo.BizObject;
import com.amarsoft.are.jbo.BizObjectManager;
import com.amarsoft.are.jbo.BizObjectQuery;
import com.amarsoft.are.jbo.JBOFactory;
import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.mobile.webservice.business.HandlerException;
import com.amarsoft.mobile.webservice.business.JSONHandler;

public class BusinessConfirmHandler extends JSONHandler{

	@Override
	public Object createResponse(JSONObject request, Properties arg1)
			throws HandlerException {
		return createJsonObject(request);
	}	
	
	private JSONObject createJsonObject(JSONObject request) throws HandlerException{
		JSONObject result=new JSONObject();
		String CustomerID=(String)request.get("CustomerID");//客户编号
		String Istenement=(String)request.get("Istenement");//是否有物业 
		String Properties=(String)request.get("Properties");//物业性质
		String CreditReport=(String)request.get("CreditReport");//信用报告显示
		String LoanStartTime=(String)request.get("LoanStartTime");//贷款起始时间
		String BuyHouseTime=(String)request.get("BuyHouseTime");//房产购买时间
		double BuildSpace=Double.parseDouble(request.get("BuildSpace").toString().equals("")?"0":request.get("BuildSpace").toString());//建筑面积
		double Sprice=Double.parseDouble(request.get("Sprice").toString().equals("")?"0":request.get("Sprice").toString());//房产购买价格
		double LoanMoney=Double.parseDouble(request.get("LoanMoney").toString().equals("")?"0":request.get("LoanMoney").toString());//房产贷款金额 
		String HouseAdd=(String)request.get("HouseAdd");//房产地址 
		String Zip=(String)request.get("Zip");//邮政编码 
		String Iscarapply=(String)request.get("Iscarapply");//车主是否为申请人 
		String Ispasstest=(String)request.get("Ispasstest");//车辆是否通过年检
		String Isruncar=(String)request.get("Isruncar");//是否为营运车辆 
		String Carmodel=(String)request.get("Carmodel");//车辆型号
		String Insuremodel=request.get("Insuremodel").toString();//车辆已购保险 
		String Insure1=(String)request.get("Insure1");//交强险保险止期 
		String Insure2=(String)request.get("Insure2");//商业险保险止期
		String Insure3=(String)request.get("Insure3");//车损险保险止期 
		String BuyCarTime=(String)request.get("BuyCarTime");//购买车辆时间
		String Carlicense=(String)request.get("Carlicense");//车牌号 
		int 		CreditCards=Integer.parseInt(request.get("CreditCards").toString().equals("")?"0":request.get("CreditCards").toString());//信用卡数量 
		double MaxLimit=Double.parseDouble(request.get("MaxLimit").toString().equals("")?"0":request.get("MaxLimit").toString());//最大额度
		double RecentlySixNo=Double.parseDouble(request.get("RecentlySixNo").toString().equals("")?"0":request.get("RecentlySixNo").toString());//最近六期数额
		String UploadCreditReport=(String)request.get("UploadCreditReport");//上传身份证
		String userid = (String)request.get("userid");
		String creditReportFile = request.get("creditReportFile")==null?"": request.get("creditReportFile").toString();
		String carIdentityFile = request.get("carIdentityFile")==null?"": request.get("carIdentity").toString();
		String houseIdentityFile = request.get("houseIdentityFile")==null?"":request.get("houseIdentity").toString();
		
		System.out.println(creditReportFile);
		System.out.println(carIdentityFile);
		System.out.println(houseIdentityFile);
		try {

			JBOFactory jbo = JBOFactory.getFactory();
			BizObjectManager m = jbo.getManager("jbo.trade.capital_info");
			BizObjectQuery query = m.createQuery("CustomerID=:userid");
			query.setParameter("userid", userid);
			
			BizObject o = query.getSingleResult(true);
			if(o==null){	
				o = m.newObject();
				o.setAttributeValue("CustomerID", userid);
			}
				o.setAttributeValue("Istenement", Istenement);
				o.setAttributeValue("Properties", Properties);
				o.setAttributeValue("CreditReport", CreditReport);
				o.setAttributeValue("LoanStartTime", LoanStartTime);
				o.setAttributeValue("BuyHouseTime", BuyHouseTime);
				o.setAttributeValue("BuildSpace", BuildSpace);
				o.setAttributeValue("Sprice", Sprice);
				o.setAttributeValue("LoanMoney", LoanMoney);
				o.setAttributeValue("HouseAdd", HouseAdd);
				o.setAttributeValue("Zip", Zip);
				o.setAttributeValue("Iscarapply", Iscarapply);
				o.setAttributeValue("Ispasstest", Ispasstest);
				o.setAttributeValue("Isruncar", Isruncar);
				o.setAttributeValue("Carmodel", Carmodel);
				o.setAttributeValue("Insuremodel", Insuremodel);
				o.setAttributeValue("Insure1", Insure1);
				o.setAttributeValue("Insure2", Insure2);
				o.setAttributeValue("Insure3", Insure3);
				o.setAttributeValue("BuyCarTime", BuyCarTime);
				o.setAttributeValue("Carlicense", Carlicense);
				o.setAttributeValue("CreditCards", CreditCards);
				o.setAttributeValue("MaxLimit", MaxLimit);
				o.setAttributeValue("RecentlySixNo", RecentlySixNo);
				o.setAttributeValue("UploadCreditReport", UploadCreditReport);
				m.saveObject(o);
				result.put("flag", "success");
		}catch(Exception e){
			e.printStackTrace();
			throw new HandlerException("savebusinessconfirm.error");
		}	
	return result;
	}
}
