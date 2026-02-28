package org.demo.whs.configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinioConfig – creates the {@link MinioClient} bean and ensures the
 * default bucket exists at application startup.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private final MinioProperties minioProperties;

    /**
     * MinioClient singleton bean.
     * Connection is lazily established; the bean only fails if credentials
     * or the endpoint are completely unreachable.
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client – endpoint: {}", minioProperties.getEndpoint());

        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        ensureBucketExists(client, minioProperties.getBucketName());

        return client;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates the bucket if it does not already exist.
     * Any error here is logged but does NOT prevent the application from starting –
     * the service layer will surface the error on the first actual upload.
     */
    private void ensureBucketExists(MinioClient client, String bucketName) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());

            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully.", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists.", bucketName);
            }
        } catch (Exception ex) {
            log.error("Failed to verify/create MinIO bucket '{}': {}", bucketName, ex.getMessage(), ex);
        }
    }
}
