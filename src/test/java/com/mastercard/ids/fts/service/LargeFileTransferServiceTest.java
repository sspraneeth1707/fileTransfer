package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.repository.OutboundRepository;
import com.mastercard.ids.fts.utils.Constants;
import com.mastercard.ids.fts.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LargeFileTransferServiceTest {

    @Mock private NGFTService ngftService;
    @Mock private S3Service s3Service;
    @Mock private FileRepository fileRepository;
    @Mock private OutboundRepository outboundRepository;
    @Mock private Utils utils;

    @InjectMocks
    private LargeFileTransferService transferService;

    private final long chunkSize = 5L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transferService = new LargeFileTransferService(
                ngftService, s3Service, fileRepository, outboundRepository, utils
        );
        // Use reflection to set private field CHUNK_SIZE
        try {
            java.lang.reflect.Field field = LargeFileTransferService.class.getDeclaredField("CHUNK_SIZE");
            field.setAccessible(true);
            field.set(transferService, chunkSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testTransferInbound_success() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("testFile");
        fileInfo.setSize(11L);

        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        Map<String, String> metadata = Map.of("meta", "data");
        String key = "key123";
        String uploadId = "upload-001";

        when(utils.getS3FileNamekey(any())).thenReturn(key);
        when(utils.getFileMetadata(any())).thenReturn(metadata);
        when(s3Service.initiateMultipartUploadRequest(any(), any())).thenReturn(uploadId);
        when(ngftService.downloadChunk(any(), anyInt(), anyLong()))
                .thenReturn(ResponseEntity.ok("chunk".getBytes()));
        when(s3Service.uploadS3(anyInt(), any(), any(), any())).thenReturn("etag-part");

        when(s3Service.completeMultipartUpload(eq(key), eq(uploadId), any())).thenReturn("final-etag");

        CompletableFuture<String> result = transferService.transferInbound(request);

        assertEquals("Completed", result.join());
        verify(fileRepository).updateFileStatusesByFileId(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);
    }

    @Test
    void testTransferInbound_uploadFailure_abortsUpload() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("testFile");
        fileInfo.setSize(10L);

        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        when(utils.getS3FileNamekey(any())).thenReturn("key");
        when(utils.getFileMetadata(any())).thenReturn(Map.of());
        when(s3Service.initiateMultipartUploadRequest(any(), any())).thenReturn("upload-id");
        when(ngftService.downloadChunk(any(), anyInt(), anyLong()))
                .thenReturn(ResponseEntity.ok("chunk".getBytes()));
        when(s3Service.uploadS3(anyInt(), any(), any(), any())).thenReturn(null); // Simulate failure

        CompletableFuture<String> result = transferService.transferInbound(request);

        assertTrue(result.join().startsWith("FAILED"));
        verify(s3Service).abortMultiPartUpload(eq(fileInfo.getId()), eq("upload-id"));
        verify(fileRepository).updateAsFailedAndIncrementRetry(eq(fileInfo.getId()), any(), any());
    }

    @Test
    void testTransferOutbound_success() throws Exception {
        OutboundFile outboundFile = new OutboundFile();
        outboundFile.setFileId("out123");
        outboundFile.setFileSize(10L); // This gives 2 partitions (5L CHUNK_SIZE)
        outboundFile.setObjectKey("object.key");

        S3FileData s3FileData = new S3FileData();
        s3FileData.setFileContent(Optional.of("chunk".getBytes()));
        s3FileData.setMetadata(Map.of("key", "val"));

        InitializeMultipartUploadNGFTResponse ngftInit = new InitializeMultipartUploadNGFTResponse("ngft-upload-id", "ngft-file-id");

        when(s3Service.getS3ObjectMetadata(any())).thenReturn(s3FileData); // used before the stream
        when(s3Service.multiPartDownloadS3(anyString(), anyLong(), anyLong())).thenReturn(s3FileData); // used inside the stream
        when(ngftService.initiateMultiPartUpload(any())).thenReturn(ngftInit);
        doNothing().when(ngftService).uploadPartFile(any(), any(), anyInt());
        doNothing().when(ngftService).completedMultiPartUpload(any(), any());

        CompletableFuture<String> result = transferService.transferOutbound(outboundFile);

        assertEquals("Completed", result.join());
        verify(outboundRepository).updateFileStatusesByFileId(outboundFile.getFileId(),
                Constants.FILE_PROCESSING_STATUS_COMPLETED,
                Constants.FILE_PROCESSING_STATUS_COMPLETED);
    }

    @Test
    void testTransferOutbound_chunkEmpty_abortsUpload() {
        OutboundFile outboundFile = new OutboundFile();
        outboundFile.setFileId("out123");
        outboundFile.setFileSize(5L);
        outboundFile.setObjectKey("object.key");

        S3FileData s3FileData = new S3FileData();
        s3FileData.setFileContent(Optional.of(new byte[0])); // Simulate empty chunk
        s3FileData.setMetadata(Map.of());

        InitializeMultipartUploadNGFTResponse ngftInit = new InitializeMultipartUploadNGFTResponse("ngft-upload-id", "ngft-file-id");

        when(s3Service.getS3ObjectMetadata(any())).thenReturn(s3FileData);
        when(ngftService.initiateMultiPartUpload(any())).thenReturn(ngftInit);

        CompletableFuture<String> result = transferService.transferOutbound(outboundFile);
        String resultValue = result.join();

        assertTrue(resultValue.startsWith("FAILED"), "Expected result to start with FAILED");
        verify(ngftService).abortMultiPartUpload(any(), eq(ngftInit));

        verify(ngftService).abortMultiPartUpload(any(), eq(ngftInit));
    }
}
