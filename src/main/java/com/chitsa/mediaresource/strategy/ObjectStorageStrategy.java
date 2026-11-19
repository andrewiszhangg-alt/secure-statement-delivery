package com.chitsa.mediaresource.strategy;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ObjectStorageStrategy {

    String uploadObject(String bucket, String objectKey, MultipartFile multipartFile);

    void deleteObject(String bucket, String objectKey);

    /** Deletes the object, then drops any cached URL for this media row. */
    default void deleteObject(UUID mediaId, String bucket, String objectKey) {
        deleteObject(bucket, objectKey);
    }

    /** Signs without the cache. Used when no media row owns the key. */
    String generatePreSignedUrlForObject(String bucket, String objectKey);

    /** Returns a cached URL for this media row, or signs and stores one. */
    default String generatePreSignedUrlForObject(UUID mediaId, String bucket, String objectKey) {
        return generatePreSignedUrlForObject(bucket, objectKey);
    }
}
