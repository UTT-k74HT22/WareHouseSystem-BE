package org.demo.whs.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQEmailProperties: Configuration properties for RabbitMQ email queues and exchanges
 */
@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq.email")
@Data
public class RabbitMQEmailProperties {
    private String exchange;
    private String queue;
    private String retryQueue;
    private String dlq;
    private String routingKey;
    private String retryRoutingKey;
    private String dlqRoutingKey;
    private int retryTtlMs;
    private int concurrentConsumers;
    private int maxConsumers;
    private int prefetch;
}