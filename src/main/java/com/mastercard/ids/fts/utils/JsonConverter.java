package com.mastercard.ids.fts.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert map to JSON string.", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbDataJson) {
        try {
            dbDataJson = dbDataJson.equals("\"{}\"") ? "{}" : dbDataJson;
            return objectMapper.readValue(dbDataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
//            throw new IllegalArgumentException("Could not convert JSON string to map.", e);
            try {
                // Fallback: maybe dbData is a JSON string that contains a JSON string
                String unwrapped = objectMapper.readValue(dbDataJson, String.class);
                return objectMapper.readValue(unwrapped, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing JSON (even after unwrap)", ex);
            }
        }
    }
}
