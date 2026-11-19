package com.chitsa.userprofile.entity;

import com.chitsa.common.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfile extends BaseJpaEntity {

    /** Global identifier from the identity provider (Cognito sub). */
    @Column(name = "idp_uuid", nullable = false, updatable = false)
    private UUID idpUuid;

    @Column(nullable = false)
    private String email;

    private String firstName;

    private String lastName;

    /** Profile is complete when name and email are present. */
    public boolean isProfileComplete() {
        return firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank()
                && email != null && !email.isBlank();
    }
}
