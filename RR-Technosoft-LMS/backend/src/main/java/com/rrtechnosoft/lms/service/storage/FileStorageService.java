package com.rrtechnosoft.lms.service.storage;

import com.rrtechnosoft.lms.config.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

/**
 * Thin wrapper around the S3 client scaffolded in application.yml
 * (app.aws.s3.*) but never previously implemented — bucket/region/keys were
 * configured with no code reading them until this module needed real file
 * storage for generated certificate PDFs.
 *
 * Uploads are private (no public-read ACL); downloads go through a
 * time-limited presigned URL rather than a permanent public link, so a
 * leaked/shared certificate link expires instead of staying valid forever.
 */
@Service("certificateFileStorageService")
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Properties s3Properties;

    private S3Client client() {
        return S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private S3Presigner presigner() {
        return S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        String accessKey = s3Properties.getAccessKey();
        String secretKey = s3Properties.getSecretKey();
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        // Falls back to env vars / instance profile / IAM role — the normal
        // path in a real deployment where keys aren't hardcoded into config.
        return DefaultCredentialsProvider.create();
    }

    /** Uploads bytes under the given key and returns a presigned, time-limited download URL. */
    public String upload(String key, byte[] content, String contentType) {
        try (S3Client s3 = client()) {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content)
            );
        }
        return presignedUrl(key);
    }

    public String presignedUrl(String key) {
        try (S3Presigner presigner = presigner()) {
            var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(s3Properties.getPresignedUrlExpiryMinutes()))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(key)
                            .build())
                    .build());
            return presigned.url().toString();
        }
    }
}
