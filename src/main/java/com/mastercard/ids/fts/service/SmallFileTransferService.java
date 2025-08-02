package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.repository.OutboundRepository;
import com.mastercard.ids.fts.utils.Constants;
import com.mastercard.ids.fts.utils.NGFTConstants;
import com.mastercard.ids.fts.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmallFileTransferService implements FileTransferService {
    private final NGFTService ngftService;
    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final OutboundRepository outboundRepository;
    private final Utils utils;

    /**
     * Downloads a file from API, uploads to S3, and logs CloudWatch metrics.
     */
    @Async
//    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallback")
    public CompletableFuture<String> transferInbound(FileDownloadRequest request) {
        long startTime = System.currentTimeMillis();
        FileInfo fileInfo = request.getFileInfo();

        try {
            fileRepository.updateFileStatusesByFileId(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS);

            log.info("Transferring small file={}, name={}, size={}", fileInfo.getId(), fileInfo.getName(), fileInfo.getSize());
            log.debug("Downloading file from NGFT: {}", fileInfo.getId());
            ResponseEntity<byte[]> response = ngftService.download(request);

            String checksum = response.getHeaders().getFirst(NGFTConstants.HEADER_FILE_CHECKSUM);
            if (Boolean.FALSE.equals(utils.verifyChecksum(response.getBody(), checksum))) {
                log.error("Checksum verification failed for File Id: {} ", request.getFileInfo().getId());
                throw new RuntimeException("Checksum verification failed.");
            }

            String key = utils.getS3FileNamekey(request);
            Map<String, String> metaData = utils.getFileMetadata(request);
            log.debug("Uploading file to S3: {} key: {}", fileInfo.getId(), key);
            String etag = s3Service.singleFileUploadS3(response.getBody(), key, metaData);

            fileRepository.updateFileStatusesByFileId(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);
            long endTime = System.currentTimeMillis();
            log.info("Inbound: Finished transferring small file {} , fileId: {} in {} ms.", fileInfo.getName(), fileInfo.getId(), (endTime - startTime));
            return CompletableFuture.completedFuture(etag);
        } catch (Exception e) {
            log.error("Error in transferring small file: {} fileId: {}", fileInfo.getName(), fileInfo.getId(), e);
            fileRepository.updateAsFailedAndIncrementRetry(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
            return CompletableFuture.completedFuture("FAILED: " + fileInfo.getId());

        }
    }

    @Async
    public CompletableFuture<String> transferOutbound(OutboundFile outboundFile) {
        long startTime = System.currentTimeMillis();
        String objectKey = outboundFile.getObjectKey();

        try {
            outboundRepository.updateFileStatusesByFileId(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS);
            S3FileData s3FileData = s3Service.singleFileDownloadS3(objectKey);
            log.info("File Downloaded from S3 Object Key :{}", objectKey);
            log.debug("File Downloaded from S3 Object Key :{} , object metadata : {}", objectKey, s3FileData.getMetadata());
            ngftService.uploadSingleFile(s3FileData);

        } catch (Exception e) {
            log.error("Error in transferring small file to NGFT objectKey: {}", objectKey, e);
            outboundRepository.updateAsFailedAndIncrementRetry(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
            return CompletableFuture.completedFuture("FAILED: " + objectKey);
        }

        outboundRepository.updateFileStatusesByFileId(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);
        long endTime = System.currentTimeMillis();
        log.info("Outbound: Finished transferring small file {} , fileId: {} in {} ms.", outboundFile.getFileName(), outboundFile.getFileId(), (endTime - startTime));
        return CompletableFuture.completedFuture("SUCCESS: " + objectKey);
    }

    public CompletableFuture<S3UploadResponse> fallback(FileDownloadRequest request, Throwable t) {
        log.error("Fallback triggered for file {}. Error: {}", request.getFileInfo().getId(), t.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("External API failed after retries.", t));
    }


//    @Recover
//    public CompletableFuture<String> recover(RuntimeException e, String objectKey, long fileSize) {
//        log.error("Max download retries reached for file: {},fileSize : {}  Error: {}", objectKey, fileSize, e.getMessage());
//        throw e;
//    }
//
//    @Recover
//    public CompletableFuture<String> recover(RuntimeException e, FileDownloadRequest request) {
//        log.error("Max download retries reached for fileId: {}. Last error: {}", request.getFileInfo().getId(), e.getMessage());
//        throw e;
//    }
}
