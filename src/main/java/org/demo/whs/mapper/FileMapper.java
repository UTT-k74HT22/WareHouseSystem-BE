package org.demo.whs.mapper;

import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * FileMapper – placeholder for mapping file-related data to DTOs and vice versa.
 * Currently empty, but can be expanded in the future if needed.
 */
@Component
public class FileMapper {

    /**
     * Convert file upload data to a FileUploadResponse DTO.
     *
     * @param objectName   the full object name as stored in the bucket
     * @param file         the original multipart file (can be null for metadata-only responses)
     * @param presignedUrl the generated presigned URL for accessing the file
     * @param expiresAt    the expiration time of the presigned URL
     * @return a FileUploadResponse DTO containing all relevant metadata and access information
     */
    public static FileUploadResponse toResponse(String objectName, MultipartFile file, String presignedUrl, Instant expiresAt) {

        if (file == null) {
            return FileUploadResponse.builder()
                    .objectName(objectName)
                    .originalFileName(null)
                    .contentType(null)
                    .size(0L)
                    .presignedUrl(presignedUrl)
                    .presignedUrlExpiresAt(expiresAt)
                    .build();
        }

        return FileUploadResponse.builder()
                .objectName(objectName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .presignedUrl(presignedUrl)
                .presignedUrlExpiresAt(expiresAt)
                .build();
    }
}
