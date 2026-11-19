package com.chitsa.mediaresource.config;

import com.chitsa.common.util.ValidationUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.aws")
public class AwsS3Properties {

    private String region = "eu-west-1";

    /** Optional static credentials; when blank the default AWS provider chain is used. */
    private String accessKey;
    private String secretKey;

    private final S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private int presignExpirationMinutes = 15;
        /** Cache drops the URL this many minutes before the S3 signature dies. */
        private int presignCacheBufferMinutes = 2;

        public Duration signatureDuration() {
            return Duration.ofMinutes(presignExpirationMinutes);
        }

        /** TTL is expiration minus the buffer so a cached URL still has time to be opened. */
        public Duration cacheTtl() {
            var ttlMinutes = presignExpirationMinutes - presignCacheBufferMinutes;
            ValidationUtil.require(ttlMinutes >= 1,
                    "app.aws.s3.presign-cache-buffer-minutes must be smaller than presign-expiration-minutes");
            return Duration.ofMinutes(ttlMinutes);
        }
    }
}
