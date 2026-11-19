package com.chitsa.mediaresource.config;

import com.chitsa.common.exception.InvalidParameterException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageConfigTest {

    @Test
    void cacheExpiresBeforeTheSignature() {
        var s3 = new AwsS3Properties.S3();

        assertThat(s3.signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(s3.cacheTtl()).isEqualTo(Duration.ofMinutes(13));
    }

    @Test
    void cacheTtlRequiresAShorterBufferThanTheSignature() {
        var s3 = new AwsS3Properties.S3();
        s3.setPresignCacheBufferMinutes(15);

        assertThatThrownBy(s3::cacheTtl)
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("app.aws.s3.presign-cache-buffer-minutes must be smaller than presign-expiration-minutes");
    }

    @Test
    void blankKeysUseTheDefaultProviderChain() {
        var properties = new AwsS3Properties();
        properties.setAccessKey(" ");
        properties.setSecretKey("");

        AwsCredentialsProvider provider = new AwsS3Config().awsCredentialsProvider(properties);

        assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
        ((DefaultCredentialsProvider) provider).close();
    }

    @Test
    void bothKeysUseStaticCredentials() {
        var properties = new AwsS3Properties();
        properties.setRegion("eu-central-1");
        properties.setAccessKey("test-access-key");
        properties.setSecretKey("test-secret-key");
        var config = new AwsS3Config();

        AwsCredentialsProvider provider = config.awsCredentialsProvider(properties);

        assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
        try (var client = config.s3Client(properties, provider);
             var presigner = config.s3Presigner(properties, provider)) {
            assertThat(client).isNotNull();
            assertThat(presigner).isNotNull();
        }
        var caches = new CacheConfig();
        var cache = caches.presignedUrlCache(properties);
        cache.put("media-id", "https://signed.example/key");
        assertThat(cache.getIfPresent("media-id")).isEqualTo("https://signed.example/key");
        var tombstones = caches.presignedUrlTombstones(properties);
        tombstones.put("media-id", Boolean.TRUE);
        assertThat(tombstones.getIfPresent("media-id")).isTrue();
    }
}
