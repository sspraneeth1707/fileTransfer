package com.mastercard.ids.fts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FileTransferServiceFactory {
    private final FileTransferService smallFileTransferService;

    private final FileTransferService largeFileTransferService;

    @Value("${file.minimum.size}")
    private long minimumFileSize;

    public FileTransferService getService(long fileSizeInBytes) {
        if (fileSizeInBytes <= minimumFileSize) {
            return smallFileTransferService;
        } else {
            return largeFileTransferService;
        }
    }
}
