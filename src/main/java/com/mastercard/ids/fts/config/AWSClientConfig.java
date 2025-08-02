package com.mastercard.ids.fts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.AcmClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

import java.net.URI;

@Configuration
public class AWSClientConfig {

    @Profile("!local")
    @Bean
    public S3Client s3Client(AWSProperties awsProperties) {
        S3ClientBuilder builder = S3Client.builder();
        configureBuilder(builder, awsProperties);
        if (awsProperties.getEndpoint() != null && !awsProperties.getEndpoint().isBlank()) {
            builder.forcePathStyle(true);
        }
        return builder.build();
    }

    @Profile("!local")
    @Bean
    public AcmClient acmClient(AWSProperties awsProperties) {
        AcmClientBuilder builder = AcmClient.builder();
        configureBuilder(builder, awsProperties);
        return builder.build();
    }

    @Profile("!local")
    @Bean
    public SecretsManagerClient secretsManagerClient(AWSProperties awsProperties) {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder();
        configureBuilder(builder, awsProperties);
        return builder.build();
    }

    @Profile("!local")
    @Bean
    public SqsClient sqsClient(AWSProperties awsProperties) {
        SqsClientBuilder builder = SqsClient.builder();

        configureBuilder(builder, awsProperties);
        return builder.build();
    }

    private <T extends AwsClientBuilder<T, U>, U> void configureBuilder(T builder, AWSProperties awsProperties) {
        builder.region(Region.of(awsProperties.getRegion()));
        if (awsProperties.getEndpoint() != null && !awsProperties.getEndpoint().isBlank()) {
            builder.overrideConfiguration(ClientOverrideConfiguration.builder().build())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())))
                    .endpointOverride(URI.create(awsProperties.getEndpoint()));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
    }
}
