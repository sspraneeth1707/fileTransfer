package com.mastercard.ids.fts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileListing {
    @JsonProperty("Receiver")
    private String receiver;
    @JsonProperty("Sender")
    private String sender;
    @JsonProperty("Files")
    private List<FileInfo> files;
}
