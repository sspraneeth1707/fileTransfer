package com.mastercard.ids.fts.controller;

import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.service.AuditService;
import com.mastercard.ids.fts.service.MeterRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("!cloud")
@RestController
@RequestMapping("/fts")
@RequiredArgsConstructor
@Slf4j
public class FileTransferController {

    private final AuditService auditService;

    private final MeterRegistryService meterRegistryService;

    @GetMapping("/files")
    public ResponseEntity<List<InboundFile>> findInboundFiles(
            @RequestParam(name = "fileId", required = false) String fileId,
            @RequestParam(name = "invocationId", required = false) String invocationId,
            @RequestParam(name = "downloadStatus", required = false) String downloadStatus,
            @RequestParam(name = "uploadStatus", required = false) String uploadStatus,
            @RequestParam(name = "abortStatus", required = false) Boolean abortStatus) {

        meterRegistryService.recordOutboundFileProcessed();
        return ResponseEntity.ok(auditService.retrieveInboundFileList(fileId, invocationId, downloadStatus, uploadStatus, abortStatus));
    }

    @GetMapping("/files/file/{fileId}")
    public Object getFileInfo(@PathVariable("fileId") String fileId) {
        List<InboundFile> inboundFiles = auditService.retrieveInboundFileList(fileId, null, null, null, null);
        if (inboundFiles == null || inboundFiles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(inboundFiles.getFirst());
    }
}

