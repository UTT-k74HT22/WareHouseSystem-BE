package org.demo.whs.service.impl;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.MinioProperties;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.StorageException;
import org.demo.whs.mapper.FileMapper;
import org.demo.whs.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * StorageServiceImpl – MinIO-backed implementation of {@link StorageService}.
 *
 * <p>Allowed file extensions are enforced before any network call is made.
 * All MinIO exceptions are caught and re-thrown as {@link StorageException}
 * so the global handler can produce a consistent JSON error response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    /** Hard limit: 50 MB per file */
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    /** Allowed MIME type prefixes / exact types */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "video/mp4", "video/webm", "video/quicktime",
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/csv"
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Validates content type and file size before uploading.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.STORAGE_001, HttpStatus.BAD_REQUEST);
        }

        validateFile(file.getContentType(), file.getSize());
    }

    private void validateFile(String contentType, long size) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Rejected file upload – unsupported content type: {}", contentType);
            throw new StorageException(ErrorCode.STORAGE_005, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        if (size > MAX_FILE_SIZE_BYTES) {
            log.warn("Rejected file upload – size {} exceeds limit {}", size, MAX_FILE_SIZE_BYTES);
            throw new StorageException(ErrorCode.STORAGE_006, HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    /**
     * Builds a unique object name: {@code folder/yyyy/MM/uuid_filename}.
     *
     * <p>Example: {@code products/2026/02/a1b2c3_photo.jpg}
     */
    private String buildObjectName(String folder, String originalFilename) {
        String safeFilename = StringUtils.cleanPath(
                Objects.requireNonNullElse(originalFilename, "file"));

        // Extract extension
        int dotIndex = safeFilename.lastIndexOf('.');
        String ext = (dotIndex >= 0) ? safeFilename.substring(dotIndex) : "";
        String baseName = (dotIndex >= 0) ? safeFilename.substring(0, dotIndex) : safeFilename;

        // Sanitize base name
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9_\\-]", "_");

        java.time.LocalDate now = java.time.LocalDate.now();
        String datePath = String.format("%d/%02d", now.getYear(), now.getMonthValue());

        String prefix = (folder != null && !folder.isBlank())
                ? folder.replaceAll("[^a-zA-Z0-9/_\\-]", "_").replaceAll("/$", "")
                : "uploads";

        return String.format("%s/%s/%s_%s%s",
                prefix, datePath, UUID.randomUUID().toString().replace("-", ""), sanitized, ext);
    }



    /**
     * Upload a single file into the default bucket under the given folder prefix.
     *
     * @param file   multipart file from the HTTP request
     * @param folder logical folder / path prefix (e.g. "products", "inbound/receipts")
     * @return upload metadata including the presigned access URL
     */
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        log.info("Uploading file: {} to folder: {}", originalFilename, folder);
        validateFile(file);

        String objectName = buildObjectName(folder, originalFilename);
        try (InputStream inputStream = file.getInputStream()) {

            // Upload the file to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1) // -1 nghĩa là để MinIO tự tính toán part size
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {} (object name: {})", file.getOriginalFilename(), objectName);

            String presignedUrl = getPresignedUrl(objectName);
            Instant expiresAt = Instant.now().plusSeconds(minioProperties.getPresignedUrlExpiry());

            return FileMapper.toResponse(objectName, file, presignedUrl, expiresAt);

        } catch (Exception e) {
            log.error("Failed to upload file: {} to MinIO", originalFilename, e);
            throw new StorageException(ErrorCode.STORAGE_001);
        }
    }

    @Override
    public FileUploadResponse uploadFile(byte[] content, String originalFileName, String contentType, String folder) {
        if (content == null || content.length == 0) {
            throw new StorageException(ErrorCode.STORAGE_001, HttpStatus.BAD_REQUEST);
        }

        validateFile(contentType, content.length);
        String objectName = buildObjectName(folder, originalFileName);

        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, content.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Generated file uploaded successfully: {} (object name: {})", originalFileName, objectName);

            String presignedUrl = getPresignedUrl(objectName);
            Instant expiresAt = Instant.now().plusSeconds(minioProperties.getPresignedUrlExpiry());

            return FileUploadResponse.builder()
                    .objectName(objectName)
                    .originalFileName(originalFileName)
                    .contentType(contentType)
                    .size(content.length)
                    .presignedUrl(presignedUrl)
                    .presignedUrlExpiresAt(expiresAt)
                    .build();
        } catch (Exception e) {
            log.error("Failed to upload generated file: {} to MinIO", originalFileName, e);
            throw new StorageException(ErrorCode.STORAGE_001);
        }
    }

    /**
     * Upload multiple files at once.
     *
     * @param files  list of multipart files
     * @param folder logical folder / path prefix
     * @return list of upload metadata, one entry per file
     */
    @Override
    public List<FileUploadResponse> uploadFiles(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new StorageException(ErrorCode.STORAGE_001, HttpStatus.BAD_REQUEST);
        }

        if (files.stream().anyMatch(Objects::isNull)) {
            throw new StorageException(ErrorCode.STORAGE_001, HttpStatus.BAD_REQUEST);
        }

        return files.stream()
                .map(file -> uploadFile(file, folder))
                .collect(Collectors.toList());
    }

    /**
     * Generate a time-limited presigned GET URL for an existing object.
     *
     * @param objectName full object name as stored in the bucket
     * @return presigned URL valid for {@code app.minio.presigned-url-expiry} seconds
     */
    @Override
    public String getPresignedUrl(String objectName) {
        log.info("Generating presigned URL for object: {}", objectName);
        try {

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .expiry(minioProperties.getPresignedUrlExpiry(), TimeUnit.SECONDS)
                            .build()
            );

            log.info("Generated presigned URL for object: {} (expires in {} seconds)", objectName, minioProperties.getPresignedUrlExpiry());
            return url;
        } catch (Exception ex) {

            log.error("Failed to generate presigned URL for object: {}", objectName, ex);
            throw new StorageException(ErrorCode.STORAGE_004); // Lỗi không lấy được URL
        }
    }

    /**
     * Delete an object from the default bucket.
     *
     * @param objectName full object name as stored in the bucket
     */
    @Override
    public void deleteFile(String objectName) {
        log.info("Deleting file: {}", objectName);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("Deleted file: {}", objectName);
        } catch (Exception ex) {
            log.error("Failed to delete file: {}", objectName, ex);
            throw new StorageException(ErrorCode.STORAGE_003);
        }
    }

    /**
     * Check whether an object exists in the bucket.
     *
     * @param objectName full object name as stored in the bucket
     * @return {@code true} if the object exists
     */
    @Override
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new StorageException(ErrorCode.STORAGE_002); // Lỗi hệ thống khác
        } catch (Exception e) {
            return false;
        }
    }
}
