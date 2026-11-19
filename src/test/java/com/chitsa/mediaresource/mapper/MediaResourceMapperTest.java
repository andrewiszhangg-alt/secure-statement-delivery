package com.chitsa.mediaresource.mapper;

import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.entity.MediaResource;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaResourceMapperTest {

    private final MediaResourceMapper mapper = new MediaResourceMapper();

    @Test
    void createCopiesTheFileDetails() {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());

        var entity = mapper.convertDtoToEntity(new MediaResourceRequestDto("account-statements", "accounts/statement.pdf", file));

        assertThat(entity.getContext()).isEqualTo("account-statements");
        assertThat(entity.getObjectKey()).isEqualTo("accounts/statement.pdf");
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getOriginalFilename()).isEqualTo("statement.pdf");
        assertThat(entity.getUrl()).isNull();
    }

    @Test
    void createLeavesFileDetailsEmptyWhenTheFileIsMissing() {
        var entity = mapper.convertDtoToEntity(new MediaResourceRequestDto("account-statements", "accounts/statement.pdf", null));

        assertThat(entity.getContentType()).isNull();
        assertThat(entity.getOriginalFilename()).isNull();
    }

    @Test
    void responseCarriesTheStoredUrl() {
        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var created = Instant.parse("2026-01-01T00:00:00Z");
        var entity = new MediaResource();
        entity.setId(id);
        entity.setContext("account-statements");
        entity.setObjectKey("accounts/statement.pdf");
        entity.setUrl("https://files.example/accounts/statement.pdf");
        entity.setContentType("application/pdf");
        entity.setOriginalFilename("statement.pdf");
        entity.setCreateTime(created);
        entity.setUpdatedTime(created);

        var response = mapper.convertEntityToResponseDto(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.url()).isEqualTo("https://files.example/accounts/statement.pdf");
        assertThat(response.objectKey()).isEqualTo("accounts/statement.pdf");
        assertThat(response.createTime()).isEqualTo(created);
    }

    @Test
    void updateIsRejected() {
        assertThatThrownBy(() -> mapper.updateEntityFromDto(null, new MediaResource()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(MediaResourceConstant.UPDATE_NOT_SUPPORTED);
    }
}
