package com.chitsa.common.web;

import com.chitsa.common.util.ErrorConstant;
import com.chitsa.security.util.SecurityConstant;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackErrorControllerTest {

    private final FallbackErrorController controller = new FallbackErrorController();

    @Test
    void missingPageShowsTheNotFoundCard() {
        var request = request(404, "/no-such-page");

        var model = (ModelAndView) controller.error(request);

        assertThat(model.getViewName()).isEqualTo(ErrorConstant.ERROR_VIEW);
        assertThat(model.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(model.getModel()).containsEntry(ErrorConstant.TITLE, ErrorConstant.PAGE_NOT_FOUND_TITLE);
        assertThat(model.getModel()).containsEntry(ErrorConstant.MESSAGE, ErrorConstant.PAGE_NOT_FOUND);
        assertThat(model.getModel().get(ErrorConstant.MESSAGE).toString()).doesNotContain("/no-such-page");
    }

    @Test
    void unknownFailureHidesTheCause() {
        var request = request(500, "/accounts");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret table"));

        var model = (ModelAndView) controller.error(request);

        assertThat(model.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(model.getModel()).containsEntry(ErrorConstant.TITLE, ErrorConstant.UNEXPECTED_TITLE);
        assertThat(model.getModel()).containsEntry(ErrorConstant.MESSAGE, ErrorConstant.UNEXPECTED);
        assertThat(model.getModel().get(ErrorConstant.MESSAGE).toString()).doesNotContain("secret");
    }

    @Test
    void missingStatusBecomesAnUnexpectedError() {
        var request = new MockHttpServletRequest();

        var model = (ModelAndView) controller.error(request);

        assertThat(model.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(model.getModel()).containsEntry(ErrorConstant.MESSAGE, ErrorConstant.UNEXPECTED);
    }

    @Test
    void forbiddenPageUsesTheAccessSentence() {
        var model = (ModelAndView) controller.error(request(403, "/accounts/private"));

        assertThat(model.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(model.getModel()).containsEntry(ErrorConstant.TITLE, ErrorConstant.NOT_ALLOWED_TITLE);
        assertThat(model.getModel()).containsEntry(ErrorConstant.MESSAGE, SecurityConstant.ACCESS_DENIED);
    }

    @Test
    void missingApiResourceReturnsAProblemDetail() {
        var response = (ResponseEntity<?>) controller.error(request(404, "/api/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var problem = (ProblemDetail) response.getBody();
        assertThat(problem.getDetail()).isEqualTo(ErrorConstant.RESOURCE_NOT_FOUND);
        assertThat(problem.getDetail()).doesNotContain("/api/missing");
    }

    @Test
    void apiFailureHidesTheCause() {
        var request = request(500, "/api/accounts");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret table"));

        var response = (ResponseEntity<?>) controller.error(request);

        var problem = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(problem.getDetail()).isEqualTo(ErrorConstant.UNEXPECTED);
        assertThat(problem.getDetail()).doesNotContain("secret");
    }

    private static MockHttpServletRequest request(int status, String path) {
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        return request;
    }
}
