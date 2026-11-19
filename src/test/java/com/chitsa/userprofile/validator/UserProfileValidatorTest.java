package com.chitsa.userprofile.validator;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.util.UserProfileConstant;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileValidatorTest {

    private final UserProfileValidator validator = new UserProfileValidator(
            new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void createRequiresTheIdentityAndEmail() {
        assertThatThrownBy(() -> validator.validateForCreate(new UserProfileRequestDto(null, " ", null, null)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("idpUuid")
                .hasMessageContaining("email");
    }

    @Test
    void updateRequiresBothNames() {
        assertThatThrownBy(() -> validator.validate(new UserProfileRequestDto(null, null, " ", "Hopper")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void rejectsANullRequest() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(UserProfileConstant.REQUEST_NOT_NULL);
    }
}
