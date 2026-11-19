package com.chitsa.account.entity;

import com.chitsa.common.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account extends BaseJpaEntity {

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "account_type_id", nullable = false)
    private UUID accountTypeId;
}
