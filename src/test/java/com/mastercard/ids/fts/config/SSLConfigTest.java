package com.mastercard.ids.fts.config;

import com.mastercard.ids.fts.service.SecretsManagerConfig;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SSLConfigTest {

    private SSLConfig sslConfig;
    private SecretsManagerConfig secretsManagerConfig;

    private static final String TEST_SECRET_NAME = "test-secret";
    private static final String CERT_KEY = "certKey";
    private static final String PASS_KEY = "passKey";

    private KeyStore testKeyStore;
    private String base64EncodedCert;

    @BeforeEach
    void setUp() throws Exception {
        // Prepare dummy keystore
        testKeyStore = KeyStore.getInstance("PKCS12");
        testKeyStore.load(null, null);
        byte[] certBytes;
        try (var out = new java.io.ByteArrayOutputStream()) {
            testKeyStore.store(out, "testpass".toCharArray());
            certBytes = out.toByteArray();
        }
        base64EncodedCert = Base64.getEncoder().encodeToString(certBytes);

        secretsManagerConfig = mock(SecretsManagerConfig.class);
        sslConfig = new SSLConfig();

        // Inject mock and values
        ReflectionTestUtils.setField(sslConfig, "fts_ssl_bundle_secretName", TEST_SECRET_NAME);
        ReflectionTestUtils.setField(sslConfig, "fts_ssl_cert_key", CERT_KEY);
        ReflectionTestUtils.setField(sslConfig, "fts_ssl_cert_pass_key", PASS_KEY);
        ReflectionTestUtils.setField(sslConfig, "secretsManagerService", secretsManagerConfig);

        // Mock secret retrieval
        Map<String, String> mockSecrets = Map.of(
                CERT_KEY, base64EncodedCert,
                PASS_KEY, "testpass"
        );
        String jsonSecret = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mockSecrets);
        when(secretsManagerConfig.getSecret(TEST_SECRET_NAME)).thenReturn(jsonSecret);
    }

    @Test
    void testSslContextCreation_success() throws Exception {
        SSLContext sslContext = sslConfig.sslContext();
        assertNotNull(sslContext);
        assertTrue(sslContext.getProvider().getName().length() > 0);
    }

    @Test
    void testSslConnectionSocketFactoryCreation_success() throws Exception {
        SSLContext sslContext = sslConfig.sslContext();
        SSLConnectionSocketFactory factory = sslConfig.sslConnectionSocketFactory(sslContext);
        assertNotNull(factory);
    }
}
