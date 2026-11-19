package com.chitsa.mediaresource.config;

import com.chitsa.common.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
@EnableConfigurationProperties({AwsS3Properties.class, MediaProperties.class})
public class AwsS3Config {

    /** Static keys from properties when both are set; otherwise env vars, profile files, instance role, etc. */
    @Bean
    AwsCredentialsProvider awsCredentialsProvider(AwsS3Properties properties) {
        if (ValidationUtil.isBlank(properties.getAccessKey()) || ValidationUtil.isBlank(properties.getSecretKey())) {
            log.info("Using default AWS credentials provider chain");
            return DefaultCredentialsProvider.create();
        }
        log.info("Using static AWS credentials from app.aws.access-key");
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }

    @Bean
    S3Client s3Client(AwsS3Properties properties, AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    S3Presigner s3Presigner(AwsS3Properties properties, AwsCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
