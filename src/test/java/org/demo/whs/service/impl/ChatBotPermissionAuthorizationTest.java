package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.chatbot.ChatBotRequest;
import org.demo.whs.repository.EmployeeRepository;
import org.demo.whs.entity.Account;
import org.demo.whs.security.CustomUserDetails;
import org.demo.whs.service.*;
import org.demo.whs.service.chatbot.ChatBotIntent;
import org.demo.whs.service.chatbot.ChatBotResponseFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatBotPermissionAuthorizationTest {
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RedisService redisService;
    @Mock private ChatBotResponseFormatter responseFormatter;
    @Mock private ProductService productService;
    @Mock private InventoryService inventoryService;
    @Mock private LocationService locationService;
    @Mock private BatchService batchService;
    @Mock private WareHouseService wareHouseService;
    @Mock private BusinessPartnerService businessPartnerService;
    @Mock private PurchaseOrdersService purchaseOrdersService;
    @Mock private SalesOrdersService salesOrdersService;
    @InjectMocks private ChatBotServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = ChatBotIntent.class, names = {
            "PRODUCT_LOOKUP", "INVENTORY_SUMMARY", "INVENTORY_BY_LOCATION", "BATCH_EXPIRING",
            "WAREHOUSE_LOOKUP", "PARTNER_LOOKUP", "INBOUND_LOOKUP", "OUTBOUND_LOOKUP"
    })
    void should_DenyModuleLookup_When_AdminRoleHasNoModulePermission(ChatBotIntent intent) {
        assertDenied(intent, "ROLE_ADMIN", "PERM_WAREHOUSE_GLOBAL_ACCESS");
    }

    @Test
    void should_DenyOrderLookup_When_ReadPermissionHasNoWarehouseScope() {
        assertDenied(ChatBotIntent.INBOUND_LOOKUP, "PERM_PURCHASE_ORDER_READ");
    }

    @Test
    void should_DenyInventoryLookup_When_ReadPermissionHasNoWarehouseScope() {
        assertDenied(ChatBotIntent.INVENTORY_SUMMARY, "PERM_INVENTORY_READ");
    }

    private void assertDenied(ChatBotIntent intent, String... authorities) {
        Account account = new Account();
        account.setId("account-1");
        account.setUsername("reviewer");
        CustomUserDetails principal = new CustomUserDetails(account, List.of(), Set.of(authorities));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
        ChatBotRequest request = new ChatBotRequest();
        request.setIntent(intent);
        request.setPayload(Map.of("sku", "example"));
        assertThat(service.chat(request).getReply()).contains("không có quyền");
        verifyNoInteractions(productService, inventoryService, locationService, batchService,
                wareHouseService, businessPartnerService, purchaseOrdersService, salesOrdersService);
    }
}
