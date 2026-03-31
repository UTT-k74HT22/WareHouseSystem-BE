package org.demo.whs.service.chatbot;

import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchInventorySnapshotResponse;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
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
        return "Chào bạn. Tôi là WHS Assistant. Tôi có thể tra cứu sản phẩm, kho bãi, đối tác và các loại đơn hàng (nhập/xuất) bằng dữ liệu thật từ hệ thống.";
    }

    public String help() {
        return """
                Tôi có thể hỗ trợ bạn tra cứu các thông tin sau:

                1. **Sản phẩm & Tồn kho**:
                   - "Tìm sản phẩm [tên/sku]"
                   - "Tồn kho SKU-001"
                   - "Sản phẩm này ở kho nào còn hàng?"
                   - "Batch sắp hết hạn trong 30 ngày"

                2. **Kho bãi & Địa điểm**:
                   - "Danh sách kho"
                   - "Thông tin kho [tên kho]"

                3. **Đối tác (NCC/Khách hàng)**:
                   - "Tìm nhà cung cấp [tên]"
                   - "Thông tin khách hàng [tên]"

                4. **Đơn hàng (PO/SO)**:
                   - "Đơn nhập PO-2024-001"
                   - "Đơn xuất SO-2024-005"
                """;
    }

    public String systemGuide() {
        return """
                **Hướng dẫn vận hành cơ bản:**
                
                - **Nhập hàng**: Tạo Đơn mua (PO) -> Xác nhận PO -> Tạo Biên bản nhập kho (Receipt) -> Xác nhận nhập kho.
                - **Xuất hàng**: Tạo Đơn bán (SO) -> Xác nhận SO -> Tạo Chuyến xuất hàng (Shipment) -> Xác nhận xuất hàng.
                - **Kiểm kê**: Sử dụng chức năng Stock Adjustment để điều chỉnh số lượng thực tế.
                - **Luân chuyển**: Sử dụng Stock Transfer để chuyển hàng giữa các kho/vị trí.
                
                Bạn cần hỗ trợ chi tiết bước nào không?
                """;
    }

    public String warehouseLookup(List<WareHouseResponse> warehouses) {
        if (warehouses.isEmpty()) {
            return "Không tìm thấy thông tin kho nào khớp với yêu cầu.";
        }

        String rows = warehouses.stream()
                .map(w -> "- **" + safe(w.getName()) + "** (`" + safe(w.getCode()) + "`) | Địa chỉ: " + safe(w.getAddress()))
                .collect(Collectors.joining("\n"));

        return "Danh sách kho tìm thấy:\n" + rows;
    }

    public String partnerLookup(List<BusinessPartnerResponse> partners) {
        if (partners.isEmpty()) {
            return "Không tìm thấy đối tác (NCC/Khách hàng) nào khớp với yêu cầu.";
        }

        String rows = partners.stream()
                .map(p -> "- **" + safe(p.getName()) + "** (`" + safe(p.getCode()) + "`) | Loại: " + (p.getType() != null ? p.getType() : "N/A") + " | ĐT: " + safe(p.getPhone()))
                .collect(Collectors.joining("\n"));

        return "Tìm thấy " + partners.size() + " đối tác:\n" + rows;
    }

    public String purchaseOrderLookup(List<PurchaseOrdersResponse> orders) {
        if (orders.isEmpty()) {
            return "Không tìm thấy đơn nhập (PO) nào khớp với mã hoặc từ khóa yêu cầu.";
        }

        String rows = orders.stream()
                .map(o -> "- **" + safe(o.getPurchaseOrderNumber()) + "** | Trạng thái: " + safe(o.getStatus()) + " | NCC ID: " + safe(o.getSupplierId()) + " | Tổng: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thông tin đơn nhập (PO):\n" + rows;
    }

    public String salesOrderLookup(List<SalesOrdersResponse> orders) {
        if (orders.isEmpty()) {
            return "Không tìm thấy đơn xuất (SO) nào khớp với mã hoặc từ khóa yêu cầu.";
        }

        String rows = orders.stream()
                .map(o -> "- **" + safe(o.getSoNumber()) + "** | Trạng thái: " + safe(o.getStatus()) + " | Khách ID: " + safe(o.getCustomerId()) + " | Tổng: " + decimal(o.getTotalAmount(), "0"))
                .collect(Collectors.joining("\n"));

        return "Thông tin đơn xuất (SO):\n" + rows;
    }


    public String noMatch(String entityType, String keyword) {
        return "Không tìm thấy " + entityType + " nào khớp với từ khóa '" + keyword + "'.";
    }

    public String noProductMatch(String keyword) {
        return "Không tìm thấy sản phẩm nào khớp với từ khóa '" + keyword + "'. Hãy thử lại bằng SKU hoặc tên sản phẩm rõ hơn.";
    }

    public String ambiguousProducts(String keyword, List<ProductResponse> products) {
        String matches = products.stream()
                .map(product -> "- `" + safe(product.getSku()) + "` | " + safe(product.getName()))
                .collect(Collectors.joining("\n"));

        return "Tìm thấy nhiều sản phẩm khớp với '" + keyword + "'. Hãy chọn rõ hơn bằng SKU:\n" + matches;
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
                Tôi chưa đủ dữ liệu để trả lời chắc chắn câu hỏi này.
                Để tránh trả lời sai, hãy hỏi theo một trong các mẫu:
                - Tồn kho [SKU/tên sản phẩm]
                - Sản phẩm [SKU/tên]
                - Batch sắp hết hạn [số ngày]
                """;
    }

    public String aiFallbackUnavailable() {
        return "Tôi không thể sử dụng AI fallback lúc này. Bạn hãy hỏi theo SKU, tên sản phẩm hoặc batch cụ thể để tôi trả dữ liệu thật từ hệ thống.";
    }

    public List<ChatBotSuggestion> getSuggestions(ChatBotIntent intent, ProductResponse product, String keyword) {
        String sku = product != null ? product.getSku() : keyword;

        if (product != null && sku != null) {
            return dynamicSuggestions(product);
        }
        return staticSuggestions(intent);
    }

    private List<ChatBotSuggestion> dynamicSuggestions(ProductResponse product) {
        String sku = product.getSku();
        return List.of(
                ChatBotSuggestion.builder()
                        .label("Xem tồn kho")
                        .intent(ChatBotIntent.INVENTORY_SUMMARY)
                        .sku(sku)
                        .build(),
                ChatBotSuggestion.builder()
                        .label("Xem vị trí")
                        .intent(ChatBotIntent.INVENTORY_BY_LOCATION)
                        .sku(sku)
                        .build(),
                ChatBotSuggestion.builder()
                        .label("Xem batch hết hạn")
                        .intent(ChatBotIntent.BATCH_EXPIRING)
                        .sku(sku)
                        .build()
        );
    }

    private List<ChatBotSuggestion> staticSuggestions(ChatBotIntent intent) {
        return switch (intent) {
            case GREETING, HELP, UNKNOWN -> List.of(
                    ChatBotSuggestion.builder().label("Tìm sản phẩm").intent(ChatBotIntent.PRODUCT_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Danh sách kho").intent(ChatBotIntent.WAREHOUSE_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Tra cứu đơn hàng").intent(ChatBotIntent.INBOUND_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Batch hết hạn").intent(ChatBotIntent.BATCH_EXPIRING).sku(null).build()
            );
            case PRODUCT_LOOKUP -> List.of(
                    ChatBotSuggestion.builder().label("Xem tồn kho").intent(ChatBotIntent.INVENTORY_SUMMARY).sku(null).build(),
                    ChatBotSuggestion.builder().label("Xem vị trí").intent(ChatBotIntent.INVENTORY_BY_LOCATION).sku(null).build(),
                    ChatBotSuggestion.builder().label("Batch hết hạn").intent(ChatBotIntent.BATCH_EXPIRING).sku(null).build()
            );
            case INVENTORY_SUMMARY, INVENTORY_BY_LOCATION -> List.of(
                    ChatBotSuggestion.builder().label("Tìm sản phẩm khác").intent(ChatBotIntent.PRODUCT_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Batch hết hạn").intent(ChatBotIntent.BATCH_EXPIRING).sku(null).build(),
                    ChatBotSuggestion.builder().label("Tra cứu đơn hàng").intent(ChatBotIntent.INBOUND_LOOKUP).sku(null).build()
            );
            case BATCH_EXPIRING -> List.of(
                    ChatBotSuggestion.builder().label("Xem thông tin sản phẩm").intent(ChatBotIntent.PRODUCT_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Danh sách kho").intent(ChatBotIntent.WAREHOUSE_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Tìm kiếm").intent(ChatBotIntent.PRODUCT_LOOKUP).sku(null).build()
            );
            case WAREHOUSE_LOOKUP, PARTNER_LOOKUP, INBOUND_LOOKUP, OUTBOUND_LOOKUP, SYSTEM_GUIDE -> List.of(
                    ChatBotSuggestion.builder().label("Tìm sản phẩm").intent(ChatBotIntent.PRODUCT_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Danh sách kho").intent(ChatBotIntent.WAREHOUSE_LOOKUP).sku(null).build(),
                    ChatBotSuggestion.builder().label("Hỗ trợ vận hành").intent(ChatBotIntent.SYSTEM_GUIDE).sku(null).build()
            );
        };
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
