package com.boot.cleanhub.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.boot.cleanhub.common.dao.CommonDaoIF;
import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.common.dto.PLog;
import com.boot.cleanhub.common.dto.PSession;


/**
 * <pre>
 * 모든 Controller 클래스의 부모클래스로써 공통기능 및 멤버 변수를 선언함.
 * </pre>
 */
@Controller
public class SuperController {

	@Autowired
	protected HttpSession httpSession; // HttpSession 객체 선언
	
	@Autowired
	private HttpServletRequest request; // request 객체 선언
	
	@Autowired
	protected PLog pLog; // pLog 객체 선언

	@Autowired
	protected PSession pSession; // pSession 객체 선언
    
    @Autowired
	protected CommonDaoIF commonDao; // System CommonDao 객체 선언
	
	/**
	 * <pre>
	 * 초기화 파라미터를 인터셉터로 부터 전달받는 메소드
	 * </pre>
	 * 
	 * @return PBox 형태의 파라미터 객체
	 */
	@ModelAttribute("initBoxs")
	public PBox initBoxs() {
		return (PBox) request.getAttribute("pBox");
	}

}
