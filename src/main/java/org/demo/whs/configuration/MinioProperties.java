package org.demo.whs.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinioProperties – binds all app.minio.* properties.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

    /** MinIO server endpoint (e.g. http://localhost:9000) */
    private String endpoint;

    /**
     * Browser-facing endpoint used to generate presigned URLs. This may differ
     * from {@link #endpoint} when MinIO is behind a reverse proxy.
     */
    private String publicEndpoint;

    /** Access key (root user) */
    private String accessKey;

    /** Secret key (root password) */
    private String secretKey;

    /** Default bucket that the application uses */
    private String bucketName;

    /** Presigned URL expiry in seconds (default 3600 = 1 hour) */
    private int presignedUrlExpiry = 3600;

    /** MinIO region (default us-east-1) */
    private String region = "us-east-1";
}
