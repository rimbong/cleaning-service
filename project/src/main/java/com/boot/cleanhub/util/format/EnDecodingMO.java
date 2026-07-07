package com.boot.cleanhub.util.format;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;

//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;


/**
 * <pre>
 * 	EnDecodingMO 공통 모듈 Class
 * </pre>
 */
public class EnDecodingMO {
	/**
	 * <pre>
	 * UTF-8 인코딩
	 * </pre>
	 * 
	 * @param data
	 *            : 입력값
	 * 
	 * @return UTF-8 인코딩 후의 값
	 * @throws UnsupportedEncodingException
	 */
	public static String encodeUTF8(String data) throws Exception {
		return URLEncoder.encode(data, "UTF-8");
	}

	/**
	 * <pre>
	 * UTF-8 디코딩
	 * </pre>
	 * 
	 * @param data
	 *            : 입력값
	 * 
	 * @return UTF-8 디코딩 후의 값
	 * @throws UnsupportedEncodingException
	 */
	public static String decodeUTF8(String msg) throws Exception {
		return URLDecoder.decode(msg, "UTF-8");
	}

	/**
	 * <pre>
	 * Base64 인코딩
	 * </pre>
	 * 
	 * @param data
	 *            : 입력값
	 * 
	 * @return Base64 인코딩 후의 값
	 * @throws UnsupportedEncodingException
	 */
	public static String encodeBase64(String data) throws Exception {
//		return new BASE64Encoder().encode(data.getBytes());
	    return Base64.getEncoder().encodeToString(data.getBytes());
	}

	/**
	 * <pre>
	 * Base64디코딩
	 * </pre>
	 * 
	 * @param data
	 *            : 입력값
	 * 
	 * @return Base64 디코딩 후의 값
	 * @throws IOException
	 */
	public static String decodeBase64(String data) throws Exception {
//		return new String(new BASE64Decoder().decodeBuffer(data));
	    return new String(Base64.getDecoder().decode(data));
	}

}
