package org.demo.whs.helpers.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.BackgroundJobProperties;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.BackgroundJobMessageDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class BackgroundJobProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final BackgroundJobProperties backgroundJobProperties;

    public void sendJobToQueue(BackgroundJob job) {
        BackgroundJobMessageDTO message = BackgroundJobMessageDTO.builder()
                .jobId(job.getId())
                .jobCode(job.getJobCode())
                .jobType(job.getJobType())
                .businessType(job.getBusinessType())
                .requestedBy(job.getRequestedBy())
                .createdAt(job.getCreatedAt())
                .build();

        rabbitTemplate.convertAndSend(
                backgroundJobProperties.getExchangeName(),
                backgroundJobProperties.getRoutingKey(),
                message
        );

        log.info("Background job sent to queue jobId={} routingKey={}", job.getId(), backgroundJobProperties.getRoutingKey());
    }
}
