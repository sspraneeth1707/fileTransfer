package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.OutboundFile;
import com.mastercard.ids.fts.model.S3FileData;
import com.mastercard.ids.fts.repository.OutboundRepository;
import com.mastercard.ids.fts.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundFileListProcessor {

    private final S3Service s3Service;
    private final OutboundRepository outboundRepository;
    private final FileTransferServiceFactory fileTransferServiceFactory;

    public void processOutboundFiles(String objectKey, long fileSize) {

        try {
            S3FileData s3FileData = s3Service.getS3ObjectMetadata(objectKey);

            var now = LocalDateTime.now();
            OutboundFile outboundFile = OutboundFile.builder()
                    .requestId(UUID.randomUUID().toString())
                    .objectKey(objectKey)
                    .receiver(s3FileData.getMetadata().get("x-mc-receiver"))
                    .fileProfile(s3FileData.getMetadata().get("x-mc-file-profile-type"))
                    .fileId(s3FileData.getMetadata().get("x-mc-file-id"))
                    .fileName(s3FileData.getMetadata().get("x-mc-file-name"))
                    .fileSize(Long.valueOf(s3FileData.getMetadata().get("x-mc-file-size")))
                    .contentType(s3FileData.getMetadata().get("content-type"))
                    .fileChecksum(s3FileData.getMetadata().get("x-mc-checksum"))
                    .fileMetadata(s3FileData.getMetadata().get("x-mc-file-metadata"))
                    .fileDownloadTs(now)
                    .fileDownloadStatus(Constants.FILE_PROCESSING_STATUS_PENDING)
                    .fileUploadTs(now)
                    .fileUploadStatus(Constants.FILE_PROCESSING_STATUS_PENDING)
                    .notificationReceivers("default@example.com")
                    .abortFile(false)
                    .retryCount(0)
                    .build();

            outboundRepository.save(outboundFile);
            log.info("Saved Outbound File {}", objectKey);
            List<OutboundFile> validFileList = outboundRepository.findNotCompletedFiles();
            log.info("Outbound : Valid files to process {}", validFileList.size());
            validFileList.forEach(file -> fileTransferServiceFactory.getService(file.getFileSize()).transferOutbound(file).join());

        } catch (Exception e) {
            log.error("Error while processing outbound files : {}", e.getMessage());
            throw e;
        }
    }
}
