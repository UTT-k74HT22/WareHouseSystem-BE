package org.demo.whs.service;

import io.minio.*;
import org.demo.whs.configuration.MinioProperties;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.exception.StorageException;
import org.demo.whs.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorageServiceImpl Unit Tests")
class StorageServiceImplTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @InjectMocks
    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        when(minioProperties.getBucketName()).thenReturn("whs-storage");
        when(minioProperties.getPresignedUrlExpiry()).thenReturn(3600);
    }

    // =========================================================================
    // uploadFile – happy path
    // =========================================================================

    @Test
    @DisplayName("uploadFile – happy path – image/jpeg succeeds and returns metadata")
    void uploadFile_happyPath_returnsFileUploadResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/whs-storage/products/2026/02/abc_photo.jpg?X-Amz-Signature=xxx");

        FileUploadResponse response = storageService.uploadFile(file, "products");

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("photo.jpg");
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getSize()).isEqualTo(file.getSize());
        assertThat(response.getObjectName()).contains("products/");
        assertThat(response.getPresignedUrl()).isNotBlank();
        assertThat(response.getPresignedUrlExpiresAt()).isNotNull();

        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("uploadFile – PDF file type – succeeds")
    void uploadFile_pdf_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "pdf-content".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/whs-storage/docs/report.pdf?sig=xxx");

        FileUploadResponse response = storageService.uploadFile(file, "docs");

        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getObjectName()).contains("docs/");
    }

    // =========================================================================
    // uploadFile – validation failures
    // =========================================================================

    @Test
    @DisplayName("uploadFile – unsupported content type – throws StorageException STORAGE_005")
    void uploadFile_unsupportedContentType_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "binary".getBytes());

        assertThatThrownBy(() -> storageService.uploadFile(file, "uploads"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    @DisplayName("uploadFile – empty file – throws StorageException STORAGE_001")
    void uploadFile_emptyFile_throwsStorageException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadFile(emptyFile, "uploads"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("uploadFile – null file – throws StorageException")
    void uploadFile_nullFile_throwsStorageException() {
        assertThatThrownBy(() -> storageService.uploadFile(null, "uploads"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("uploadFile – file exceeds 50 MB – throws StorageException STORAGE_006")
    void uploadFile_fileTooLarge_throwsStorageException() {
        byte[] oversizedContent = new byte[(int) (50L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile(
                "file", "bigfile.jpg", "image/jpeg", oversizedContent);

        assertThatThrownBy(() -> storageService.uploadFile(file, "uploads"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File size exceeds maximum allowed limit");
    }

    // =========================================================================
    // uploadFile – MinIO failure
    // =========================================================================

    @Test
    @DisplayName("uploadFile – MinIO putObject throws – propagates as StorageException STORAGE_001")
    void uploadFile_minioThrows_throwsStorageException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> storageService.uploadFile(file, "products"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File upload failed");
    }

    // =========================================================================
    // uploadFiles – batch
    // =========================================================================

    @Test
    @DisplayName("uploadFiles – multiple files – returns list with same size")
    void uploadFiles_multipleFiles_returnsListOfResponses() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "img1.jpg", "image/jpeg", "bytes1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "img2.png", "image/png", "bytes2".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/presigned-url");

        List<FileUploadResponse> responses = storageService.uploadFiles(List.of(file1, file2), "gallery");

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("uploadFiles – empty list – returns empty list")
    void uploadFiles_emptyList_returnsEmptyList() {
        List<FileUploadResponse> responses = storageService.uploadFiles(List.of(), "gallery");
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("uploadFiles – null list – returns empty list")
    void uploadFiles_nullList_returnsEmptyList() {
        List<FileUploadResponse> responses = storageService.uploadFiles(null, "gallery");
        assertThat(responses).isEmpty();
    }

    // =========================================================================
    // getPresignedUrl
    // =========================================================================

    @Test
    @DisplayName("getPresignedUrl – happy path – returns URL")
    void getPresignedUrl_happyPath_returnsUrl() throws Exception {
        String expectedUrl = "http://localhost:9000/whs-storage/products/img.jpg?sig=xxx";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);

        String url = storageService.getPresignedUrl("products/img.jpg");

        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("getPresignedUrl – MinIO error – throws StorageException STORAGE_004")
    void getPresignedUrl_minioError_throwsStorageException() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new IOException("MinIO unreachable"));

        assertThatThrownBy(() -> storageService.getPresignedUrl("products/img.jpg"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to generate presigned URL");
    }

    // =========================================================================
    // deleteFile
    // =========================================================================

    @Test
    @DisplayName("deleteFile – happy path – no exception thrown")
    void deleteFile_happyPath_noException() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatCode(() -> storageService.deleteFile("products/img.jpg"))
                .doesNotThrowAnyException();

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("deleteFile – MinIO error – throws StorageException STORAGE_003")
    void deleteFile_minioError_throwsStorageException() throws Exception {
        doThrow(new IOException("Delete failed"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatThrownBy(() -> storageService.deleteFile("products/img.jpg"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File deletion failed");
    }

    // =========================================================================
    // fileExists
    // =========================================================================

    @Test
    @DisplayName("fileExists – object found – returns true")
    void fileExists_objectFound_returnsTrue() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

        boolean result = storageService.fileExists("products/img.jpg");

        assertThat(result).isTrue();
    }
}
