package com.amarsoft.p2ptrade.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.amarsoft.awe.util.json.JSONObject;
import com.amarsoft.awe.util.json.JSONValue;

public class BatchUserRegister extends RunTradeService {
	static String serverURL = "http://10.1.14.17:7002/P2P_Service/runtrade.jsp";
	static String dbURL = "jdbc:oracle:thin:@10.1.96.3:1521:tpepspdev";
	static String dbUser = "amarsoft_1";
	static String dbPwd = "amarsoft_1_pw123";
	public static void runBatchRegister(Connection conn) throws Exception{
		String sqlSelect = "select * from USER_IMPORT_TEMP where Status is null and USERMAINID is null and USERSUBID is null";
		String sqlUpdate = "update USER_IMPORT_TEMP set Status=?,UserID=?,UserMainID=?,UserSubID=? where SerialNo=?";
		PreparedStatement pstmSelect = null;
		PreparedStatement pstmUpdate = null;
		ResultSet rs = null;
		try {
			pstmSelect = conn.prepareStatement(sqlSelect);
			pstmUpdate = conn.prepareStatement(sqlUpdate);
			rs = pstmSelect.executeQuery();
			while(rs.next()){
				String serialNo = rs.getString("SERIALNO");
				String userName = rs.getString("USERNAME");
				String password = rs.getString("PASSWORD");
				String mobile = rs.getString("MOBILE");
				String email = rs.getString("EMAIL");
				
				String method = "register";
				String requestFormat="json";
				String status = "S";
				
				JSONObject json = new JSONObject();
				json.put("Password", password);
				json.put("UserName", userName);
				json.put("Mobile", mobile);
				json.put("EMail", email);
				
				JSONObject request = new JSONObject();
				request.put("RequestParams", json);
				request.put("deviceType", "Pc");
				System.out.println(request.toString());
				String requestStr = request.toString();
				String sSignKey = createSignKey(method, requestFormat, requestStr);
				String result = runHttp(serverURL,method,requestFormat,requestStr,sSignKey);
				System.out.println("运行结果：" + result);
				pstmUpdate.setString(2, null);
				pstmUpdate.setString(3, null);
				pstmUpdate.setString(4, null);
				
				JSONObject response = (JSONObject) JSONValue.parse(result);
				if(response.get("returnCode") != null && "SUCCESS".equals((String)response.get("returnCode"))){
					JSONObject responseParams = (JSONObject) response.get("ResponseParams");
					String success = (String) responseParams.get("SuccessFlag");
					String userMainID = (String) responseParams.get("UserMainID");
					String userSubID = (String) responseParams.get("UserSubID");
					String userID = (String) responseParams.get("UserID");
					if("S".equals(success)){
						status = "S";
						pstmUpdate.setString(2, userID);
						pstmUpdate.setString(3, userMainID);
						pstmUpdate.setString(4, userSubID);
					}else{
						status = "F";
					}
				}else{
					status = "F";
				}
				pstmUpdate.setString(1, status);
				pstmUpdate.setString(5, serialNo);
				pstmUpdate.execute();
				conn.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			conn.rollback();
		} finally{
			if(rs != null){
				rs.close();
			}
			if(pstmSelect != null){
				pstmSelect.close();
			}
			if(pstmUpdate != null){
				pstmUpdate.close();
			}
			if(conn != null){
				conn.close();
			}
		}
	}
	
	public static Connection getConnection() throws Exception
	{
		Class.forName("oracle.jdbc.driver.OracleDriver");
		Connection connection = java.sql.DriverManager.getConnection(dbURL,dbUser,dbPwd);
		return connection;
	}
	
	public static void main(String args []){
		try {
			Connection conn = getConnection();
			runBatchRegister(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
