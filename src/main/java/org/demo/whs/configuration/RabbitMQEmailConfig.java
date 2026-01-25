package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQEmailConfig: Configuration for RabbitMQ email queue
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEmailConfig {

    private final EmailProperties emailProperties;

    /**
     * Create email queue
     */
    @Bean
    public Queue emailQueue() {
        log.info("Creating email queue: {}", emailProperties.getQueueName());
        return new Queue(emailProperties.getQueueName(), true); // durable = true
    }

    /**
     * Create email exchange
     */
    @Bean
    public TopicExchange emailExchange() {
        log.info("Creating email exchange: {}", emailProperties.getExchangeName());
        return new TopicExchange(emailProperties.getExchangeName());
    }

    /**
     * Bind queue to exchange with routing key
     */
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

    /**
     * Message converter for JSON serialization
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * Container factory for listener
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}
