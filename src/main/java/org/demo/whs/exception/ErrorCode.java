package org.demo.whs.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Authentication errors
    AUTH_001("AUTH_001", "Tên đăng nhập hoặc mật khẩu không đúng"),
    AUTH_002("AUTH_002", "Không tìm thấy người dùng"),
    AUTH_003("AUTH_003", "Bạn không có quyền thực hiện thao tác này"),
    AUTH_004("AUTH_004", "Vui lòng kích hoạt tài khoản trước khi đăng nhập"),
    AUTH_005("AUTH_005", "Token không hợp lệ hoặc đã hết hạn"),
    AUTH_006("AUTH_006", "Refresh token không hợp lệ hoặc đã hết hạn"),
    AUTH_007("AUTH_007", "Tài khoản đã bị khóa do phát hiện hoạt động bất thường"),
    AUTH_008("AUTH_008", "Đăng nhập sai quá nhiều lần, vui lòng thử lại sau"),
    AUTH_009("AUTH_009", "Tài khoản chưa hoạt động"),
    AUTH_010("AUTH_010", "Không tìm thấy tài khoản"),

    // Password reset errors
    RESET_001("RESET_001", "Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"),
    RESET_002("RESET_002", "Token đặt lại mật khẩu đã được sử dụng"),
    RESET_003("RESET_003", "Mật khẩu chưa đáp ứng yêu cầu bảo mật"),
    RESET_004("RESET_004", "Mật khẩu mới không được trùng mật khẩu cũ"),
    RESET_005("RESET_005", "Mật khẩu cũ không đúng"),
    // Role
    ROLE_001("ROLE_001", "Không tìm thấy vai trò"),
    ROLE_002("ROLE_002", "Danh sách quyền không được để trống"),
    ROLE_003("ROLE_003", "Danh sách mã quyền không hợp lệ"),
    ROLE_004("ROLE_004", "Tên vai trò đã tồn tại"),
    ROLE_005("ROLE_005", "Không thể xóa vai trò mặc định"),
    ROLE_006("ROLE_006", "Vai trò đang được người dùng sử dụng"),
    // OTP errors
    OTP_001("OTP_001", "Loại OTP là bắt buộc"),
    OTP_002("OTP_002", "Không tìm thấy tài khoản"),
    OTP_003("OTP_003", "Email đã được xác minh, không thể gửi lại OTP đăng ký"),
    OTP_004("OTP_004", "Bạn đã vượt quá số lần gửi OTP trong ngày"),
    OTP_005("OTP_005", "Vui lòng chờ trước khi gửi lại OTP"),
    OTP_006("OTP_006", "OTP không hợp lệ hoặc đã hết hạn"),

    // Ware House errors
    WHS_001("WHS_001", "Không tìm thấy kho"),
    WHS_002("WHS_002", "Kho không đủ tồn"),
    WHS_003("WHS_003", "Thao tác kho không hợp lệ"),
    WHS_004("WHS_004", "Mã kho đã tồn tại"),

    WH_001("WHS_001", "Không tìm thấy kho"),
    WH_002("WHS_002", "Kho không đủ tồn"),
    WH_003("WHS_003", "Thao tác kho không hợp lệ"),
    WH_004("WHS_004", "Mã kho đã tồn tại"),
    WH_005("WHS_005", "Không thể xóa kho vì vẫn còn vị trí đang hoạt động"),
    WH_006("WHS_006", "Không thể xóa kho vì vẫn còn tồn kho"),
    WH_007("WHS_007", "Kho đã ở trạng thái ngừng hoạt động"),
    WH_008("WHS_008", "Kho đang được dữ liệu khác tham chiếu"),
    INV_001("INV_001", "Không tìm thấy tồn kho"),
    INV_002("INV_002", "Số lượng bỏ giữ chỗ vượt quá số lượng đang giữ chỗ"),
    INV_003("INV_003", "Thông tin chiều tồn kho không hợp lệ"),
    INV_004("INV_004", "Số lượng khả dụng không đủ"),

    STA_001("STA_001", "Yêu cầu điều chỉnh kho không hợp lệ"),
    STA_002("STA_002", "Không thể chuyển sang trạng thái điều chỉnh kho này"),
    STA_003("STA_003", "Lý do từ chối là bắt buộc"),
    STA_404("STA_404", "Không tìm thấy phiếu điều chỉnh kho"),
    STF_001("STF_001", "Không tìm thấy phiếu chuyển kho"),
    STF_002("STF_002", "Không thể chuyển sang trạng thái chuyển kho này"),
    STF_003("STF_003", "Số lượng phải lớn hơn 0"),

    // Location errors
    LOC_001("LOC_001", "Không tìm thấy vị trí kho"),
    LOC_002("LOC_002", "Dữ liệu vị trí kho không hợp lệ"),
    LOC_003("LOC_003", "Mã vị trí đã tồn tại trong kho này"),
    LOC_004("LOC_004", "Kho chưa hoạt động nên không thể tạo vị trí"),
    LOC_005("LOC_005", "Không thể chuyển sang trạng thái vị trí này"),
    LOC_006("LOC_006", "Không thể đổi trạng thái vì vị trí vẫn còn tồn kho"),
    LOC_007("LOC_007", "Vị trí chưa hoạt động để thực hiện chuyển kho"),
    LOC_008("LOC_008", "Loại vị trí không phù hợp để chuyển kho"),
    LOC_009("LOC_009", "Vị trí đã vượt quá sức chứa"),
    LOC_010("LOC_010", "Sức chứa đang sử dụng của vị trí không đủ cho thao tác này"),

    // Product errors
    PROD_001("PROD_001", "Không tìm thấy sản phẩm"),
    PROD_002("PROD_002", "SKU sản phẩm đã tồn tại"),
    PROD_003("PROD_003", "Dữ liệu sản phẩm không hợp lệ"),
    PROD_004("PROD_004", "Không tìm thấy danh mục hoặc danh mục chưa hoạt động"),
    PROD_005("PROD_005", "Không tìm thấy đơn vị tính"),
    PROD_006("PROD_006", "Không thể tắt theo dõi lô vì vẫn còn tồn kho theo lô"),
    PROD_007("PROD_007", "Mức tồn tối đa phải lớn hơn hoặc bằng mức tồn tối thiểu"),
    PROD_008("PROD_008", "Điểm đặt hàng lại phải nằm giữa mức tồn tối thiểu và tối đa"),

    // Category errors
    CAT_001("CAT_001", "Không tìm thấy danh mục"),
    CAT_002("CAT_002", "Mã hoặc tên danh mục đã tồn tại"),

    // UnitsOfMeasure errors
    UOM_001("UOM_001", "Không tìm thấy đơn vị tính"),
    UOM_002("UOM_002", "Mã đơn vị tính đã tồn tại"),
    UOM_003("UOM_003", "Dữ liệu đơn vị tính không hợp lệ"),
    UOM_004("UOM_004", "Đơn vị tính đang được sản phẩm sử dụng"),

    // Employee errors
    EMP_001("EMP_001", "Không tìm thấy nhân viên"),
    EMP_002("EMP_002", "Mã nhân viên đã tồn tại"),
    EMP_003("EMP_003", "Tài khoản đã được liên kết với nhân viên"),
    EMP_004("EMP_004", "Không tìm thấy tài khoản"),
    EMP_005("EMP_005", "Không thể chuyển sang trạng thái nhân viên này"),
    EMP_006("EMP_006", "Không tìm thấy kho hoặc kho chưa hoạt động"),

    // Business Partner errors
    BP_001("BP_001", "Không tìm thấy đối tác"),
    BP_002("BP_002", "Mã đối tác đã tồn tại"),
    BP_003("BP_003", "Trạng thái đối tác không hợp lệ"),
    BP_004("BP_004", "Đối tác chưa hoạt động"),

    // Customer errors
    CUST_001("CUST_001", "Không tìm thấy khách hàng"),
    CUST_002("CUST_002", "Mã khách hàng đã tồn tại"),
    CUST_003("CUST_003", "Trạng thái khách hàng không hợp lệ"),
    CUST_004("CUST_004", "Đối tác không thuộc loại khách hàng"),

    // Purchase order errors
    PO_001("PO_001", "Không tìm thấy đơn mua hàng"),
    PO_002("PO_002", "Chỉ có thể chỉnh sửa đơn mua hàng ở trạng thái nháp"),
    PO_003("PO_003", "Không thể chuyển đơn mua hàng sang trạng thái này"),

    // Purchase order line errors
    POL_001("POL_001", "Không tìm thấy dòng đơn mua hàng"),
    POL_002("POL_002", "Số lượng của dòng đơn mua hàng phải lớn hơn 0"),
    POL_003("POL_003", "Không tìm thấy sản phẩm của dòng đơn mua hàng"),
    POL_004("POL_004", "Sản phẩm của dòng đơn mua hàng chưa hoạt động"),
    POL_005("POL_005", "Sản phẩm đã có trong đơn mua hàng này"),
    POL_006("POL_006", "Số lượng đã nhận không được vượt quá số lượng đặt"),
    POL_007("POL_007", "Đơn giá phải lớn hơn hoặc bằng 0"),
    POL_008("POL_008", "Số lượng đặt phải lớn hơn 0"),

    // OrderLine errors
    ORDER_001("ORDER_001", "Không tìm thấy dòng đơn hàng"),
    // Background job errors
    JOB_001("JOB_001", "Không tìm thấy tác vụ nền"),
    JOB_002("JOB_002", "Không thể chuyển tác vụ nền sang trạng thái này"),
    JOB_003("JOB_003", "Không thể chạy lại tác vụ nền này"),
    JOB_004("JOB_004", "Không thể hủy tác vụ nền này"),
    JOB_005("JOB_005", "Không tìm thấy bộ xử lý tác vụ nền"),
    JOB_006("JOB_006", "Định dạng xuất dữ liệu không được hỗ trợ"),
    JOB_007("JOB_007", "Loại báo cáo không được hỗ trợ"),
    JOB_008("JOB_008", "Dữ liệu tác vụ nền không hợp lệ"),

    // Common errors
    COM_001("COM_001", "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại"),
    COM_002("COM_002", "Hệ thống gặp sự cố, vui lòng thử lại sau"),
    COM_003("COM_003", "Dữ liệu gửi lên sai định dạng"),
    COM_004("COM_004", "Không tìm thấy dữ liệu yêu cầu"),
    COM_005("COM_005", "Dữ liệu đã tồn tại"),
    COM_006("COM_006", "Số trang không được nhỏ hơn 0"),
    COM_007("COM_007", "Kích thước trang phải lớn hơn 0"),
    COM_008("COM_008", "Kích thước trang không được vượt quá 100"),
    COM_009("COM_009", "Dữ liệu đang được tiến trình khác xử lý, vui lòng thử lại sau"),
    COM_010("COM_010", "Thao tác bị gián đoạn bởi tiến trình khác"),
    COM_011("COM_011", "Phương thức yêu cầu không được hỗ trợ"),
    COM_012("COM_012", "Định dạng nội dung yêu cầu không được hỗ trợ"),
    COM_013("COM_013", "Thiếu tham số bắt buộc trong yêu cầu"),

    // Batch errors
    BATCH_001("BATCH_001", "Không tìm thấy lô hàng"),
    BATCH_002("BATCH_002", "Số lô đã tồn tại cho sản phẩm này"),
    BATCH_003("BATCH_003", "Không thể chuyển lô hàng sang trạng thái này"),
    BATCH_004("BATCH_004", "Lô hàng đã hết hạn"),
    BATCH_005("BATCH_005", "Ngày sản xuất không được ở tương lai"),
    BATCH_006("BATCH_006", "Ngày hết hạn phải sau ngày sản xuất"),
    BATCH_007("BATCH_007", "Không thể cách ly lô vì vẫn còn tồn kho được giữ chỗ"),
    BATCH_008("BATCH_008", "Không thể đưa lô hết hạn trở lại sử dụng"),
    BATCH_009("BATCH_009", "Sản phẩm không hỗ trợ theo dõi theo lô"),
    BATCH_010("BATCH_010", "Không thể đổi sản phẩm của lô sau khi tạo"),
    BATCH_011("BATCH_011", "Không thể cập nhật trạng thái lô qua API này"),
    BATCH_012("BATCH_012", "Lô không khả dụng cho thao tác kho"),
    BATCH_013("BATCH_013", "Lô đã được cách ly"),
    BATCH_014("BATCH_014", "Không thể cách ly lô đã thu hồi"),
    BATCH_015("BATCH_015", "Chỉ lô khả dụng mới có thể được cách ly"),
    BATCH_016("BATCH_016", "Không thể đưa lô trở lại sử dụng vì lô không ở trạng thái cách ly"),
    BATCH_017("BATCH_017", "Không thể đưa lô hết hạn trở lại sử dụng"),
    BATCH_018("BATCH_018", "Không thể đưa lô đã thu hồi trở lại sử dụng"),
    BATCH_019("BATCH_019", "Khoảng ngày lọc lô hàng không hợp lệ"),
    BATCH_020("BATCH_020", "Lô hàng đã hết hạn"),

    // Inbound Receipt Line errors
    IRL_001("IRL_001", "Không tìm thấy dòng phiếu nhập"),
    IRL_002("IRL_002", "Không tìm thấy phiếu nhập"),
    IRL_003("IRL_003", "Chỉ có thể sửa dòng khi phiếu nhập ở trạng thái nháp"),
    IRL_004("IRL_004", "Dòng đơn mua hàng không thuộc phiếu nhập này"),
    IRL_005("IRL_005", "Sản phẩm chưa hoạt động"),
    IRL_006("IRL_006", "Vị trí không thuộc kho của phiếu nhập"),
    IRL_007("IRL_007", "Vị trí không khả dụng vì đang ngừng hoạt động hoặc bảo trì"),
    IRL_008("IRL_008", "Sản phẩm theo dõi theo lô bắt buộc phải chọn lô"),
    IRL_009("IRL_009", "Sản phẩm không theo dõi theo lô nên không được chọn lô"),
    IRL_010("IRL_010", "Lô không phù hợp với trạng thái chất lượng"),
    IRL_011("IRL_011", "Không thể sử dụng lô đã hết hạn hoặc bị thu hồi"),
    IRL_012("IRL_012", "Số lượng vượt quá phần còn lại của dòng đơn mua hàng"),
    IRL_013("IRL_013", "Thông tin phân tách này đã tồn tại trong phiếu nhập"),
    IRL_014("IRL_014", "Trạng thái cách ly bắt buộc phải có ghi chú"),
    IRL_015("IRL_015", "Trạng thái chất lượng không hợp lệ"),
    IRL_016("IRL_016", "Không tìm thấy dòng đơn mua hàng"),

    // Rate limiting errors
    RATE_LIMIT_EXCEEDED("RATE_001", "Bạn thao tác quá nhanh, vui lòng thử lại sau"),

    // Email errors
    EMAIL_NOT_FOUND("EMAIL_001", "Không tìm thấy lịch sử email"),
    EMAIL_002("EMAIL_002", "Email đã được xác minh hoặc không hợp lệ"),

    // Storage (MinIO) errors
    STORAGE_001("STORAGE_001", "Tải tệp lên thất bại"),
    STORAGE_002("STORAGE_002", "Không tìm thấy tệp trong kho lưu trữ"),
    STORAGE_003("STORAGE_003", "Xóa tệp thất bại"),
    STORAGE_004("STORAGE_004", "Không thể tạo đường dẫn truy cập tệp"),
    STORAGE_005("STORAGE_005", "Loại tệp không được hỗ trợ"),
    STORAGE_006("STORAGE_006", "Dung lượng tệp vượt quá giới hạn cho phép"),

    // Permission errors
    PERM_001("PERM_001", "Không tìm thấy quyền"),
    PERM_002("PERM_002", "Tên quyền đã tồn tại"),
    PERM_003("PERM_003", "Quyền đã được gán cho vai trò"),
    PERM_004("PERM_004", "Dữ liệu quyền không hợp lệ"),

    PERM_005("PERM_005", "Mã quyền đã tồn tại"),
    PERM_006("PERM_006", "Quyền cho tài nguyên và hành động này đã tồn tại"),
    PERM_007("PERM_007", "Quyền đang được vai trò sử dụng"),
    PERM_008("PERM_008", "Quyền chưa được gán cho vai trò"),
    PERM_009("PERM_009", "Tài nguyên không hợp lệ"),
    PERM_010("PERM_010", "Hành động không hợp lệ"),
    PERM_011("PERM_011", "Không thể thay đổi mã quyền"),
    PERM_012("PERM_012", "Không thể thay đổi tài nguyên của quyền"),
    PERM_013("PERM_013", "Permission source is temporarily unavailable"),
    //User Role errors
    USER_ROLE_001("USER_ROLE_001", "Không tìm thấy người dùng"),
    USER_ROLE_002("USER_ROLE_002", "Không tìm thấy vai trò cần gán"),
    USER_ROLE_003("USER_ROLE_003", "Vai trò đã được gán cho người dùng"),
    USER_ROLE_004("USER_ROLE_004", "Người dùng không có vai trò này"),
    USER_ROLE_005("USER_ROLE_005", "Danh sách vai trò cần gán không được để trống"),
    USER_ROLE_006("USER_ROLE_006", "Danh sách mã vai trò không hợp lệ"),
    USER_ROLE_007("USER_ROLE_007", "Không tìm thấy liên kết người dùng và vai trò"),
    USER_ROLE_008("USER_ROLE_008", "Không thể gỡ vai trò khỏi người dùng");

    // Role Permission errors

    private final String code;
    private final String message;

    public static ErrorCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}
