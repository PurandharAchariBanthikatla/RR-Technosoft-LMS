package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds app.aws.s3.* (bucket/region/credentials/presigned-url-expiry-minutes). */
@Component
@ConfigurationProperties(prefix = "app.aws.s3")
@Getter
@Setter
public class AwsProperties {
    private String bucket;
    private String region = "ap-south-1";
    private String accessKey;
    private String secretKey;
    private int presignedUrlExpiryMinutes = 60;
}
