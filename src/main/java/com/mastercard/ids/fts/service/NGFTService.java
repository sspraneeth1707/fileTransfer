package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.utils.Utils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NGFTService {

    @Value("${file.api.base-url}")
    private String baseUrl;

    @Value("${file.api.list-endpoint}")
    private String listEndpoint;

    @Value("${file.api.initiate-endpoint}")
    private String initiateEndpoint;

    @Value("${file.api.cancel-endpoint}")
    private String cancelEndpoint;

    @Value("${file.api.complete-endpoint}")
    private String completeEndpoint;

    @Value("${file.api.info-endpoint}")
    private String infoEndpoint;

    @Value("${fts.ngft.receiver}")
    private String ftsLogicalAddress;

    @Value("${fts.ngft.filelist.page-size}")
    private int ngftFileListPageSize;

    @Value("${fts.ngft.filelist.status}")
    private String ngftFileListStatus;

    @Value("${file.api.transfer-endpoint}")
    private String transferEndpoint;

    @Value("${file.chunk.size}")
    private long CHUNK_SIZE;

    private final RestClient restClient;
    private final Utils utils;

    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackFileList")
    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public List<FileInfo> retrieveFileList() {
        log.debug("Retrieving file list");
        List<FileInfo> fileList = null;
        try {
            int currentPage = 1;

            var request = new FileListRequest();
            request.setReceiver(ftsLogicalAddress);
            request.setPageSize(ngftFileListPageSize);
            request.setFileStatus(ngftFileListStatus);

            fileList = new ArrayList<>();
            FileListResponse fileListResponse = null;
            do {
                fileListResponse = retrieveFileList(request, currentPage);
                fileList.addAll(fileListResponse.getFileListing().getFiles());
                currentPage++;
            } while (fileListResponse != null && currentPage <= fileListResponse.getTotalPages());
        } catch (Exception e) {
            log.error("Error retrieving file list", e);
            throw new RuntimeException("Failed to retrieve file list", e);
        }
        return fileList;
    }

    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackFileList")
    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public FileListResponse retrieveFileList(FileListRequest request, int pageNumber) {
        log.debug("Retrieving file list. page number: {}", pageNumber);
        try {
            String url = baseUrl + listEndpoint;
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-mc-receiver", request.getReceiver());
            headers.set("x-mc-page-size", String.valueOf(request.getPageSize()));
            headers.set("x-mc-page-number", String.valueOf(pageNumber));
            headers.set("x-mc-file-status", request.getFileStatus());
            ResponseEntity<FileListResponse> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(FileListResponse.class);
            log.debug("Retrieved file list. TotalRecords={}, CurrentPageSize={}, CurrentPage={}", response.getBody().getTotalRecords(), response.getBody().getCurrentPageSize(), response.getBody().getCurrentPage());
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file list", e);
            throw new RuntimeException("Failed to retrieve file list", e);
        }
    }

    public CompletableFuture<FileListResponse> fallbackFileList(FileListRequest request, Throwable t) {
        log.error("External API is down, returning empty file list.");
        return CompletableFuture.completedFuture(new FileListResponse());
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public ResponseEntity<byte[]> download(FileDownloadRequest request) {
        try {
            log.debug("Download file. Id={}", request.getFileInfo().getId());
            String url = baseUrl + transferEndpoint;
            HttpHeaders headers = utils.getDownloadFileHeader(request);

            log.debug("Download file. Id={}, Headers={}", request.getFileInfo().getId(), headers);
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to fetch data. Status: " + response.getStatusCode());
            }
            return response;
        } catch (Exception e) {
            log.error("downloadFromNGFT singleFileDownloadFromNGFT: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ResponseEntity<byte[]> downloadChunk(FileDownloadRequest request, Integer partition, long fileSize) {
        String fileId = request.getFileInfo().getId();
        try {
            String url = baseUrl + transferEndpoint;
            HttpHeaders headers = utils.getDownloadFileHeader(request);
            long startByte = (long) (partition - 1) * CHUNK_SIZE;
            long endByte;

            if (partition * CHUNK_SIZE < fileSize) {
                endByte = (long) partition * CHUNK_SIZE;
            } else {
                endByte = fileSize;
            }
            headers.set("Range", "bytes=" + startByte + "-" + endByte);

            log.debug("Download chunk from NGFT. fileId={}, partition={}, byte={}-{} ", fileId, partition, startByte, endByte);

            ResponseEntity<byte[]> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // Throwing exception to trigger retry without updating status immediately
                throw new RuntimeException(String.format("Failed to fetch data. fileId=%s, partition=%s, statusCode=%s", fileId, partition, response.getStatusCode()));
            }
            return response;
        } catch (Exception e) {
            log.error(String.format("Error in downloading a chunk. fileId=%s, partition=%s", fileId, partition), e);
            throw new RuntimeException(e);
        }
    }

    public void uploadSingleFile(S3FileData fileData) {
        try {
            String url = baseUrl + transferEndpoint;
            HttpHeaders headers = utils.getUploadFileHeader(fileData);

            Resource resource = new ByteArrayResource(fileData.getFileContent().get());

            ResponseEntity<FileUploadResponse> response = restClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(resource)
                    .retrieve()
                    .toEntity(FileUploadResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Single File uploaded successfully.");
                log.debug("Single file Upload to NGFT Response {}", response);
            } else {
                log.error("Single File upload to NGFT API responded with error status {}", response.getStatusCode());
                throw new HttpClientErrorException(response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error uploading file to NGFT Code: {} message: {} ", e.getStatusCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred while calling File Transfer service", e);
            throw e;
        }
    }


    public InitializeMultipartUploadNGFTResponse initiateMultiPartUpload(S3FileData fileData) {
        try {
            Map<String, String> metadata = fileData.getMetadata();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-mc-sender", metadata.get("x-mc-sender"));
            headers.set("x-mc-receiver", metadata.get("x-mc-receiver"));
            headers.set("x-mc-file-name", metadata.get("x-mc-file-name"));
            headers.set("x-mc-file-size", metadata.get("x-mc-file-size"));
            headers.set("x-mc-checksum", metadata.get("x-mc-checksum"));
            headers.set("x-mc-part-size", String.valueOf(CHUNK_SIZE));
            headers.set("x-mc-file-content-type", metadata.get("content-type"));
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            ResponseEntity<Object> response = restClient.post()
                    .uri(baseUrl + initiateEndpoint)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(Object.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Initiale multipart response: {}", response.toString());
                String uploadId = response.getHeaders().getFirst("x-mc-multipart-upload-id");
                String fileId = response.getHeaders().getFirst("x-mc-file-id");
                return new InitializeMultipartUploadNGFTResponse(fileId, uploadId);
            } else {
                log.error("Initiale multipart API responded with error status {}", response.getStatusCode());
                throw new HttpServerErrorException(response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error initiating multipart upload to NGFT", e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void uploadPartFile(S3FileData fileData, InitializeMultipartUploadNGFTResponse multiPartUploadID, Integer partition) throws Exception {
        try {
            Map<String, String> metadata = fileData.getMetadata();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-mc-sender", metadata.get("x-mc-sender"));
            headers.set("x-mc-receiver", metadata.get("x-mc-receiver"));
            headers.set("x-mc-file-name", metadata.get("x-mc-file-name"));
            headers.set("x-mc-part-num", String.valueOf(partition));
            headers.set("Content-Length", String.valueOf(fileData.getFileContent().get().length));
            headers.set("x-mc-multipart-upload-id", multiPartUploadID.getUploadId());
            headers.set("x-mc-file-id", multiPartUploadID.getFileID());
            headers.set("x-mc-file-content-type", metadata.get("content-type"));
            headers.set(HttpHeaders.CONTENT_RANGE, fileData.getFileContentRange().get());
            headers.set("x-mc-part-checksum", DigestUtils.md5DigestAsHex(fileData.getFileContent().get()));
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            Resource resource = new ByteArrayResource(fileData.getFileContent().get());
            ResponseEntity<Void> response = restClient.put()
                    .uri(baseUrl + transferEndpoint)
                    .headers(h -> h.addAll(headers))
                    .body(resource)
                    .retrieve()
                    .toEntity(Void.class);
            if (response.getStatusCode().is2xxSuccessful() && utils.verifyChecksum(fileData.getFileContent().get(), response.getHeaders().getFirst("x-mc-checksum"))) {
                log.debug("Successful multipart Upload - part {} to NGFT response: {}", partition, response);
                log.trace("Checksum validation for part :{} ,Content Range: {}  ,Checksum calculated {} - checksum response {}", partition, fileData.getFileContentRange().get(), DigestUtils.md5DigestAsHex(fileData.getFileContent().get()), response.getHeaders().getFirst("x-mc-checksum"));
            } else {
                log.error("Error multipart Upload - part {} checksum : {} to NGFT response: {}", partition, utils.verifyChecksum(fileData.getFileContent().get(), response.getHeaders().getFirst("x-mc-checksum")), response);
                throw new RuntimeException(response.toString());
            }
        } catch (Exception e) {
            log.error("Error uploadPartFile to NGFT: " + e.getMessage());
            throw e;
        }
    }

    public void completedMultiPartUpload(S3FileData fileData, InitializeMultipartUploadNGFTResponse multiPartUploadID) {
        try {
            Map<String, String> metadata = fileData.getMetadata();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-mc-sender", metadata.get("x-mc-sender"));
            headers.set("x-mc-receiver", metadata.get("x-mc-receiver"));
            headers.set("x-mc-file-name", metadata.get("x-mc-file-name"));
            headers.set("x-mc-multipart-upload-id", multiPartUploadID.getUploadId());
            headers.set("x-mc-file-id", multiPartUploadID.getFileID());

            ResponseEntity<Object> response = restClient.put()
                    .uri(baseUrl + completeEndpoint)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(Object.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Completed Multi Part upload response: {}", response);
            } else {
                log.error("Error completing multipart API responded with error status {}", response.getStatusCode());
                throw new RuntimeException(response.toString());
            }
        } catch (Exception e) {
            log.error("Error completing multipart Upload to NGFT: " + e.getMessage());
            throw e;
        }
    }

    public void abortMultiPartUpload(S3FileData fileData, InitializeMultipartUploadNGFTResponse multiPartUploadID) {
        try {
            Map<String, String> metadata = fileData.getMetadata();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-mc-sender", metadata.get("x-mc-sender"));
            headers.set("x-mc-receiver", metadata.get("x-mc-receiver"));
            headers.set("x-mc-file-name", metadata.get("x-mc-file-name"));
            headers.set("x-mc-multipart-upload-id", multiPartUploadID.getUploadId());
            headers.set("x-mc-file-id", multiPartUploadID.getFileID());

            ResponseEntity<String> response = restClient.put()
                    .uri(baseUrl + cancelEndpoint)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(String.class);
            log.info("abort upload response: {}", response.toString());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("abort response.getHeaders() {}", response.getHeaders().toString());

            } else {
                log.error("Error Abort multipart upload to NGFT API responded with error status {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error aborting multipart Upload to NGFT: " + e.getMessage());
            throw e;
        }
    }

}
