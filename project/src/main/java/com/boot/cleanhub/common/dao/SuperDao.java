package com.boot.cleanhub.common.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <pre>
 * 	CommonDaoImpl 구현체
 * </pre>
 */

@Repository
public class SuperDao {

	@Autowired
	protected SqlSession sqlSessionTemplate;	
	
}
