package com.chitsa.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createTime;

    @Column(nullable = false)
    private Instant updatedTime;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createTime = now;
        updatedTime = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedTime = Instant.now();
    }
}
