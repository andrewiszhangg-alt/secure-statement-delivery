package com.chitsa.accountstatement.controller;

import com.chitsa.account.controller.AccountWebController;
import com.chitsa.admin.AdminWebController;
import com.chitsa.common.exception.EntityNotFoundException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.common.util.ErrorConstant;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** HTML error page for the web controllers; REST controllers keep the JSON handler. */
@Slf4j
@ControllerAdvice(assignableTypes = {
        AccountStatementWebController.class, AccountWebController.class, AdminWebController.class
})
public class WebExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        log.warn("Web not found: {}", ex.getMessage());
        return errorPage(model, HttpStatus.NOT_FOUND, ErrorConstant.NOT_FOUND_TITLE, ex.getMessage());
    }

    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalid(InvalidParameterException ex, Model model) {
        log.warn("Web invalid request: {}", ex.getMessage());
        return errorPage(model, HttpStatus.BAD_REQUEST, ErrorConstant.REQUEST_FAILED_TITLE, ex.getMessage());
    }

    @ExceptionHandler(ObjectStorageApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleStorage(ObjectStorageApiException ex, Model model) {
        log.error("Web storage error", ex);
        return errorPage(model, HttpStatus.BAD_GATEWAY, ErrorConstant.UNEXPECTED_TITLE,
                MediaResourceConstant.STORAGE_UNAVAILABLE);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOptimisticLock(OptimisticLockingFailureException ex, Model model) {
        log.warn("Web optimistic lock: {}", ex.getMessage());
        return errorPage(model, HttpStatus.CONFLICT, ErrorConstant.REQUEST_FAILED_TITLE,
                PersistenceConstant.VERSION_CONFLICT);
    }

    private static String errorPage(Model model, HttpStatus status, String title, String message) {
        model.addAttribute(ErrorConstant.STATUS, status.value());
        model.addAttribute(ErrorConstant.TITLE, title);
        model.addAttribute(AccountStatementConstant.ERROR_MESSAGE, message);
        return AccountStatementConstant.ERROR_PAGE;
    }
}
