package org.demo.whs.configuration;

import jakarta.mail.internet.MimeMessage;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@Profile("test")
class TestRateLimitConfig {

    @Bean
    ProxyManager<String> proxyManager() {
        return Mockito.mock(ProxyManager.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
        return mailSender;
    }
}
