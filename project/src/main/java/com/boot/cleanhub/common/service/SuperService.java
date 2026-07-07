package com.boot.cleanhub.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.boot.cleanhub.common.dao.CommonDaoIF;
import com.boot.cleanhub.common.dto.PLog;
import com.boot.cleanhub.common.dto.PSession;

/**
 * <pre>
 * 	모든 Service 클래스의 부모클래스로써 공통기능 및 멤버 변수를 선언함.
 * </pre>
 */
public class SuperService {

	@Autowired
	protected PLog pLog; // logging 객체 선언
	
	@Autowired
	protected PSession pSession; // session 객체 선언
	
	@Autowired
	protected CommonDaoIF commonDao; // System CommonDao 객체 선언	

	/**
	 * <pre>
	 * Transaction 강제 Rollback
	 * </pre>
	 * 
	 * @param
	 * @return
	 */
	protected void rollBack() {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
	}

}
