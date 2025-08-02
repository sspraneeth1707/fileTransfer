package com.mastercard.ids.fts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

//@Configuration
@Slf4j
@Profile("!local")
@Service
@DependsOn("secretsManagerClient")
public class SecretsManagerConfig {

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    public String getSecret(String secretName) {
        GetSecretValueResponse response = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId(secretName)
                .build());

        return response.secretString();
    }

}
