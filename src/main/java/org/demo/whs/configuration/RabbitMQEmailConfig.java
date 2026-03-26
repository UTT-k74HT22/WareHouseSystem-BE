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
 * RabbitMQEmailConfig: Configuration cho queue email.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // tránh load vào test nếu chưa cần
public class RabbitMQEmailConfig {

    private final RabbitMQEmailProperties rabbitMQEmailProperties;

    // Tạo queue email chính, durable để đảm bảo message không bị mất khi broker restart
    @Bean
    public Queue emailQueue() {
        log.info("Creating email queue: {}", rabbitMQEmailProperties.getQueue());
        return QueueBuilder.durable(rabbitMQEmailProperties.getQueue()).build();
    }

    // Tạo exchange kiểu topic để có thể route message dựa trên routing key linh hoạt
    @Bean
    public TopicExchange emailExchange() {
        log.info("Creating email exchange: {}", rabbitMQEmailProperties.getExchange());
        return new TopicExchange(rabbitMQEmailProperties.getExchange(), true, false);
    }

    // Binding queue emailQueue với exchange emailExchange sử dụng routing key từ properties
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        log.info("Binding queue {} to exchange {} with routing key {}",
                rabbitMQEmailProperties.getQueue(),
                rabbitMQEmailProperties.getExchange(),
                rabbitMQEmailProperties.getRoutingKey());
        return BindingBuilder
                .bind(emailQueue)
                .to(emailExchange)
                .with(rabbitMQEmailProperties.getRoutingKey());
    }

    // Cấu hình retry queue với TTL và dead-letter exchange để tự động retry khi message bị reject hoặc gặp lỗi
    @Bean
    public Queue emailRetryQueue() {
        log.info("Creating email retry queue: {}", rabbitMQEmailProperties.getRetryQueue());
        return QueueBuilder.durable(rabbitMQEmailProperties.getRetryQueue())
                .withArgument("x-message-ttl", rabbitMQEmailProperties.getRetryTtlMs())
                .withArgument("x-dead-letter-exchange", rabbitMQEmailProperties.getExchange())
                .withArgument("x-dead-letter-routing-key", rabbitMQEmailProperties.getRoutingKey())
                .build();
    }

    // Binding retry queue với exchange để message sau khi hết TTL sẽ được route lại vào queue chính
    @Bean
    public Binding emailRetryBinding(Queue emailRetryQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailRetryQueue)
                .to(emailExchange)
                .with(rabbitMQEmailProperties.getRetryRoutingKey());
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        log.info("Creating email DLQ: {}", rabbitMQEmailProperties.getDlq());
        return QueueBuilder.durable(rabbitMQEmailProperties.getDlq())
                .build();
    }

    @Bean
    public Binding emailDeadLetterBinding(Queue emailDeadLetterQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue)
                .to(emailExchange)
                .with(rabbitMQEmailProperties.getDlqRoutingKey());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory emailListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter   // <-- dùng bean jacksonMessageConverter chung
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(rabbitMQEmailProperties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(rabbitMQEmailProperties.getMaxConsumers());
        factory.setPrefetchCount(rabbitMQEmailProperties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
