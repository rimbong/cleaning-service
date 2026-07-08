package com.boot.cleanhub.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <pre>
 *   GET /login 진입점 — 프론트 구성에 맞는 로그인 지점으로 안내한다.
 *
 *   [왜 필요한가]
 *   SPA 에서 로그인 화면은 서버 URL 이 아니라 프론트 라우트(예: /app 안의 Vue 라우터)다.
 *   그래서 사용자가 습관적으로 주소창에 /login 을 치면 갈 곳이 없다. 이 컨트롤러가
 *   /login 을 받아 구성에 맞는 위치로 리다이렉트한다.
 *     - SPA 구성 : auth.login-redirect=/app  → Vue 라우터가 로그인 화면을 그림
 *     - SSR 구성 : auth.login-redirect 를 실제 로그인 페이지 URL 로 지정
 *
 *   [부가효과] 우리가 GET /login 매핑을 점유하므로, 스프링 시큐리티가 자동생성하던
 *   기본 로그인 폼(DefaultLoginPageGeneratingFilter)이 노출되지 않는다.
 *
 *   ※ 이건 "사용자가 /login 을 직접 친 경우"만 담당한다. 보호 리소스 미인증 접근 시의
 *     자동 처리(/admin/** 리다이렉트 / 그 외 401 JSON)는 SessionSecurityConfig 가 별도로 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.06
 * @version 1.0
 */
@Controller
public class LoginEntryController {

    /** /login 리다이렉트 대상(구성값). 기본 SPA 루트(/), SSR 은 실제 로그인 페이지 URL 로 오버라이드 */
    @Value("${auth.login-redirect:/}")
    private String loginRedirect;

    @GetMapping("/login")
    public String loginEntry() {
        return "redirect:" + loginRedirect;
    }
}
