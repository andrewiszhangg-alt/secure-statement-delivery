package com.chitsa.common.web;

import com.chitsa.common.util.ErrorConstant;
import com.chitsa.security.util.SecurityConstant;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/** Fallback for a missing page or an unhandled error. API calls receive a problem detail. */
@Slf4j
@Controller
@RequestMapping("/error")
public class FallbackErrorController implements ErrorController {

    @RequestMapping
    public Object error(HttpServletRequest request) {
        var code = statusCode(request);
        var path = path(request);
        logFailure(code, path, request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
        if (path.startsWith("/api/") || "/api".equals(path)) {
            return ResponseEntity.status(code)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(code), apiDetail(code)));
        }
        var model = new ModelAndView(ErrorConstant.ERROR_VIEW);
        model.setStatus(HttpStatusCode.valueOf(code));
        model.addObject(ErrorConstant.STATUS, code);
        model.addObject(ErrorConstant.TITLE, pageTitle(code));
        model.addObject(ErrorConstant.MESSAGE, pageDetail(code));
        return model;
    }

    private static int statusCode(HttpServletRequest request) {
        var value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Integer code && code >= 400 && code <= 599) {
            return code;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private static String path(HttpServletRequest request) {
        var uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (!(uri instanceof String value) || value.isBlank()) {
            return "";
        }
        var context = request.getContextPath() == null ? "" : request.getContextPath();
        return value.startsWith(context) ? value.substring(context.length()) : value;
    }

    private static void logFailure(int code, String path, Object exception) {
        if (exception instanceof Throwable thrown) {
            log.error("Request failed status={} path={}", code, path, thrown);
            return;
        }
        log.warn("Request failed status={} path={}", code, path);
    }

    private static String pageTitle(int code) {
        return switch (code) {
            case 404 -> ErrorConstant.PAGE_NOT_FOUND_TITLE;
            case 403 -> ErrorConstant.NOT_ALLOWED_TITLE;
            case 400, 405 -> ErrorConstant.REQUEST_FAILED_TITLE;
            default -> ErrorConstant.UNEXPECTED_TITLE;
        };
    }

    private static String pageDetail(int code) {
        return switch (code) {
            case 404 -> ErrorConstant.PAGE_NOT_FOUND;
            case 403 -> SecurityConstant.ACCESS_DENIED;
            case 400, 405 -> ErrorConstant.REQUEST_FAILED;
            default -> ErrorConstant.UNEXPECTED;
        };
    }

    private static String apiDetail(int code) {
        return switch (code) {
            case 404 -> ErrorConstant.RESOURCE_NOT_FOUND;
            case 403 -> SecurityConstant.ACCESS_DENIED;
            case 405 -> ErrorConstant.METHOD_NOT_ALLOWED;
            case 400 -> ErrorConstant.REQUEST_FAILED;
            default -> ErrorConstant.UNEXPECTED;
        };
    }
}
