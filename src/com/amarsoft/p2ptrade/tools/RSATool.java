package com.amarsoft.p2ptrade.tools;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;

import javax.crypto.Cipher;

import com.amarsoft.are.ARE;


public class RSATool {
	
	protected static Properties parseArgs(String[] args)throws Exception{
		Properties result = new Properties();
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-e")){
				result.put("action", "e");
				result.put("text", args[i+1]);
			}
			else if(args[i].equals("-d")){
				result.put("action", "d");
				result.put("text", args[i+1]);
			}
			else if(args[i].equals("-key")){
				result.put("key", args[i+1]);
			}
		}
		return result;
	}
	
	/**
	 * @param text 待解密的内容
	 * @param keyPath 密钥存放路径
	 * 
	 * @return 解密后的text
	 */
	public static String decryptString(String text,String keyPath) throws Exception{
		byte[] key = null;
		byte[] data = null;
		RsaUtil re= new RsaUtil();
		
		String sPrikey = new String(getBytes(keyPath)).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
		key = Base64.decode(sPrikey);
		re.setDecryptKey(key);
		data = re.decrypt(Base64.decode(text));
		return new String(data);
	}

	/**
	 * @param text
	 * @return 转化为BASE64的字节流
	 */
	public static byte[] getBase64Decode(String text) throws Exception{
		return Base64.decode(text);
	}
	
	/**
	 * @param s
	 * @return BASE64的字节流转化为字符串
	 */
	public static String getBase64Encode(byte [] s) throws Exception{
		return Base64.encode(s);
	}


	/**
	 * @param text
	 * @return 转化为BASE64的字节流
	 */
	public static void main(String[] args)throws Exception {
		if(args.length==0){
			System.out.println("参数使用举例：-e -text 明文 -key pem公钥文件路径\r\n-e -text 密文 -key pkcs8密钥文件路径");
			return;
		}
		Properties p = parseArgs(args);
		if(p.containsKey("action")==false){
			System.out.println("缺少参数-e或-d");
			return;
		}
		if(p.containsKey("text")==false){
			System.out.println("缺少参数-text");
			return;
		}
		if(p.containsKey("key")==false){
			System.out.println("缺少参数-key");
			return;
		}
		
		String sAction = p.getProperty("action");
		String sText = p.getProperty("text");//"jfwomcr258025gjinihasdfy,10376987";//"mkf5zlNfP6gZ75amzUzxrZEHa8LIRBm/kOnq9w4SUXkg8UII52U/+/XqeJGk Ar8EmNXOxlsYMdBhURPEfTBg4DG6FTgU9WtNtPsYm9uzQwcTWXaeG0JN8m5+ B2ad9d3TwfEeugJ3UN9mi7AjbGHXVFRXW7LN/oe3+AX/CAbikw8=";//"25QOH1H7H9K3B5M7_MOBILESERVICE";//args[1];
		String sFile = p.getProperty("key");//"/Users/flian/Desktop/publicKey.pem";//args[2];
		RsaUtil re= new RsaUtil();
		byte[] data = null;
		
		
		
		byte[] key = null;
		//System.out.println(sPkey);
		if(sAction.equalsIgnoreCase("e")){
			//加密
			String sPubkey = new String(getBytes(sFile)).replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
			key = Base64.decode(sPubkey);
			re.setEncryptKey(key);
			data = re.encrypt(sText.getBytes());
			System.out.println(Base64.encode(data));
		}
		else if(sAction.equalsIgnoreCase("d")){
			//解密
			
			String sPrikey = new String(getBytes(sFile)).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
			System.out.println(sPrikey);
			key = Base64.decode(sPrikey);
			re.setDecryptKey(key);
			System.out.println(sText);
			data = re.decrypt(Base64.decode(sText));
			System.out.println(new String(data));
		}
		else{
			System.out.println("第一个参数无效");
		}
		//System.in.read();
		
	}

	public static byte[] getBytes(String filePath){  
        byte[] buffer = null;  
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {  
            File file = new File(filePath);  
            fis = new FileInputStream(file);  
            bos = new ByteArrayOutputStream(1000);  
            byte[] b = new byte[1000];  
            int n;  
            while ((n = fis.read(b)) != -1) {  
                bos.write(b, 0, n);  
            }  
            buffer = bos.toByteArray();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally{
        	try {
				if(fis != null){
					fis.close();
				}
				if(bos != null){
					bos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return buffer;  
    }  
}

class RsaUtil  {
	
	protected byte[] encryptKey;//加密秘钥
	protected byte[] decryptKey;//解密秘钥
	protected String iv;

	public void setEncryptKey(byte[] key) {
		this.encryptKey = key;
	}

	public void setDecryptKey(byte[] key) {
		this.decryptKey= key;
	}

	public void setIV(String iv) {
		this.iv = iv;
	}

	public byte[] encrypt(byte[] data) throws Exception {
		KeySpec keySpec = new X509EncodedKeySpec(this.encryptKey);
    	PublicKey pbk =KeyFactory.getInstance("RSA").generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, pbk);
        return cipher.doFinal(data);
	}

	public byte[] decrypt(byte[] data) throws Exception {
		KeySpec keySpec = new PKCS8EncodedKeySpec (this.decryptKey);
    	PrivateKey prk = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        // 获取私钥
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, prk);
        return cipher.doFinal(data);
	}

}

class Base64 {  
    private static final char[] legalChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();  
  
    public static String encode(byte[] data) {  
        int start = 0;  
        int len = data.length;  
        StringBuffer buf = new StringBuffer(data.length * 3 / 2);  
  
        int end = len - 3;  
        int i = start;  
        int n = 0;  
  
        while (i <= end) {  
            int d = ((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 0x0ff) << 8) | (((int) data[i + 2]) & 0x0ff);  
  
            buf.append(legalChars[(d >> 18) & 63]);  
            buf.append(legalChars[(d >> 12) & 63]);  
            buf.append(legalChars[(d >> 6) & 63]);  
            buf.append(legalChars[d & 63]);  
  
            i += 3;  
  
            if (n++ >= 14) {  
                n = 0;  
                buf.append(" ");  
            }  
        }  
  
        if (i == start + len - 2) {  
            int d = ((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 255) << 8);  
  
            buf.append(legalChars[(d >> 18) & 63]);  
            buf.append(legalChars[(d >> 12) & 63]);  
            buf.append(legalChars[(d >> 6) & 63]);  
            buf.append("=");  
        } else if (i == start + len - 1) {  
            int d = (((int) data[i]) & 0x0ff) << 16;  
  
            buf.append(legalChars[(d >> 18) & 63]);  
            buf.append(legalChars[(d >> 12) & 63]);  
            buf.append("==");  
        }  
  
        return buf.toString();  
    }  
  
    private static int decode(char c) {  
        if (c >= 'A' && c <= 'Z')  
            return ((int) c) - 65;  
        else if (c >= 'a' && c <= 'z')  
            return ((int) c) - 97 + 26;  
        else if (c >= '0' && c <= '9')  
            return ((int) c) - 48 + 26 + 26;  
        else  
            switch (c) {  
            case '+':  
                return 62;  
            case '/':  
                return 63;  
            case '=':  
                return 0;  
            default:  
                throw new RuntimeException("unexpected code: " + c);  
            }  
    }  
  
    /** 
     * Decodes the given Base64 encoded String to a new byte array. The byte array holding the decoded data is returned. 
     */  
  
    public static byte[] decode(String s) {  
  
        ByteArrayOutputStream bos = new ByteArrayOutputStream();  
        try {  
            decode(s, bos);  
        } catch (IOException e) {  
            throw new RuntimeException();  
        }  
        byte[] decodedBytes = bos.toByteArray();  
        try {  
            bos.close();  
            bos = null;  
        } catch (IOException ex) {  
            System.err.println("Error while decoding BASE64: " + ex.toString());  
        }  
        return decodedBytes;  
    }  
  
    private static void decode(String s, OutputStream os) throws IOException {  
        int i = 0;  
  
        int len = s.length();  
  
        while (true) {  
            while (i < len && s.charAt(i) <= ' ')  
                i++;  
  
            if (i == len)  
                break;  
  
            int tri = (decode(s.charAt(i)) << 18) + (decode(s.charAt(i + 1)) << 12) + (decode(s.charAt(i + 2)) << 6) + (decode(s.charAt(i + 3)));  
  
            os.write((tri >> 16) & 255);  
            if (s.charAt(i + 2) == '=')  
                break;  
            os.write((tri >> 8) & 255);  
            if (s.charAt(i + 3) == '=')  
                break;  
            os.write(tri & 255);  
  
            i += 4;  
        }  
    }  
}  
