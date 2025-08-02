package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.FileDownloadRequest;
import com.mastercard.ids.fts.model.OutboundFile;

import java.util.concurrent.CompletableFuture;

public interface FileTransferService {
    CompletableFuture<String> transferInbound(FileDownloadRequest request);

    //    S3UploadResponse fallback(FileDownloadRequest request, Throwable t);
    CompletableFuture<String> transferOutbound(OutboundFile outboundFile);

}
