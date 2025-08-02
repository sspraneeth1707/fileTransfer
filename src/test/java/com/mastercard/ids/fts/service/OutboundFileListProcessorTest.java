package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.OutboundFile;
import com.mastercard.ids.fts.model.S3FileData;
import com.mastercard.ids.fts.repository.OutboundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OutboundFileListProcessorTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private OutboundRepository outboundRepository;

    @Mock
    private FileTransferServiceFactory fileTransferServiceFactory;

    @Mock
    private LargeFileTransferService largeFileTransferService;

    @InjectMocks
    private OutboundFileListProcessor outboundFileListProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private S3FileData createMockS3FileData() {
        S3FileData s3FileData = new S3FileData();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-mc-receiver", "receiver1");
        metadata.put("x-mc-file-profile-type", "profileA");
        metadata.put("x-mc-file-id", "file123");
        metadata.put("x-mc-file-name", "test-file.txt");
        metadata.put("x-mc-file-size", "12345");
        metadata.put("content-type", "text/plain");
        metadata.put("x-mc-checksum", "checksum123");
        metadata.put("x-mc-file-metadata", "{}");
        s3FileData.setMetadata(metadata);
        return s3FileData;
    }

    @Test
    void testProcessOutboundFiles_success() {
        String objectKey = "test-object-key";
        long fileSize = 12345L;

        S3FileData mockS3Data = createMockS3FileData();
        when(s3Service.getS3ObjectMetadata(objectKey)).thenReturn(mockS3Data);

        // Simulate saved OutboundFile
        OutboundFile savedFile = OutboundFile.builder()
                .fileId("file123")
                .fileSize(fileSize)
                .objectKey(objectKey)
                .build();

        when(outboundRepository.save(any(OutboundFile.class))).thenReturn(savedFile);
        when(outboundRepository.findNotCompletedFiles()).thenReturn(List.of(savedFile));
        when(fileTransferServiceFactory.getService(fileSize)).thenReturn(largeFileTransferService);
        when(largeFileTransferService.transferOutbound(any(OutboundFile.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> outboundFileListProcessor.processOutboundFiles(objectKey, fileSize));

        verify(s3Service, times(1)).getS3ObjectMetadata(objectKey);
        verify(outboundRepository, times(1)).save(any(OutboundFile.class));
        verify(fileTransferServiceFactory, times(1)).getService(fileSize);
        verify(largeFileTransferService, times(1)).transferOutbound(savedFile);
    }

    @Test
    void testProcessOutboundFiles_failure_s3ServiceThrows() {
        String objectKey = "bad-object-key";
        long fileSize = 123L;

        when(s3Service.getS3ObjectMetadata(objectKey)).thenThrow(new RuntimeException("S3 failure"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                outboundFileListProcessor.processOutboundFiles(objectKey, fileSize));

        assertEquals("S3 failure", exception.getMessage());
        verify(outboundRepository, never()).save(any());
        verify(fileTransferServiceFactory, never()).getService(anyLong());
    }
}
