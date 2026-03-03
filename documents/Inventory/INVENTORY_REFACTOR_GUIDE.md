# Inventory Module Refactor Guide (Technical Documentation)

## 1. Tổng quan (Overview)
Mô-đun Inventory đã được refactor từ mô hình **Rich Entity (JPA Relationships)** sang mô hình **Thin Entity (ID-based)**. Sự thay đổi này nhằm mục tiêu tối ưu hóa hiệu năng, tránh lỗi N+1 và đồng bộ hóa với phong cách lập trình (codebase style) của toàn bộ dự án.

## 2. Những thay đổi chính (Core Changes)

### 2.1. Entity: Từ Rich sang Thin
*   **Trước đây:** Sử dụng các annotation `@ManyToOne` kèm theo `@JoinColumn(insertable = false, updatable = false)`.
*   **Hiện tại:** Gỡ bỏ hoàn toàn các Object Relationship (`Products`, `Warehouses`, `Locations`, `Batch`). Chỉ giữ lại các trường ID dạng `String` (`productId`, `warehouseId`, `locationId`, `batchId`).
*   **Lý do:** Giảm bớt gánh nặng cho Hibernate Persistence Context, tránh việc tự động JOIN không kiểm soát và đồng bộ với các entity khác như `Products`, `Warehouses`.
*   **Optimistic Locking:** Gỡ bỏ trường `@Version` để thống nhất với tiêu chuẩn chung của dự án (không sử dụng Versioning trừ khi có yêu cầu đặc thù).

### 2.2. Service: Chiến lược "Bulk Mapping"
Để thay thế cho việc `JOIN FETCH` trong JPA, chúng ta sử dụng chiến lược **Bulk Mapping** (Application-level Join) trong `InventoryServiceImpl`:

1.  **Bước 1 (Fetch Page):** Truy vấn danh sách `Inventory` theo phân trang (chỉ lấy ID và các trường số lượng). Đây là query cực nhanh trên 1 bảng duy nhất.
2.  **Bước 2 (Collect IDs):** Thu thập tất cả các ID duy nhất của Product, Warehouse, Location, Batch từ danh sách vừa lấy được bằng `Set<String>`.
3.  **Bước 3 (Bulk Fetch):** Sử dụng các Repository tương ứng gọi `findAllById(ids)` để lấy thông tin chi tiết của các thực thể liên quan chỉ bằng **duy nhất 1 query trên mỗi bảng**.
4.  **Bước 4 (Mapping):** Sử dụng `InventoryMapper` kết hợp với các `Map` dữ liệu vừa fetch để điền thông tin (SKU, Name, Code,...) vào `InventoryResponse`.

**Lợi ích:** 
*   Tránh triệt để lỗi N+1.
*   Hiệu năng ổn định (Tổng số query luôn là `1 + K` với K là số bảng liên quan, bất kể page size là 10 hay 100).

### 2.3. Specification: Sử dụng Subqueries
Vì không còn quan hệ JPA trực tiếp, việc filter theo các thuộc tính của bảng liên quan (như `productSku`, `productName`, `batchNumber`) được thực hiện thông qua **JPA Subqueries**.

```java
// Ví dụ Subquery cho Product SKU/Name
Subquery<String> productSubquery = query.subquery(String.class);
var productRoot = productSubquery.from(Products.class);
productSubquery.select(productRoot.get("id"));
// ... add predicates for SKU/Name
predicates = cb.and(predicates, root.get("productId").in(productSubquery));
```

### 2.4. Controller & Sorting
*   **Sorting:** Gỡ bỏ khả năng sắp xếp theo `availableQuantity`.
*   **Lý do:** `availableQuantity` là một **Transient Field** (trường tính toán trong Java), không tồn tại trong Database nên không thể thực hiện `ORDER BY` trực tiếp. Việc cho phép sort theo trường này sẽ gây lỗi `PropertyReferenceException`.

## 3. Hướng dẫn bảo trì (Maintenance Guide)

### Khi cần thêm thông tin vào Response:
1.  Thêm field vào `InventoryResponse`.
2.  Cập nhật `InventoryServiceImpl` để thu thập ID và fetch thêm dữ liệu từ Repository mới (nếu cần).
3.  Cập nhật `InventoryMapper.toResponse` để map dữ liệu từ Map vào DTO.

### Khi cần thêm tiêu chí lọc (Filter):
1.  Thêm field vào `InventoryFilterRequest`.
2.  Cập nhật `InventorySpecification` bằng cách sử dụng `Subquery` nếu tiêu chí lọc nằm ở bảng khác, hoặc `cb.equal/like` nếu tiêu chí nằm trực tiếp trong bảng `inventory`.

## 4. Kết luận
Cấu trúc mới giúp hệ thống Warehouse vận hành mượt mà hơn khi lượng dữ liệu tồn kho tăng lên hàng triệu bản ghi, đồng thời giữ cho code sạch sẽ, dễ hiểu và tuân thủ đúng kiến trúc "Thin Entity" của dự án.
