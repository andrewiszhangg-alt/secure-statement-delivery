package com.chitsa.common.exception;

import com.chitsa.common.util.ErrorConstant;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.security.util.SecurityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ProblemDetail handleInvalidParameter(InvalidParameterException ex) {
        log.warn("Invalid parameter: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Foreign keys are RESTRICT, so deleting a parent that still has children lands here. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Integrity violation: {}", ex.getMostSpecificCause().getMessage(), ex);
        return problem(HttpStatus.CONFLICT, PersistenceConstant.DATA_CONFLICT);
    }

    /** Reports a version mismatch as a conflict. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock: {}", ex.getMessage(), ex);
        return problem(HttpStatus.CONFLICT, PersistenceConstant.VERSION_CONFLICT);
    }

    /** The specific reason stays in the log. Clients always see the same sentence. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, SecurityConstant.ACCESS_DENIED);
    }

    @ExceptionHandler(ObjectStorageApiException.class)
    public ProblemDetail handleStorage(ObjectStorageApiException ex) {
        log.error("Storage error", ex);
        return problem(HttpStatus.BAD_GATEWAY, MediaResourceConstant.STORAGE_UNAVAILABLE);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail handleUnsupported(UnsupportedOperationException ex) {
        log.warn("Unsupported operation: {}", ex.getMessage());
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled API error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorConstant.UNEXPECTED);
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
