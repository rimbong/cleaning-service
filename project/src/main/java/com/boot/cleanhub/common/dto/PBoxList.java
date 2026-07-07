package com.boot.cleanhub.common.dto;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * ArrayList를 상속받아 구현한 SBoxList
 * 
 * @param <E>
 */
@Component
@Qualifier("pBoxList")
public class PBoxList<E> extends ArrayList<E> {

	/**
	 * serialVersionID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * <pre>
	 * PBoxList 생성자
	 * </pre>
	 * 
	 */
	public PBoxList() {
		super();
	}

	/**
	 * <pre>
	 * List 를 가져와 PBoxList로 변환하는 오버로딩 된 생성자
	 * </pre>
	 * 
	 * @param list
	 *            변환할 대상의 List 객체
	 */
	@SuppressWarnings("unchecked")
	public PBoxList(List<?> list) {
		if (list != null) {
			Iterator<?> it = list.iterator();
			while (it.hasNext()) {
				this.add((E) it.next());
			}
		}
	}

	/**
	 * <pre>
	 * PBoxList에 Object 셋팅하는 메소드
	 * </pre>
	 * 
	 * @param obj
	 *            SBoxList에 셋팅할 값
	 */
	public void set(E obj) {
		super.add(obj);
	}

	/**
	 * <pre>
	 * PBoxList 에 Object Array 셋팅하는 메소드
	 * </pre>
	 * 
	 * @param objs
	 *            : 추가할 Object Array
	 */
	public void set(E[] objs) {
		for (E obj : objs) {
			super.add(obj);
		}
	}

	/**
	 * <pre>
	 * JSON 형식의 Data 를 SBox를 포함한 PBoxList Data로 Settting 함
	 * </pre>
	 * 
	 * @param jsonData
	 *            : json 형식의 Data
	 */
	@SuppressWarnings("unchecked")
	public void setJson(String jsonData) {

		Object obj = JSONValue.parse(jsonData);
		JSONArray array = (JSONArray) obj;

		for (int i = 0; i < array.size(); i++) {
			if (array.get(i) instanceof JSONObject) {
				PBox sBox = new PBox();
				sBox.putAll((JSONObject) array.get(i));
				this.set((E) sBox);
			} else if (array.get(i) instanceof JSONArray) {
				Object tmp = JSONValue.parse((String) array.get(i));
				JSONArray sub_array = (JSONArray) tmp;
				System.out.println(sub_array.toJSONString());
				for (int index = 0; i < sub_array.size(); index++) {
					if (array.get(index) instanceof JSONObject) {
						PBox tmpBox = new PBox();
						tmpBox.putAll((JSONObject) array.get(index));
						this.set((E) tmpBox);
					}
				}
			}

		}

	}

	/**
	 * <pre>
	 * PBoxList 를 String 으로 출력하는 메소드
	 * </pre>
	 * 
	 * @return SBoxList 출력 String
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer();

		if (this != null) {
			Iterator<E> itList = this.iterator();
			while (itList.hasNext()) {
				E obj = itList.next();
				if (obj instanceof PBox) {
					sb.append(obj.toString());
				} else if (obj instanceof PBoxList) {
					sb.append("SBoxList Object");
				} else if (obj instanceof String[]) {
					String temp = "";
					for (int i = 0; i < ((String[]) obj).length; i++) {
						temp += ((String[]) obj)[i];
						temp += ",";
					}
					if (((String[]) obj).length > 0) {
						sb.append(temp.substring(0, temp.length() - 1));
					}
				} else if (obj instanceof String) {
					sb.append((String) obj);
				} else {
					sb.append(obj);
				}
				sb.append(",");
			}
		} 
		return sb.length() > 0 ? sb.toString().substring(0, sb.length() - 1) : "";
	}

	/**
	 * <pre>
	 * PBoxList 를 줄바꿈 하여 String 으로 출력하는 메소드
	 * </pre>
	 * 
	 * @return PBoxList 출력 String
	 */
	public String println() {

		StringBuffer sb = new StringBuffer();

		if (this != null) {
			Iterator<E> itList = this.iterator();
			while (itList.hasNext()) {
				E obj = itList.next();
				if (obj instanceof PBox) {
					sb.append(obj.toString());
				} else if (obj instanceof PBoxList) {
					sb.append("SBoxList Object");
				} else if (obj instanceof String[]) {
					String temp = "";
					for (int i = 0; i < ((String[]) obj).length; i++) {
						temp += ((String[]) obj)[i];
						temp += ",";
					}
					if (((String[]) obj).length > 0) {
						sb.append(temp.substring(0, temp.length() - 1));
					}
				} else if (obj instanceof String) {
					sb.append((String) obj);
				} else {
					sb.append(obj);
				}
				sb.append(",");
				sb.append("\n");
			}
		}
		return sb.length() > 0 ? sb.toString().substring(0, sb.length() - 2) : "";
	}

