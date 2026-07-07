package com.boot.cleanhub.error;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.boot.cleanhub.common.api.ApiResponse;

/**
 * <pre>
 *   40x/50x 서블릿 에러 처리기.
 *   - API 요청(/api/** 또는 Accept: application/json)  → 표준 ApiResponse(JSON)
 *   - 페이지 요청                                       → Thymeleaf HTML 에러 페이지
 *   덕분에 SPA 는 항상 JSON 에러를, 브라우저 직접 접근은 보기 좋은 에러 페이지를 받는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.05
 * @version 2.0
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        int status = resolveStatus(request);
        String message = defaultMessage(status);

        // [1] API 요청이면 JSON 으로 응답
        if (isApiRequest(request)) {
            String code = codeOf(status);
            return ResponseEntity.status(status).body(ApiResponse.error(code, message));
        }

        // [2] 페이지 요청이면 HTML 에러 페이지
        model.addAttribute("code", status);
        model.addAttribute("message", message);
        if (status == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        }
        return "error/500";
    }

    private int resolveStatus(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            try {
                return Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String defaultMessage(int status) {
        if (status == HttpStatus.NOT_FOUND.value()) {
            return "요청하신 리소스를 찾을 수 없습니다.";
        }
        if (status == HttpStatus.FORBIDDEN.value()) {
            return "접근 권한이 없습니다.";
        }
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return "인증이 필요합니다.";
        }
        return "서버에 오류가 발생했습니다.";
    }

    private String codeOf(int status) {
        switch (status) {
            case 404: return "NOT_FOUND";
            case 403: return "FORBIDDEN";
            case 401: return "UNAUTHORIZED";
            case 400: return "BAD_REQUEST";
            default:  return "INTERNAL_ERROR";
        }
    }

    /** /api/ 경로이거나 Accept 헤더가 JSON 이면 API 요청으로 본다. */
    private boolean isApiRequest(HttpServletRequest request) {
        Object originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = originalUri != null ? originalUri.toString() : request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
