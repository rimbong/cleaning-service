package com.boot.cleanhub.config;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver; // Added SessionLocaleResolver import
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.boot.cleanhub.interceptor.EventCheckInterceptor;
/**
 * <pre>
 *   WebConfig
 *  application.yml에 작성할 수 없는 상세한 세팅은 여기서 한다. 
 * </pre>
 * @author In-seong Hwang
 * @since 2025.08.06
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private EventCheckInterceptor eventCheckInterceptor;

    /**
     * <pre>
     *   정적 리소스 핸들러 — SPA(정적 프론트)를 루트(/)로 서빙한다(대민 /, 관리자 /admin).
     *   - 요청 경로에 해당하는 정적 파일이 있으면 그 파일 서빙(js/css/img/favicon 등)
     *   - 없으면 진입점 index.html 로 폴백 → 브라우저 딥링크 새로고침(/admin/clients 등) 대응
     *   - 단, 백엔드(API/시스템) 경로는 폴백에서 제외해 정상 404 가 나게 한다
     *     (예: 없는 /api/xxx 가 SPA HTML 로 200 되면 안 되므로).
     *   ※ 리소스 핸들러는 컨트롤러(@RequestMapping)보다 우선순위가 낮아, 존재하는 /api·/auth 등은
     *     컨트롤러가 먼저 처리한다. 여기 폴백에는 "컨트롤러가 안 가져간 미매칭 요청"만 도달한다.
     * </pre>
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        // 백엔드 경로는 SPA 폴백 대상이 아님 → null 반환(정적 파일 없으면 그대로 404)
                        if (isBackendPath(resourcePath)) {
                            return null;
                        }
                        Resource requested = location.createRelative(resourcePath);
                        // 실제 정적 "파일"이 있으면 그걸 서빙. 빈/디렉터리 경로는 폴백 대상.
                        boolean isFileRequest = !resourcePath.isEmpty() && !resourcePath.endsWith("/");
                        if (isFileRequest && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // 매칭되는 정적 파일이 없으면 SPA 진입점으로 폴백(딥링크·루트 진입 대응)
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    /**
     * SPA 폴백에서 제외할 백엔드(API/시스템) 경로 판별 — 여기 해당하면 index.html 폴백 대신 정상 404.
     * (경로는 리소스 핸들러가 주는 상대경로라 앞 슬래시가 없다)
     */
    private static boolean isBackendPath(String path) {
        return path.startsWith("api/")
                || path.startsWith("actuator/") || path.equals("actuator")
                || path.startsWith("ws-chat")
                || path.equals("error");
    }
    
    /**
     * 다국어 지원을 위한 LocaleResolver를 Bean으로 등록합니다.
     * @return LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        // 세션에 지정된 로케일(?lang= 로 전환)이 우선. 세션 값이 없을 때의 기본 로케일을
        // determineDefaultLocale 오버라이드로 Accept-Language 헤더에서 가져온다(없으면 한국어).
        // → 웹(?lang=)과 API(Accept-Language)를 모두 지원.
        return new SessionLocaleResolver() {
            @Override
            @NonNull
            protected Locale determineDefaultLocale(@NonNull HttpServletRequest request) {
                Locale requestLocale = request.getLocale();
                return requestLocale != null ? requestLocale : Locale.KOREA;
            }
        };
    }
    
    /**
     * 언어 변경을 위한 인터셉터를 Bean으로 등록합니다.
     * lang 파라미터를 받아서 언어를 변경합니다.
     * @return LocaleChangeInterceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }
    
    /**
     * 인터셉터 등록.
     *  - eventCheckInterceptor 는 요청정보를 pBox 로 모아 컨트롤러에 넘기는 전처리기.
     *    SPA 정적 자산(assets·favicon)·error 요청엔 불필요하므로 제외한다.
     *    (SPA HTML 진입 자체는 index.html 폴백이라 자산 제외만으로 대부분의 정적 요청이 빠진다)
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(eventCheckInterceptor)
                .addPathPatterns("/**") // 모든 경로에 대해 인터셉터 적용
                .excludePathPatterns("/assets/**", "/favicon.svg", "/error");  // 정적 자산·error 는 제외
    }
    
}
