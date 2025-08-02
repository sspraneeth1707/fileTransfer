package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.S3FileData;
import com.mastercard.ids.fts.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class S3ServiceImplTest {
    @Mock S3Client s3Client;
    @Mock Utils utils;
    @InjectMocks S3ServiceImpl s3Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set bucketName via reflection
        try {
            var field = S3ServiceImpl.class.getDeclaredField("bucketName");
            field.setAccessible(true);
            field.set(s3Service, "test-bucket");
        } catch (Exception ignored) {}
    }

    @Test
    void uploadS3_success() {
        UploadPartResponse response = UploadPartResponse.builder().eTag("etag").build();
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenReturn(response);
        String result = s3Service.uploadS3(1, new byte[]{1,2,3}, "key", "uploadId");
        assertEquals("etag", result);
    }

    @Test
    void uploadS3_exception() {
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.uploadS3(1, new byte[]{1}, "key", "uploadId"));
    }

    @Test
    void singleFileUploadS3_success() {
        PutObjectResponse response = PutObjectResponse.builder().eTag("etag").build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(response);
        String result = s3Service.singleFileUploadS3(new byte[]{1,2,3}, "key", Map.of());
        assertEquals("etag", result);
    }

    @Test
    void singleFileUploadS3_exception() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.singleFileUploadS3(new byte[]{1}, "key", Map.of()));
    }

    @Test
    void initiateMultipartUploadRequest_success() {
        CreateMultipartUploadResponse response = CreateMultipartUploadResponse.builder().uploadId("uploadId").build();
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(response);
        String result = s3Service.initiateMultipartUploadRequest("key", Map.of());
        assertEquals("uploadId", result);
    }

    @Test
    void initiateMultipartUploadRequest_exception() {
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.initiateMultipartUploadRequest("key", Map.of()));
    }

    @Test
    void completeMultipartUpload_success() {
        CompleteMultipartUploadResponse response = CompleteMultipartUploadResponse.builder().eTag("etag").build();
        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(response);
        String result = s3Service.completeMultipartUpload("key", "uploadId", List.of());
        assertEquals("etag", result);
    }

    @Test
    void completeMultipartUpload_exception() {
        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.completeMultipartUpload("key", "uploadId", List.of()));
    }

    @Test
    void abortMultiPartUpload_success() {
        // No need to stub doNothing for void methods in Mockito
        assertDoesNotThrow(() -> s3Service.abortMultiPartUpload("key", "uploadId"));
    }

    @Test
    void abortMultiPartUpload_exception() {
        doThrow(new RuntimeException("fail")).when(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        assertThrows(RuntimeException.class, () -> s3Service.abortMultiPartUpload("key", "uploadId"));
    }

    @Test
    void singleFileDownloadS3_success() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag("etag").metadata(Map.of("foo", "bar")).build();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1,2,3});
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(getObjectResponse, inputStream);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        when(utils.verifyChecksum(any(), any())).thenReturn(false); // checksum ok
        S3FileData data = s3Service.singleFileDownloadS3("key");
        assertTrue(data.getFileContent().isPresent());
        assertEquals("bar", data.getMetadata().get("foo"));
    }

    @Test
    void singleFileDownloadS3_checksumFail() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag("etag").metadata(Map.of()).build();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1,2,3});
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(getObjectResponse, inputStream);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        when(utils.verifyChecksum(any(), any())).thenReturn(true); // checksum fail
        assertThrows(RuntimeException.class, () -> s3Service.singleFileDownloadS3("key"));
    }

    @Test
    void singleFileDownloadS3_exception() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.singleFileDownloadS3("key"));
    }

    @Test
    void getS3ObjectMetadata_success() {
        HeadObjectResponse response = HeadObjectResponse.builder().metadata(Map.of("foo", "bar")).build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);
        S3FileData data = s3Service.getS3ObjectMetadata("key");
        assertEquals("bar", data.getMetadata().get("foo"));
    }

    @Test
    void getS3ObjectMetadata_exception() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.getS3ObjectMetadata("key"));
    }

    @Test
    void multiPartDownloadS3_success() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().metadata(Map.of("foo", "bar")).build();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1,2,3});
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(getObjectResponse, inputStream);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        S3FileData data = s3Service.multiPartDownloadS3("key", 0, 2);
        assertTrue(data.getFileContent().isPresent());
        assertEquals("bar", data.getMetadata().get("foo"));
    }

    @Test
    void multiPartDownloadS3_exception() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> s3Service.multiPartDownloadS3("key", 0, 2));
    }
}
