package com.mastercard.ids.fts.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import static org.junit.jupiter.api.Assertions.*;

class SESConfigTest {
    private SESConfig sesConfig;

    @BeforeEach
    void setUp() {
        sesConfig = new SESConfig();
    }

    @Test
    void sesClient_shouldReturnNonNullClientWithCorrectRegion() {
        ReflectionTestUtils.setField(sesConfig, "region", "us-east-1");
        SesClient client = sesConfig.sesClient();
        assertNotNull(client);
        assertEquals(Region.of("us-east-1"), client.serviceClientConfiguration().region());
    }

    @Test
    void sesClient_shouldThrowExceptionIfRegionIsNull() {
        ReflectionTestUtils.setField(sesConfig, "region", null);
        Exception ex = assertThrows(Exception.class, () -> sesConfig.sesClient());
        assertTrue(ex.getMessage().contains("region") || ex instanceof NullPointerException);
    }
}

