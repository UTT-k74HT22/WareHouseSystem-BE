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

    /** Access key (root user) */
    private String accessKey;

    /** Secret key (root password) */
    private String secretKey;

    /** Default bucket that the application uses */
    private String bucketName;

    /** Presigned URL expiry in seconds (default 3600 = 1 hour) */
    private int presignedUrlExpiry = 3600;
}
