package com.mastercard.ids.fts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercard.ids.fts.model.S3FileData;
import com.mastercard.ids.fts.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Profile("!local")
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final Utils utils;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String uploadS3(int partition, byte[] body, String key, String uploadId) {
        try {
            byte[] md5Bytes = DigestUtils.md5Digest(body);
            String base64EncodedMD5 = Base64.getEncoder().encodeToString(md5Bytes);

            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .uploadId(uploadId)
                    .key(key)
                    .partNumber(partition)
                    .contentMD5(base64EncodedMD5)
                    .build();
            UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(body));
            log.debug("Upload part : uploadS3 : {}", uploadPartResponse.toString());
            return uploadPartResponse.eTag();
        } catch (Exception e) {
            log.error("Error Upload chunk to S3 : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String singleFileUploadS3(byte[] body, String key, Map<String, String> metadata) {
        try {
            byte[] md5Bytes = DigestUtils.md5Digest(body);
            String base64EncodedMD5 = Base64.getEncoder().encodeToString(md5Bytes);

            PutObjectResponse putObjectResponse = s3Client.putObject(PutObjectRequest.builder()
                    .key(key)
                    .bucket(bucketName)
                    .metadata(metadata)
                    .contentMD5(base64EncodedMD5)
                    .build(), RequestBody.fromBytes(body));
            log.debug("singleFileUploadS3 {} ", putObjectResponse.toString());
//            emailService.sendEmail("Single File Uploaded to S3", "Single File uploaded to S3 successfully: " + key);
            return putObjectResponse.eTag();
        } catch (Exception e) {
            log.error("Error Single file Upload to S3: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String initiateMultipartUploadRequest(String key, Map<String, String> metadata) {
        try {
            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .metadata(metadata)
                    .build();
            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
            return createResponse.uploadId();

        } catch (Exception e) {
            log.error("Error Initiating MultiPart File Upload Request to S3: Key : {} , {}", key, e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String completeMultipartUpload(String key, String uploadId, List<CompletedPart> completedParts) {
        try {
            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .uploadId(uploadId)
                    .key(key)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();
            log.debug("Completing Multipart Upload: {}", completeMultipartUploadRequest.toString());
            String eTag = s3Client.completeMultipartUpload(completeMultipartUploadRequest).eTag();

//        emailService.sendEmail("Multipart File Uploaded to S3", "Multipart File uploaded to S3 successfully: " + key);

            return eTag;
        } catch (Exception e) {
            log.error("Error Completing MultiPart File Upload Request to S3: Key : {} , UploadId: {}, {}", key, uploadId, e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void abortMultiPartUpload(String key, String uploadId) {
        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                    .uploadId(uploadId)
                    .key(key)
                    .bucket(bucketName)
                    .build();
            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
            log.info("Aborting Multipart Upload with FileId: {} and upload : {}", key, uploadId);
        } catch (Exception e) {
            log.error("Error Aborting file Upload to S3: Key:{}, UploadId:{}, {}", key, uploadId, e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public S3FileData singleFileDownloadS3(String objectKey) {
        String decodedObjectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(decodedObjectKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getObjectRequest);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            log.debug("Single file download from S3 ObjectResponse: {}", s3Stream.response().toString());
            if (utils.verifyChecksum(byteArrayOutputStream.toByteArray(), s3Stream.response().eTag())) {
                log.error("Checksum verification failed for file download from s3 decodedObjectKey: {}", decodedObjectKey);
                throw new RuntimeException("Checksum verification failed.");
            }
            Map<String, String> metadata = s3Stream.response().metadata();

            //            emailService.sendEmail("Single File Downloaded from S3", "Single File downloaded Successfully from S3: " + decodedObjectKey);
            return new S3FileData(Optional.of(byteArrayOutputStream.toByteArray()), metadata, Optional.ofNullable(s3Stream.response().contentRange()));
        } catch (Exception e) {
            log.error("Error downloading file from s3: " + e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public S3FileData getS3ObjectMetadata(String objectKey) {
        try {
            String decodedObjectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(decodedObjectKey)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            log.debug("S3 object metadata : {}", headObjectResponse.toString());
            return new S3FileData(Optional.empty(), headObjectResponse.metadata(), Optional.empty()); // Returns custom metadata
        } catch (Exception e) {
            log.error("Error fetching metadata from S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public S3FileData multiPartDownloadS3(String decodedObjectKey, long startByte, long endByte) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(decodedObjectKey)
                .range("bytes=" + startByte + "-" + endByte)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getObjectRequest);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            log.trace("S3 part startbytes {} endbytes {} content length {} resp{}", startByte, endByte, byteArrayOutputStream.size(), s3Stream.toString());
            log.debug("Download chunk from s3 decodedObjectKey {},ObjectResponse {}", decodedObjectKey, s3Stream.response().toString());

            Map<String, String> metadata = s3Stream.response().metadata();

            S3FileData fileData = new S3FileData(Optional.of(byteArrayOutputStream.toByteArray()), metadata, Optional.ofNullable(s3Stream.response().contentRange()));
//            emailService.sendEmail("Multipart File Downloaded from S3", "Multipart File downloaded Successfully from S3: " + decodedObjectKey);
            return fileData;
        } catch (Exception e) {
            log.error("Error downloading chunk from S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Recover
    private S3FileData recover(RuntimeException e, String objectKey) {
        log.error("S3 - Max download retries reached for objectKey: {}. Last error: {}", objectKey, e.getMessage());
        return new S3FileData(Optional.empty(), Map.of("error", "Download failed after retries"), Optional.empty());
    }

//    @Recover
//    private String recover(RuntimeException e, String objectKey) {
//        log.error("S3 - Max download retries reached for objectKey: {}. Last error: {}", objectKey, e.getMessage());
//        throw new RuntimeException("Failed to download and verify checksum after multiple retries.", e);
//    }

    @Recover
    private S3FileData recover(RuntimeException e, String decodedObjectKey, long startByte, long endByte) {
        log.error("S3 - Max download retries reached for objectKey: {}. Last error: {}", decodedObjectKey, e.getMessage());
        return new S3FileData(Optional.empty(), Map.of("error", "Download failed after retries"), Optional.empty());
    }

    private LocalDateTime parseDateTime(String input) {
        try {
            return input != null ? LocalDateTime.parse(input) : null;
        } catch (Exception e) {
            return null; // fallback
        }
    }
}
