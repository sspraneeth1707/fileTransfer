package com.mastercard.ids.fts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mastercard.ids.fts.service.ACMService;
import com.mastercard.ids.fts.service.SecretsManagerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Configuration
@DependsOn({"secretsManagerConfig"})
@Profile("!local")
public class SSLConfig {

    @Value("${spring.cloud.aws.secretsmanager.fts-ssl-bundle-secretName}")
    private String fts_ssl_bundle_secretName;
    @Value("${spring.cloud.aws.secretsmanager.fts-ssl-cert-key}")
    private String fts_ssl_cert_key;
    @Value("${spring.cloud.aws.secretsmanager.fts-ssl-cert-pass-key}")
    private String fts_ssl_cert_pass_key;

    @Autowired
    private SecretsManagerConfig secretsManagerService;

    @Bean("ftsSslContext")
    public SSLContext sslContext() throws Exception {

        String fts_ssl_bundle = secretsManagerService.getSecret(fts_ssl_bundle_secretName);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,String> secrets = objectMapper.readValue(fts_ssl_bundle, Map.class);

        String certFile = secrets.get(fts_ssl_cert_key);
        String keyStorePassword = secrets.get(fts_ssl_cert_pass_key);

        log.debug("Certificate file: {}", certFile);
        byte[] certFile_bytes = Base64.getDecoder().decode(certFile);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(certFile_bytes), keyStorePassword.toCharArray());

        SSLContext sslContext = new SSLContextBuilder()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .build();

        Provider provider = sslContext.getProvider();
        log.info("Provider: {}", provider);
        log.info("Provider Info: {}", provider.getInfo());
        log.info("Supported Protocols: {}", Arrays.toString(sslContext.getSupportedSSLParameters().getProtocols()));
        log.info("Supported CipherSuites: {}", Arrays.toString(sslContext.getSupportedSSLParameters().getCipherSuites()));

        return sslContext;
    }

    @Bean("ftsSslConnectionSocketFactory")
    public SSLConnectionSocketFactory sslConnectionSocketFactory(@Qualifier("ftsSslContext") SSLContext sslContext) throws Exception {
        return new SSLConnectionSocketFactory(sslContext);
    }
}
