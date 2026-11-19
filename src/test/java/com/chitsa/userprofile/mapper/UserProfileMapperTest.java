package com.chitsa.userprofile.mapper;

import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileMapperTest {

    private final UserProfileMapper mapper = new UserProfileMapper();

    @Test
    void createCopiesEmailAndNames() {
        var idpUuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var dto = new UserProfileRequestDto(idpUuid, "  ada@example.com  ", " Ada ", " Lovelace ");

        var entity = mapper.convertDtoToEntity(dto);

        assertThat(entity.getIdpUuid()).isNull();
        assertThat(entity.getEmail()).isEqualTo("ada@example.com");
        assertThat(entity.getFirstName()).isEqualTo("Ada");
        assertThat(entity.getLastName()).isEqualTo("Lovelace");
    }

    @Test
    void createStoresBlankNamesAsNull() {
        var dto = new UserProfileRequestDto(UUID.randomUUID(), "ada@example.com", " ", null);

        var entity = mapper.convertDtoToEntity(dto);

        assertThat(entity.getFirstName()).isNull();
        assertThat(entity.getLastName()).isNull();
    }

    @Test
    void updateCopiesNamesOnly() {
        var idpUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        var entity = new UserProfile();
        entity.setIdpUuid(idpUuid);
        entity.setEmail("ada@example.com");
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");

        mapper.updateEntityFromDto(
                new UserProfileRequestDto(UUID.randomUUID(), "other@example.com", " Grace ", " Hopper "),
                entity);

        assertThat(entity.getIdpUuid()).isEqualTo(idpUuid);
        assertThat(entity.getEmail()).isEqualTo("ada@example.com");
        assertThat(entity.getFirstName()).isEqualTo("Grace");
        assertThat(entity.getLastName()).isEqualTo("Hopper");
    }

    @Test
    void responseCarriesTheStoredProfile() {
        var id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        var created = Instant.parse("2026-01-01T00:00:00Z");
        var updated = Instant.parse("2026-01-02T00:00:00Z");
        var entity = new UserProfile();
        entity.setId(id);
        entity.setEmail("ada@example.com");
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");
        entity.setCreateTime(created);
        entity.setUpdatedTime(updated);

        var response = mapper.convertEntityToResponseDto(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.createTime()).isEqualTo(created);
        assertThat(response.updatedTime()).isEqualTo(updated);
        assertThat(response.profileComplete()).isTrue();
    }
}
