package com.mastercard.ids.fts.model;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class InitializeMultipartUploadNGFTResponse {
    private String fileID;
    private String uploadId;
}
