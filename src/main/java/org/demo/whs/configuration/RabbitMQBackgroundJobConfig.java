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

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class RabbitMQBackgroundJobConfig {

    private final BackgroundJobProperties backgroundJobProperties;

    @Bean
    public Queue backgroundJobQueue() {
        log.info("Creating background job queue: {}", backgroundJobProperties.getQueueName());
        return new Queue(backgroundJobProperties.getQueueName(), true);
    }

    @Bean
    public TopicExchange backgroundJobExchange() {
        log.info("Creating background job exchange: {}", backgroundJobProperties.getExchangeName());
        return new TopicExchange(backgroundJobProperties.getExchangeName());
    }

    @Bean
    public Binding backgroundJobBinding(Queue backgroundJobQueue, TopicExchange backgroundJobExchange) {
        return BindingBuilder.bind(backgroundJobQueue)
                .to(backgroundJobExchange)
                .with(backgroundJobProperties.getRoutingKey());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory backgroundJobListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(backgroundJobProperties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(backgroundJobProperties.getMaxConcurrentConsumers());
        factory.setPrefetchCount(backgroundJobProperties.getPrefetchCount());
        return factory;
    }
}
