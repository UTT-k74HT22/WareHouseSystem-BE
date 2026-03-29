package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.service.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * StorageController – REST API for file upload/management via MinIO.
 *
 * <pre>
 * POST   /api/v1/storage/upload          – upload single file
 * POST   /api/v1/storage/upload/batch    – upload multiple files
 * GET    /api/v1/storage/presigned-url   – get presigned GET URL
 * DELETE /api/v1/storage/{objectName}    – delete a stored file
 * GET    /api/v1/storage/exists          – check if file exists
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Validated
@Tag(name = "Storage", description = "File upload and management via MinIO")
public class StorageController {

    private final StorageService storageService;

    // =========================================================================
    // Upload single file
    // =========================================================================

    /**
     * Upload a single file.
     *
     * @param file   the multipart file
     * @param folder logical folder prefix (e.g. "products", "inbound"). Defaults to "uploads".
     * @return upload metadata including presigned URL
     */
    @Operation(summary = "Upload a single file to MinIO storage")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_STORAGE_CREATE')")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadFile(
            @Parameter(description = "File to upload (image, video, PDF, …)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Logical folder prefix, e.g. 'products' or 'inbound/receipts'")
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {

        String originalFilename = file == null ? null : file.getOriginalFilename();
        log.info("POST /api/v1/storage/upload – file='{}', folder='{}'",
                originalFilename, folder);

        FileUploadResponse response = storageService.uploadFile(file, folder);
        return ResponseEntity.ok(BaseResponse.success(response, "File uploaded successfully"));
    }

    // =========================================================================
    // Upload multiple files
    // =========================================================================

    /**
     * Upload multiple files at once (max 10 per request).
     */
    @Operation(summary = "Upload multiple files in a single request (max 10)")
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_STORAGE_CREATE')")
    public ResponseEntity<BaseResponse<List<FileUploadResponse>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {

        int fileCount = files == null ? 0 : files.size();
        log.info("POST /api/v1/storage/upload/batch – count={}, folder='{}'",
                fileCount, folder);

        if (fileCount > 10) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("COM_003", "Maximum 10 files per batch upload", null));
        }

        List<FileUploadResponse> responses = storageService.uploadFiles(files, folder);
        return ResponseEntity.ok(
                BaseResponse.success(responses, fileCount + " file(s) uploaded successfully"));
    }

    // =========================================================================
    // Presigned URL
    // =========================================================================

    /**
     * Generate a presigned GET URL for an already-stored object.
     *
     * @param objectName full object path stored in MinIO (as returned by upload)
     */
    @Operation(summary = "Generate a presigned URL to access a stored file")
    @GetMapping("/presigned-url")
    @PreAuthorize("hasAuthority('PERM_STORAGE_READ')")
    public ResponseEntity<BaseResponse<Map<String, String>>> getPresignedUrl(
            @NotBlank(message = "objectName must not be blank")
            @RequestParam("objectName") String objectName) {

        log.info("GET /api/v1/storage/presigned-url – objectName='{}'", objectName);

        String url = storageService.getPresignedUrl(objectName);
        return ResponseEntity.ok(
                BaseResponse.success(Map.of("presignedUrl", url), "Presigned URL generated"));
    }

    // =========================================================================
    // Delete file
    // =========================================================================

    /**
     * Delete a stored object by query param.
     * Recommended for object names that contain nested path segments.
     *
     * Example:
     * DELETE /api/v1/storage?objectName=products/2026/03/file.png
     *
     * @param objectName full object path
     */
    @Operation(summary = "Delete a file from MinIO storage")
    @DeleteMapping(params = "objectName")
    @PreAuthorize("hasAuthority('PERM_STORAGE_DELETE')")
    public ResponseEntity<BaseResponse<Void>> deleteFile(
            @NotBlank(message = "objectName must not be blank")
            @RequestParam("objectName") String objectName) {

        log.info("DELETE /api/v1/storage?objectName={}", objectName);

        storageService.deleteFile(objectName);
        return ResponseEntity.ok(BaseResponse.success(null, "File deleted successfully"));
    }

    /**
     * Backward-compatible delete endpoint using path segment wildcard.
     *
     * Example:
     * DELETE /api/v1/storage/products/2026/03/file.png
     *
     * @param objectName full object path captured from wildcard path variable
     */
    @DeleteMapping("/{*objectName}")
    @PreAuthorize("hasAuthority('PERM_STORAGE_DELETE')")
    public ResponseEntity<BaseResponse<Void>> deleteFileLegacyPath(
            @PathVariable String objectName) {

        String normalizedObjectName = objectName.startsWith("/")
                ? objectName.substring(1)
                : objectName;

        log.info("DELETE /api/v1/storage/{}", normalizedObjectName);

        storageService.deleteFile(normalizedObjectName);
        return ResponseEntity.ok(BaseResponse.success(null, "File deleted successfully"));
    }

    // =========================================================================
    // Exists check
    // =========================================================================

    /**
     * Check whether an object exists in the bucket.
     */
    @Operation(summary = "Check if a file exists in MinIO storage")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('PERM_STORAGE_READ')")
    public ResponseEntity<BaseResponse<Map<String, Boolean>>> fileExists(
            @NotBlank(message = "objectName must not be blank")
            @RequestParam("objectName") String objectName) {

        log.info("GET /api/v1/storage/exists – objectName='{}'", objectName);

        boolean exists = storageService.fileExists(objectName);
        return ResponseEntity.ok(
                BaseResponse.success(Map.of("exists", exists)));
    }
}
