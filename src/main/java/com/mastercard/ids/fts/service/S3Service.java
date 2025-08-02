package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.S3FileData;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Map;

public interface S3Service {


//    void init();
    String uploadS3(int partition, byte[] body, String key, String uploadId);
    String singleFileUploadS3(byte[] body, String key, Map<String, String> metadata);
    String initiateMultipartUploadRequest(String key, Map<String, String> metadata);
    String completeMultipartUpload(String key, String uploadId, List<CompletedPart> completedParts);
    void abortMultiPartUpload(String key, String uploadId);
    S3FileData singleFileDownloadS3(String objectKey);
    S3FileData getS3ObjectMetadata(String objectKey);
    S3FileData multiPartDownloadS3(String decodedObjectKey, long startByte, long endByte);

}
