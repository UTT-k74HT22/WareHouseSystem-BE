package org.demo.whs.service.processor;

import org.demo.whs.entity.BackgroundJob;

public interface BackgroundJobProcessor {

    /**
     * Determines if this processor can handle the given background job.
     *
     * @param job the background job to check
     * @return true if this processor supports the job, false otherwise
     */
    boolean supports(BackgroundJob job);

    /**
     * Processes the given background job.
     *
     * @param job the background job to process
     */
    void process(BackgroundJob job);
}
