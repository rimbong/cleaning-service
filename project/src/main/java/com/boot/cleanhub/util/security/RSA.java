package com.boot.cleanhub.util.security;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.stereotype.Component;

/* 
 * RSA 관련 함수
 * encode , decode는 BASE64 방식과 HEX(16진수) 방식 2가지가 있다.
 */
@Component
public class RSA {

    private static final String INSTANCE_TYPE = "RSA";
    private String publicKeyModulus = "";
    private String publicKeyExponent = "";
    private String publicKeyString = "";
    private String privateKeyString = "";
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    /**
     * <pre>
     * 쓰레드 세이프한 싱글톤 패턴
     * 클래스안에 클래스(Holder)를 두어 JVM의 Class loader 매커니즘과 Class가 로드되는 시점을 이용하는 방법
     * </pre>
     * 
     * @return 인스턴스
     */

    // public RSA() {}

    // private static class RSAHolder {
    //     public static final RSA instance = new RSA();
    // }

    // public static RSA getInstance() {
    //     return RSAHolder.instance;
    // }

    public String getPublicKeyModulus() {
        return publicKeyModulus;
    }

    public void setPublicKeyModulus(String publicKeyModulus) {
        this.publicKeyModulus = publicKeyModulus;
    }

    public String getPublicKeyExponent() {
        return publicKeyExponent;
    }

    public void setPublicKeyExponent(String publicKeyExponent) {
        this.publicKeyExponent = publicKeyExponent;
    }
    
    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public void setPrivateKeyString(String privateKeyString) {
        this.privateKeyString = privateKeyString;
    }

    public String getPublicKeyString() {
        return publicKeyString;
    }

    public void setPublicKeyString(String publicKeyString) {
        this.publicKeyString = publicKeyString;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void init() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(INSTANCE_TYPE);
            generator.initialize(1024, new SecureRandom()); // 1024 or 2048 , 보안은 2048 속도는 1024

            KeyPair keyPair = generator.genKeyPair();
            KeyFactory keyFactory = KeyFactory.getInstance(INSTANCE_TYPE);

            publicKey = keyPair.getPublic(); // 공개키
            privateKey = keyPair.getPrivate(); // 개인키

            RSAPublicKeySpec publicKeySpec = (RSAPublicKeySpec) keyFactory.getKeySpec(publicKey,
                    RSAPublicKeySpec.class);
            publicKeyModulus = publicKeySpec.getModulus().toString(16);
            publicKeyExponent = publicKeySpec.getPublicExponent().toString(16);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] encryptRsa(String plainText, PublicKey publicKey) throws InvalidKeyException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(INSTANCE_TYPE);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plainText.getBytes());
    }

    public String decryptRsa(PrivateKey privateKey, byte[] encryptedBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedValue = new String(decryptedBytes, "utf-8"); // 문자 인코딩 주의.
        return decryptedValue;
    }

    public PublicKey convertToPubKey(byte[] publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(INSTANCE_TYPE);
        return keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));
    }

    public PrivateKey convertToPvtKey(byte[] privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(INSTANCE_TYPE);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }

    public byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[] {};
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            byte value = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            bytes[(int) Math.floor(i / 2)] = value;
        }
        return bytes;
    }

    public String byteArrayToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toString(bytes[i] & 0xff, 16));
        }
        return sb.toString();
    }

    public String encodeBase64ToString(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public byte[] decodeBase64(String str) {
        return Base64.getDecoder().decode(str.getBytes());
    }
}