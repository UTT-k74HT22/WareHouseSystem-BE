package org.demo.whs.entity.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * FileUploadResponse – returned after a successful file upload to MinIO.
 */
@Getter
@Builder
public class FileUploadResponse {

    /** Object name (path) inside the bucket, e.g. "products/2026/image.png" */
    private String objectName;

    /** Original file name provided by the client */
    private String originalFileName;

    /** MIME type of the uploaded file */
    private String contentType;

    /** File size in bytes */
    private long size;

    /** Time-limited presigned URL for direct browser access */
    private String presignedUrl;

    /** When the presigned URL expires */
    private Instant presignedUrlExpiresAt;
}
