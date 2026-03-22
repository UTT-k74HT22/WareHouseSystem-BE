package org.demo.whs.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.background-job")
@Data
public class BackgroundJobProperties {

    private String queueName = "wms.background-job.queue";
    private String exchangeName = "wms.background-job.exchange";
    private String routingKey = "wms.background-job.process";
    private int concurrentConsumers = 2;
    private int maxConcurrentConsumers = 6;
    private int prefetchCount = 1;
    private String exportFolder = "background-jobs/exports";
}
