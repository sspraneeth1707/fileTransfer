package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.repository.OutboundRepository;
import com.mastercard.ids.fts.utils.Constants;
import com.mastercard.ids.fts.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeFileTransferService implements FileTransferService {
    private final NGFTService ngftService;
    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final OutboundRepository outboundRepository;
    private final Utils utils;

    @Value("${file.chunk.size}")
    private long CHUNK_SIZE;

    /**
     * Downloads a file from API, uploads to S3, and logs CloudWatch metrics.
     */
    @Async
//    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public CompletableFuture<String> transferInbound(FileDownloadRequest request) {
        long startTime = System.currentTimeMillis();
        FileInfo fileInfo = request.getFileInfo();

        try {
            fileRepository.updateFileStatusesByFileId(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS);
            long fileSize = fileInfo.getSize();
            final int noOfChunks = (int) (fileSize / CHUNK_SIZE);
            log.info("Transferring large file={}, name={}, size={}, noOfChunks={}", fileInfo.getId(), fileInfo.getName(), fileInfo.getSize(), noOfChunks + 1);

            List<Integer> partitions = IntStream.range(1, noOfChunks + 1)
                    .boxed()
                    .toList();
            List<CompletedPart> completedParts = new ArrayList<>();

            String key = utils.getS3FileNamekey(request); //will override the file if upload with same key
            Map<String, String> metaData = utils.getFileMetadata(request);
            String uploadId = s3Service.initiateMultipartUploadRequest(key, metaData);
            log.debug("Initiated multipart upload to S3. fileId={}, uploadId={}", fileInfo.getId(), uploadId);

            try {
                partitions.parallelStream().forEach(partition -> {
                    ResponseEntity<byte[]> ngftChunkResponseEntity = ngftService.downloadChunk(request, partition, fileSize);
                    log.trace("NGFT partNumber {} content-length {} resp{}", partition, ngftChunkResponseEntity.getBody().length, ngftChunkResponseEntity);
                    log.debug("Downloaded chunk from NGFT. fileId={}, partNumber={}", fileInfo.getId(), partition);

                    String eTagFromS3 = s3Service.uploadS3(partition, ngftChunkResponseEntity.getBody(), key, uploadId); // Directly return the ETag
                    if (eTagFromS3 != null) {
                        log.debug("Uploaded chunk to S3. FileId={}, Partition={}", fileInfo.getId(), partition);
                        completedParts.add(CompletedPart.builder()
                                .eTag(eTagFromS3)
                                .partNumber(partition)
                                .build());
                    } else {
                        log.error("Failed to upload part {} to S3 for file {} ,fileId : {}. Aborting large file transfer.", partition, fileInfo.getName(), fileInfo.getId());
                        throw new RuntimeException(String.format("Failed to upload part to S3. Aborting large file transfer. FileId=%s, Partition=%d", fileInfo.getId(), partition));
                    }
                });
                long remainingBytes = fileSize - noOfChunks * CHUNK_SIZE;
                if (remainingBytes > 0) {
                    log.debug("Handling remaining bytes: {}", remainingBytes);
                    int lastPartNumber = noOfChunks + 1;
                    ResponseEntity<byte[]> ngftChunkResponseEntity = ngftService.downloadChunk(request, lastPartNumber, fileSize);
                    log.trace("NGFT partNumber {} content-length {} resp{}", lastPartNumber, ngftChunkResponseEntity.getBody().length, ngftChunkResponseEntity.toString());
                    log.debug("Downloaded chunk from NGFT. fileId={}, partNumber={}", fileInfo.getId(), lastPartNumber);

                    String eTagFromS3 = s3Service.uploadS3(lastPartNumber, ngftChunkResponseEntity.getBody(), key, uploadId); // Directly return the ETag
                    if (eTagFromS3 != null) {
                        completedParts.add(CompletedPart.builder()
                                .eTag(eTagFromS3)
                                .partNumber(lastPartNumber)
                                .build());
                    } else {
                        log.error("Failed to upload part {} to S3 for file {} fileId: {}. Aborting large file transfer.", lastPartNumber, fileInfo.getName(), fileInfo.getId());
                        throw new RuntimeException(String.format("Failed to upload part to S3. Aborting large file transfer. FileId=%s, Partition=%d", fileInfo.getId(), lastPartNumber));
                    }
                }

                completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));
                String finalEtag = s3Service.completeMultipartUpload(key, uploadId, completedParts);
                log.debug("Multipart upload completed for fileId={}, finalEtag={}", fileInfo.getId(), finalEtag);

                // Update the fileDownloadStatus and fileUploadStatus as 'Completed'
                fileRepository.updateFileStatusesByFileId(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);

            } catch (Exception e) {
                log.error("Error during multipart download and upload for file {} fileId: {} : {}", fileInfo.getName(), fileInfo.getId(), e.getMessage());
                s3Service.abortMultiPartUpload(fileInfo.getId(), uploadId);
                throw e;
            }
            long endTime = System.currentTimeMillis();
            log.info("Inbound: Finished transferring large file:{} , fileId: {} to S3 in {} ms.", fileInfo.getName(), fileInfo.getId(), (endTime - startTime));
            return CompletableFuture.completedFuture("Completed");
        } catch (Exception e) {
            log.error("Error while Transfer Inbound file Exception: {}", e.getMessage());
            fileRepository.updateAsFailedAndIncrementRetry(fileInfo.getId(), Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
            return CompletableFuture.completedFuture("FAILED: " + fileInfo.getId());

        }
    }

    @Async
