package com.mastercard.ids.fts.scheduler;

import com.mastercard.ids.fts.service.FileSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScheduler {

    private final FileSchedulerService fileSchedulerService;

    /**
     * Scheduler that runs every 1 hour to check for new files, retrieve file info, and download/upload in parallel.
     */
//    @Scheduled(fixedRate = 3600000) // Runs every 1 hr
    @Scheduled(fixedRate = 8, timeUnit = TimeUnit.HOURS) // Runs every 4 hr
    public void runFileDownloadJob() {
        String invocationId = UUID.randomUUID().toString();
        LocalDateTime invocationTs = LocalDateTime.now();
        fileSchedulerService.process(invocationId, invocationTs);
    }
}


