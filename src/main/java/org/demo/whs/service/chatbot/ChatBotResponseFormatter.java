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
        return "Chào bạn. Tôi là WHS Assistant. Tôi có thể tra cứu sản phẩm, kho bãi, đối tác và đơn hàng bằng dữ liệu thật từ hệ thống.";
    }

    public String help() {
        return """
                Tôi có thể hỗ trợ bạn tra cứu:

                1. Sản phẩm & tồn kho
                - Tìm sản phẩm [tên/SKU]
                - Tồn kho SKU-001
                - Sản phẩm này ở kho nào?
                - Batch sắp hết hạn trong 30 ngày

                2. Kho bãi
                - Danh sách kho
                - Thông tin kho [tên kho]

                3. Đối tác
                - Tìm nhà cung cấp [tên]
                - Thông tin khách hàng [tên]

                4. Đơn hàng
                - Đơn nhập PO-2024-001
                - Đơn xuất SO-2024-005
                """;
    }

    public String systemGuide() {
        return """
                Hướng dẫn vận hành:
                - Nhập hàng: Tạo PO → Xác nhận → Nhập kho
                - Xuất hàng: Tạo SO → Xác nhận → Giao hàng
                - Kiểm kê: Dùng Stock Adjustment
                - Luân chuyển: Dùng Stock Transfer
                """;
    }

    public String warehouseLookup(List<WareHouseResponse> warehouses) {
        if (warehouses.isEmpty()) return "Không tìm thấy kho phù hợp.";

        String rows = warehouses.stream()
                .map(w -> "- " + safe(w.getName()) + " (" + safe(w.getCode()) + ") | Địa chỉ: " + safe(w.getAddress()))
                .collect(Collectors.joining("\n"));

        return "Danh sách kho:\n" + rows;
    }

    public String warehouseLocations(WareHouseResponse warehouse, List<LocationResponse> locations) {
        if (locations == null || locations.isEmpty()) {
            return "Kho `" + safe(warehouse.getCode()) + "` - " + safe(warehouse.getName()) + " chưa có vị trí nào.";
        }

        String rows = locations.stream()
                .map(location -> "- `" + safe(location.getCode()) + "` | " + safe(location.getName())
                        + " | Zone: " + safe(location.getZone())
                        + " | Type: " + safe(location.getType())
                        + " | Status: " + safe(location.getStatus()))
                .collect(Collectors.joining("\n"));

        return "Các vị trí của kho `" + safe(warehouse.getCode()) + "` - " + safe(warehouse.getName()) + ":\n" + rows;
    }

    public String warehouseLocationsOverview(List<LocationResponse> locations) {
        if (locations == null || locations.isEmpty()) {
            return "Không tìm thấy vị trí kho nào.";
        }

        String rows = locations.stream()
                .map(location -> "- `" + safe(location.getCode()) + "` | " + safe(location.getName())
                        + " | Kho: " + safe(location.getWarehouseName() != null ? location.getWarehouseName() : location.getWarehouseId())
                        + " | Zone: " + safe(location.getZone())
                        + " | Type: " + safe(location.getType()))
                .collect(Collectors.joining("\n"));

        return "Danh sách vị trí kho:\n" + rows;
    }

    public String partnerLookup(List<BusinessPartnerResponse> partners) {
        if (partners.isEmpty()) {
            return "Không tìm thấy đối tác nào phù hợp với yêu cầu.";
        }

        String rows = partners.stream()
                .map(p -> "- " + safe(p.getName()) + " (`" + safe(p.getCode()) + "`) | Loại: " + safe(p.getType()) + " | ĐT: " + safe(p.getPhone()))
                .collect(Collectors.joining("\n"));

        return "Tìm thấy " + partners.size() + " đối tác:\n" + rows;
    }

    public String purchaseOrderLookup(List<PurchaseOrdersResponse> orders) {
        if (orders.isEmpty()) {
            return "Không tìm thấy đơn nhập (PO) nào phù hợp với mã hoặc từ khóa yêu cầu.";
        }

        String rows = orders.stream()
                .map(o -> "- " + safe(o.getPurchaseOrderNumber()) + " | Trạng thái: " + safe(o.getStatus()) + " | NCC ID: " + safe(o.getSupplierId()) + " | Tổng: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thông tin đơn nhập (PO):\n" + rows;
    }

    public String purchaseOrderDetail(PurchaseOrdersResponse order) {
        return """
                Chi tiết đơn nhập:
                - Mã đơn: %s
                - Nhà cung cấp ID: %s
                - Kho ID: %s
                - Ngày đặt: %s
                - Ngày giao dự kiến: %s
                - Trạng thái: %s
                - Tạm tính: %s
                - Thuế: %s
                - Tổng tiền: %s
                - Ghi chú: %s
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
            return "Không tìm thấy đơn xuất (SO) nào phù hợp với mã hoặc từ khóa yêu cầu.";
        }

        String rows = orders.stream()
                .map(o -> "- " + safe(o.getSoNumber()) + " | Trạng thái: " + safe(o.getStatus()) + " | Khách ID: " + safe(o.getCustomerId()) + " | Tổng: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thông tin đơn xuất (SO):\n" + rows;
    }

    public String salesOrderDetail(SalesOrdersResponse order) {
        return """
                Chi tiết đơn xuất:
                - Mã đơn: %s
                - Khách hàng ID: %s
                - Kho ID: %s
                - Ngày đặt: %s
                - Ngày giao dự kiến: %s
                - Trạng thái: %s
                - Tạm tính: %s
                - Thuế: %s
                - Tổng tiền: %s
                - Ghi chú: %s
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
        return "Không tìm thấy " + entityType + " nào phù hợp với từ khóa '" + keyword + "'.";
    }

    public String noProductMatch(String keyword) {
        return "Không tìm thấy sản phẩm nào phù hợp với từ khóa '" + keyword + "'. Hãy thử lại bằng SKU hoặc tên sản phẩm rõ hơn.";
    }

    public String ambiguousProducts(String keyword, List<ProductResponse> products) {
        String matches = products.stream()
                .map(product -> "- `" + safe(product.getSku()) + "` | " + safe(product.getName()))
                .collect(Collectors.joining("\n"));

        return "Tìm thấy nhiều sản phẩm phù hợp với '" + keyword + "'. Hãy chọn rõ hơn bằng SKU:\n" + matches;
    }

    public String productLookup(List<ProductResponse> products) {
        if (products.size() == 1) {
            ProductResponse product = products.get(0);
            return """
                    Thông tin sản phẩm:
                    - SKU: `%s`
                    - Tên: %s
                    - Danh mục: %s
                    - Đơn vị: %s
                    - Giá bán: %s
                    - Trạng thái: %s
                    """.formatted(
                    safe(product.getSku()),
                    safe(product.getName()),
                    safe(product.getCategoryName()),
                    safe(product.getUomName()),
                    decimal(product.getSellingPrice(), "Chưa có"),
                    product.getStatus() != null ? product.getStatus().name() : "UNKNOWN"
            );
        }

        String rows = products.stream()
                .map(product -> "- `" + safe(product.getSku()) + "` | " + safe(product.getName())
                        + " | Giá: " + decimal(product.getSellingPrice(), "Chưa có"))
                .collect(Collectors.joining("\n"));

        return "Tìm thấy " + products.size() + " sản phẩm:\n" + rows;
    }

    public String inventorySummary(ProductResponse product, InventorySummaryResponse summary) {
        return """
                Tồn kho hiện tại:
                - SKU: `%s`
                - Tên: %s
                - On hand: %s
                - Reserved: %s
                - Available: %s
                - Số kho có hàng: %s
                - Số vị trí có hàng: %s
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

        return "Tồn kho theo vị trí cho `" + safe(product.getSku()) + "` - " + safe(product.getName()) + ":\n" + body;
    }

    public String noInventoryByLocation(ProductResponse product) {
        return "Không tìm thấy tồn kho theo vị trí cho `" + safe(product.getSku()) + "` - " + safe(product.getName()) + ".";
    }

    public String batchExpiringGlobal(Integer thresholdDays, List<BatchExpiringResponse> batches) {
        String body = batches.stream()
                .limit(10)
                .map(batch -> "- `" + safe(batch.getBatchNumber()) + "` | " + safe(batch.getProductSku())
                        + " | " + safe(batch.getProductName())
                        + " | Hết hạn: " + safe(batch.getExpiryDate())
                        + " | Còn khả dụng: " + decimal(available(batch.getInventorySnapshot()), "0"))
                .collect(Collectors.joining("\n"));

        return "Top batch sắp hết hạn trong " + thresholdDays + " ngày:\n" + body;
    }

    public String batchExpiringByProduct(ProductResponse product, Integer thresholdDays, List<BatchByProductResponse> batches) {
        String body = batches.stream()
                .limit(10)
                .map(batch -> "- `" + safe(batch.getBatchNumber()) + "` | Hết hạn: " + safe(batch.getExpiryDate())
                        + " | Trạng thái: " + (batch.getStatus() != null ? batch.getStatus().name() : "UNKNOWN")
                        + " | Còn khả dụng: " + decimal(available(batch.getInventorySnapshot()), "0"))
                .collect(Collectors.joining("\n"));

        return "Batch sắp hết hạn cho `" + safe(product.getSku()) + "` - " + safe(product.getName())
                + " trong " + thresholdDays + " ngày:\n" + body;
    }

    public String noBatchExpiring(Integer thresholdDays, ProductResponse product) {
        if (product == null) {
            return "Không có batch nào sắp hết hạn trong " + thresholdDays + " ngày.";
        }

        return "Không có batch nào sắp hết hạn trong " + thresholdDays + " ngày cho `"
                + safe(product.getSku()) + "` - " + safe(product.getName()) + ".";
    }

    public String unsupportedRealTimeQuestion() {
        return """
                Tôi chưa có đủ dữ liệu để trả lời chắc chắn câu hỏi này.
                Hãy hỏi theo một trong các mẫu:
                - Tồn kho [SKU/tên sản phẩm]
                - Sản phẩm [SKU/tên]
                - Batch sắp hết hạn [số ngày]
                """;
    }

    public String aiFallbackUnavailable() {
        return "Tôi không thể sử dụng AI fallback lúc này. Bạn hãy hỏi theo SKU, tên sản phẩm hoặc batch cụ thể để tôi trả dữ liệu thật từ hệ thống.";
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
                suggestion("Sản phẩm này còn bao nhiêu hàng?", ChatBotIntent.INVENTORY_SUMMARY, sku, "Tồn kho " + sku, Boolean.FALSE),
                suggestion("Sản phẩm này đang ở đâu trong kho?", ChatBotIntent.INVENTORY_BY_LOCATION, sku, "Tồn kho theo vị trí " + sku, Boolean.FALSE),
                suggestion("Batch của sản phẩm này có sắp hết hạn không?", ChatBotIntent.BATCH_EXPIRING, sku, "Batch sắp hết hạn " + sku, Boolean.FALSE)
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
        return suggestion(label, intent, null, query, Boolean.FALSE);
    }

    private ChatBotSuggestion inputSuggestion(String label, ChatBotIntent intent, String query) {
        return suggestion(label, intent, null, query, Boolean.TRUE);
    }

    private ChatBotSuggestion suggestion(String label, ChatBotIntent intent, String sku, String query, Boolean requiresInput) {
        return new ChatBotSuggestion(label, intent, sku, query, requiresInput);
    }

    private String formatLocationLine(InventoryByLocationResponse location, LocationInventoryItemResponse item) {
        String batchPart = item.getBatchNumber() != null ? " | Batch: `" + item.getBatchNumber() + "`" : "";
        return "- Kho: " + safe(location.getWarehouseName())
                + " | Vị trí: " + safe(location.getLocationCode()) + " - " + safe(location.getLocationName())
                + batchPart
                + " | Available: " + decimal(item.getAvailableQuantity(), "0")
                + " | On hand: " + decimal(item.getOnHandQuantity(), "0")
                + " | Reserved: " + decimal(item.getReservedQuantity(), "0");
    }

    private String formatPurchaseOrderLines(List<PurchaseOrderLinesResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- Chưa có dòng hàng nào.";
        }

        return "Các dòng hàng:\n" + lines.stream()
                .map(line -> "- Dòng " + safe(line.getLineNumber())
                        + " | Product ID: " + safe(line.getProductId())
                        + " | Ordered: " + decimal(line.getQuantityOrdered(), "0")
                        + " | Received: " + decimal(line.getQuantityReceived(), "0")
                        + " | Unit price: " + decimal(line.getUnitPrice(), "0")
                        + " | Line total: " + decimal(line.getLineTotal(), "0"))
                .collect(Collectors.joining("\n"));
    }

    private String formatSalesOrderLines(List<SalesOrderLinesResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- Chưa có dòng hàng nào.";
        }

        return "Các dòng hàng:\n" + lines.stream()
                .map(line -> "- Dòng " + safe(line.getLineNumber())
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
