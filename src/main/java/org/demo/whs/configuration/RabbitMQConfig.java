package org.demo.whs.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQConfig: Configuration for RabbitMQ connection and template
 */
@Configuration
public class RabbitMQConfig {

    private final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") int port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password,
            @Value("${spring.rabbitmq.virtual-host:/}") String virtualHost) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host); //CachingConnectionFactory có constructor nhận host, sẽ tự động dùng port mặc định 5672 nếu không set thêm
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED); // sủ dụng publisher confirm để đảm bảo message đã được broker nhận, CORRELATED sẽ trả về correlationData trong callback để dễ tracking
        factory.setPublisherReturns(true);
        return factory;
    }

    // MessageConverter sử dụng Jackson để serialize/deserialize message thành JSON
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate sẽ tự động sử dụng MessageConverter để serialize/deserialize message
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true); // bắt buộc phải có mandatory để nhận được callback khi message không thể route đến queue nào

        // Thiết lập callback để log kết quả gửi message
        // -> case ack=true: message đã được broker nhận thành công, case ack=false: message không được broker nhận (có thể do lỗi kết nối hoặc broker từ chối), case returnedMessage: message không thể route đến queue nào
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                // Message đã được broker nhận thành công
                logger.info("Message sent successfully: " + correlationData);
            } else {
                // Message không được broker nhận, có thể do lỗi kết nối hoặc broker từ chối
                logger.error("Failed to send message: " + correlationData + ", cause: " + cause);
            }
        });

        template.setReturnsCallback(returnedMessage -> {
            // Message không thể route đến queue nào, sẽ nhận được callback này nếu setMandatory(true)
            logger.error("Returned message: " + returnedMessage);
        });

        return template;
    }

    // RabbitAdmin để tự động declare queue, exchange, binding khi ứng dụng khởi động
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
