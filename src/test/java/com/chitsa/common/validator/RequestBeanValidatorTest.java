package com.chitsa.common.validator;

import com.chitsa.common.exception.InvalidParameterException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestBeanValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final RequestBeanValidator beanValidator = new RequestBeanValidator(validator);

    @Test
    void validate_acceptsACompleteCreate() {
        beanValidator.validate(new SampleRequest("savings", UUID.randomUUID(), null), OnCreate.class);
    }

    @Test
    void validate_checksTheDefaultGroupWhenNoOperationGroupIsPassed() {
        assertThatThrownBy(() -> beanValidator.validate(new SampleRequest("  ", null, null)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void validate_reportsCreateRulesWithTheDefaultGroup() {
        assertThatThrownBy(() -> beanValidator.validate(new SampleRequest("  ", null, null), OnCreate.class))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("id must not be null; name must not be blank");
    }

    @Test
    void validate_reportsUpdateRulesWithTheDefaultGroup() {
        assertThatThrownBy(() -> beanValidator.validate(new SampleRequest("", UUID.randomUUID(), "  "), OnUpdate.class))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("name must not be blank; note must not be blank");
    }

    private record SampleRequest(
            @NotBlank String name,
            @NotNull(groups = OnCreate.class) UUID id,
            @NotBlank(groups = OnUpdate.class) String note) {
    }
}
