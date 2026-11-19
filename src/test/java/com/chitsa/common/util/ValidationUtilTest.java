package com.chitsa.common.util;

import com.chitsa.common.exception.InvalidParameterException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilTest {

    @Test
    void require_acceptsATrueCondition() {
        ValidationUtil.require(true, "name is required");
    }

    @Test
    void require_rejectsAFalseCondition() {
        assertThatThrownBy(() -> ValidationUtil.require(false, "name is required"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("name is required");
    }

    @Test
    void fail_alwaysRejects() {
        assertThatThrownBy(() -> ValidationUtil.fail("Complete your profile"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("Complete your profile");
    }

    @Test
    void isBlank_acceptsText() {
        assertThat(ValidationUtil.isBlank("savings")).isFalse();
    }

    @Test
    void isBlank_rejectsNullEmptyAndWhitespace() {
        assertThat(ValidationUtil.isBlank(null)).isTrue();
        assertThat(ValidationUtil.isBlank("")).isTrue();
        assertThat(ValidationUtil.isBlank("  ")).isTrue();
    }

    @Test
    void hasNull_acceptsPresentValues() {
        assertThat(ValidationUtil.hasNull()).isFalse();
        assertThat(ValidationUtil.hasNull("savings", "ada@bank.test")).isFalse();
    }

    @Test
    void hasNull_rejectsAMissingValue() {
        assertThat(ValidationUtil.hasNull((Object[]) null)).isTrue();
        assertThat(ValidationUtil.hasNull("savings", null)).isTrue();
    }

    @Test
    void requireNotBlank_rejectsBlankText() {
        assertThatThrownBy(() -> ValidationUtil.requireNotBlank("  ", "name is required"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("name is required");
    }

    @Test
    void requireNotBlank_acceptsText() {
        ValidationUtil.requireNotBlank("savings", "name is required");
    }

    @Test
    void requireNonNull_rejectsNull() {
        assertThatThrownBy(() -> ValidationUtil.requireNonNull(null, "request must not be null"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("request must not be null");
    }

    @Test
    void requireNonNull_acceptsAValue() {
        ValidationUtil.requireNonNull("savings", "request must not be null");
    }

    @Test
    void requireUuid_parsesAUuid() {
        var id = UUID.randomUUID();

        assertThat(ValidationUtil.requireUuid("  " + id + "  ", "account type is invalid")).isEqualTo(id);
    }

    @Test
    void requireUuid_rejectsBlankAndNonUuidText() {
        assertThatThrownBy(() -> ValidationUtil.requireUuid("  ", "account type is invalid"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("account type is invalid");
        assertThatThrownBy(() -> ValidationUtil.requireUuid("not-a-uuid", "account type is invalid"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("account type is invalid");
    }
}
