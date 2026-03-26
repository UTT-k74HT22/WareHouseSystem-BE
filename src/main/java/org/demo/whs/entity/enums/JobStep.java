package org.demo.whs.entity.enums;

public enum JobStep {
    RECEIVED,
    VALIDATING_INPUT,
    READING_SOURCE,
    QUERYING_DATA,
    PROCESSING_DATA,
    WRITING_OUTPUT,
    UPLOADING_ARTIFACT,
    FINALIZING
}
