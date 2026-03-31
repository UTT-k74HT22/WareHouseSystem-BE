package org.demo.whs.service.chatbot;

import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchInventorySnapshotResponse;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.dto.response.chatbot.ChatBotSuggestion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatBotResponseFormatter {

    public String greeting() {
        return "Chao ban. Toi la WHS Assistant. Toi co the tra cuu san pham, kho bai, doi tac va don hang bang du lieu that tu he thong.";
    }

    public String help() {
        return """
                Toi co the ho tro ban tra cuu:

                1. San pham va ton kho
                - Tim san pham [ten/sku]
                - Ton kho SKU-001
                - San pham nay o kho nao?
                - Batch sap het han trong 30 ngay

                2. Kho bai
                - Danh sach kho
                - Thong tin kho [ten kho]

                3. Doi tac
                - Tim nha cung cap [ten]
                - Thong tin khach hang [ten]

                4. Don hang
                - Don nhap PO-2024-001
                - Don xuat SO-2024-005
                """;
    }

    public String systemGuide() {
        return """
                Huong dan van hanh co ban:
                - Nhap hang: Tao PO -> Xac nhan PO -> Tao receipt -> Xac nhan nhap kho
                - Xuat hang: Tao SO -> Xac nhan SO -> Tao shipment -> Xac nhan xuat kho
                - Kiem ke: Dung Stock Adjustment de dieu chinh ton
                - Luan chuyen: Dung Stock Transfer de chuyen hang giua kho/vi tri
                """;
    }

    public String warehouseLookup(List<WareHouseResponse> warehouses) {
        if (warehouses.isEmpty()) {
            return "Khong tim thay thong tin kho nao khop voi yeu cau.";
        }

        String rows = warehouses.stream()
                .map(w -> "- " + safe(w.getName()) + " (`" + safe(w.getCode()) + "`) | Dia chi: " + safe(w.getAddress()))
                .collect(Collectors.joining("\n"));

        return "Danh sach kho tim thay:\n" + rows;
    }

    public String warehouseLocations(WareHouseResponse warehouse, List<LocationResponse> locations) {
        if (locations == null || locations.isEmpty()) {
            return "Kho `" + safe(warehouse.getCode()) + "` - " + safe(warehouse.getName()) + " chua co vi tri nao.";
        }

        String rows = locations.stream()
                .map(location -> "- `" + safe(location.getCode()) + "` | " + safe(location.getName())
                        + " | Zone: " + safe(location.getZone())
                        + " | Type: " + safe(location.getType())
                        + " | Status: " + safe(location.getStatus()))
                .collect(Collectors.joining("\n"));

        return "Cac vi tri cua kho `" + safe(warehouse.getCode()) + "` - " + safe(warehouse.getName()) + ":\n" + rows;
    }

    public String warehouseLocationsOverview(List<LocationResponse> locations) {
        if (locations == null || locations.isEmpty()) {
            return "Khong tim thay vi tri kho nao.";
        }

        String rows = locations.stream()
                .map(location -> "- `" + safe(location.getCode()) + "` | " + safe(location.getName())
                        + " | Kho: " + safe(location.getWarehouseName() != null ? location.getWarehouseName() : location.getWarehouseId())
                        + " | Zone: " + safe(location.getZone())
                        + " | Type: " + safe(location.getType()))
                .collect(Collectors.joining("\n"));

        return "Danh sach vi tri kho:\n" + rows;
    }

    public String partnerLookup(List<BusinessPartnerResponse> partners) {
        if (partners.isEmpty()) {
            return "Khong tim thay doi tac nao khop voi yeu cau.";
        }

        String rows = partners.stream()
                .map(p -> "- " + safe(p.getName()) + " (`" + safe(p.getCode()) + "`) | Loai: " + safe(p.getType()) + " | DT: " + safe(p.getPhone()))
                .collect(Collectors.joining("\n"));

        return "Tim thay " + partners.size() + " doi tac:\n" + rows;
    }

    public String purchaseOrderLookup(List<PurchaseOrdersResponse> orders) {
        if (orders.isEmpty()) {
            return "Khong tim thay don nhap (PO) nao khop voi ma hoac tu khoa yeu cau.";
        }

        String rows = orders.stream()
                .map(o -> "- " + safe(o.getPurchaseOrderNumber()) + " | Trang thai: " + safe(o.getStatus()) + " | NCC ID: " + safe(o.getSupplierId()) + " | Tong: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thong tin don nhap (PO):\n" + rows;
    }

    public String purchaseOrderDetail(PurchaseOrdersResponse order) {
        return """
                Chi tiet don nhap:
                - Ma don: %s
                - Nha cung cap ID: %s
                - Kho ID: %s
                - Ngay dat: %s
                - Ngay giao du kien: %s
                - Trang thai: %s
                - Tam tinh: %s
                - Thue: %s
                - Tong tien: %s
                - Ghi chu: %s
                %s
                """.formatted(
                safe(order.getPurchaseOrderNumber()),
                safe(order.getSupplierId()),
                safe(order.getWarehouseId()),
                safe(order.getOrderDate()),
                safe(order.getExpectedDeliveryDate()),
                safe(order.getStatus()),
                decimal(order.getSubTotal(), "0"),
                decimal(order.getTaxAmount(), "0"),
                decimal(order.getTotalAmount(), "0"),
                safe(order.getNotes()),
                formatPurchaseOrderLines(order.getLines())
        );
    }

    public String salesOrderLookup(List<SalesOrdersResponse> orders) {
        if (orders.isEmpty()) {
            return "Khong tim thay don xuat (SO) nao khop voi ma hoac tu khoa yeu cau.";
        }

        String rows = orders.stream()
                .map(o -> "- " + safe(o.getSoNumber()) + " | Trang thai: " + safe(o.getStatus()) + " | Khach ID: " + safe(o.getCustomerId()) + " | Tong: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thong tin don xuat (SO):\n" + rows;
    }

    public String salesOrderDetail(SalesOrdersResponse order) {
        return """
                Chi tiet don xuat:
                - Ma don: %s
                - Khach hang ID: %s
                - Kho ID: %s
                - Ngay dat: %s
                - Ngay giao du kien: %s
                - Trang thai: %s
                - Tam tinh: %s
                - Thue: %s
                - Tong tien: %s
                - Ghi chu: %s
                %s
                """.formatted(
                safe(order.getSoNumber()),
                safe(order.getCustomerId()),
                safe(order.getWarehouseId()),
                safe(order.getOrderDate()),
                safe(order.getRequestedDeliveryDate()),
                safe(order.getStatus()),
                decimal(order.getSubTotal(), "0"),
                decimal(order.getTaxAmount(), "0"),
                decimal(order.getTotalAmount(), "0"),
                safe(order.getNotes()),
                formatSalesOrderLines(order.getLines())
        );
    }

    public String noMatch(String entityType, String keyword) {
        return "Khong tim thay " + entityType + " nao khop voi tu khoa '" + keyword + "'.";
    }

    public String noProductMatch(String keyword) {
        return "Khong tim thay san pham nao khop voi tu khoa '" + keyword + "'. Hay thu lai bang SKU hoac ten san pham ro hon.";
    }

    public String ambiguousProducts(String keyword, List<ProductResponse> products) {
        String matches = products.stream()
                .map(product -> "- `" + safe(product.getSku()) + "` | " + safe(product.getName()))
                .collect(Collectors.joining("\n"));

        return "Tim thay nhieu san pham khop voi '" + keyword + "'. Hay chon ro hon bang SKU:\n" + matches;
    }

    public String productLookup(List<ProductResponse> products) {
        if (products.size() == 1) {
            ProductResponse product = products.get(0);
            return """
                    Thong tin san pham:
                    - SKU: `%s`
                    - Ten: %s
                    - Danh muc: %s
                    - Don vi: %s
                    - Gia ban: %s
                    - Trang thai: %s
                    """.formatted(
                    safe(product.getSku()),
                    safe(product.getName()),
                    safe(product.getCategoryName()),
                    safe(product.getUomName()),
                    decimal(product.getSellingPrice(), "Chua co"),
                    product.getStatus() != null ? product.getStatus().name() : "UNKNOWN"
            );
        }

        String rows = products.stream()
                .map(product -> "- `" + safe(product.getSku()) + "` | " + safe(product.getName())
                        + " | Gia: " + decimal(product.getSellingPrice(), "Chua co"))
                .collect(Collectors.joining("\n"));

        return "Tim thay " + products.size() + " san pham:\n" + rows;
    }

    public String inventorySummary(ProductResponse product, InventorySummaryResponse summary) {
        return """
                Ton kho hien tai:
                - SKU: `%s`
                - Ten: %s
                - On hand: %s
                - Reserved: %s
                - Available: %s
                - So kho co hang: %s
                - So vi tri co hang: %s
                """.formatted(
                safe(product.getSku()),
                safe(product.getName()),
                decimal(summary.getTotalOnHandQuantity(), "0"),
                decimal(summary.getTotalReservedQuantity(), "0"),
                decimal(summary.getTotalAvailableQuantity(), "0"),
                summary.getWarehouseCount() != null ? summary.getWarehouseCount() : 0,
                summary.getLocationCount() != null ? summary.getLocationCount() : 0
        );
    }

    public String inventoryByLocation(ProductResponse product, List<InventoryByLocationResponse> locations) {
        String body = locations.stream()
                .flatMap(location -> location.getItems().stream().map(item -> formatLocationLine(location, item)))
                .collect(Collectors.joining("\n"));

        return "Ton kho theo vi tri cho `" + safe(product.getSku()) + "` - " + safe(product.getName()) + ":\n" + body;
    }

    public String noInventoryByLocation(ProductResponse product) {
        return "Khong tim thay ton kho theo vi tri cho `" + safe(product.getSku()) + "` - " + safe(product.getName()) + ".";
    }

    public String batchExpiringGlobal(Integer thresholdDays, List<BatchExpiringResponse> batches) {
        String body = batches.stream()
                .limit(10)
                .map(batch -> "- `" + safe(batch.getBatchNumber()) + "` | " + safe(batch.getProductSku())
                        + " | " + safe(batch.getProductName())
                        + " | Het han: " + safe(batch.getExpiryDate())
                        + " | Con kha dung: " + decimal(available(batch.getInventorySnapshot()), "0"))
                .collect(Collectors.joining("\n"));

        return "Top batch sap het han trong " + thresholdDays + " ngay:\n" + body;
    }

    public String batchExpiringByProduct(ProductResponse product, Integer thresholdDays, List<BatchByProductResponse> batches) {
        String body = batches.stream()
                .limit(10)
                .map(batch -> "- `" + safe(batch.getBatchNumber()) + "` | Het han: " + safe(batch.getExpiryDate())
                        + " | Trang thai: " + (batch.getStatus() != null ? batch.getStatus().name() : "UNKNOWN")
                        + " | Con kha dung: " + decimal(available(batch.getInventorySnapshot()), "0"))
                .collect(Collectors.joining("\n"));

        return "Batch sap het han cho `" + safe(product.getSku()) + "` - " + safe(product.getName())
                + " trong " + thresholdDays + " ngay:\n" + body;
    }

    public String noBatchExpiring(Integer thresholdDays, ProductResponse product) {
        if (product == null) {
            return "Khong co batch nao sap het han trong " + thresholdDays + " ngay.";
        }

        return "Khong co batch nao sap het han trong " + thresholdDays + " ngay cho `"
                + safe(product.getSku()) + "` - " + safe(product.getName()) + ".";
    }

    public String unsupportedRealTimeQuestion() {
        return """
                Toi chua du du lieu de tra loi chac chan cau hoi nay.
                Hay hoi theo mot trong cac mau:
                - Ton kho [SKU/ten san pham]
                - San pham [SKU/ten]
                - Batch sap het han [so ngay]
                """;
    }

    public String aiFallbackUnavailable() {
        return "Toi khong the su dung AI fallback luc nay. Ban hay hoi theo SKU, ten san pham hoac batch cu the de toi tra du lieu that tu he thong.";
    }

    public List<ChatBotSuggestion> getSuggestions(ChatBotIntent intent, ProductResponse product, String keyword) {
        if (product != null && product.getSku() != null) {
            return dynamicSuggestions(product);
        }
        return staticSuggestions(intent);
    }

    private List<ChatBotSuggestion> dynamicSuggestions(ProductResponse product) {
        String sku = product.getSku();
        return List.of(
                ChatBotSuggestion.builder()
                        .label("Sản phẩm này còn bao nhiêu hàng?")
                        .intent(ChatBotIntent.INVENTORY_SUMMARY)
                        .sku(sku)
                        .query("Tồn kho " + sku)
                        .requiresInput(Boolean.FALSE)
                        .build(),
                ChatBotSuggestion.builder()
                        .label("Sản phẩm này đang ở đâu trong kho?")
                        .intent(ChatBotIntent.INVENTORY_BY_LOCATION)
                        .sku(sku)
                        .query("Tồn kho theo vị trí " + sku)
                        .requiresInput(Boolean.FALSE)
                        .build(),
                ChatBotSuggestion.builder()
                        .label("Batch của sản phẩm này có sắp hết hạn không?")
                        .intent(ChatBotIntent.BATCH_EXPIRING)
                        .sku(sku)
                        .query("Batch sắp hết hạn " + sku)
                        .requiresInput(Boolean.FALSE)
                        .build()
        );
    }

    private List<ChatBotSuggestion> staticSuggestions(ChatBotIntent intent) {
        return switch (intent) {
            case GREETING, HELP, UNKNOWN -> List.of(
                    inputSuggestion("Tra cứu sản phẩm", ChatBotIntent.PRODUCT_LOOKUP, "Tìm sản phẩm "),
                    actionSuggestion("Danh sách kho", ChatBotIntent.WAREHOUSE_LOOKUP, "Xem danh sách các kho"),
                    actionSuggestion("Batch sắp hết hạn", ChatBotIntent.BATCH_EXPIRING, "Liệt kê các batch sắp hết hạn trong 30 ngày"),
                    inputSuggestion("Nhập mã đơn nhập", ChatBotIntent.INBOUND_LOOKUP, "Đơn nhập "),
                    inputSuggestion("Nhập mã đơn xuất", ChatBotIntent.OUTBOUND_LOOKUP, "Đơn xuất ")
            );
            case PRODUCT_LOOKUP, INVENTORY_SUMMARY, INVENTORY_BY_LOCATION, BATCH_EXPIRING -> List.of(
                    inputSuggestion("Nhập tên hoặc SKU khác", ChatBotIntent.PRODUCT_LOOKUP, "Tìm sản phẩm "),
                    inputSuggestion("Nhập mã đơn nhập", ChatBotIntent.INBOUND_LOOKUP, "Đơn nhập "),
                    inputSuggestion("Nhập mã đơn xuất", ChatBotIntent.OUTBOUND_LOOKUP, "Đơn xuất ")
            );
            case WAREHOUSE_LOOKUP, PARTNER_LOOKUP, INBOUND_LOOKUP, OUTBOUND_LOOKUP, SYSTEM_GUIDE -> List.of(
                    inputSuggestion("Tra cứu sản phẩm", ChatBotIntent.PRODUCT_LOOKUP, "Tìm sản phẩm "),
                    actionSuggestion("Danh sách kho", ChatBotIntent.WAREHOUSE_LOOKUP, "Xem danh sách các kho"),
                    actionSuggestion("Hướng dẫn sử dụng", ChatBotIntent.SYSTEM_GUIDE, "Hướng dẫn sử dụng hệ thống")
            );
        };
    }

    private ChatBotSuggestion actionSuggestion(String label, ChatBotIntent intent, String query) {
        return ChatBotSuggestion.builder()
                .label(label)
                .intent(intent)
                .query(query)
                .requiresInput(Boolean.FALSE)
                .build();
    }

    private ChatBotSuggestion inputSuggestion(String label, ChatBotIntent intent, String query) {
        return ChatBotSuggestion.builder()
                .label(label)
                .intent(intent)
                .query(query)
                .requiresInput(Boolean.TRUE)
                .build();
    }

    private String formatLocationLine(InventoryByLocationResponse location, LocationInventoryItemResponse item) {
        String batchPart = item.getBatchNumber() != null ? " | Batch: `" + item.getBatchNumber() + "`" : "";
        return "- Kho: " + safe(location.getWarehouseName())
                + " | Vi tri: " + safe(location.getLocationCode()) + " - " + safe(location.getLocationName())
                + batchPart
                + " | Available: " + decimal(item.getAvailableQuantity(), "0")
                + " | On hand: " + decimal(item.getOnHandQuantity(), "0")
                + " | Reserved: " + decimal(item.getReservedQuantity(), "0");
    }

    private String formatPurchaseOrderLines(List<PurchaseOrderLinesResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- Chua co dong hang nao.";
        }

        return "Cac dong hang:\n" + lines.stream()
                .map(line -> "- Dong " + safe(line.getLineNumber())
                        + " | Product ID: " + safe(line.getProductId())
                        + " | Ordered: " + decimal(line.getQuantityOrdered(), "0")
                        + " | Received: " + decimal(line.getQuantityReceived(), "0")
                        + " | Unit price: " + decimal(line.getUnitPrice(), "0")
                        + " | Line total: " + decimal(line.getLineTotal(), "0"))
                .collect(Collectors.joining("\n"));
    }

    private String formatSalesOrderLines(List<SalesOrderLinesResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- Chua co dong hang nao.";
        }

        return "Cac dong hang:\n" + lines.stream()
                .map(line -> "- Dong " + safe(line.getLineNumber())
                        + " | Product ID: " + safe(line.getProductId())
                        + " | Ordered: " + decimal(line.getQuantityOrdered(), "0")
                        + " | Shipped: " + decimal(line.getQuantityShipped(), "0")
                        + " | Unit price: " + decimal(line.getUnitPrice(), "0")
                        + " | Line total: " + decimal(line.getLineTotal(), "0"))
                .collect(Collectors.joining("\n"));
    }

    private BigDecimal available(BatchInventorySnapshotResponse snapshot) {
        return snapshot != null && snapshot.getTotalAvailableQuantity() != null
                ? snapshot.getTotalAvailableQuantity()
                : BigDecimal.ZERO;
    }

    private String decimal(BigDecimal value, String fallback) {
        return value == null ? fallback : value.stripTrailingZeros().toPlainString();
    }

    private String safe(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }
}
