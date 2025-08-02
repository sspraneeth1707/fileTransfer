package com.mastercard.ids.fts.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonConverterTest {

    private final JsonConverter converter = new JsonConverter();

    @Test
    void testConvertToDatabaseColumn_success() {
        Map<String, Object> map = Map.of("key", "value", "count", 5);

        String json = converter.convertToDatabaseColumn(map);

        assertTrue(json.contains("\"key\":\"value\""));
        assertTrue(json.contains("\"count\":5"));
    }

    @Test
    void testConvertToDatabaseColumn_nullMap() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute_validJson() {
        String json = "{\"key\":\"value\",\"count\":10}";

        Map<String, Object> result = converter.convertToEntityAttribute(json);

        assertEquals("value", result.get("key"));
        assertEquals(10, result.get("count"));
    }

    @Test
    void testConvertToEntityAttribute_emptyJsonObject() {
        String json = "\"{}\""; // escaped empty map

        Map<String, Object> result = converter.convertToEntityAttribute(json);

        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToEntityAttribute_fallbackDoubleEncoded() {
        // JSON string that contains a JSON string
        String dbValue = "\"{\\\"key\\\":\\\"val\\\"}\"";

        Map<String, Object> result = converter.convertToEntityAttribute(dbValue);

        assertEquals("val", result.get("key"));
    }

    @Test
    void testConvertToEntityAttribute_invalidJson_shouldThrowException() {
        String invalidJson = "{invalid-json}";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute(invalidJson));

        assertTrue(exception.getMessage().contains("Error parsing JSON"));
    }
}
