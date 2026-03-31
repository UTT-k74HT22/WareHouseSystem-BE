package org.demo.whs.service.chatbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatBotIntentResolverTest {

    private final ChatBotIntentResolver resolver = new ChatBotIntentResolver();

    @Test
    void shouldClassifyInventoryByLocationBeforeWarehouseLookup() {
        ChatBotCommand command = resolver.resolve("Sản phẩm này đang ở kho nào?");

        assertEquals(ChatBotIntent.INVENTORY_BY_LOCATION, command.intent());
    }

    @Test
    void shouldClassifyInventorySummaryBeforeOutboundLookup() {
        ChatBotCommand command = resolver.resolve("Số lượng tồn của SKU-001 còn bao nhiêu?");

        assertEquals(ChatBotIntent.INVENTORY_SUMMARY, command.intent());
    }

    @Test
    void shouldKeepOutboundLookupForSalesOrderCodes() {
        ChatBotCommand command = resolver.resolve("Đơn xuất SO-2024-005");

        assertEquals(ChatBotIntent.OUTBOUND_LOOKUP, command.intent());
    }

    @Test
    void shouldKeepWarehouseLookupForWarehouseListingQueries() {
        ChatBotCommand command = resolver.resolve("Danh sách kho");

        assertEquals(ChatBotIntent.WAREHOUSE_LOOKUP, command.intent());
    }
}
