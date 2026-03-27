-- Theo dõi số lượng hàng thực tế đang chiếm chỗ trong một location, thay vì phải đi SUM(inventory) mỗi lần cần kiểm tra.
-- Đây là kỹ thuật denormalization để tăng hiệu năng — thay vì query tổng hợp tốn kém, chỉ cần đọc 1 cột.
ALTER TABLE locations
    ADD COLUMN used_capacity DECIMAL(15, 2) NOT NULL DEFAULT 0
    COMMENT 'Current number of items stored in this location'
AFTER capacity;

-- ISUM(on_hand_quantity + reserved_quantity) AS total_quantity
-- Với dữ liệu **đã tồn tại trước khi migration**, cột mới sẽ mặc định là `0` — sai so với thực tế.
-- Lệnh `UPDATE` này tính lại từ bảng `inventory` hiện có để đảm bảo `used_capacity` **khớp ngay từ đầu**, không bị lệch.
-- Dùng `LEFT JOIN + COALESCE` để xử lý location chưa có inventory → giữ nguyên `0`.

-- ### Tại sao cần cả 2 trường `on_hand + reserved`?
--
-- | Loại                | Ý nghĩa |
-- | `on_hand_quantity` | Hàng có thể dùng tự do |
-- | `reserved_quantity` | Hàng đã lock cho đơn hàng, nhưng **vẫn đang chiếm chỗ vật lý** |
-- → Cả hai đều đang **nằm trong kho**, nên đều tính vào `used_capacity`.
-- ### Vòng đời sau migration
-- ```
-- Nhập kho   → used_capacity tăng
-- Xuất kho   → used_capacity giảm
-- Cancel     → used_capacity revert
UPDATE locations l
    LEFT JOIN (
    SELECT
    location_id,
    SUM(on_hand_quantity + reserved_quantity) AS total_quantity
    FROM inventory
    GROUP BY location_id
    ) inv ON l.id = inv.location_id
    SET l.used_capacity = COALESCE(inv.total_quantity, 0);