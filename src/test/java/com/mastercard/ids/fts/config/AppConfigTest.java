package com.mastercard.ids.fts.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppConfigTest {
    private final AppConfig config = new AppConfig();

    @Test
    void httpClient_shouldReturnNonNullClient() {
        SSLConnectionSocketFactory socketFactory = mock(SSLConnectionSocketFactory.class);
        CloseableHttpClient client = config.httpClient(socketFactory);
        assertNotNull(client);
    }

    @Test
    void restClient_shouldReturnNonNullRestClient() {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        RestClient restClient = config.restClient(httpClient);
        assertNotNull(restClient);
    }

    @Test
    void restClientLocal_shouldReturnNonNullRestClient() {
        RestClient restClient = config.restClientLocal();
        assertNotNull(restClient);
    }
}
