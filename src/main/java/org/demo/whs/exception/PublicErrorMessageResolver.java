package org.demo.whs.exception;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class PublicErrorMessageResolver {

    private static final Map<String, String> EXACT_MESSAGES = Map.ofEntries(
            Map.entry("Missing idempotency key", "Thiếu khóa chống gửi yêu cầu trùng lặp"),
            Map.entry("Duplicate request", "Yêu cầu này đã được xử lý trước đó"),
            Map.entry("Invalid pagination parameters", "Tham số phân trang không hợp lệ"),
            Map.entry("Unauthenticated request", "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"),
            Map.entry("Product ID mismatch", "Sản phẩm không khớp với dữ liệu yêu cầu"),
            Map.entry("Warehouse ID mismatch", "Kho không khớp với dữ liệu yêu cầu"),
            Map.entry("Location does not belong to warehouse", "Vị trí không thuộc kho đã chọn"),
            Map.entry("Locations must belong to the same warehouse", "Các vị trí phải thuộc cùng một kho"),
            Map.entry("Source and destination locations must be different", "Vị trí nguồn và vị trí đích phải khác nhau"),
            Map.entry("Transfer locations must belong to the provided warehouse", "Vị trí chuyển phải thuộc kho đã chọn"),
            Map.entry("Batch does not belong to the provided product", "Lô hàng không thuộc sản phẩm đã chọn"),
            Map.entry("Quantity must be > 0", "Số lượng phải lớn hơn 0"),
            Map.entry("Quantity must be greater than 0", "Số lượng phải lớn hơn 0"),
            Map.entry("Unit price cannot be negative", "Đơn giá không được nhỏ hơn 0"),
            Map.entry("Rejection reason is required", "Lý do từ chối là bắt buộc"),
            Map.entry("User account not found", "Không tìm thấy tài khoản người dùng"),
            Map.entry("User role not found", "Không tìm thấy vai trò của người dùng"),
            Map.entry("Only draft transfer can be submitted", "Chỉ phiếu chuyển kho ở trạng thái nháp mới có thể gửi duyệt"),
            Map.entry("Only pending transfer can be completed", "Chỉ phiếu chuyển kho đang chờ xử lý mới có thể hoàn tất"),
            Map.entry("Only draft or pending transfer can be cancelled", "Chỉ phiếu chuyển kho nháp hoặc đang chờ mới có thể hủy"),
            Map.entry("Sales order must have at least one line", "Đơn bán hàng phải có ít nhất một dòng sản phẩm"),
            Map.entry("Only draft or confirmed sales orders can be cancelled", "Chỉ đơn bán hàng nháp hoặc đã xác nhận mới có thể hủy"),
            Map.entry("Cannot cancel sales order with active shipments", "Không thể hủy đơn bán hàng đang có phiếu xuất hoạt động"),
            Map.entry("Purchase order line does not belong to this receipt", "Dòng đơn mua hàng không thuộc phiếu nhập này")
    );

    private static final Map<String, String> PHRASE_MESSAGES = createPhraseMessages();

    public String resolve(String errorCode, String message) {
        ErrorCode knownError = ErrorCode.fromCode(errorCode);
        if (message == null || message.isBlank()) {
            return knownError != null ? knownError.getMessage() : ErrorCode.COM_002.getMessage();
        }
        if (containsVietnameseCharacter(message)) {
            return message;
        }

        String exactMessage = EXACT_MESSAGES.get(message);
        if (exactMessage != null) {
            return exactMessage;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : PHRASE_MESSAGES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return knownError != null ? knownError.getMessage() : ErrorCode.COM_002.getMessage();
    }

    private static Map<String, String> createPhraseMessages() {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("not enough available stock", "Số lượng tồn khả dụng không đủ");
        messages.put("insufficient available stock", "Số lượng tồn khả dụng không đủ");
        messages.put("not enough reserved stock", "Số lượng tồn đã giữ chỗ không đủ");
        messages.put("not found at source", "Không tìm thấy tồn kho tại vị trí nguồn");
        messages.put("source location not found", "Không tìm thấy vị trí nguồn");
        messages.put("destination location not found", "Không tìm thấy vị trí đích");
        messages.put("inventory not found", "Không tìm thấy tồn kho");
        messages.put("warehouse not found", "Không tìm thấy kho");
        messages.put("location not found", "Không tìm thấy vị trí kho");
        messages.put("product not found", "Không tìm thấy sản phẩm");
        messages.put("batch not found", "Không tìm thấy lô hàng");
        messages.put("customer not found", "Không tìm thấy khách hàng");
        messages.put("supplier not found", "Không tìm thấy nhà cung cấp");
        messages.put("employee not found", "Không tìm thấy nhân viên");
        messages.put("sales order not found", "Không tìm thấy đơn bán hàng");
        messages.put("purchase order not found", "Không tìm thấy đơn mua hàng");
        messages.put("inbound receipt not found", "Không tìm thấy phiếu nhập");
        messages.put("outbound shipment not found", "Không tìm thấy phiếu xuất");
        messages.put("stock adjustment not found", "Không tìm thấy phiếu điều chỉnh kho");
        messages.put("stock transfer not found", "Không tìm thấy phiếu chuyển kho");
        messages.put("already exists", "Dữ liệu đã tồn tại");
        messages.put("cannot be negative", "Giá trị không được nhỏ hơn 0");
        messages.put("must be greater than zero", "Giá trị phải lớn hơn 0");
        messages.put("must be greater than 0", "Giá trị phải lớn hơn 0");
        messages.put("is required", "Thiếu thông tin bắt buộc");
        messages.put("invalid status transition", "Không thể chuyển sang trạng thái này");
        messages.put("status transition is not allowed", "Không thể chuyển sang trạng thái này");
        messages.put("does not belong", "Dữ liệu không thuộc phạm vi đã chọn");
        messages.put("not active", "Dữ liệu chưa ở trạng thái hoạt động");
        messages.put("permission", "Bạn không có quyền thực hiện thao tác này");
        messages.put("concurrent", "Dữ liệu vừa được tiến trình khác cập nhật, vui lòng tải lại");
        return messages;
    }

    private boolean containsVietnameseCharacter(String value) {
        return value.matches(".*[À-ỹ].*");
    }
}
