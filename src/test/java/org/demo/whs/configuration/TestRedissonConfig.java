package org.demo.whs.configuration;

import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test configuration for RedissonClient to avoid requiring a Redis server during tests
 */
@Configuration
@Profile("test")
public class TestRedissonConfig {

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RLock mockLock = Mockito.mock(RLock.class);

        try {
            when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(mockLock.tryLock()).thenReturn(true);
            when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        } catch (InterruptedException ignored) {
        }

        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        return redissonClient;
    }
}