CREATE TABLE background_jobs (
                                 id CHAR(36) NOT NULL PRIMARY KEY,
                                 job_code VARCHAR(64) NOT NULL UNIQUE,
                                 job_type VARCHAR(50) NOT NULL,
                                 business_type VARCHAR(100) NOT NULL,
                                 status VARCHAR(50) NOT NULL,
                                 current_step VARCHAR(100) NULL,

                                 requested_by CHAR(36) NOT NULL,
                                 request_payload LONGTEXT NULL,
                                 request_hash VARCHAR(128) NULL,

                                 progress_percent INT NOT NULL DEFAULT 0,
                                 processed_rows BIGINT NOT NULL DEFAULT 0,
                                 total_rows BIGINT NOT NULL DEFAULT 0,

                                 result_file_name VARCHAR(255) NULL,
                                 storage_object_key VARCHAR(500) NULL,
                                 result_mime_type VARCHAR(100) NULL,
                                 result_file_size BIGINT NULL,

                                 error_code VARCHAR(100) NULL,
                                 error_message TEXT NULL,

                                 started_at DATETIME NULL,
                                 finished_at DATETIME NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 created_by VARCHAR(50) NULL,
                                 updated_by VARCHAR(50) NULL,

                                 INDEX idx_background_jobs_requested_by_created_at (requested_by, created_at),
                                 INDEX idx_background_jobs_status_created_at (status, created_at),
                                 INDEX idx_background_jobs_type_status_created_at (job_type, status, created_at),
                                 INDEX idx_background_jobs_request_hash (request_hash)
);


