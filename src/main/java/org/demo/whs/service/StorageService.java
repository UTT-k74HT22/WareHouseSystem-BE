package org.demo.whs.service;

import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * StorageService – contract for all file-storage operations backed by MinIO.
 */
public interface StorageService {

    /**
     * Upload a single file into the default bucket under the given folder prefix.
     *
     * @param file   multipart file from the HTTP request
     * @param folder logical folder / path prefix (e.g. "products", "inbound/receipts")
     * @return upload metadata including the presigned access URL
     */
    FileUploadResponse uploadFile(MultipartFile file, String folder);

    /**
     * Upload generated content without going through MultipartFile.
     *
     * @param content      file content bytes
     * @param originalFileName logical original file name
     * @param contentType  MIME type
     * @param folder       logical folder / path prefix
     * @return upload metadata including the presigned access URL
     */
    FileUploadResponse uploadFile(byte[] content, String originalFileName, String contentType, String folder);

    /**
     * Upload multiple files at once.
     *
     * @param files  list of multipart files
     * @param folder logical folder / path prefix
     * @return list of upload metadata, one entry per file
     */
    List<FileUploadResponse> uploadFiles(List<MultipartFile> files, String folder);

    /**
     * Generate a time-limited presigned GET URL for an existing object.
     *
     * @param objectName full object name as stored in the bucket
     * @return presigned URL valid for {@code app.minio.presigned-url-expiry} seconds
     */
    String getPresignedUrl(String objectName);

    /**
     * Delete an object from the default bucket.
     *
     * @param objectName full object name as stored in the bucket
     */
    void deleteFile(String objectName);

    /**
     * Check whether an object exists in the bucket.
     *
     * @param objectName full object name as stored in the bucket
     * @return {@code true} if the object exists
     */
    boolean fileExists(String objectName);
}
