package com.mastercard.ids.fts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private String code;
    private String message;
    private String type;
    private String incidenceId;
}