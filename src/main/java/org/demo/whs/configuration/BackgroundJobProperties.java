package org.demo.whs.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq.background-job")
@Data
public class BackgroundJobProperties {
    private String queue;
    private String exchange;
    private String routingKey;
    private String retryQueue;
    private String dlq;
    private String retryRoutingKey;
    private String dlqRoutingKey;
    private int retryTtlMs;
    private int concurrentConsumers = 2;
    private int maxConsumers = 4;
    private int prefetch = 1;
    private String exportFolder = "background-jobs/exports";
}