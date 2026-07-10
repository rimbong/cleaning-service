package com.boot.cleanhub.config;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.filter.RefreshCookieAutoLoginFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 *   [세션 기반 보안 체인] — @Order(2)
 *
 *   ■ 이중 체인의 큰 그림 (요청은 URL에 따라 "둘 중 하나"의 체인만 탄다. 혼용 아님)
 *     요청 도착
 *       ├─ /api/**  → JwtApiSecurityConfig(@Order(1))  : JWT·무상태(STATELESS) — "API 존"
 *       └─ 그 외    → 이 클래스(@Order(2))              : 세션·폼 로그인       — "웹(세션) 존"
 *     숫자가 작은 체인부터 "내 담당 URL인가?"를 검사하고, 담당이면 그 체인만 적용된다.
 *     이 클래스는 requestMatchers 를 안 걸었으므로 "JWT 체인이 안 가져간 나머지 전부"를 담당.
 *
 *   ■ 이 체인이 제공하는 인증 흐름 (정식 auth 모듈 — com.boot.cleanhub.auth)
 *     1) POST /auth/login  (form-encoded)   → 세션 로그인(JSESSIONID 쿠키)
 *     2) POST /auth/api/token (세션 필요)    → API 존용 JWT 발급
 *     3) 이후 /api/** 는 Bearer 토큰으로 호출     → JwtApiSecurityConfig 참고
 *
 *   ■ 로그인 처리는 코드가 아니라 "필터"가 한다
 *     POST /auth/login 을 처리하는 컨트롤러는 없다.
 *     formLogin 설정만 하면 스프링 시큐리티의 UsernamePasswordAuthenticationFilter 가
 *     그 URL 을 가로채서: 파라미터 추출 → AuthenticationManager 에 인증 위임
 *     → 성공 시 세션에 인증 저장 + successHandler 호출.
 *
 *   ■ 인증 3형제의 역할 분담 (아래 빈 정의 참고)
 *     AuthenticationManager  : 인증 "총괄 창구". 인증 요청을 받아 Provider 들에게 위임.
 *     AuthenticationProvider : 인증 "실무자". 실제 검증 한 가지 방식을 담당.
 *                              (여기선 DaoAuthenticationProvider = DB 사용자 + 비밀번호 대조)
 *     UserDetailsService     : "사용자 조회원". username 으로 DB에서 사용자를 찾아온다.
 *     흐름: Filter → Manager → Provider → (UserDetailsService 로 조회 + PasswordEncoder 로 대조)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.1 (2026.07.06 — auth 정식 모듈 승격·URL 정식화(/auth/**). 2.0: auth 도입·구 데모 삭제·설명 주석)
 */
@Configuration
@EnableWebSecurity
@Order(2)
public class SessionSecurityConfig {

    /** JSON 응답(ApiResponse) 직렬화용 */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 렌더모드 — spa(현재) / ssr. application.yml 의 app.mode.
     * 현재 URL 규칙·예외처리(미인증 401 JSON 등)는 이미 spa 동작이라 그대로 둔다.
     * ssr 도입 시 이 값으로 분기(미인증 → 로그인 페이지 리다이렉트 등)를 추가한다.
     */
    @Value("${app.mode:spa}")
    private String appMode;

    /** 세션 로그인용 사용자 조회 서비스(auth 모듈의 auth_user 테이블) */
    @Autowired
    @Qualifier("authUserDetailsService")
    private UserDetailsService userDetailsService;

    /** refresh 쿠키로 페이지 요청 시 세션을 재생성하는 자동로그인 필터(로그인 유지) */
    @Autowired
    private RefreshCookieAutoLoginFilter refreshCookieAutoLoginFilter;

    @Bean
    public SecurityFilterChain sessionFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 허용 — 구체 정책(허용 오리진 등)은 CorsConfig 의 CorsConfigurationSource 빈이 정의
            .cors(Customizer.withDefaults())

            // CSRF 비활성 — 데모 편의. (실서비스에서 "세션 쿠키" 인증을 쓴다면 CSRF 보호를 켜고
            //  프론트에서 토큰을 같이 보내는 구성을 권장. JWT 헤더 인증은 CSRF 영향 없음)
            .csrf(csrf -> csrf.disable())

