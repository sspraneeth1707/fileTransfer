package com.mastercard.ids.fts.utils;

import com.mastercard.ids.fts.model.FileDownloadRequest;
import com.mastercard.ids.fts.model.FileInfo;
import com.mastercard.ids.fts.model.S3FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    @Value("${spring.cloud.aws.s3.inbound_directory}")
    private String S3_INBOUND_DIRECTORY;

    public String getFileExtension(String filename) {
        List<String> compoundExtensions = List.of(".tar");

        String lower = filename.toLowerCase();

        for (String ext : compoundExtensions) {
            if(lower.contains(ext)){
                int index =lower.lastIndexOf(ext);
                return lower.substring(index+1);
            }

        }
        int dotIndex = lower.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < lower.length() - 1) {
            return lower.substring(dotIndex + 1);
        } else {
            return "";
        }
    }

    public Map<String, String> getFileMetadata(FileDownloadRequest request) {
        Map<String, String> metaData = new HashMap<>();
        FileInfo fileInfo = request.getFileInfo();
        metaData.put("x-mc-receiver", request.getReceiver());
        metaData.put("x-mc-sender", request.getSender());
        if (fileInfo != null) {
            metaData.put("x-mc-file-name", fileInfo.getName());
            metaData.put("x-mc-file-id", fileInfo.getId());
            metaData.put("x-mc-checksum", fileInfo.getChecksum());
            metaData.put("content-type", fileInfo.getContentType());
            metaData.put("x-mc-file-size", String.valueOf(fileInfo.getSize()));
            metaData.put("metaData", fileInfo.getMetadata() != null ? fileInfo.getMetadata().toString() : null);
        }
        return metaData;
    }

    public HttpHeaders getDownloadFileHeader(FileDownloadRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-mc-receiver", request.getReceiver());
        headers.set("x-mc-sender", request.getSender());
        FileInfo fileInfo = request.getFileInfo();
        headers.set("x-mc-file-id", fileInfo.getId());
        headers.set("content-type", fileInfo.getContentType());
        headers.set("x-mc-fileName", fileInfo.getName());
        return headers;
    }

    public HttpHeaders getUploadFileHeader(S3FileData fileData) {
        Map<String, String> metadata = fileData.getMetadata();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-mc-sender", metadata.get("x-mc-sender"));
        headers.set("x-mc-receiver", metadata.get("x-mc-receiver"));
        headers.set("x-mc-file-name", metadata.get("x-mc-file-name"));
        headers.set("x-mc-file-size", metadata.get("x-mc-file-size"));
        headers.set("x-mc-file-content-type", metadata.get("content-type"));
        headers.set("x-mc-checksum", DigestUtils.md5DigestAsHex(fileData.getFileContent().get()));
//        headers.set("x-mc-checksum", "332d0acc96e83293068621362446d891");

        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return headers;
    }

    public String getS3FileNamekey(FileDownloadRequest request) {
        FileInfo fileInfo = request.getFileInfo();
        return String.format("%s/%s_%s.%s", S3_INBOUND_DIRECTORY, fileInfo.getId(), LocalDateTime.now(), getFileExtension(fileInfo.getName()));
    }

    public Boolean verifyChecksum(byte[] content, String checksum) {
        String calculatedChecksum = DigestUtils.md5DigestAsHex(content);
        return checksum.equals(calculatedChecksum);
    }

}
