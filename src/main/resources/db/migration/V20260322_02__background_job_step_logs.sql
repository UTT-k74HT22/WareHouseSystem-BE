CREATE TABLE background_job_step_logs (
                                          id CHAR(36) NOT NULL PRIMARY KEY,
                                          job_id CHAR(36) NOT NULL,
                                          step_name VARCHAR(100) NOT NULL,
                                          status VARCHAR(50) NOT NULL,
                                          message TEXT NULL,
                                          progress_percent INT NOT NULL DEFAULT 0,
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          created_by VARCHAR(50) NULL,
                                          updated_by VARCHAR(50) NULL,

                                          CONSTRAINT fk_job_step_logs_job
                                              FOREIGN KEY (job_id) REFERENCES background_jobs(id)
                                                  ON DELETE CASCADE,

                                          INDEX idx_job_step_logs_job_created_at (job_id, created_at)
);
