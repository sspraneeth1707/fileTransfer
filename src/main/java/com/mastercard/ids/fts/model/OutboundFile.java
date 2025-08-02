package com.mastercard.ids.fts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "OUTBOUND_FILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundFile {

    @Id
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "object_key", nullable = false, length = 100)
    private String objectKey;

    @Column(name = "receiver", nullable = false, length = 50)
    private String receiver;

    @Column(name = "file_profile", length = 30)
    private String fileProfile;

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "file_name", nullable = false, length = 50)
    private String fileName;

    @Column(name = "file_size", nullable = false, precision = 12)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "file_checksum", nullable = false, length = 32)
    private String fileChecksum;

    @Column(name = "file_metadata")
    private String fileMetadata;

    @Column(name = "file_download_ts")
    private LocalDateTime fileDownloadTs;

    @Column(name = "file_download_status", length = 20)
    private String fileDownloadStatus;

    @Column(name = "file_upload_ts")
    private LocalDateTime fileUploadTs;

    @Column(name = "file_upload_status", length = 20)
    private String fileUploadStatus;

    @Column(name = "notification_receivers", columnDefinition = "text")
    private String notificationReceivers;

    @Column(name = "abort_file")
    private Boolean abortFile;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

}
