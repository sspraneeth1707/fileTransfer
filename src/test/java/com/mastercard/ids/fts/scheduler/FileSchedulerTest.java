package com.mastercard.ids.fts.scheduler;

import com.mastercard.ids.fts.service.FileSchedulerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class FileSchedulerTest {
    @Test
    void shouldInvokeFileSchedulerServiceWithValidParams() {
        // Arrange
        FileSchedulerService mockService = mock(FileSchedulerService.class);
        FileScheduler scheduler = new FileScheduler(mockService);

        // Act
        scheduler.runFileDownloadJob();

        // Assert
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> tsCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(mockService, times(1)).process(idCaptor.capture(), tsCaptor.capture());

        String capturedId = idCaptor.getValue();
        LocalDateTime capturedTs = tsCaptor.getValue();

        assertNotNull(capturedId);
        assertFalse(capturedId.isEmpty());
        assertNotNull(capturedTs);
    }
}
