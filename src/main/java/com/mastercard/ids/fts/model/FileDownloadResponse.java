package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDownloadResponse {
    private String requestId;
    private String fileName;
    private String fileId;
    private String fileDescription;
    private String checksum;
    private long contentLength;
    private String contentDisposition;
    private String contentType;
    private String status;
}
