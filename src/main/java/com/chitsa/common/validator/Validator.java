package com.chitsa.common.validator;

/** Validates a request before create or update. */
public interface Validator<T> {

    void validate(T target);
}
