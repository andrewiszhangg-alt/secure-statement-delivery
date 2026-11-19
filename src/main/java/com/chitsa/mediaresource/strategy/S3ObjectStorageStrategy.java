package com.chitsa.mediaresource.strategy;

import com.chitsa.common.util.ValidationUtil;
import com.chitsa.mediaresource.config.AwsS3Properties;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectStorageStrategy implements ObjectStorageStrategy {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3Properties awsS3Properties;
    private final Cache<String, String> presignedUrlCache;
    private final Cache<String, Boolean> presignedUrlTombstones;

    @Override
    public String uploadObject(String bucket, String objectKey, MultipartFile multipartFile) {
        requireBucketAndKey(bucket, objectKey);
        ValidationUtil.requireNonNull(multipartFile, MediaResourceConstant.FILE_NOT_NULL);

        log.info("Uploading objectKey={} to bucket={}", objectKey, bucket);
        try {
            var putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    multipartFile.getInputStream(), multipartFile.getSize()));

            var url = s3Client.utilities()
                    .getUrl(GetUrlRequest.builder().bucket(bucket).key(objectKey).build())
                    .toString();
            log.info("Uploaded objectKey={}", objectKey);
            return url;
        } catch (SdkException | IOException e) {
            throw storageFailure("upload", bucket, objectKey, e);
        }
    }

    @Override
    public String generatePreSignedUrlForObject(String bucket, String objectKey) {
        requireBucketAndKey(bucket, objectKey);
        return sign(bucket, objectKey);
    }

    @Override
    public String generatePreSignedUrlForObject(UUID mediaId, String bucket, String objectKey) {
        requireBucketAndKey(bucket, objectKey);
        ValidationUtil.requireNonNull(mediaId, MediaResourceConstant.MEDIA_ID_NOT_NULL);
        var key = cacheKey(mediaId);
        if (isTombstoned(key)) {
            return sign(bucket, objectKey);
        }
        var cached = presignedUrlCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        var url = sign(bucket, objectKey);
        if (isTombstoned(key)) {
            return url;
        }
        presignedUrlCache.put(key, url);
        if (isTombstoned(key)) {
            presignedUrlCache.invalidate(key);
        }
        return url;
    }

    @Override
    public void deleteObject(String bucket, String objectKey) {
        requireBucketAndKey(bucket, objectKey);
        deleteFromStorage(bucket, objectKey);
    }

    /** The cached URL stays until S3 accepts the delete. */
    @Override
    public void deleteObject(UUID mediaId, String bucket, String objectKey) {
        requireBucketAndKey(bucket, objectKey);
        ValidationUtil.requireNonNull(mediaId, MediaResourceConstant.MEDIA_ID_NOT_NULL);
        deleteFromStorage(bucket, objectKey);
        var key = cacheKey(mediaId);
        presignedUrlTombstones.put(key, Boolean.TRUE);
        presignedUrlCache.invalidate(key);
    }

    private String sign(String bucket, String objectKey) {
        log.info("Generating presigned URL for objectKey={} bucket={}", objectKey, bucket);
        try {
            var presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(awsS3Properties.getS3().signatureDuration())
                    .getObjectRequest(builder -> builder.bucket(bucket).key(objectKey))
                    .build();
            var url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Presigned URL created for objectKey={}", objectKey);
            return url;
        } catch (SdkException e) {
            throw storageFailure("presign", bucket, objectKey, e);
        }
    }

    private void deleteFromStorage(String bucket, String objectKey) {
        log.info("Deleting objectKey={} from bucket={}", objectKey, bucket);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            log.info("Deleted objectKey={} from bucket={}", objectKey, bucket);
        } catch (SdkException e) {
            throw storageFailure("delete", bucket, objectKey, e);
        }
    }

    private boolean isTombstoned(String key) {
        return presignedUrlTombstones.getIfPresent(key) != null;
    }

    private static String cacheKey(UUID mediaId) {
        return mediaId.toString();
    }

    private static void requireBucketAndKey(String bucket, String objectKey) {
        ValidationUtil.requireNotBlank(bucket, MediaResourceConstant.BUCKET_BLANK);
        ValidationUtil.requireNotBlank(objectKey, MediaResourceConstant.OBJECT_KEY_BLANK);
    }

    private ObjectStorageApiException storageFailure(String operation, String bucket, String objectKey, Exception e) {
        log.error("S3 {} failed for objectKey={} bucket={}: {}", operation, objectKey, bucket, e.getMessage());
        return new ObjectStorageApiException(
                "S3 " + operation + " failed for objectKey=" + objectKey + " bucket=" + bucket, e);
    }
}
