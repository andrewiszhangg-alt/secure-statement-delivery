package com.chitsa.accounttype.entity;

import com.chitsa.common.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A kind of account (Savings, Credit card, ...). The default rows are seeded by the V3 migration. */
@Entity
@Table(name = "account_types")
@Getter
@Setter
public class AccountType extends BaseJpaEntity {

    /** Stable identifier, e.g. CREDIT_CARD. */
    @Column(nullable = false, length = 30)
    private String code;

    /** Display name, e.g. Credit card. */
    @Column(nullable = false, length = 100)
    private String label;
}
