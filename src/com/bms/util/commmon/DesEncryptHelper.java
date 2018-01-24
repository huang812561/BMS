package com.bms.util.commmon;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.codec.binary.Base64;

public class DesEncryptHelper {
	
		private static final String KEY = "2316dd18e53e410e9f0216a6f5118eb4";
	 	private DesEncryptHelper() {
	    }

	    public static String encryption(String data) throws EncryptException {
	        return encodeBase64(encryptDES(data, KEY));
	    }

	    public static String encryption(String data, String key) throws EncryptException {
	        return encodeBase64(encryptDES(data, key));
	    }

	    public static String decryption(String data) throws DecryptException {
	        return decryptDES(decodeBase64(data.getBytes()), KEY);
	    }

	    public static String decryption(String data, String key) throws DecryptException {
	        return decryptDES(decodeBase64(data.getBytes()), key);
	    }

	    private static byte[] encryptDES(String data, String key) throws EncryptException {
	        try {
	            SecretKeyFactory e = SecretKeyFactory.getInstance("DES");
	            SecretKey securekey = e.generateSecret(new DESKeySpec(key.getBytes("UTF-8")));
	            Cipher cipher = Cipher.getInstance("DES");
	            cipher.init(1, securekey, SecureRandom.getInstance("SHA1PRNG"));
	            return cipher.doFinal(data.getBytes("UTF8"));
	        } catch (Exception var5) {
	            var5.printStackTrace();
	            throw new EncryptException(MessageEnum.DES_ENCRYPTION_FAILED.getMessage());
	        }
	    }

	    private static String decryptDES(byte[] data, String key) throws DecryptException {
	        try {
	            SecretKeyFactory e = SecretKeyFactory.getInstance("DES");
	            SecretKey securekey = e.generateSecret(new DESKeySpec(key.getBytes("UTF-8")));
	            Cipher cipher = Cipher.getInstance("DES");
	            cipher.init(2, securekey, SecureRandom.getInstance("SHA1PRNG"));
	            return new String(cipher.doFinal(data), "UTF-8");
	        } catch (Exception var5) {
	            var5.printStackTrace();
	            throw new DecryptException(MessageEnum.DES_DECRYPTION_FAILED.getMessage());
	        }
	    }

	    private static String encodeBase64(byte[] binaryData) throws EncryptException {
	        try {
	            return Base64.encodeBase64String(binaryData);
	        } catch (Exception var2) {
	            var2.printStackTrace();
	            throw new EncryptException(MessageEnum.BASE64_ENCODING_FAILED.getMessage());
	        }
	    }

	    private static byte[] decodeBase64(byte[] binaryData) throws DecryptException {
	        try {
	            return Base64.decodeBase64(binaryData);
	        } catch (Exception var2) {
	            var2.printStackTrace();
	            throw new DecryptException(MessageEnum.BASE64_DECODING_FAILED.getMessage());
	        }
	    }
}
