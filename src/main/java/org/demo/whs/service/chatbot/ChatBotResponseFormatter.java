package org.demo.whs.service.chatbot;

import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchInventorySnapshotResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatBotResponseFormatter {

    public String greeting() {
        return "Chào bạn. Tôi là WHS Assistant. Tôi có thể tra cứu sản phẩm, tồn kho, vị trí tồn và batch sắp hết hạn bằng dữ liệu thật từ hệ thống.";
    }

    public String help() {
        return """
                Tôi ưu tiên trả lời bằng dữ liệu thật từ WMS và chỉ gọi AI khi thật sự cần.

                Bạn có thể hỏi:
                - Tồn kho SKU-001 còn bao nhiêu?
                - Sản phẩm TV Samsung đang ở kho nào còn hàng?
                - Batch nào sắp hết hạn trong 30 ngày?
                - Thông tin sản phẩm SKU-001
                """;
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
