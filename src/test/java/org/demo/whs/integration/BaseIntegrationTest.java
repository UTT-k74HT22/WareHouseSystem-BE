package org.demo.whs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.helpers.producer.EmailProducerService;
import org.demo.whs.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * BaseIntegrationTest: Base class for Full-Stack Integration Tests.
 * Runs with in-memory H2 MySQL compatibility profile, excludes real Redis/RabbitMQ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.demo.whs.configuration.RedisConfig.class,
                org.demo.whs.configuration.RabbitMQConfig.class,
                org.demo.whs.configuration.RabbitMQEmailConfig.class,
                EmailProducerService.class
        })
})
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataFactory dataFactory;

    @Autowired
    protected InventoryRepository inventoryRepository;

    @Autowired
    protected StockMovementsRepository stockMovementsRepository;

    @Autowired
    protected PurchaseOrdersRepository purchaseOrdersRepository;

    @Autowired
    protected PurchaseOrderLinesRepository purchaseOrderLinesRepository;

    @Autowired
    protected InboundReceiptsRepository inboundReceiptsRepository;

    @Autowired
    protected InboundReceiptLinesRepository inboundReceiptLinesRepository;

    @Autowired
    protected SalesOrdersRepository salesOrdersRepository;

    @Autowired
    protected SalesOrderLinesRepository salesOrderLinesRepository;

    @Autowired
    protected OutboundShipmentsRepository outboundShipmentsRepository;

    @Autowired
    protected OutboundShipmentLinesRepository outboundShipmentLinesRepository;

    @Autowired
    protected StockAdjustmentsRepository stockAdjustmentsRepository;

    @Autowired
    protected InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    protected LocationRepository locationRepository;

    @Autowired
    protected BatchRepository batchRepository;

    @Autowired
    protected ProductRepository productRepository;

    protected static final String DEFAULT_ADMIN_USER = "admin";

    @BeforeEach
    void setUpBase() {
        dataFactory.createAccountWithRole(DEFAULT_ADMIN_USER, "ADMIN");
    }

    protected String asJson(Object object) throws Exception {
        if (object instanceof String str) {
            return str;
        }
        return objectMapper.writeValueAsString(object);
    }

    protected ResultActions performPost(String url, Object body, String username, String... permissions) throws Exception {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return mockMvc.perform(post(url)
                .with(user(username).authorities(authorities))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(body)));
    }

    protected ResultActions performPut(String url, Object body, String username, String... permissions) throws Exception {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toList();

        var requestBuilder = put(url)
                .with(user(username).authorities(authorities))
                .contentType(MediaType.APPLICATION_JSON);

        if (body != null) {
            requestBuilder.content(asJson(body));
        }

        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions performGet(String url, String username, String... permissions) throws Exception {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return mockMvc.perform(get(url)
                .with(user(username).authorities(authorities))
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performDelete(String url, String username, String... permissions) throws Exception {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return mockMvc.perform(delete(url)
                .with(user(username).authorities(authorities)));
    }
}
