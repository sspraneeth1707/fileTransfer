package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadResponse {
        private String token;
        private String requestId;
        private String expiry;

}