            // H2 콘솔이 <frame> 을 쓰므로 frame 차단 해제(개발용)
            .headers(headers -> headers.frameOptions().disable())

            // 자동로그인 필터 — 인가 검사 전에 refresh 쿠키로 세션을 재생성(서버·브라우저 재시작 후
            // 새로고침해도 관리자 페이지 유지). 로그인 폼 필터보다 앞에 둔다.
            .addFilterBefore(refreshCookieAutoLoginFilter, UsernamePasswordAuthenticationFilter.class)

            // 이 체인이 사용할 인증 총괄 창구/실무자 지정(아래 빈 정의 참고)
            .authenticationManager(sessionAuthenticationManager())
            .authenticationProvider(sessionAuthenticationProvider())

            // ── URL 별 접근 규칙 ──────────────────────────────────────────────
            // "어떤 URL 은 로그인해야 접근 가능한가"를 선언하는 곳.
            // 위에서부터 순서대로 매칭되므로 구체적인 규칙을 먼저 쓴다.
            .authorizeHttpRequests(authz -> authz
                // JWT 발급 등 "로그인한 사용자 전용" API 구역
                .antMatchers("/auth/api/**").authenticated()
                // 관리자 전용 "페이지" — ROLE_ADMIN 필요(인가 근거 = 세션에 저장된 권한).
                //  /admin(정확히) 만 보호. /admin/login·/admin/denied 는 공개(아래 anyRequest permitAll).
                //  미로그인 → 관리자 로그인 리다이렉트 / 권한 부족 → 403 페이지(아래 예외 처리)
                //  ※ 자동로그인(POST /api/auth/refresh)이 세션을 함께 재생성하므로, 재시작 후 새로고침도 통과.
                .antMatchers("/admin").hasRole("ADMIN")
                // 나머지 전부 허용(테스트 페이지들)
                .anyRequest().permitAll())

