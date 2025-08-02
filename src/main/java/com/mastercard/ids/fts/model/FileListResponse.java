package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileListResponse {
    private String requestId;
    private String date;
    private int currentPage;
    private int currentPageSize;
    private int totalPages;
    private int totalRecords;
    private FileListing fileListing;
}
