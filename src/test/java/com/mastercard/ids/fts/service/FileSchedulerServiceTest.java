package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.FileInfo;
import com.mastercard.ids.fts.model.SchedulerLog;
import com.mastercard.ids.fts.repository.SchedulerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileSchedulerServiceTest {
    @Mock NGFTService ngftService;
    @Mock InboundFileListProcessor inboundFileListProcessor;
    @Mock SchedulerRepository schedulerRepository;
    @InjectMocks FileSchedulerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FileSchedulerService(ngftService, inboundFileListProcessor, schedulerRepository);
    }

    @Test
    void process_shouldHandleNoFilesFound_nullList() {
        when(ngftService.retrieveFileList()).thenReturn(null);
        service.process("inv1", LocalDateTime.now());
        verify(schedulerRepository).save(any(SchedulerLog.class));
    }

    @Test
    void process_shouldHandleNoFilesFound_emptyList() {
        when(ngftService.retrieveFileList()).thenReturn(List.of());
        service.process("inv1", LocalDateTime.now());
        verify(schedulerRepository).save(any(SchedulerLog.class));
    }

    @Test
    void process_shouldHandleFilesFoundAndProcessed() {
        FileInfo file = new FileInfo();
        when(ngftService.retrieveFileList()).thenReturn(List.of(file));
        when(inboundFileListProcessor.process(anyString(), anyList())).thenReturn(List.of(file));
        service.process("inv1", LocalDateTime.now());
        verify(schedulerRepository).save(any(SchedulerLog.class));
    }

    @Test
    void process_shouldHandleExceptionAndLogFailed() {
        when(ngftService.retrieveFileList()).thenThrow(new RuntimeException("fail"));
        service.process("inv1", LocalDateTime.now());
        verify(schedulerRepository).save(any(SchedulerLog.class));
    }

    @Test
    void saveSchedulerLog_shouldSaveCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        // Use reflection to call private method
        try {
            var m = FileSchedulerService.class.getDeclaredMethod("saveSchedulerLog", String.class, LocalDateTime.class, String.class, int.class);
            m.setAccessible(true);
            m.invoke(service, "inv1", now, "Success", 5);
            verify(schedulerRepository).save(argThat(log ->
                log.getInvocationId().equals("inv1") &&
                log.getInvocationTime().equals(now) &&
                log.getStatus().equals("Success") &&
                log.getTotalFileCount() == 5
            ));
        } catch (Exception e) {
            fail(e);
        }
    }
}

