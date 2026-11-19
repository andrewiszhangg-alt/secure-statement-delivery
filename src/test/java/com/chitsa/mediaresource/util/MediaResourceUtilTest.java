package com.chitsa.mediaresource.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class MediaResourceUtilTest {

    @Test
    void toCreateRequestKeepsTheContextKeyAndFile() {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());

        var request = MediaResourceUtil.toCreateRequest("account-statements", "accounts/statement.pdf", file);

        assertThat(request.context()).isEqualTo("account-statements");
        assertThat(request.objectKey()).isEqualTo("accounts/statement.pdf");
        assertThat(request.multipartFile()).isSameAs(file);
    }
}
