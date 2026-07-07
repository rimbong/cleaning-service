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
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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
     * 정적 리소스 핸들러 설정
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // SPA(정적 프론트) 통합 경로: /app/**  (React/Vue 종류 무관)
        //  - 요청 경로에 해당하는 정적 파일이 있으면 그 파일 서빙(js/css/img 등)
        //  - 없으면 진입점 index.html 로 폴백 → 브라우저 딥링크 새로고침(/app/crud/list 등) 대응
        //  - 빌드 산출물은 classpath:/static/app/ 에 배치(프론트 base 도 '/app/' 로 맞춘다)
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/")
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        // 실제 정적 "파일"이 있으면 그걸 서빙(js/css/img 등).
                        //  빈 경로("/app/")나 디렉터리 요청("/app/foo/")은 파일이 아니므로 폴백 대상.
                        boolean isFileRequest = !resourcePath.isEmpty() && !resourcePath.endsWith("/");
                        if (isFileRequest && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // 매칭되는 정적 파일이 없으면 SPA 진입점으로 폴백(딥링크·루트 진입 대응)
                        return new ClassPathResource("/static/app/index.html");
                    }
                });
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
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
     * SPA 루트 진입점(/app, /app/)을 index.html 로 포워딩.
     *  - 리소스 핸들러(/app/**)는 하위경로가 "비어 있는"(/app/) 요청을 리졸버 호출 전에 404 처리하므로,
     *    루트만 여기서 forward 로 보정한다(내부 포워드라 리다이렉트 루프 위험 없음).
     *  - 딥링크(/app/admin/... 등)는 리소스 핸들러가 index.html 로 폴백하므로 여기서 다루지 않는다.
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addViewController("/app").setViewName("forward:/app/index.html");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }

    /**
     * 인터셉터 등록
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(eventCheckInterceptor)
                .addPathPatterns("/**") // 모든 경로에 대해 인터셉터 적용
                .excludePathPatterns("/static/**", "/error", "/app/**");  // static 리소스, SPA, error 페이지는 제외
    }
    
}
