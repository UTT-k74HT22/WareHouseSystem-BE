package org.demo.whs.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Configuration class for OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}api")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ware House System API")
                        .version("1.0.0")
                        .description("API documentation for Ware House Service")
                        .contact(new Contact()
                                .name("DungHD")
                                .email("dunghd.dev@gmail.com"))
                        .license(new License()
                                .name("Private License")))
                .servers(List.of(
                        new Server().url("http://localhost:8080" + contextPath).description("Local Development Server")
                ));
    }
}
