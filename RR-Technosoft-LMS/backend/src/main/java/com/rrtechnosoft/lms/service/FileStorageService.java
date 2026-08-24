package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.config.AwsProperties;
import com.rrtechnosoft.lms.dto.response.FileUploadResponse;
import com.rrtechnosoft.lms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Thin wrapper around S3 used by every module that accepts file uploads
 * (Learning Resources, Video Library, resumes). Objects are stored under a
 * `folder/` prefix per feature so a bucket listing stays navigable; the
 * object key is what's persisted in *_key columns so a fresh presigned URL
 * can be minted on read without re-uploading.
 */
@Service("generalFileStorageService")
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public FileUploadResponse upload(MultipartFile file, String folder, java.util.Set<String> allowedContentTypePrefixes) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File is required");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (allowedContentTypePrefixes != null && !allowedContentTypePrefixes.isEmpty()
                && allowedContentTypePrefixes.stream().noneMatch(contentType::startsWith)) {
            throw ApiException.badRequest("Unsupported file type: " + contentType);
        }

        String extension = extractExtension(file.getOriginalFilename());
        String key = "%s/%s%s".formatted(folder, UUID.randomUUID(), extension);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(awsProperties.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            log.error("Failed to read uploaded file for key {}", key, e);
            throw ApiException.badRequest("Could not read uploaded file");
        } catch (S3Exception e) {
            log.error("S3 upload failed for key {}", key, e);
            throw new ApiException("File upload failed, please try again", org.springframework.http.HttpStatus.BAD_GATEWAY);
        }

        String publicUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(
                awsProperties.getBucket(), awsProperties.getRegion(), key);
        return new FileUploadResponse(publicUrl, key, file.getSize());
    }

    /** Presigned GET URL, used when a resource/video is private and access should be time-limited. */
    public String presignedDownloadUrl(String key) {
        if (key == null) return null;
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(awsProperties.getPresignedUrlExpiryMinutes()))
                .getObjectRequest(GetObjectRequest.builder().bucket(awsProperties.getBucket()).key(key).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(awsProperties.getBucket()).key(key).build());
        } catch (S3Exception e) {
            // Not fatal — the DB row is still removed; log for a manual bucket cleanup pass.
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
