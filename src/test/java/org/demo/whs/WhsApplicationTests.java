package org.demo.whs;

import org.demo.whs.configuration.RabbitMQConfig;
import org.demo.whs.configuration.RabbitMQEmailConfig;
import org.demo.whs.configuration.RedisConfig;
import org.demo.whs.helpers.producer.EmailProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                RedisConfig.class,
                RabbitMQConfig.class,
                RabbitMQEmailConfig.class,
                EmailProducerService.class
        })
})
class WhsApplicationTests {

    @Test
    void contextLoads() {
    }

}
