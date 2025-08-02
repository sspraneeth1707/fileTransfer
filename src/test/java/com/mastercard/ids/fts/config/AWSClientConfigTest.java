package com.mastercard.ids.fts.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AWSClientConfigTest {
    private AWSClientConfig config;
    private AWSProperties awsProperties;

    @BeforeEach
    void setUp() {
        config = new AWSClientConfig();
        awsProperties = mock(AWSProperties.class);
    }

    @Test
    void s3Client_withEndpoint_shouldUseStaticCredentialsAndEndpoint() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn("http://localhost:9000");
        when(awsProperties.getAccessKey()).thenReturn("access");
        when(awsProperties.getSecretKey()).thenReturn("secret");
        S3Client client = config.s3Client(awsProperties);
        assertNotNull(client);
    }

    @Test
    void s3Client_withoutEndpoint_shouldUseDefaultCredentials() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn(null);
        S3Client client = config.s3Client(awsProperties);
        assertNotNull(client);
    }

    @Test
    void acmClient_withValidProps_shouldReturnClient() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn(null);
        AcmClient client = config.acmClient(awsProperties);
        assertNotNull(client);
    }

    @Test
    void secretsManagerClient_withValidProps_shouldReturnClient() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn(null);
        SecretsManagerClient client = config.secretsManagerClient(awsProperties);
        assertNotNull(client);
    }

    @Test
    void sqsClient_withValidProps_shouldReturnClient() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn(null);
        SqsClient client = config.sqsClient(awsProperties);
        assertNotNull(client);
    }

    @Test
    void s3Client_withNullRegion_shouldThrowException() {
        when(awsProperties.getRegion()).thenReturn(null);
        assertThrows(Exception.class, () -> config.s3Client(awsProperties));
    }

    @Test
    void s3Client_withInvalidEndpoint_shouldThrowException() {
        when(awsProperties.getRegion()).thenReturn("us-east-1");
        when(awsProperties.getEndpoint()).thenReturn("not-a-uri");
        when(awsProperties.getAccessKey()).thenReturn("access");
        when(awsProperties.getSecretKey()).thenReturn("secret");
        assertThrows(Exception.class, () -> config.s3Client(awsProperties));
    }
}

