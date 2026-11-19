package com.chitsa.common.exception;

import com.chitsa.common.util.ErrorConstant;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returnsTheDomainMessage() {
        var id = UUID.randomUUID();

        var problem = handler.handleNotFound(new EntityNotFoundException("Account not found: " + id));

        assertProblem(problem, HttpStatus.NOT_FOUND, "Account not found: " + id);
    }

    @Test
    void handleInvalidParameter_returnsTheFieldMessage() {
        var problem = handler.handleInvalidParameter(new InvalidParameterException("name must not be blank"));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "name must not be blank");
    }

    @Test
    void handleDuplicate_returnsTheDomainMessage() {
        var problem = handler.handleDuplicate(new DuplicateResourceException("Profile already exists"));

        assertProblem(problem, HttpStatus.CONFLICT, "Profile already exists");
    }

    @Test
    void handleIntegrityViolation_hidesTheDatabaseCause() {
        var problem = handler.handleIntegrityViolation(
                new DataIntegrityViolationException("insert", new IllegalStateException("duplicate key idp_uuid")));

        assertProblem(problem, HttpStatus.CONFLICT, PersistenceConstant.DATA_CONFLICT);
        assertThat(problem.getDetail()).doesNotContain("idp_uuid");
    }

    @Test
    void handleOptimisticLock_hidesTheRowIdentifier() {
        var problem = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("UserProfile", UUID.randomUUID()));

        assertProblem(problem, HttpStatus.CONFLICT, PersistenceConstant.VERSION_CONFLICT);
        assertThat(problem.getDetail()).doesNotContain("UserProfile");
    }

    @Test
    void handleUnexpected_hidesTheCause() {
        var problem = handler.handleUnexpected(new IllegalStateException("secret table"));

        assertProblem(problem, HttpStatus.INTERNAL_SERVER_ERROR, ErrorConstant.UNEXPECTED);
        assertThat(problem.getDetail()).doesNotContain("secret");
    }

    @Test
    void handleAccessDenied_hidesTheSpecificReason() {
        var problem = handler.handleAccessDenied(new AccessDeniedException("Authenticated principal has no claims"));

        assertProblem(problem, HttpStatus.FORBIDDEN, SecurityConstant.ACCESS_DENIED);
        assertThat(problem.getDetail()).doesNotContain("claims");
    }

    @Test
    void handleStorage_hidesTheBucketAndObjectKey() {
        var problem = handler.handleStorage(
                new ObjectStorageApiException("S3 upload failed for objectKey=secret bucket=private"));

        assertProblem(problem, HttpStatus.BAD_GATEWAY, MediaResourceConstant.STORAGE_UNAVAILABLE);
        assertThat(problem.getDetail()).doesNotContain("secret", "private");
    }

    @Test
    void handleUnsupported_returnsTheOperationMessage() {
        var problem = handler.handleUnsupported(
                new UnsupportedOperationException(MediaResourceConstant.UPDATE_NOT_SUPPORTED));

        assertProblem(problem, HttpStatus.METHOD_NOT_ALLOWED, MediaResourceConstant.UPDATE_NOT_SUPPORTED);
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
