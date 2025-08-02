package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.FileInfo;
import com.mastercard.ids.fts.model.SchedulerLog;
import com.mastercard.ids.fts.repository.SchedulerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSchedulerService {

    private final NGFTService ngftService;

    private final InboundFileListProcessor inboundFileListProcessor;

    private final SchedulerRepository schedulerRepository;

    public void process(String invocationId, LocalDateTime invocationTs) {
        log.info("Checking for new files. Invocation Id: {}", invocationId);

        List<FileInfo> fileList = null;
        try {
            // Step 1: Retrieve file list
            fileList = ngftService.retrieveFileList();

            if (fileList == null || fileList.isEmpty()) {
                log.info("No files found in NGFT. Invocation Id: {}", invocationId);
                saveSchedulerLog(invocationId, invocationTs, "Success", 0);
                return;
            }

            log.info("File list count from NGFT: {}", fileList.size());

            // Step 2: Filter out already processed file IDs from PostgreSQL
            List<FileInfo> validFileList = inboundFileListProcessor.process(invocationId, fileList);
            int processedCount = validFileList != null ? validFileList.size() : 0;

            log.info("Files processed: {}", processedCount);

            // Step 3: Update the Scheduler Job status
            saveSchedulerLog(invocationId, invocationTs, "Success", processedCount);

            log.info("Finished processing files. Invocation Id: {}", invocationId);

        } catch (Exception e) {
            int totalFileCount = (fileList != null) ? fileList.size() : 0;
            saveSchedulerLog(invocationId, invocationTs, "Failed", totalFileCount);
            log.error("Error in scheduled file processing. Invocation Id: {}. Error: {}", invocationId, e.getMessage(), e);
        }
    }

    private void saveSchedulerLog(String invocationId, LocalDateTime invocationTs, String status, int fileCount) {
        SchedulerLog schedulerLog = new SchedulerLog();
        schedulerLog.setInvocationId(invocationId);
        schedulerLog.setInvocationTime(invocationTs);
        schedulerLog.setStatus(status);
        schedulerLog.setTotalFileCount(fileCount);

        schedulerRepository.save(schedulerLog);
        log.debug("Saved scheduler log for invocation={}", invocationId);
    }
}
