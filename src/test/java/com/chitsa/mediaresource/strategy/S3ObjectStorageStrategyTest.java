package com.chitsa.mediaresource.strategy;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.mediaresource.config.AwsS3Properties;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageStrategyTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Utilities utilities;

    private static final UUID MEDIA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private Cache<String, String> cache;

    private Cache<String, Boolean> tombstones;

    private S3ObjectStorageStrategy strategy;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().build();
        tombstones = Caffeine.newBuilder().build();
        strategy = new S3ObjectStorageStrategy(s3Client, s3Presigner, new AwsS3Properties(), cache, tombstones);
    }

    @Test
    void uploadStoresTheObjectAndLogsTheKey() throws Exception {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());
        when(s3Client.utilities()).thenReturn(utilities);
        when(utilities.getUrl(any(GetUrlRequest.class))).thenReturn(URI.create("https://bucket.s3.amazonaws.com/statement.pdf").toURL());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        var logs = attachLogs();

        var url = strategy.uploadObject("account-statements", "accounts/statement.pdf", file);

        assertThat(url).isEqualTo("https://bucket.s3.amazonaws.com/statement.pdf");
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("account-statements");
        assertThat(request.getValue().key()).isEqualTo("accounts/statement.pdf");
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(infoMessages(logs)).noneMatch(message -> message.contains("https://"));
        assertThat(infoMessages(logs)).anyMatch(message -> message.contains("accounts/statement.pdf"));
        detach(logs);
    }

    @Test
    void uploadRejectsAMissingFile() {
        assertThatThrownBy(() -> strategy.uploadObject("bucket", "key", null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.FILE_NOT_NULL);
    }

    @Test
    void uploadRejectsABlankBucketOrKey() {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());

        assertThatThrownBy(() -> strategy.uploadObject(" ", "key", file))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.BUCKET_BLANK);
        assertThatThrownBy(() -> strategy.uploadObject("bucket", " ", file))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.OBJECT_KEY_BLANK);
    }

    @Test
    void uploadWrapsAStorageFailure() {
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("denied").statusCode(403).build());

        assertThatThrownBy(() -> strategy.uploadObject("bucket", "key", file))
                .isInstanceOf(ObjectStorageApiException.class)
                .hasMessage("S3 upload failed for objectKey=key bucket=bucket");
    }

    @Test
    void uploadWrapsAnUnreadableFile() throws Exception {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenThrow(new IOException("unreadable"));

        assertThatThrownBy(() -> strategy.uploadObject("bucket", "key", file))
                .isInstanceOf(ObjectStorageApiException.class)
                .hasMessage("S3 upload failed for objectKey=key bucket=bucket");
    }

    @Test
    void presignReusesTheCachedUrlUntilDelete() throws Exception {
        var signed = presigned("https://signed.example/statement.pdf");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        assertThat(strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf"))
                .isEqualTo("https://signed.example/statement.pdf");
        assertThat(strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf"))
                .isEqualTo("https://signed.example/statement.pdf");
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));

        strategy.deleteObject(MEDIA_ID, "bucket", "statement.pdf");
        strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf");
        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void aFailedDeleteKeepsTheCachedUrl() throws Exception {
        var signed = presigned("https://signed.example/statement.pdf");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("denied").statusCode(403).build());
        strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf");

        assertThatThrownBy(() -> strategy.deleteObject(MEDIA_ID, "bucket", "statement.pdf"))
                .isInstanceOf(ObjectStorageApiException.class);

        strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf");
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void anInFlightSignIsNotCachedAfterDelete() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var signed = presigned("https://signed.example/statement.pdf");
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("sign was not released");
            }
            return signed;
        });
        var failure = new AtomicReference<Throwable>();
        var signer = new Thread(() -> {
            try {
                strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf");
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });

        signer.start();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        strategy.deleteObject(MEDIA_ID, "bucket", "statement.pdf");
        release.countDown();
        signer.join(5_000);

        assertThat(failure.get()).isNull();
        strategy.generatePreSignedUrlForObject(MEDIA_ID, "bucket", "statement.pdf");
        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void aKeyWithoutAMediaIdIsNotCached() throws Exception {
        var signed = presigned("https://signed.example/statement.pdf");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);

        strategy.generatePreSignedUrlForObject("bucket", "statement.pdf");
        strategy.generatePreSignedUrlForObject("bucket", "statement.pdf");

        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    private static PresignedGetObjectRequest presigned(String url) throws Exception {
        var presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create(url).toURL());
        return presigned;
    }

    @Test
    void deleteWrapsAStorageFailure() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("denied").statusCode(403).build());

        assertThatThrownBy(() -> strategy.deleteObject("bucket", "statement.pdf"))
                .isInstanceOf(ObjectStorageApiException.class)
                .hasMessage("S3 delete failed for objectKey=statement.pdf bucket=bucket");
    }

    @Test
    void presignWrapsAStorageFailure() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("denied").statusCode(403).build());

        assertThatThrownBy(() -> strategy.generatePreSignedUrlForObject("bucket", "statement.pdf"))
                .isInstanceOf(ObjectStorageApiException.class)
                .hasMessage("S3 presign failed for objectKey=statement.pdf bucket=bucket");
    }

    private static ListAppender<ILoggingEvent> attachLogs() {
        var logger = (Logger) LoggerFactory.getLogger(S3ObjectStorageStrategy.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(S3ObjectStorageStrategy.class)).detachAppender(appender);
    }

    private static List<String> infoMessages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
