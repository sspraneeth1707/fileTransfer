package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3UploadResponse {
    private String fileName;
    private String s3Url;
    private String status;

    public S3UploadResponse(String fileId, String s3Url, String success) {
    }
}
