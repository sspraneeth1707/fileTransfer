package com.mastercard.ids.fts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "INBOUND_FILE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundFile {

    @Column(name = "invocation_id", nullable = false, length = 36)
    private String invocationId;

    @Id
    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "sender", nullable = false, length = 50)
    private String sender;

    @Column(name = "file_profile", nullable = true, length = 30)
    private String fileProfile;

    @Column(name = "file_name", nullable = false, length = 50)
    private String fileName;

    @Column(name = "file_size", nullable = false, precision = 12)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "file_checksum", nullable = false, length = 32)
    private String fileChecksum;

    @Column(name = "file_metadata")
//    @Column(name = "file_metadata", columnDefinition = "json")
//    @Convert(converter = JsonConverter.class)
    private String fileMetadata;

    @Column(name = "file_created_date", nullable = false)
    private LocalDateTime fileCreatedDate;

    @Column(name = "file_download_ts", nullable = false)
    private LocalDateTime fileDownloadTs;

    @Column(name = "file_download_status", nullable = false, length = 20)
    private String fileDownloadStatus;

    @Column(name = "file_upload_ts", nullable = false)
    private LocalDateTime fileUploadTs;

    @Column(name = "file_upload_status", nullable = false, length = 20)
    private String fileUploadStatus;

    @Column(name = "notification_receivers", nullable = false, columnDefinition = "text")
    private String notificationReceivers;

    @Column(name = "abort_file", nullable = false)
    private Boolean abortFile;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

}
