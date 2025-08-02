package com.mastercard.ids.fts.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"!local"})
@Data
@Configuration
public class AWSProperties {
    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;
    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;
    @Value("${spring.cloud.aws.region.static}")
    private String region;
    @Value("${spring.cloud.aws.endpoint:}")
    private String endpoint;
    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bucketName;
}

