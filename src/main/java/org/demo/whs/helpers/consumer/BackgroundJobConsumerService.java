package org.demo.whs.helpers.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.BackgroundJobMessageDTO;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.service.processor.BackgroundJobProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class BackgroundJobConsumerService {

    private final BackgroundJobRepository backgroundJobRepository;
    private final List<BackgroundJobProcessor> backgroundJobProcessors;
    private final JobStatusUpdater jobStatusUpdater;

    // The queue name is injected from BackgroundJobProperties using Spring Expression Language (SpEL)
    @RabbitListener(queues = "#{backgroundJobProperties.queue}",
            containerFactory = "backgroundJobListenerContainerFactory")
    public void consumeJob(BackgroundJobMessageDTO messageDTO) {
        BackgroundJob job = backgroundJobRepository.findById(messageDTO.getJobId())
                .orElseThrow(() -> new NotFoundException(
                        "Background job not found with ID: " + messageDTO.getJobId(),
                        ErrorCode.JOB_001
                ));

        BackgroundJobProcessor processor = backgroundJobProcessors.stream()
                .filter(candidate -> candidate.supports(job))
                .findFirst()
                .orElse(null);

        if (processor == null) {
            log.warn("No processor found for background job id={} type={}", job.getId(), job.getJobType());
            jobStatusUpdater.markFailed(job.getId(), ErrorCode.JOB_005.getCode(), ErrorCode.JOB_005.getMessage(), "DISPATCH");
            return;
        }

        log.info("Dispatching background job id={} to processor={}", job.getId(), processor.getClass().getSimpleName());
        processor.process(job);
    }
}
