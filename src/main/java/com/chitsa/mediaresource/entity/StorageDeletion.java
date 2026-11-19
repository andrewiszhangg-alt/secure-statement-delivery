package com.chitsa.mediaresource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** An S3 object that still needs to be deleted. The row is the retry record, not a cache. */
@Entity
@Table(name = "storage_deletions")
@Getter
@Setter
public class StorageDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String context;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "create_time", nullable = false)
    private Instant createTime;
}
