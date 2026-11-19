package com.chitsa.mediaresource.validator;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.mediaresource.config.MediaProperties;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.signature.MediaSignatureRules;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaResourceValidatorTest {

    private MediaResourceValidator validator;

    @BeforeEach
    void setUp() {
        var properties = new MediaProperties();
        properties.setAllowedContentTypes(List.of("application/pdf"));
        properties.setAllowedExtensions(List.of("pdf"));
        validator = new MediaResourceValidator(properties,
                new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()),
                new MediaSignatureRules());
    }

    @Test
    void acceptsAListedTypeAndExtension() {
        assertThatCode(() -> validator.validate(request("statement.pdf", "application/pdf", "%PDF-1.4")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsTheTypeAndExtensionInAnyCase() {
        assertThatCode(() -> validator.validate(request("notes.PDF", "APPLICATION/PDF", "%PDF-1.4")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBytesThatAreNotAPdf() {
        assertThatThrownBy(() -> validator.validate(request("statement.pdf", "application/pdf", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.SIGNATURE_MISMATCH);
    }

    @Test
    void rejectsANullRequest() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.REQUEST_NOT_NULL);
    }

    @Test
    void rejectsABlankContextAndKey() {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());

        assertThatThrownBy(() -> validator.validate(new MediaResourceRequestDto(" ", " ", file)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("context")
                .hasMessageContaining("objectKey");
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> validator.validate(request("statement.pdf", "application/pdf", "")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.FILE_EMPTY);
    }

    @Test
    void rejectsAContentTypeOutsideTheAllowList() {
        assertThatThrownBy(() -> validator.validate(request("statement.pdf", "text/html", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.CONTENT_TYPE_NOT_ALLOWED.formatted("text/html", List.of("application/pdf")));
    }

    @Test
    void rejectsAMissingContentType() {
        assertThatThrownBy(() -> validator.validate(request("statement.pdf", null, "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.CONTENT_TYPE_NOT_ALLOWED.formatted("null", List.of("application/pdf")));
    }

    @Test
    void rejectsAnExtensionOutsideTheAllowList() {
        assertThatThrownBy(() -> validator.validate(request("statement.exe", "application/pdf", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.EXTENSION_NOT_ALLOWED.formatted("statement.exe", List.of("pdf")));
    }

    @Test
    void rejectsAFileWithNoExtension() {
        assertThatThrownBy(() -> validator.validate(request("statement", "application/pdf", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.NO_EXTENSION);
    }

    @Test
    void rejectsATrailingDot() {
        assertThatThrownBy(() -> validator.validate(request("statement.", "application/pdf", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.NO_EXTENSION);
    }

    @Test
    void rejectsABlankFilename() {
        assertThatThrownBy(() -> validator.validate(request(" ", "application/pdf", "statement")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.FILENAME_BLANK);
    }

    private static MediaResourceRequestDto request(String filename, String contentType, String body) {
        var file = new MockMultipartFile("file", filename, contentType, body.getBytes());
        return new MediaResourceRequestDto("account-statements", "accounts/statement.pdf", file);
    }
}
