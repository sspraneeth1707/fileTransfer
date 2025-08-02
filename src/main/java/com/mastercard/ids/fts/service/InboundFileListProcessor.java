package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.FileDownloadRequest;
import com.mastercard.ids.fts.model.FileInfo;
import com.mastercard.ids.fts.model.FileMetadata;
import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.utils.Constants;
import com.mastercard.ids.fts.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class InboundFileListProcessor {
    private final FileTransferServiceFactory fileTransferServiceFactory;
    private final FileRepository fileRepository;
    private final Utils utils;


    @Value("${fts.ngft.receiver}")
    private String ftsReceiverLogicalAddress;

    @Value("${fts.ngft.sender}")
    private String ftsSenderLogicalAddress;

    public List<FileInfo> process(String invocationId, List<FileInfo> fileList) {
        //  Step 2: Filter out already processed file IDs from PostgreSQL
        List<String> fileIdsFromNGFT = fileList.stream().map(FileInfo::getId).toList();
        List<String> fileIdsToBeProcessed = filterProcessedFileIds(fileIdsFromNGFT);  // 🔄 Only process new files
        if (fileIdsToBeProcessed.isEmpty()) {
            log.info("No files to process.");
            return Collections.emptyList();
        }

        //  Step 3: Validate File Type BEFORE Fetching File Info
        List<FileInfo> validFiles = fileList.stream()
                .filter(file -> fileIdsToBeProcessed.contains(file.getId()))  // Only process new files
                .filter(file -> isValidFileType(file.getName()))  // 🔄 Check if CSV/ZIP
                .toList();
        if (validFiles.isEmpty()) {
            log.debug("No valid files to process.");
            return Collections.emptyList();
        }

        //Step 4: Insert FileDetails into InboundFile table
        var now = LocalDateTime.now();
        List<InboundFile> inboundFiles = validFiles.stream()
                .map(fileInfo -> InboundFile.builder()
                                .fileId(fileInfo.getId())
                                .invocationId(invocationId)
                                .requestId("requestId")
                                .sender("sender")
                                .fileProfile(fileInfo.getFileProfile())
                                .fileName(fileInfo.getName())
                                .fileSize(fileInfo.getSize())
                                .contentType(fileInfo.getContentType())
                                .fileChecksum(fileInfo.getChecksum())
//                        .fileMetadata(metadataMap(fileInfo.getMetadata()))
                                .fileMetadata(null)

                                .fileCreatedDate(parseToDateTime(fileInfo.getCreatedDate()))
                                .fileDownloadTs(now)
                                .fileDownloadStatus(Optional.ofNullable(fileInfo.getStatus()).orElse(Constants.FILE_PROCESSING_STATUS_PENDING))
                                .fileUploadTs(now)
                                .fileUploadStatus(Constants.FILE_PROCESSING_STATUS_PENDING)
                                .notificationReceivers("default@example.com") // Update this if needed
                                .abortFile(false)
                                .build()
                )
                .toList();

        fileRepository.saveAll(inboundFiles);
        log.info("Saved {} inbound files", inboundFiles.size());

        // Step 5: Retrieve the FileDetails whose download and upload status is not completed, abort status is false and add them to the valid file for re processing
        List<InboundFile> failedFilesFromDB = fileRepository.findNotCompletedFiles();

        List<FileInfo> filesToProcess = failedFilesFromDB.stream()
                .map(this::convertToFileInfo)
                .sorted(Comparator.comparing(FileInfo::getSize))
                .toList();
        log.info("TotalFileCount={}, FilesToBeProcessed={}, ValidFileCount={}, NotCompletedCount={}", fileList.size(), fileIdsToBeProcessed.size(), validFiles.size(), filesToProcess.size());

        // Step 6: Download and upload files
        List<CompletableFuture<String>> uploadFutures = filesToProcess.stream()
                .map(fileInfoResponse -> CompletableFuture.supplyAsync(() -> {
                    FileDownloadRequest downloadRequest = new FileDownloadRequest();
                    downloadRequest.setReceiver(ftsReceiverLogicalAddress);
                    downloadRequest.setSender(ftsSenderLogicalAddress);
                    downloadRequest.setFileInfo(fileInfoResponse);

                    return fileTransferServiceFactory.getService(fileInfoResponse.getSize()).transferInbound(downloadRequest).join();
                }))
                .toList();

        // Wait for all uploads to complete
        uploadFutures.forEach(CompletableFuture::join);
        return validFiles;
    }

    // Filtering missing file IDs
    private List<String> filterProcessedFileIds(List<String> retrievedFileIds) {
        List<String> existingFileIds = fileRepository.findExistingCompletedFileIds();
        Set<String> existingFileIdsSet = Set.copyOf(existingFileIds);

        return retrievedFileIds.stream()
                .filter(fileId -> !existingFileIdsSet.contains(fileId))
                .toList();
    }

    // Utility to Validate File Type Before Fetching File Info
    private boolean isValidFileType(String fileName) {
        if (fileName == null) return false;
        final String fileExtension = utils.getFileExtension(fileName);
        return fileExtension.equalsIgnoreCase("csv") || fileExtension.equalsIgnoreCase("zip") || fileExtension.equalsIgnoreCase("xlsx");
    }

    private Map<String, Object> metadataMap(List<FileMetadata> metadata) {
        return metadata.stream()
                .collect(Collectors.toMap(
                        FileMetadata::getKey,
                        FileMetadata::getValue
                ));
    }

    private FileInfo convertToFileInfo(InboundFile file) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(file.getFileId());
        fileInfo.setName(file.getFileName());
        fileInfo.setDescription(null); // Set if stored elsewhere
        fileInfo.setSize(file.getFileSize().longValue());
        fileInfo.setCreatedDate(file.getFileCreatedDate().toString());
        fileInfo.setChecksum(file.getFileChecksum());
        fileInfo.setLocation(null); // Set if stored
        fileInfo.setStatus(file.getFileDownloadStatus());
        fileInfo.setStatusTime(file.getFileDownloadTs().toString());
//        if (file.getFileMetadata() != null) {
//            List<FileMetadata> metadataList = file.getFileMetadata().entrySet().stream()
//                    .map(entry -> new FileMetadata(entry.getKey(), entry.getValue().toString()))
//                    .collect(Collectors.toList());
//            fileInfo.setMetadata(metadataList);
//        }
        fileInfo.setMetadata(null);

        fileInfo.setContentType(file.getContentType());
        fileInfo.setFileProfile(file.getFileProfile());
        fileInfo.setFileAliases(List.of()); // Fill in if available
        fileInfo.setTransferRecords(List.of()); // Fill in if available
        return fileInfo;
    }

    private LocalDateTime parseToDateTime(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date '{}'. Falling back to now()", dateStr);
            throw e;
        }
    }
}
