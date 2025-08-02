package com.mastercard.ids.fts.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@Configuration
@EnableRetry
public class AppConfig {

    @Bean("ftsHttpClient")
    @Profile("!local")
    public CloseableHttpClient httpClient(@Qualifier("ftsSslConnectionSocketFactory") SSLConnectionSocketFactory socketFactory) {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    @Bean
    @Profile("!local")
    public RestClient restClient(@Qualifier("ftsHttpClient") CloseableHttpClient httpClient) {
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    @Profile("local")
    public RestClient restClientLocal() {
        return RestClient.builder()
                .build();
    }
}