            // ── 폼 로그인 ────────────────────────────────────────────────────
            // loginProcessingUrl: 이 URL 로 POST(form-encoded: username, password)가 오면
            //   UsernamePasswordAuthenticationFilter 가 가로채 인증을 수행한다(컨트롤러 불필요).
            // successHandler/failureHandler: 기본값은 "페이지 리다이렉트"라 SPA 에 부적합
            //   → JSON(ApiResponse)으로 응답하도록 교체.
            //
            // ※ loginPage() 를 지정하지 않은 이유: 로그인 "화면"은 서버(Thymeleaf)가 아니라
            //   클라이언트(허브 테스트 페이지의 폼 / Vue SPA)가 담당하기 때문. 서버는 처리 창구만 제공.
            //   · loginPage("/경로")        = 커스텀 로그인 화면(GET) 지정 + 미인증 접근 시 그리로 리다이렉트
            //   · loginProcessingUrl("/경로") = 아이디/비밀번호 "처리"(POST) — 화면과 별개
            .formLogin(form -> form
                // loginPage("/login"): 두 가지 효과 —
                //  (1) 스프링 기본 자동생성 로그인 폼(DefaultLoginPageGeneratingFilter)을 끈다.
                //  (2) GET /login 을 우리 LoginEntryController 로 흘려보낸다(구성에 맞게 리다이렉트).
                //  ※ loginPage 는 "세션 존 전역 미인증 진입점"도 /login 으로 바꾸지만,
                //    아래 exceptionHandling 의 "경로별" 진입점이 이를 덮으므로 전역 누수는 없다
                //    (/admin/** → 관리자 로그인, 그 외 → 401 JSON).
                .loginPage("/login")
                // 로그인 "처리" URL(공용) — SPA/스모크/관리자 로그인 화면 모두 여기로 POST.
                .loginProcessingUrl("/auth/login")
                .successHandler((request, response, authentication) ->
                    writeJson(response, HttpServletResponse.SC_OK,
                        ApiResponse.ok(Collections.singletonMap("username", authentication.getName()), "로그인 성공")))
                .failureHandler((request, response, exception) ->
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        ApiResponse.error("LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.")))
                .permitAll())

            // ── 로그아웃 ─────────────────────────────────────────────────────
            // POST /auth/logout → 세션 무효화 + JSESSIONID 쿠키 삭제 후 JSON 응답.
            // (JWT refresh 토큰 폐기는 별도로 POST /api/auth/logout 호출 — JWT 존 담당)
            // ※ 암묵 동작: CSRF 를 비활성화하면 스프링 시큐리티가 로그아웃을 POST 로
            //   제한하지 않아 GET /auth/logout 으로도 동작한다(데모에선 무방,
            //   CSRF 를 켜면 자동으로 POST 전용이 됨).
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                    writeJson(response, HttpServletResponse.SC_OK,
                        ApiResponse.ok(null, "로그아웃 되었습니다.")))
                .permitAll())

            // ── 미인증 접근 처리 ─────────────────────────────────────────────
            // 기본 동작은 로그인 "페이지로 리다이렉트"인데, API(/auth/api/**)에는
            // 리다이렉트 대신 401 JSON 이 맞으므로 해당 경로만 EntryPoint 를 교체.
            // ── 예외 처리: 관심사를 "경로별"로 분기(전역 loginPage 대신) ──────────
            //  [미인증(401 계열)]
            //   · /admin/**       → 관리자 로그인 화면으로 리다이렉트(브라우저 페이지 흐름)
            //   · 그 외(일반·SPA·API) → 401 JSON (SPA 가 자체 로그인 화면을 띄우는 관례)
            //  ※ 매핑은 삽입 순서대로 첫 매치 적용 → /admin/** 을 먼저, AnyRequest 를 catch-all 로.
            //  [인가 실패(403)]
            //   · /admin/**       → 403 거부 "페이지" forward
            //   · 그 외           → 403 JSON
            .exceptionHandling(except -> except
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/admin/login"),
                    new AntPathRequestMatcher("/admin/**"))
                .defaultAuthenticationEntryPointFor(
                    (request, response, authException) ->
                        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                            ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다.")),
                    AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler((request, response, ex) -> {
                    if (request.getRequestURI().startsWith("/admin")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        request.getRequestDispatcher("/admin/denied").forward(request, response);
                    } else {
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                            ApiResponse.error("FORBIDDEN", "이 리소스에 접근할 권한이 없습니다."));
                    }
                }));

        return http.build();
    }

    /**
     * [인증 실무자] DaoAuthenticationProvider
     * "DB에서 사용자를 찾아(UserDetailsService) 비밀번호를 대조(PasswordEncoder)"하는
     * 가장 표준적인 인증 방식. 아이디/비밀번호 로그인은 대부분 이걸로 처리된다.
     */
    @Bean
    @Primary
    public AuthenticationProvider sessionAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // 사용자 조회원 연결
        authProvider.setPasswordEncoder(passwordEncoder());     // 비밀번호 대조 방식 연결
        return authProvider;
    }

    /**
     * [인증 총괄 창구] AuthenticationManager
     * 필터가 만든 "인증 요청(아이디/비밀번호 묶음)"을 받아 등록된 Provider 들에게
     * 차례로 물어보고, 처리 가능한 Provider 가 검증한 결과를 돌려준다.
     * (여기선 Provider 가 하나뿐이라 사실상 위 Provider 를 감싼 껍데기)
     */
    @Bean
    @Primary
    public AuthenticationManager sessionAuthenticationManager() throws Exception {
        return new ProviderManager(sessionAuthenticationProvider());
    }

    /**
     * [비밀번호 대조 방식] BCrypt
     * 저장: encode("1234") → "$2a$10$..." 해시(매번 다른 salt 포함).
     * 검증: matches(입력값, 저장해시) — 해시를 되돌리는 게 아니라 같은 방식으로 다시 계산해 비교.
     * ⚠️ DB에 평문을 넣으면 matches 가 무조건 false → 로그인 불가(AuthUserSeeder 가 해시로 시딩).
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** ApiResponse 를 JSON 으로 직접 써 주는 공용 헬퍼(핸들러들이 사용) */
    private void writeJson(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
