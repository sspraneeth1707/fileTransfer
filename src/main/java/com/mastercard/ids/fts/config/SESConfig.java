package com.mastercard.ids.fts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Profile("!local")
@Configuration
public class SESConfig {
    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("your-access-key", "your-secret-key")))
                .build();
    }
}