//    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public CompletableFuture<String> transferOutbound(OutboundFile outboundFile) {
        try {
            outboundRepository.updateFileStatusesByFileId(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_IN_PROGRESS, Constants.FILE_PROCESSING_STATUS_IN_PROGRESS);
            long startTime = System.currentTimeMillis();
            String objectKey = outboundFile.getObjectKey();
            long fileSize = outboundFile.getFileSize();
            log.info("Transferring large file:{}, size:{}", objectKey, fileSize);

            final int noOfChunks = (int) (fileSize / CHUNK_SIZE);
            List<Integer> partitions = IntStream.range(1, noOfChunks + 1)
                    .boxed()
                    .toList();
            S3FileData s3FileData = s3Service.getS3ObjectMetadata(objectKey);
            log.debug("Large File S3 Object Key :{} , object metadata : {}", objectKey, s3FileData.getMetadata());
            InitializeMultipartUploadNGFTResponse initiateMultiPartUpload = ngftService.initiateMultiPartUpload(s3FileData);
            log.info("Initialize multipart upload to NGFT - uploadId: {} fileId : {}", initiateMultiPartUpload.getUploadId(), initiateMultiPartUpload.getFileID());

            try {
                partitions.parallelStream().forEach(partition -> outboundTransferChunk(objectKey, fileSize, partition, initiateMultiPartUpload));

                long remainingBytes = fileSize - noOfChunks * CHUNK_SIZE;
                if (remainingBytes > 0) {
                    log.debug("Handling remaining bytes: {}", remainingBytes);
                    int lastPartNumber = noOfChunks + 1;
                    outboundTransferChunk(objectKey, fileSize, lastPartNumber, initiateMultiPartUpload);
                }
                //Complete multipart upload
                ngftService.completedMultiPartUpload(s3FileData, initiateMultiPartUpload);

                outboundRepository.updateFileStatusesByFileId(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_COMPLETED, Constants.FILE_PROCESSING_STATUS_COMPLETED);

            } catch (Exception e) {
                log.error("Error during multipart download and upload for objectKey {}: {}", objectKey, e.getMessage());
                //abort multipart upload
                ngftService.abortMultiPartUpload(s3FileData, initiateMultiPartUpload);
                throw e;
            }
            long endTime = System.currentTimeMillis();
            log.info("Outbound: Finished transferring large file {} fileSize: {} to NGFT in {} ms.", objectKey, fileSize, (endTime - startTime));
            return CompletableFuture.completedFuture("Completed");
        } catch (Exception e) {
            outboundRepository.updateAsFailedAndIncrementRetry(outboundFile.getFileId(), Constants.FILE_PROCESSING_STATUS_FAILED, Constants.FILE_PROCESSING_STATUS_FAILED);
            log.error("Error while Transfer Outbound file Exception: {}", e.getMessage());
            return CompletableFuture.completedFuture("FAILED: " + outboundFile.getObjectKey());

        }
    }

    private void outboundTransferChunk(String objectKey, long fileSize, Integer partition, InitializeMultipartUploadNGFTResponse initiateMultiPartUpload) {
        try {
            //Download chunk from s3
            S3FileData s3Chunk = downloadChunkFromS3(objectKey, partition, fileSize);
            log.debug("Downloaded chunk from S3 - objectKey={}, Part={} ", objectKey, partition);

            if (s3Chunk.getFileContent().isEmpty() || s3Chunk.getFileContent().get().length == 0) {
                log.error("Downloaded chunk is empty for objectKey={}, Part={}. Aborting upload.", objectKey, partition);
                throw new RuntimeException(String.format("Empty chunk received from S3. objectKey=%s, Partition=%d", objectKey, partition));
            }
            //upload to NGFT
            ngftService.uploadPartFile(s3Chunk, initiateMultiPartUpload, partition);
        } catch (Exception e) {
            log.error("Failed to upload part {} to NGFT for objectKey {}. Aborting large file transfer.", partition, objectKey);
            throw new RuntimeException(String.format("Failed to upload part to NGFT. Aborting large file transfer. objectKey=%s, Partition=%d", objectKey, partition));
        }
    }

    private S3FileData downloadChunkFromS3(String objectKey, Integer partition, long fileSize) {
        long startByte = (partition - 1) * CHUNK_SIZE;
        long endByte;

        if (partition * CHUNK_SIZE < fileSize) {
            endByte = (long) partition * CHUNK_SIZE - 1;
        } else {
            endByte = fileSize - 1;
        }
        String decodedObjectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);

        return s3Service.multiPartDownloadS3(decodedObjectKey, startByte, endByte);

    }
}
