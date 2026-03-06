package org.demo.whs.configuration;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
class TestRateLimitConfig {

    @Bean
    ProxyManager<String> proxyManager() {
        return Mockito.mock(ProxyManager.class, Mockito.RETURNS_DEEP_STUBS);
    }
}
