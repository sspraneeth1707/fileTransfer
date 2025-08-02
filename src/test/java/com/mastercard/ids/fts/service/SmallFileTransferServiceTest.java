package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.repository.OutboundRepository;
import com.mastercard.ids.fts.utils.Constants;
import com.mastercard.ids.fts.utils.NGFTConstants;
import com.mastercard.ids.fts.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmallFileTransferServiceTest {

    @InjectMocks
    private SmallFileTransferService service;

    @Mock
    private NGFTService ngftService;

    @Mock
    private S3Service s3Service;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private OutboundRepository outboundRepository;

    @Mock
    private Utils utils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTransferInbound_success() {
        // Arrange
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("file.txt");
        fileInfo.setSize(100L);

        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        byte[] fileContent = "test data".getBytes();
        String checksum = "mockChecksum";

        HttpHeaders headers = new HttpHeaders();
        headers.add(NGFTConstants.HEADER_FILE_CHECKSUM, checksum); // Corrected line
        ResponseEntity<byte[]> response = ResponseEntity.ok().headers(headers).body(fileContent);

        when(ngftService.download(request)).thenReturn(response);
        when(utils.verifyChecksum(fileContent, checksum)).thenReturn(true);
        when(utils.getS3FileNamekey(request)).thenReturn("s3/key/path/file.txt");
        when(utils.getFileMetadata(request)).thenReturn(Map.of("meta", "value"));
        when(s3Service.singleFileUploadS3(fileContent, "s3/key/path/file.txt", Map.of("meta", "value")))
                .thenReturn("etag123");

        // Act
        CompletableFuture<String> result = service.transferInbound(request);

        // Assert
        assertEquals("etag123", result.join());

        verify(fileRepository).updateFileStatusesByFileId(
                "file123", Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS
        );
        verify(fileRepository).updateFileStatusesByFileId(
                "file123", Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED
        );
    }


    @Test
    void testTransferInbound_checksumFailure() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("file.txt");
        fileInfo.setSize(100L);

        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        byte[] fileContent = "bad data".getBytes();
        String checksum = "invalidChecksum";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-NGFT-CHECKSUM", checksum);
        ResponseEntity<byte[]> response = ResponseEntity.ok().headers(headers).body(fileContent);

        when(ngftService.download(request)).thenReturn(response);
        when(utils.verifyChecksum(fileContent, checksum)).thenReturn(false);

        CompletableFuture<String> result = service.transferInbound(request);
        assertEquals("FAILED: file123", result.join());

        verify(fileRepository).updateAsFailedAndIncrementRetry("file123", Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
    }

    @Test
    void testTransferOutbound_success() {
        OutboundFile outboundFile = new OutboundFile();
        outboundFile.setFileId("out123");
        outboundFile.setFileName("out.txt");
        outboundFile.setObjectKey("object.key");

        S3FileData s3FileData = new S3FileData(
                Optional.of("sample".getBytes()),
                Map.of("meta", "value"),
                Optional.empty()
        );

        when(s3Service.singleFileDownloadS3("object.key")).thenReturn(s3FileData);

        CompletableFuture<String> result = service.transferOutbound(outboundFile);
        assertEquals("SUCCESS: object.key", result.join());

        verify(outboundRepository).updateFileStatusesByFileId("out123", Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS);
        verify(ngftService).uploadSingleFile(s3FileData);
        verify(outboundRepository).updateFileStatusesByFileId("out123", Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);
    }


    @Test
    void testTransferOutbound_failure() {
        OutboundFile outboundFile = new OutboundFile();
        outboundFile.setFileId("out456");
        outboundFile.setObjectKey("bad.key");

        when(s3Service.singleFileDownloadS3("bad.key")).thenThrow(new RuntimeException("S3 error"));

        CompletableFuture<String> result = service.transferOutbound(outboundFile);
        assertEquals("FAILED: bad.key", result.join());

        verify(outboundRepository).updateAsFailedAndIncrementRetry("out456", Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
    }

    @Test
    void testFallback() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file999");
        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        RuntimeException cause = new RuntimeException("Downstream unavailable");

        CompletableFuture<S3UploadResponse> future = service.fallback(request, cause);
        assertTrue(future.isCompletedExceptionally());
    }
}
