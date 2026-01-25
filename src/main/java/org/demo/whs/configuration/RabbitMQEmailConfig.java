package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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

    private final EmailProperties emailProperties;

    @Bean
    public Queue emailQueue() {
        log.info("Creating email queue: {}", emailProperties.getQueueName());
        return new Queue(emailProperties.getQueueName(), true);
    }

    @Bean
    public TopicExchange emailExchange() {
        log.info("Creating email exchange: {}", emailProperties.getExchangeName());
        return new TopicExchange(emailProperties.getExchangeName());
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        log.info("Binding queue {} to exchange {} with routing key {}",
                emailProperties.getQueueName(),
                emailProperties.getExchangeName(),
                emailProperties.getRoutingKey());
        return BindingBuilder
                .bind(emailQueue)
                .to(emailExchange)
                .with(emailProperties.getRoutingKey());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory emailListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter   // <-- dùng bean jacksonMessageConverter chung
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}
