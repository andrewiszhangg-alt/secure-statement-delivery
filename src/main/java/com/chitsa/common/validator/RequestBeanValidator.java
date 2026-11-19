package com.chitsa.common.validator;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.ValidationUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs the Bean Validation annotations on a request DTO and reports every failing field at once.
 * The Default group is always checked; pass {@link OnCreate} or {@link OnUpdate} for operation-specific rules.
 */
@Component
@RequiredArgsConstructor
public class RequestBeanValidator {

    private final Validator validator;

    /** Rejects a null request, then runs {@link #validate(Object, Class[])}. */
    public void requireValid(Object target, String nullMessage, Class<?>... groups) {
        ValidationUtil.requireNonNull(target, nullMessage);
        validate(target, groups);
    }

    public void validate(Object target, Class<?>... groups) {
        var checked = Stream.concat(Stream.of(Default.class), Arrays.stream(groups)).toArray(Class<?>[]::new);
        var violations = validator.validate(target, checked);
        if (!violations.isEmpty()) {
            throw new InvalidParameterException(violations.stream()
                    .sorted(Comparator.comparing((ConstraintViolation<Object> v) -> v.getPropertyPath().toString()))
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; ")));
        }
    }
}
