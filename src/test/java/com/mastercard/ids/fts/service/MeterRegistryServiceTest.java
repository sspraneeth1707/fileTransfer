package com.mastercard.ids.fts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MeterRegistryServiceTest {

    private MeterRegistryService meterRegistryService;

    @BeforeEach
    void setUp() {
        meterRegistryService = new MeterRegistryService();
    }

    @Test
    void testRecordOutboundFileProcessed_doesNotThrow() {
        assertDoesNotThrow(() -> meterRegistryService.recordOutboundFileProcessed());
    }
}
