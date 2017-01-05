package com.amarsoft.p2ptrade.tools;



import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 三重des加密算法
 * @author flian
 *
 */
public class Des3Encryption  {
	
	protected byte[] key;
	
	protected String iv;

	public void setIV(String iv) {
		this.iv = iv;
	}

	public void setEncryptKey(byte[] key) {
		this.key = key;
	}

	public void setDecryptKey(byte[] key) {
		setEncryptKey(key);
	}

	public byte[] encrypt(byte[] data) throws Exception {
		Key deskey = null;  
        DESedeKeySpec spec = new DESedeKeySpec(this.key);  
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");  
        deskey = keyfactory.generateSecret(spec);  
        Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");  
        IvParameterSpec ips = new IvParameterSpec(iv.getBytes());  
        cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);  
        byte[] encryptData = cipher.doFinal(data);  
        return encryptData;  
	}

	public byte[] decrypt(byte[] data) throws Exception {
		Key deskey = null;  
        DESedeKeySpec spec = new DESedeKeySpec(key);  
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");  
        deskey = keyfactory.generateSecret(spec);  
        Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");  
        IvParameterSpec ips = new IvParameterSpec(iv.getBytes());  
        cipher.init(Cipher.DECRYPT_MODE, deskey, ips);  
  
        byte[] decryptData = cipher.doFinal(data);  
  
        return decryptData;
	}

}
