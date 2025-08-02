package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.S3FileData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Map;

@Profile("local")
@Service
@Slf4j
public class S3ServiceLocalImpl implements S3Service {

    @Override
    public String uploadS3(int partition, byte[] body, String key, String uploadId) {
        log.debug("Upload chunk to S3: partition:{}, key:{}, uploadId:{}", partition, key, uploadId);
        return "Not supported in local";
    }

    @Override
    public String singleFileUploadS3(byte[] body, String key, Map<String, String> metadata) {
        log.debug("Upload to S3. singleFileUploadS3: key:{}, metadata:{}", key, metadata);
        return "Not supported in local";
    }

    @Override
    public String initiateMultipartUploadRequest(String key, Map<String, String> metadata) {
        log.debug("Upload to S3. initiateMultipartUploadRequest: key:{}, metadata:{}", key, metadata);
        return "Not supported in local";
    }

    @Override
    public String completeMultipartUpload(String key, String uploadId, List<CompletedPart> completedParts) {
        log.debug("Upload to S3. completeMultipartUpload: key:{}, uploadId:{}", key, uploadId);
        return "Not supported in local";
    }

    @Override
    public void abortMultiPartUpload(String key, String uploadId) {
        log.debug("completeMultipartUpload: key:{}, uploadId:{}", key, uploadId);
    }

    @Override
    public S3FileData singleFileDownloadS3(String objectKey) {
        log.debug("Single File Download from S3. Not Supported in Local . ObjectKeu : {}", objectKey);
        return null;
    }

    @Override
    public S3FileData getS3ObjectMetadata(String objectKey) {
        log.debug("Get S3 Object Metadata. Not Supported in Local . ObjectKeu : {}", objectKey);
        return null;
    }

    @Override
    public S3FileData multiPartDownloadS3(String decodedObjectKey, long startByte, long endByte) {
        log.debug("Multipart File Download from S3. Not Supported in Local . decodedObjectKey : {} , startByte : {} , endByte : {}", decodedObjectKey, startByte, endByte);
        return null;
    }
}

