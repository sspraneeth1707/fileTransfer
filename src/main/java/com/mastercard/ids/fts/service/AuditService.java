package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.FileInfoResponse;
import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Profile("!cloud")
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final FileRepository inboundFileRepository;

    public List<InboundFile> retrieveInboundFileList(String fileId, String invocationId, String downloadStatus, String uploadStatus, Boolean abortStatus) {
        InboundFile probe = new InboundFile();
        probe.setInvocationId(invocationId);
        probe.setFileId(fileId);
        probe.setFileDownloadStatus(downloadStatus);
        probe.setFileUploadStatus(uploadStatus);
        probe.setAbortFile(abortStatus);
        Example<InboundFile> example = Example.of(probe);
        List<InboundFile> fileList = inboundFileRepository.findBy(example, query ->
                query.sortBy(Sort.by("fileDownloadTs")).all());
        log.info("File count:{}", fileList.size());
        return fileList;
    }
}
