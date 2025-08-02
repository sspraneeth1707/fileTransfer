package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class FileInfo {
    private String id;
    private String name;
    private String description;
    private long size;
    private String createdDate;
    private String checksum;
    private String location;
    private String status;
    private String statusTime;
    private List<FileMetadata> metadata;
    private String contentType;
    private String fileProfile;
    private List<FileAlias> fileAliases;
    private List<TransferRecord> transferRecords;
}
