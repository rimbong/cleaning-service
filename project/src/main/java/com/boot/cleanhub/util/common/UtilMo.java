package com.boot.cleanhub.util.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * <pre>
 * 	UtilMo 공통 모듈 Class
 * </pre>
 */
public class UtilMo {


    /**
     * <pre>
     *   개행, 빈칸, 공백 등 전부 제거
     * </pre>
     * @author In-seong Hwang
     * @since 2024.08.13
     * @version 1.0
     */
    public static String removeTrimAll(String str) {
        String result = "";
        if (hasText(str)) {
            result = str.trim();
            result = str.replaceAll(" ", "");
            result = str.replaceAll("(\r\n|\r|\n|\n\r)", "");
        }
        return result;
    }

	/**
	 * <pre>
	 *  null 또는 빈 문자열인지 체크
	 * </pre>
	 */
	public static Boolean isEmptyOrNull(String str) {
		boolean bTF = false;
		if (str == null || str.trim().length() == 0) {
			bTF = true;
		}
		return bTF;
	}
	/**
	 * <pre>
	 *   문자열이 null,길이 0,공백  이 세가지의 경우에 전부 해당하지 않는 경우 true를 리턴한다.
	 * </pre>
	 * @author In-seong Hwang
	 * @since 2023.03.29
	 * @version 1.0
	 */
	public static boolean hasText(String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}
	/**
	 * <pre>
	 *   문자열이 null,길이 0,공백  이 세가지의 경우에 전부 해당하지 않는 경우 true를 리턴한다.
	 * </pre>
	 * @author In-seong Hwang
	 * @since 2023.03.29
	 * @version 1.0
	 */
	public static boolean hasNotText(String str) {
		return !(str != null && !str.isEmpty() && containsText(str));
	}
    /**
	 * <pre>
	 *   각 문자별 공백 체크를 한다. 
	 * </pre>
	 * @author In-seong Hwang
	 * @since 2023.03.29
	 * @version 1.0
	 */
    private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}
	/**
	 * <pre>
	 *  null 또는 빈 문자열이라면 replace 문자열로 대체
	 * </pre>
	 */
	public static String ifHasNotText(String str, String replaceStr) {
		if (hasNotText(str)) {
			str = replaceStr;
		}
		return str;
	}

	/**
	 * <pre>
	 * 정해진 길이만큼 랜덤 숫자 생성 메서드
	 * </pre>
	 */
	public static String getRandomInt(int randomNumber) {
		Random random = new Random();
		String result = "";

		for (int i = 0; i < randomNumber; i++) {
			result += random.nextInt(10);
		}

		return result;
	}

	/**
	 * <pre>
	 * 대문자, 소문자, 숫자 포함하여 정해진 길이만큼 랜덤 문자열 생성 메서드
	 * </pre>
	 */
	public static String getRandomString(int randomNumber) {
		Random random = new Random();
		String result = "";

		for (int i = 0; i < randomNumber; i++) {
			int choice = random.nextInt(3);

			// 대문자
			if (choice == 0) {
				char bch = (char) (random.nextInt(26) + 65);
				result += Character.toString(bch);

				// 소문자
			} else if (choice == 1) {
				char sch = (char) (random.nextInt(26) + 97);
				result += Character.toString(sch);

				// 숫자
			} else {
				result += random.nextInt(10);
			}
		}

		return result;
	}

	/**
	 * <pre>
	 * 대문자, 숫자 포함하여 정해진 길이만큼 랜덤 문자열 생성 메서드
	 * </pre>
	 */
	public static String getRandomUpperString(int randomNumber) {
		Random random = new Random();
		String result = "";

		for (int i = 0; i < randomNumber; i++) {
			int choice = random.nextInt(2);

			// 대문자
			if (choice == 0) {
				char bch = (char) (random.nextInt(26) + 65);
				result += Character.toString(bch);

			}
			// 숫자
			else {
				result += random.nextInt(10);
			}
		}

		return result;
	}

	/**
	 * <pre>
	 * 영문 숫자 포함 난수 발생후 유일한 Token값 생성 메소드
	 * </pre>
	 * 
	 * @param size
	 *             발생시킬 난수의 개수
	 * @return 입력받은 개수만큼 난수를 통해 생성된 숫자와 영문의 조합값
	 */
	public static String getToken(int randomNumber) {
		Random rand = new Random();
		String token = "";

		int i = 50;
		while (i-- > 0) {
			char ch = (char) (rand.nextInt(26) + 97);
			int in = rand.nextInt(9);
			token += Character.toString(ch) + in;
		}
		return token.substring(0, randomNumber);
	}

	/**
	 * <pre>
	 *   유니코드 파일을 특수문자로 맵핑함.
	 * </pre>
	 * 
	 * @param str
	 *            : unicode 문자열
	 * @return 특수문자 맵핑 문자열
	 */
	public static String mappingUnicode(String str) {

		// 파일명에 사용되는 특수문자
		char[] ch = { '~', '!', '@', '#', '$', '%', '&', '(', ')', '=', ';', '[', ']', '{', '}', '^', '-' };
		try {
			for (char c : ch) {
				String encodeData = URLEncoder.encode(c + "", "UTF-8");
				str = str.replaceAll(encodeData, "\\" + c);
			}
			str = str.replaceAll("%2B", "+"); // 띄워쓰기 의 경우 치환함
			str = str.replaceAll("%2C", "_"); // 콤마의 경우 언더바로 치환함
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}

	/**
	 * <pre>
	 * Email 유효성 체크 xxxx.xxxx@xxxx.com 의 경우 유효성 검증함.
	 * </pre>
	 * 
	 * @param data : 검증 데이터
	 * 
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatEmail(String data) {
		return (data.trim().matches("(\\w+\\.)*\\w+\\@(\\w+\\.)+\\w+")) ? true : false;
	}

	/**
	 * <pre>
	 * 주민등록 번호 유효성 체크 xxxxxx-xxxxxxx 의 형태임.
	 * </pre>
	 * 
	 * @param data
	 *             : 검증 데이터
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatRegNum(String data) {

		data = data.trim().replaceAll("\\-", "");

		// [1] 주민번호의 개수 비교
		if (data.length() != 13) {
			return false;
		}

		// [2] 주민번호 가중치 계산
		int regNumWeight[] = { 2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5 }; // 주민등록 번호 가중치
		int sum = 0;
		for (int i = 0; i < 12; i++) {
			sum += (Character.getNumericValue(data.charAt(i)) * regNumWeight[i]);
		}

		// [3] 주민번호 최종 검증
		int result = (sum % 11);
		if (result == 0) {
			result = 1;
		} else if (result == 1) {
			result = 0;
		} else {
			result = 11 - result;
		}

		if (result == Character.getNumericValue(data.charAt(12))) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * <pre>
	 * 전화번호 형식 xxxxxx-xxx 포맷 유효성 검증
	 * </pre>
	 * 
	 * @param data
	 *             : 검증할 데이터
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatTelCode(String data) {
		return (data.trim()
				.matches("(02|031|032|033|041|042|043|051|052|053|054|055|062|061|063|064|070)(\\d{3,4})(\\d{4})"))
						? true
						: false;
	}

	/**
	 * <pre>
	 * 핸드폰 번호 형식 xxx-xxxx-xxxx 포맷 유효성 검증
	 * </pre>
	 * 
	 * @param data
	 *             : 검증할 데이터
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatPhoneCode(String data) {
		return (data.trim().matches("(010|011|016|017|018|019)(\\d{3,4})(\\d{4})")) ? true : false;
	}

	/**
	 * <pre>
	 * 문자열 MaxLength 유효성 검증
	 * </pre>
	 * 
	 * @param data
	 *             : 검증할 데이터
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatMaxStringLength(String data, int maxLen) {
		return (data.trim().length() < maxLen) ? true : false;
	}

	/**
	 * <pre>
	 * 문자열 Max Byte 유효성 검증
	 * </pre>
	 * 
	 * @param data
	 *             : 검증할 데이터
	 * @return 검증결과 (true/false)
	 */
	public static boolean checkFormatMaxByteSize(String data, int maxLen) {
		return (data.trim().getBytes().length > maxLen) ? true : false;
	}
    
    /**
     * 입력이 안전한지 검증 (알파벳, 숫자, 공백, 한글만 허용)
     * @param input 사용자 입력
     * @return 안전하면 true, 아니면 false
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return false;
        }
        Pattern tagPattern = Pattern.compile(
            "<\\s*script\\s*>|<\\/\\s*script\\s*>",
            Pattern.CASE_INSENSITIVE
        );
        Pattern inputPattern = Pattern.compile("^[a-zA-Z0-9\\s\\uAC00-\\uD7AF]*$");
        
        // <script> 또는 </script> 태그(공백 포함) 감지
        if (tagPattern.matcher(input).find()) {
            return false;
        }
        return inputPattern.matcher(input).matches();
    }
    
    /**
     * HTML 콘텍스트에서 <script> 태그(공백 포함)만 이스케이프
     * @param input 사용자 입력
     * @return <script> 태그가 이스케이프된 문자열
     */
    public static String escapeScriptTag(String input) {
        if (input == null) {
            return "";
        }
        // <script> 또는 </script> 태그를 공백 포함 여부와 관계없이 탐지
        Pattern tagPattern = Pattern.compile(
            "<\\s*script\\s*>|<\\/\\s*script\\s*>",
            Pattern.CASE_INSENSITIVE
        );
    
        // <script> 또는 </script> 태그(공백 포함)가 포함된 경우에만 이스케이프
        if (tagPattern.matcher(input).find()) {
            StringBuilder escaped = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '<':
                        escaped.append("&lt;");
                        break;
                    case '>':
                        escaped.append("&gt;");
                        break;
                    case '&':
                        escaped.append("&amp;");
                        break;
                    case '"':
                        escaped.append("&quot;");
                        break;
                    case '\'':
                        escaped.append("&#x27;");
                        break;
                    default:
                        escaped.append(c);
                }
            }
            return escaped.toString();
        }
        // <script>가 없으면 원본 반환
        return input;
    }
    /**
     * <pre>
     *   사용자의 모든 아이피를 얻기
     * </pre>
     * @author In-seong Hwang
     * @since 2026.02.25
     * @version 1.0
     */
    public ArrayList<String> getClientIpList(HttpServletRequest req) throws IOException, Exception {
        ArrayList<String> ipList = new ArrayList<String>();

        String oneIpAddress = null;
        String xffAddr = null;
        String remoteAddr = null;

        Enumeration<NetworkInterface> en = null;
        NetworkInterface ni = null;
        Enumeration<InetAddress> inetAddresses = null;
        InetAddress ia = null;

        en = NetworkInterface.getNetworkInterfaces();
        if (en == null) {
            return null;
        }

        while (en.hasMoreElements()) {
            ni = en.nextElement();
            if (ni == null || ni.isLoopback()) {
                continue;
            }

            inetAddresses = ni.getInetAddresses();
            if (inetAddresses == null) {
                continue;
            }

            while (inetAddresses.hasMoreElements()) {
                ia = inetAddresses.nextElement();
                if (ia == null) {
                    continue;
                }

                if (ia.getHostAddress() != null && ia.getHostAddress().indexOf(".") > -1) {
                    oneIpAddress = ia.getHostAddress();

                    if (oneIpAddress != null && oneIpAddress.length() > 0) {
                        ipList.add(oneIpAddress);
                    }
                    break;
                }
            }
        }

        // X-Forwarded-For 에서 얻기
        xffAddr = req.getHeader("X-Forwarded-For");
        if (xffAddr != null && xffAddr.length() > 0 && !xffAddr.equals("unknown")) {
            ipList.add(xffAddr);
        }

        // RemoteAddr 얻기
        remoteAddr = req.getRemoteAddr();
        if (remoteAddr != null && remoteAddr.length() > 0) {
            ipList.add(remoteAddr);
        }

        return ipList;
    }
}
