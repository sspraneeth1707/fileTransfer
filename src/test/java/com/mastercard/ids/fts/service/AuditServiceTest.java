package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Example;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRetrieveInboundFileList_returnsMatchingFiles() {
        // Arrange
        String fileId = "file123";
        String invocationId = "invoke456";
        String downloadStatus = "DOWNLOADED";
        String uploadStatus = "UPLOADED";
        Boolean abortStatus = false;

        InboundFile matchedFile = new InboundFile();
        matchedFile.setFileId(fileId);
        matchedFile.setInvocationId(invocationId);
        matchedFile.setFileDownloadStatus(downloadStatus);
        matchedFile.setFileUploadStatus(uploadStatus);
        matchedFile.setAbortFile(abortStatus);

        List<InboundFile> mockList = List.of(matchedFile);

        // Mock the repository to return the list
        when(fileRepository.findBy(any(Example.class), any()))
                .thenAnswer(invocation -> {
                    Example<InboundFile> example = invocation.getArgument(0);
                    var queryFunc = invocation.getArgument(1);
                    return mockList;
                });

        // Act
        List<InboundFile> result = auditService.retrieveInboundFileList(fileId, invocationId, downloadStatus, uploadStatus, abortStatus);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(fileId, result.get(0).getFileId());
        verify(fileRepository, times(1)).findBy(any(Example.class), any());
    }

    @Test
    void testRetrieveInboundFileList_returnsEmptyListWhenNoMatch() {
        // Arrange
        when(fileRepository.findBy(any(Example.class), any()))
                .thenReturn(List.of());

        // Act
        List<InboundFile> result = auditService.retrieveInboundFileList("noMatch", "noMatch", "none", "none", true);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileRepository, times(1)).findBy(any(Example.class), any());
    }
}
