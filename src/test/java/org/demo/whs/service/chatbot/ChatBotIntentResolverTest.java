package org.demo.whs.service.chatbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatBotIntentResolverTest {

    private final ChatBotIntentResolver resolver = new ChatBotIntentResolver();

    @Test
    void shouldClassifyInventoryByLocationBeforeWarehouseLookup() {
        ChatBotCommand command = resolver.resolve("San pham nay dang o kho nao?");

        assertEquals(ChatBotIntent.INVENTORY_BY_LOCATION, command.intent());
    }

    @Test
    void shouldClassifyInventorySummaryBeforeOutboundLookup() {
        ChatBotCommand command = resolver.resolve("So luong ton cua SKU-001 con bao nhieu?");

        assertEquals(ChatBotIntent.INVENTORY_SUMMARY, command.intent());
    }

    @Test
    void shouldKeepOutboundLookupForSalesOrderCodes() {
        ChatBotCommand command = resolver.resolve("Don xuat SO-2024-005");

        assertEquals(ChatBotIntent.OUTBOUND_LOOKUP, command.intent());
    }

    @Test
    void shouldKeepWarehouseLookupForWarehouseListingQueries() {
        ChatBotCommand command = resolver.resolve("Danh sach kho");

        assertEquals(ChatBotIntent.WAREHOUSE_LOOKUP, command.intent());
    }

    @Test
    void shouldClassifyBatchExpiringForLoSapHetHanQueries() {
        ChatBotCommand command = resolver.resolve("Lo sap het han");

        assertEquals(ChatBotIntent.BATCH_EXPIRING, command.intent());
    }

    @Test
    void shouldKeepWarehouseLookupForWarehouseLocationQueries() {
        ChatBotCommand command = resolver.resolve("Cac vi tri cua kho TEST FLOW");

        assertEquals(ChatBotIntent.WAREHOUSE_LOOKUP, command.intent());
    }
}
