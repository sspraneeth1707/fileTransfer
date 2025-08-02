package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.S3FileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3ServiceLocalImplTest {
    private S3ServiceLocalImpl service;

    @BeforeEach
    void setUp() {
        service = new S3ServiceLocalImpl();
    }

    @Test
    void uploadS3_returnsNotSupported() {
        String result = service.uploadS3(1, new byte[]{1,2,3}, "key", "uploadId");
        assertEquals("Not supported in local", result);
    }

    @Test
    void singleFileUploadS3_returnsNotSupported() {
        String result = service.singleFileUploadS3(new byte[]{1,2,3}, "key", Map.of());
        assertEquals("Not supported in local", result);
    }

    @Test
    void initiateMultipartUploadRequest_returnsNotSupported() {
        String result = service.initiateMultipartUploadRequest("key", Map.of());
        assertEquals("Not supported in local", result);
    }

    @Test
    void completeMultipartUpload_returnsNotSupported() {
        String result = service.completeMultipartUpload("key", "uploadId", List.of());
        assertEquals("Not supported in local", result);
    }

    @Test
    void abortMultiPartUpload_doesNotThrow() {
        assertDoesNotThrow(() -> service.abortMultiPartUpload("key", "uploadId"));
    }

    @Test
    void singleFileDownloadS3_returnsNull() {
        assertNull(service.singleFileDownloadS3("key"));
    }

    @Test
    void getS3ObjectMetadata_returnsNull() {
        assertNull(service.getS3ObjectMetadata("key"));
    }

    @Test
    void multiPartDownloadS3_returnsNull() {
        assertNull(service.multiPartDownloadS3("key", 0, 10));
    }
}

