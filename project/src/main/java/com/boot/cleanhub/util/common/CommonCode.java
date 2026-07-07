package com.boot.cleanhub.util.common;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.common.dto.PBoxList;

/**
 * <pre>
 * 공통코드 클래스
 * CommonCode.[codeName] 으로 호출 함
 * 
 * 공통코드 종류 : 
 *  - 전화번호 국번 , 이메일 주소, 핸드폰 앞3자리
 *  
 * 기타 추가하고 싶은 코드를 아래와 같이 static final로 추가하면 됨.
 * </pre>
 */
public class CommonCode {
	
	/* 공통 */
	// 목록 개수 공통코드
	public static final PBoxList<PBox> rowSizeList = new PBoxList<PBox>();
	
	// 정렬  유형 공통코드
	public static final PBoxList<PBox> orderTypeList = new PBoxList<PBox>();	
	
	static {
		/* 공통 */
		rowSizeList.setJson("[" +
			"{\"KEY\":\"10\",\"VALUE\":\"10개\"}" +
			",{\"KEY\":\"50\",\"VALUE\":\"50개\"}" +
			",{\"KEY\":\"100\",\"VALUE\":\"100개\"}" +
			",{\"KEY\":\"500\",\"VALUE\":\"500개\"}" +
			",{\"KEY\":\"1000\",\"VALUE\":\"1000개\"}" +
		"]");
		
		orderTypeList.setJson("[" +
			"{\"KEY\":\"DESC\",\"VALUE\":\"내림차순\"}" +
			",{\"KEY\":\"ASC\",\"VALUE\":\"오름차순\"}" +
		"]");		
	
	}

}
