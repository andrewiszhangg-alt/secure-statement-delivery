package com.chitsa.accountstatement.entity;

import com.chitsa.common.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "account_statements")
@Getter
@Setter
public class AccountStatement extends BaseJpaEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "media_resource_id", nullable = false)
    private UUID mediaResourceId;
}
