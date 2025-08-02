package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileListRequest {
    private String receiver;   // x-mc-receiver (Logical address of receiver)
    private String sender;     // x-mc-sender (Logical address of sender)
    private String fileId;     // x-mc-file-id (Optional - File ID to search)
    private String startDate;  // x-mc-start-date (Optional - Start Date for range search)
    private String endDate;    // x-mc-end-date (Optional - End Date for range search)
    private String fileStatus; // x-mc-file-status (Optional - File status filter)
    private boolean ascending; // x-mc-is-ascending (Optional - Sort order)
    private int pageNumber;    // x-mc-page-number (Optional - Page number)
    private int pageSize;      // x-mc-page-size (Optional - Page size)
}
