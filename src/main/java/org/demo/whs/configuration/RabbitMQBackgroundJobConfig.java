package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQBackgroundJobConfig: Configuration cho queue background job.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class RabbitMQBackgroundJobConfig {

    private final BackgroundJobProperties backgroundJobProperties;

    // Tạo queue chính cho background job, durable để đảm bảo message không bị mất khi broker restart
    @Bean
    public Queue backgroundJobQueue() {
        log.info("Creating background job queue: {}", backgroundJobProperties.getQueue());
        return QueueBuilder.durable(backgroundJobProperties.getQueue()).build();
    }

    // Tạo exchange kiểu topic để có thể route message dựa trên routing key linh hoạt
    @Bean
    public TopicExchange backgroundJobExchange() {
        log.info("Creating background job exchange: {}", backgroundJobProperties.getExchange());
        return new TopicExchange(backgroundJobProperties.getExchange(), true, false);
    }

    // Binding queue backgroundJobQueue với exchange backgroundJobExchange sử dụng routing key từ properties
    @Bean
    public Binding backgroundJobBinding(Queue backgroundJobQueue, TopicExchange backgroundJobExchange) {
        return BindingBuilder.bind(backgroundJobQueue)
                .to(backgroundJobExchange)
                .with(backgroundJobProperties.getRoutingKey());
    }

    // Cấu hình retry queue với TTL và dead-letter exchange để tự động retry khi message bị reject hoặc gặp lỗi
    @Bean
    public Queue backgroundJobRetryQueue() {
        log.info("Creating background job retry queue: {}", backgroundJobProperties.getRetryQueue());
        return QueueBuilder.durable(backgroundJobProperties.getRetryQueue())
                .withArgument("x-message-ttl", backgroundJobProperties.getRetryTtlMs())
                .withArgument("x-dead-letter-exchange", backgroundJobProperties.getExchange())
                .withArgument("x-dead-letter-routing-key", backgroundJobProperties.getRoutingKey())
                .build();
    }

    // Binding queue backgroundJobRetryQueue với exchange backgroundJobExchange sử dụng routing key từ properties
    @Bean
    public Binding backgroundJobRetryBinding(Queue backgroundJobRetryQueue, TopicExchange backgroundJobExchange) {
        return BindingBuilder.bind(backgroundJobRetryQueue)
                .to(backgroundJobExchange)
                .with(backgroundJobProperties.getRetryRoutingKey());
    }

    // Tạo dead-letter queue để nhận các message bị reject hoặc gặp lỗi sau khi retry hết số lần cho phép
    @Bean
    public Queue backgroundJobDeadLetterQueue() {
        log.info("Creating background job DLQ: {}", backgroundJobProperties.getDlq());
        return QueueBuilder.durable(backgroundJobProperties.getDlq()).build();
    }

    // Binding queue backgroundJobDeadLetterQueue với exchange backgroundJobExchange sử dụng routing key từ properties
    @Bean
    public Binding backgroundJobDeadLetterBinding(Queue backgroundJobDeadLetterQueue, TopicExchange backgroundJobExchange) {
        return BindingBuilder.bind(backgroundJobDeadLetterQueue)
                .to(backgroundJobExchange)
                .with(backgroundJobProperties.getDlqRoutingKey());
    }

    // Cấu hình RabbitListenerContainerFactory riêng cho background job để có thể tùy chỉnh số lượng consumer, prefetch, v.v.
    @Bean
    public SimpleRabbitListenerContainerFactory backgroundJobListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(backgroundJobProperties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(backgroundJobProperties.getMaxConsumers());
        factory.setPrefetchCount(backgroundJobProperties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
