package com.mastercard.ids.fts.controller;

import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.service.AuditService;
import com.mastercard.ids.fts.service.MeterRegistryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileTransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private MeterRegistryService meterRegistryService;

    private InboundFile mockInboundFile() {
        InboundFile file = new InboundFile();
        file.setFileId("file123");
        file.setInvocationId("inv123");
        file.setFileDownloadStatus("DOWNLOADED");
        file.setFileUploadStatus("UPLOADED");
        file.setAbortFile(false);
        file.setFileCreatedDate(LocalDateTime.now());
        return file;
    }

    @Test
    void testFindInboundFiles_success() throws Exception {
        InboundFile file = mockInboundFile();
        when(auditService.retrieveInboundFileList(any(), any(), any(), any(), any()))
                .thenReturn(List.of(file));

        mockMvc.perform(get("/fts/files")
                        .param("fileId", "file123")
                        .param("invocationId", "inv123")
                        .param("downloadStatus", "DOWNLOADED")
                        .param("uploadStatus", "UPLOADED")
                        .param("abortStatus", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value("file123"));

        verify(meterRegistryService).recordOutboundFileProcessed();
        verify(auditService).retrieveInboundFileList("file123", "inv123", "DOWNLOADED", "UPLOADED", false);
    }

    @Test
    void testGetFileInfo_found() throws Exception {
        InboundFile file = mockInboundFile();
        when(auditService.retrieveInboundFileList(eq("file123"), any(), any(), any(), any()))
                .thenReturn(List.of(file));

        mockMvc.perform(get("/fts/files/file/file123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value("file123"));

        verify(auditService).retrieveInboundFileList("file123", null, null, null, null);
    }

    @Test
    void testGetFileInfo_notFound() throws Exception {
        when(auditService.retrieveInboundFileList(eq("invalidFileId"), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/fts/files/file/invalidFileId"))
                .andExpect(status().isNotFound());

        verify(auditService).retrieveInboundFileList("invalidFileId", null, null, null, null);
    }
}
