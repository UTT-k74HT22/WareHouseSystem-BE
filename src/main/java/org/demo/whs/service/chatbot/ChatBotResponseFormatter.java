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
        return "Chao ban. Toi la WHS Assistant. Toi co the tra cuu san pham, ton kho, vi tri ton va batch sap het han bang du lieu that tu he thong.";
    }

    public String help() {
        return """
                Toi uu tien tra loi bang du lieu that tu WMS va chi goi AI khi that su can.

                Ban co the hoi:
                - Ton kho SKU-001 con bao nhieu?
                - San pham TV Samsung dang o kho nao con hang?
                - Batch nao sap het han trong 30 ngay?
                - Thong tin san pham SKU-001
                """;
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
                De tranh tra loi sai, hay hoi theo mot trong cac mau:
                - Ton kho [SKU/ten san pham]
                - San pham [SKU/ten]
                - Batch sap het han [so ngay]
                """;
    }

    public String aiFallbackUnavailable() {
        return "Toi khong the su dung AI fallback luc nay. Ban hay hoi theo SKU, ten san pham hoac batch cu the de toi tra du lieu that tu he thong.";
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