	/**
	 * <pre>
	 * PBoxList 를 JSON 타입으로 변환하는 메소드
	 * </pre>
	 * 
	 * @return JSON 변환 문자열
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public String toJSON() throws IOException {
		PBoxList<Object> cloneObj = this.recursiveEncode((PBoxList<Object>) ((PBoxList<Object>) this).clone(), "euc-kr");
		return this.recursiveChangeJson(cloneObj);
	}

	/**
	 * <pre>
	 * PBoxList 를 JSON 타입으로 변환하는 메소드
	 * </pre>
	 * 
	 * @param encode
	 *            = 인코딩
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public String toJSON(String encode) throws IOException {
		PBoxList<Object> cloneObj = this.recursiveEncode((PBoxList<Object>) ((PBoxList<Object>) this).clone(), encode);
		return this.recursiveChangeJson(cloneObj);
	}

	/**
	 * <pre>
	 * PBoxList 내의 함수 인코딩 메소드.
	 * 재귀호출 함수로써 오버로딩하여 사용된다.
	 * </pre>
	 * 
	 * @param list
	 *            Source Object
	 * @param encode
	 *            Encode Type
	 * @return Result Object(SBoxList)
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private PBoxList<Object> recursiveEncode(PBoxList<Object> list, String encode) throws IOException {

		for (int i = 0; i < list.size(); i++) {

			if (list.get(i) instanceof PBox) {
				list.set(i, this.recursiveEncode((PBox) list.get(i), encode));
			} else if (list.get(i) instanceof PBoxList) {
				list.set(i, this.recursiveEncode((PBoxList<Object>) list.get(i), encode));
			} else if (list.get(i) instanceof String) {
				list.set(i, URLEncoder.encode((String) list.get(i), encode));
			} else if (list.get(i) instanceof String[]) {
				int len = ((String[]) list.get(i)).length;
				String[] temp = new String[len];
				System.arraycopy((String[]) list.get(i), 0, temp, 0, len);
				for (int j = 0; j < temp.length; j++) {
					temp[j] = URLEncoder.encode(temp[j], encode);
				}
				list.set(i, temp);

			} else if (list.get(i) instanceof java.sql.Time) {
				list.set(i, URLEncoder.encode(((java.sql.Time) list.get(i)).toString(), encode));
			} else if (list.get(i) instanceof java.sql.Date) {
				list.set(i, URLEncoder.encode(((java.sql.Date) list.get(i)).toString(), encode));
			} else if (list.get(i) instanceof java.sql.Timestamp) {
				String result = ((java.sql.Timestamp) list.get(i)).toString()
						.substring(0, ((java.sql.Timestamp) list.get(i)).toString().indexOf("."));
				list.set(i, URLEncoder.encode(result, encode));
			}
		}
		return list;
	}

	/**
	 * <pre>
	 * PBox 내의 함수 인코딩 메소드.
	 * 재귀호출 함수로써 오버로딩하여 사용된다.
	 * </pre>
	 * 
	 * @param PBox
	 *            Source Object
	 * @param encode
	 *            Encode Type
	 * @return Result Object(SBox)
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private PBox recursiveEncode(PBox pBox, String encode) throws IOException {

		Iterator<String> it = pBox.keySet().iterator();

		while (it.hasNext()) {
			String key = (String) it.next();

			if (pBox.get(key) instanceof PBox) {
				pBox.set(key, recursiveEncode((PBox) pBox.get(key), encode));
			} else if (pBox.get(key) instanceof PBoxList) {
				pBox.set(key, this.recursiveEncode((PBoxList<Object>) pBox.get(key), encode));
			} else if (pBox.get(key) instanceof String) {
				pBox.set(key, URLEncoder.encode(pBox.getString(key), encode));
			} else if (pBox.get(key) instanceof String[]) {
				int len = ((String[]) pBox.get(key)).length;
				String[] temp = new String[len];
				System.arraycopy((String[]) pBox.get(key), 0, temp, 0, len);
				for (int j = 0; j < temp.length; j++) {
					temp[j] = URLEncoder.encode(temp[j], encode);
				}
				pBox.set(key, temp);
			} else if (pBox.get(key) instanceof java.sql.Time) {
				pBox.set(key, URLEncoder.encode(((java.sql.Time) pBox.get(key)).toString(), encode));
			} else if (pBox.get(key) instanceof java.sql.Date) {
				pBox.set(key, URLEncoder.encode(((java.sql.Date) pBox.get(key)).toString(), encode));
			} else if (pBox.get(key) instanceof java.sql.Timestamp) {
				String result = ((java.sql.Timestamp) pBox.get(key)).toString().substring(0,
						((java.sql.Timestamp) pBox.get(key)).toString().indexOf("."));
				pBox.set(key, URLEncoder.encode(result, encode));
			}

		}

		return pBox;
	}

	/**
	 * <pre>
	 * PBox를 JSON 타입으로 재귀호출하며 문자열로 변환한다.
	 * </pre>
	 * 
	 * @param pBox
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String recursiveChangeJson(PBox pBox) {
		StringBuffer sb = new StringBuffer();

		if (pBox != null && pBox.size() != 0) {
			sb.append("{");

			Iterator<String> it = pBox.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				sb.append("\"" + key + "\"");
				sb.append(":");
				if (pBox.get(key) instanceof PBox) {
					sb.append(this.recursiveChangeJson((PBox) pBox.get(key)));
					sb.append(",");

				} else if (pBox.get(key) instanceof PBoxList) {
					sb.append(this.recursiveChangeJson((PBoxList<Object>) pBox.get(key)));
					sb.append(",");
				} else if (pBox.get(key) instanceof String[]) {
					sb.append("[");
					int len = ((String[]) pBox.get(key)).length;
					String[] temp = new String[len];
					System.arraycopy((String[]) pBox.get(key), 0, temp, 0, len);
					for (int j = 0; j < temp.length; j++) {
						sb.append("\"" + temp[j] + "\"");
						sb.append(",");
					}
					sb.replace(sb.length() - 1, sb.length(), "");
					sb.append("],");

				} else {
					sb.append("\"" + pBox.get(key) + "\"");
					sb.append(",");
				}
			}
			sb.replace(sb.length() - 1, sb.length(), "");
			sb.append("}");
		}

		return sb.toString();
	}

	/**
	 * <pre>
	 * PBoxList를 JSON 타입으로 재귀호출하며 문자열로 변환한다.
	 * </pre>
	 * 
	 * @param PBoxList
	 *            Data
	 * @return : PBoxList 내부 데이터를 String,String의 형태로 출력함.
	 */
	@SuppressWarnings("unchecked")
	private String recursiveChangeJson(PBoxList<Object> list) {
		StringBuffer sb = new StringBuffer();

		if (list != null && list.size() != 0) {

			sb.append("[");
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof PBox) {
					sb.append(this.recursiveChangeJson((PBox) list.get(i)));
					sb.append(",");
				} else if (list.get(i) instanceof PBoxList) {
					sb.append(this.recursiveChangeJson((PBoxList<Object>) list.get(i)));
					sb.append(",");
				} else if (list.get(i) instanceof String[]) {
					sb.append("[");
					int len = ((String[]) list.get(i)).length;
					String[] temp = new String[len];
					System.arraycopy((String[]) list.get(i), 0, temp, 0, len);
					for (int j = 0; j < temp.length; j++) {
						sb.append("\"" + temp[j] + "\"");
						sb.append(",");
					}
					sb.replace(sb.length() - 1, sb.length(), "");
					sb.append("],");

				} else {
					sb.append("\"" + list.get(i) + "\"");
					sb.append(",");
				}
			}
			sb.replace(sb.length() - 1, sb.length(), "");
			sb.append("]");

		}

		return sb.toString();
	}

}