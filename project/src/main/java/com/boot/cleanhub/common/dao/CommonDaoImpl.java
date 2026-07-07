package com.boot.cleanhub.common.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.common.dto.PBoxList;

/**
 * <pre>
 * 	CRUD Dao 정의, 레거시 방식 사용
 * </pre>
 */

@Repository
public class CommonDaoImpl extends SuperDao implements CommonDaoIF {

	/**
	 * <pre>
	 * List 형태의 데이터 조회 구현 메소드
	 * </pre>
	 * 
	 * @param key
	 *            mybatis의 xml 문서 id값
	 * @param pBox
	 *            SQL 파라미터
	 * @return
	 */
	public PBoxList<PBox> selectList(String key, PBox pBox) {

		List<Object> list = sqlSessionTemplate.selectList(key, pBox);
		return new PBoxList<PBox>(list);
	}

	/**
	 * <pre>
	 * 데이터 등록 구현메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public int insert(String key, PBox pBox) {
		return sqlSessionTemplate.insert(key, pBox);
	}

	/**
	 * <pre>
	 * 데이터 단건 조회 구현메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public PBox select(String key, PBox pBox) {
		return sqlSessionTemplate.selectOne(key, pBox);
	}

	/**
	 * <pre>
	 * 데이터 수정 구현메소드
	 * </pre>
	 * 
	 * @param key
	 * 			  mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public int update(String key, PBox pBox) {
		return sqlSessionTemplate.update(key, pBox);
	}

	/**
	 * <pre>
	 * 데이터 삭제 구현메소드
	 * </pre>
	 * 
	 * @param key
	 *            mabatis 의 xml id값
	 * @param pBox
	 *            SQL파라미터
	 * @return
	 */
	public int delete(String key, PBox pBox) {
		return sqlSessionTemplate.delete(key, pBox);
	}

}
