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
@Table(name = "SCHEDULER_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerLog {
    @Id
    @Column(name = "INVOCATION_ID", nullable = false, length = 36)
    private String invocationId;

    @Column(name = "INVOCATION_TIME", nullable = false)
    private LocalDateTime invocationTime;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "TOTAL_FILE_COUNT", nullable = false)
    private Integer totalFileCount = 0;
}
