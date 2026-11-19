package com.chitsa.mediaresource.entity;

import com.chitsa.common.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "media_resources")
@Getter
@Setter
public class MediaResource extends BaseJpaEntity {

    /** Storage context (e.g. S3 bucket). */
    @Column(nullable = false)
    private String context;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(nullable = false, length = 2000)
    private String url;

    private String contentType;

    private String originalFilename;
}
