package com.boot.cleanhub.common.dao;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.common.dto.PBoxList;

/**
 * <pre>
 * 	CRUD 인터페이스 정의
 * </pre>
 */
public interface CommonDaoIF {

	/**
	 * <pre>
	 * List 형태의 데이터 조회 추상 메소드
	 * </pre>
	 * 
	 * @param key
	 *            mybatis의 xml 문서 id값
	 * @param pBox
	 *            SQL 파라미터
	 * @return
	 */
	public abstract PBoxList<PBox> selectList(String key, PBox pBox);

	/**
	 * <pre>
	 * 데이터 등록 추상메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public abstract int insert(String key, PBox pBox);

	/**
	 * <pre>
	 * 데이터 단건 조회 추상메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public abstract PBox select(String key, PBox pBox);

	/**
	 * <pre>
	 * 데이터 수정 추상메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public abstract int update(String key, PBox pBox);

	/**
	 * <pre>
	 * 데이터 삭제 추상메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public abstract int delete(String key, PBox pBox);

}