# TÀI LIỆU USECASE HỆ THỐNG QUẢN LÝ KHO (WMS)

---

## MỤC LỤC

1. [Quản lý xác thực & phân quyền](#1-quản-lý-xác-thực--phân-quyền)
2. [Quản lý người dùng & vai trò](#2-quản-lý-người-dùng--vai-trò)
3. [Quản lý nhân viên](#3-quản-lý-nhân-viên)
4. [Quản lý kho hàng](#4-quản-lý-kho-hàng)
5. [Quản lý vị trí trong kho](#5-quản-lý-vị-trí-trong-kho)
6. [Quản lý sản phẩm](#6-quản-lý-sản-phẩm)
7. [Quản lý danh mục sản phẩm](#7-quản-lý-danh-mục-sản-phẩm)
8. [Quản lý đơn vị tính](#8-quản-lý-đơn-vị-tính)
9. [Quản lý đối tác kinh doanh](#9-quản-lý-đối-tác-kinh-doanh)
10. [Quản lý lô hàng (Batch)](#10-quản-lý-lô-hàng-batch)
11. [Quản lý tồn kho](#11-quản-lý-tồn-kho)
12. [Nghiệp vụ nhập kho](#12-nghiệp-vụ-nhập-kho)
13. [Nghiệp vụ xuất kho](#13-nghiệp-vụ-xuất-kho)
14. [Điều chỉnh tồn kho](#14-điều-chỉnh-tồn-kho)
15. [Chuyển kho](#15-chuyển-kho)
16. [Lịch sử di chuyển tồn kho](#16-lịch-sử-di-chuyển-tồn-kho)
17. [Quản lý tệp tin (MinIO)](#17-quản-lý-tệp-tin-minio)
18. [Quản lý email](#18-quản-lý-email)
19. [Báo cáo & xuất dữ liệu](#19-báo-cáo--xuất-dữ-liệu)
20. [Dashboard](#20-dashboard)
21. [Quản lý tác vụ nền](#21-quản-lý-tác-vụ-nền)
22. [Chatbot AI](#22-chatbot-ai)

---

## 1. QUẢN LÝ XÁC THỰC & PHÂN QUYỀN

**Mô tả:** Quản lý đăng nhập, đăng ký, xác thực người dùng và bảo mật tài khoản.

### 1.1 Đăng nhập
- **Actor:** Người dùng hệ thống
- **Mô tả:** Người dùng đăng nhập bằng tên đăng nhập và mật khẩu
- **Kết quả:** Trả về JWT access token và refresh token
- **Ràng buộc:** Giới hạn 5 lần/giây/IP

### 1.2 Làm mới token
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Làm mới access token khi hết hạn bằng refresh token
- **Ràng buộc:** Giới hạn 10 lần/phút/người dùng

### 1.3 Đăng ký tài khoản
- **Actor:** Người dùng mới
- **Mô tả:** Tạo tài khoản mới với thông tin đăng ký

### 1.4 Quên mật khẩu
- **Actor:** Người dùng
- **Mô tả:** Gửi OTP về email để đặt lại mật khẩu
- **Ràng buộc:** Giới hạn 3 lần/15 phút/IP

### 1.5 Xác thực OTP quên mật khẩu
- **Actor:** Người dùng
- **Mô tả:** Xác minh OTP nhận được qua email
- **Ràng buộc:** Giới hạn 5 lần thử/15 phút/IP

### 1.6 Đặt lại mật khẩu
- **Actor:** Người dùng
- **Mô tả:** Đặt mật khẩu mới sau khi xác thực OTP thành công
- **Ràng buộc:** Giới hạn 3 lần/15 phút/IP

### 1.7 Đổi mật khẩu
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Thay đổi mật khẩu hiện tại của tài khoản

### 1.8 Kiểm tra quyền
- **Actor:** Hệ thống
- **Mô tả:** Kiểm tra người dùng hiện tại có quyền thực hiện hành động cụ thể không

### 1.9 Xem quyền của tôi
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả quyền hạn được gán cho tài khoản hiện tại

### 1.10 Gửi OTP
- **Actor:** Người dùng
- **Mô tả:** Gửi OTP đến email (công khai, không yêu cầu đăng nhập)

### 1.11 Xác thực OTP
- **Actor:** Người dùng
- **Mô tả:** Xác minh mã OTP cho email và loại OTP cụ thể

---

## 2. QUẢN LÝ NGƯỜI DÙNG & VAI TRÒ

**Mô tả:** Quản lý tài khoản, vai trò (Role), quyền hạn (Permission) và gán quyền cho người dùng theo mô hình RBAC.

### 2.1 Quản lý vai trò (Role)

#### 2.1.1 Tạo vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Tạo vai trò mới với tên và mô tả

#### 2.1.2 Xem danh sách vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách phân trang các vai trò, có thể tìm kiếm và lọc theo mặc định

#### 2.1.3 Xem chi tiết vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xem thông tin chi tiết của một vai trò theo ID

#### 2.1.4 Cập nhật vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa tên và mô tả của vai trò

#### 2.1.5 Xóa vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xóa mềm vai trò (soft delete)

#### 2.1.6 Xem người dùng theo vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách phân trang người dùng được gán một vai trò cụ thể

### 2.2 Quản lý quyền hạn (Permission)

#### 2.2.1 Tạo quyền hạn
- **Actor:** Quản trị viên
- **Mô tả:** Tạo quyền mới (tài nguyên + hành động)

#### 2.2.2 Xem danh sách quyền hạn
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách phân trang, lọc theo tài nguyên, hành động, từ khóa

#### 2.2.3 Xem chi tiết quyền hạn
- **Actor:** Quản trị viên
- **Mô tả:** Xem thông tin chi tiết của một quyền hạn

#### 2.2.4 Cập nhật quyền hạn
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa tài nguyên hoặc hành động của quyền hạn

#### 2.2.5 Xóa quyền hạn
- **Actor:** Quản trị viên
- **Mô tả:** Xóa quyền hạn khỏi hệ thống

### 2.3 Gán quyền cho vai trò

#### 2.3.1 Gán nhiều quyền cho vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Gán hàng loạt quyền hạn cho một vai trò

#### 2.3.2 Gỡ quyền khỏi vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xóa một quyền hạn khỏi vai trò

#### 2.3.3 Xem quyền của vai trò
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách quyền hạn của một vai trò, lọc theo tài nguyên

### 2.4 Gán vai trò cho người dùng

#### 2.4.1 Gán nhiều vai trò cho người dùng
- **Actor:** Quản trị viên
- **Mô tả:** Gán hàng loạt vai trò cho một tài khoản

#### 2.4.2 Gỡ vai trò khỏi người dùng
- **Actor:** Quản trị viên
- **Mô tả:** Xóa một vai trò khỏi tài khoản người dùng

#### 2.4.3 Xem vai trò của người dùng
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách vai trò được gán cho một người dùng

### 2.5 Quản lý người dùng

#### 2.5.1 Xem danh sách quản lý
- **Actor:** Quản trị viên
- **Mô tả:** Xem tất cả người dùng có vai trò Manager

#### 2.5.2 Xem chi tiết người dùng
- **Actor:** Quản trị viên
- **Mô tả:** Xem thông tin tài khoản theo ID

---

## 3. QUẢN LÝ NHÂN VIÊN

**Mô tả:** Quản lý hồ sơ nhân viên làm việc trong kho.

### 3.1 Tạo nhân viên
- **Actor:** Quản trị viên / Quản lý
- **Mô tả:** Tạo hồ sơ nhân viên mới với mã nhân viên, tên, kho phụ trách

### 3.2 Xem danh sách nhân viên
- **Actor:** Quản trị viên / Quản lý
- **Mô tả:** Xem danh sách phân trang, lọc theo từ khóa, trạng thái, kho

### 3.3 Xem chi tiết nhân viên
- **Actor:** Quản trị viên / Quản lý
- **Mô tả:** Xem thông tin chi tiết nhân viên theo ID

### 3.4 Cập nhật nhân viên
- **Actor:** Quản trị viên / Quản lý
- **Mô tả:** Chỉnh sửa thông tin nhân viên

### 3.5 Xóa nhân viên
- **Actor:** Quản trị viên / Quản lý
- **Mô tả:** Xóa mềm nhân viên khỏi hệ thống

---

## 4. QUẢN LÝ KHO HÀNG

**Mô tả:** Quản lý thông tin các kho hàng trong hệ thống.

### 4.1 Tạo kho hàng
- **Actor:** Quản trị viên
- **Mô tả:** Tạo kho mới với tên, địa chỉ, loại kho

### 4.2 Xem danh sách kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả kho hàng

### 4.3 Xem tất cả kho (không phân trang)
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phẳng tất cả kho (dùng cho dropdown)

### 4.4 Xem chi tiết kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin chi tiết kho theo ID

### 4.5 Cập nhật kho
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa thông tin kho hàng

### 4.6 Thay đổi trạng thái kho
- **Actor:** Quản trị viên
- **Mô tả:** Chuyển trạng thái kho (ACTIVE, INACTIVE)

### 4.7 Xóa kho
- **Actor:** Quản trị viên
- **Mô tả:** Xóa kho hàng khỏi hệ thống

---

## 5. QUẢN LÝ VỊ TRÍ TRONG KHO

**Mô tả:** Quản lý vị trí lưu trữ hàng hóa trong kho (lối, kệ, ô, ngăn).

### 5.1 Tạo vị trí
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Tạo vị trí mới trong kho (lối, kệ, bin, v.v.)

### 5.2 Xem danh sách vị trí
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả vị trí

### 5.3 Xem chi tiết vị trí
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin vị trí theo ID

### 5.4 Xem vị trí theo kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách vị trí trong một kho cụ thể

### 5.5 Tìm kiếm vị trí
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tìm kiếm theo nhiều tiêu chí: kho, mã, tên, zone, loại, trạng thái

### 5.6 Cập nhật vị trí
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Chỉnh sửa thông tin vị trí

### 5.7 Thay đổi trạng thái vị trí
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Chuyển trạng thái (AVAILABLE, OCCUPIED, BLOCKED)

### 5.8 Xóa vị trí
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Xóa mềm vị trí khỏi hệ thống

---

## 6. QUẢN LÝ SẢN PHẨM

**Mô tả:** Quản lý danh mục sản phẩm lưu trữ trong kho.

### 6.1 Tạo sản phẩm
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Tạo sản phẩm mới với SKU, tên, danh mục, đơn vị tính, cờ theo dõi lô

### 6.2 Cập nhật sản phẩm
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Chỉnh sửa thông tin sản phẩm

### 6.3 Xem chi tiết sản phẩm
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin sản phẩm theo ID

### 6.4 Tìm sản phẩm theo SKU
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tra cứu sản phẩm bằng mã SKU (không phân biệt hoa thường)

### 6.5 Xem danh sách sản phẩm
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả sản phẩm

### 6.6 Tìm kiếm nâng cao
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tìm kiếm sản phẩm với nhiều bộ lọc

### 6.7 Xóa sản phẩm
- **Actor:** Quản trị viên / Quản lý kho
- **Mô tả:** Xóa mềm sản phẩm

### 6.8 Xem sản phẩm theo danh mục
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách sản phẩm thuộc một danh mục cụ thể

### 6.9 Xem sản phẩm theo dõi lô
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách sản phẩm yêu cầu theo dõi lô (batch tracking)

---

## 7. QUẢN LÝ DANH MỤC SẢN PHẨM

**Mô tả:** Quản lý phân loại sản phẩm.

### 7.1 Tạo danh mục
- **Actor:** Quản trị viên
- **Mô tả:** Tạo danh mục sản phẩm mới

### 7.2 Xem danh sách danh mục
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo trạng thái

### 7.3 Xem chi tiết danh mục
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin danh mục theo ID

### 7.4 Cập nhật danh mục
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa thông tin danh mục

### 7.5 Thay đổi trạng thái danh mục
- **Actor:** Quản trị viên
- **Mô tả:** Kích hoạt / vô hiệu hóa danh mục

---

## 8. QUẢN LÝ ĐƠN VỊ TÍNH

**Mô tả:** Quản lý đơn vị đo lường sản phẩm (PCS, KG, L, v.v.).

### 8.1 Tạo đơn vị tính
- **Actor:** Quản trị viên
- **Mô tả:** Tạo đơn vị tính mới

### 8.2 Xem tất cả đơn vị tính
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách tất cả đơn vị tính (không phân trang)

### 8.3 Xem chi tiết đơn vị tính
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin đơn vị tính theo ID

### 8.4 Cập nhật đơn vị tính
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa thông tin đơn vị tính

### 8.5 Xóa đơn vị tính
- **Actor:** Quản trị viên
- **Mô tả:** Xóa cứng đơn vị tính (không được xóa nếu đang được sản phẩm tham chiếu)

---

## 9. QUẢN LÝ ĐỐI TÁC KINH DOANH

**Mô tả:** Quản lý nhà cung cấp và khách hàng.

### 9.1 Tạo đối tác
- **Actor:** Quản trị viên
- **Mô tả:** Tạo đối tác mới (nhà cung cấp, khách hàng, hoặc cả hai)

### 9.2 Xem tất cả đối tác
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách tất cả đối tác

### 9.3 Xem chi tiết đối tác
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin đối tác theo ID

### 9.4 Cập nhật đối tác
- **Actor:** Quản trị viên
- **Mô tả:** Chỉnh sửa thông tin đối tác

### 9.5 Xóa đối tác
- **Actor:** Quản trị viên
- **Mô tả:** Xóa mềm đối tác

### 9.6 Thay đổi trạng thái đối tác
- **Actor:** Quản trị viên
- **Mô tả:** Chuyển trạng thái (ACTIVE, INACTIVE, BLACKLISTED)

### 9.7 Tìm kiếm đối tác
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tìm kiếm theo mã, tên, loại, trạng thái (có phân trang)

---

## 10. QUẢN LÝ LÔ HÀNG (BATCH)

**Mô tả:** Quản lý lô sản phẩm, theo dõi hạn sử dụng, truy xuất nguồn gốc.

### 10.1 Tạo lô hàng
- **Actor:** Quản lý kho
- **Mô tả:** Tạo lô mới cho sản phẩm theo dõi lô (số lô, ngày sản xuất, hạn dùng, số lượng)

### 10.2 Xem chi tiết lô hàng
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin lô hàng theo ID

### 10.3 Xem danh sách lô hàng
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo từ khóa, sản phẩm, kho, trạng thái, khoảng ngày sản xuất, khoảng hạn dùng

### 10.4 Truy xuất nguồn gốc lô hàng
- **Actor:** Quản lý kho
- **Mô tả:** Xem toàn bộ lịch sử truy xuất từ nhập đến xuất của một lô

### 10.5 Xem lô sắp hết hạn
- **Actor:** Quản lý kho
- **Mô tả:** Xem danh sách lô sắp đến hạn dùng (ngưỡng cấu hình, tùy chọn lọc theo kho)

### 10.6 Gợi ý FIFO
- **Actor:** Quản lý kho
- **Mô tả:** Gợi ý lô cũ nhất đủ điều kiện để xuất kho theo nguyên tắc FIFO

### 10.7 Xem lô theo sản phẩm
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả lô của một sản phẩm kèm tổng hợp tồn kho

### 10.8 Thay đổi trạng thái lô
- **Actor:** Quản lý kho
- **Mô tả:** Chuyển trạng thái lô (dùng endpoint chuyên biệt cho workflow)

### 10.9 Cập nhật lô hàng
- **Actor:** Quản lý kho
- **Mô tả:** Cập nhật thông tin lô (ngày sản xuất, hạn dùng, số lượng)

### 10.10 Cách ly lô hàng (Quarantine)
- **Actor:** Quản lý kho
- **Mô tả:** Chuyển lô từ AVAILABLE sang QUARANTINE (bắt buộc có lý do)

### 10.11 Giải phóng lô hàng (Release)
- **Actor:** Quản lý kho
- **Mô tả:** Giải phóng lô từ QUARANTINE về AVAILABLE (bắt buộc có ghi chú)

---

## 11. QUẢN LÝ TỒN KHO

**Mô tả:** Quản lý số lượng tồn kho theo sản phẩm, kho, vị trí, lô.

### 11.1 Xem danh sách tồn kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo sản phẩm, kho, vị trí, lô

### 11.2 Xem tổng hợp tồn kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tổng hợp tồn kho của sản phẩm (tổng on-hand, reserved, available trên tất cả vị trí)

### 11.3 Xem tồn kho theo vị trí
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tồn kho nhóm theo vị trí trong kho

### 11.4 Kiểm tra khả dụng
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Kiểm tra số lượng sản phẩm yêu cầu có khả dụng trong kho không

### 11.5 Giữ hàng (Reserve)
- **Actor:** Hệ thống / Quản lý kho
- **Mô tả:** Giữ tồn kho cho đơn hàng (áp dụng chiến lược phân bổ FIFO)

### 11.6 Hủy giữ hàng (Unreserve)
- **Actor:** Hệ thống / Quản lý kho
- **Mô tả:** Giải phóng hàng đã giữ cho đơn hàng

### 11.7 Tăng tồn kho
- **Actor:** Hệ thống
- **Mô tả:** Tăng tồn kho thực tế (từ nhập kho hoặc điều chỉnh)

### 11.8 Giảm tồn kho
- **Actor:** Hệ thống
- **Mô tả:** Giảm tồn kho thực tế (từ xuất kho hoặc điều chỉnh)

---

## 12. NGHIỆP VỤ NHẬP KHO

**Mô tả:** Quản lý quy trình nhập hàng từ đơn đặt hàng mua đến biên bản nhận hàng.

### 12.1 Quản lý đơn đặt hàng mua (Purchase Order)

#### 12.1.1 Tạo đơn mua nháp
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Tạo đơn đặt hàng mua mới ở trạng thái DRAFT

#### 12.1.2 Xem danh sách đơn mua
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo số đơn, nhà cung cấp, kho, trạng thái, khoảng ngày đặt, khoảng ngày dự kiến giao

#### 12.1.3 Xem chi tiết đơn mua
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin đơn mua theo ID

#### 12.1.4 Cập nhật đơn mua nháp
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Chỉnh sửa đơn mua khi đang ở trạng thái DRAFT

#### 12.1.5 Xóa đơn mua nháp
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Xóa đơn mua nháp

#### 12.1.6 Xác nhận đơn mua
- **Actor:** Quản lý mua hàng
- **Mô tả:** Chuyển đơn mua từ DRAFT sang CONFIRMED

### 12.2 Quản lý dòng đơn mua (Purchase Order Lines)

#### 12.2.1 Xem dòng theo đơn mua
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả dòng hàng của một đơn mua

#### 12.2.2 Thêm dòng đơn mua
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Thêm dòng hàng vào đơn mua

#### 12.2.3 Cập nhật dòng đơn mua
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Chỉnh sửa dòng hàng (số lượng, giá, v.v.)

#### 12.2.4 Xóa dòng đơn mua
- **Actor:** Nhân viên mua hàng
- **Mô tả:** Xóa dòng hàng khỏi đơn mua

### 12.3 Quản lý biên bản nhận hàng (Inbound Receipt)

#### 12.3.1 Tạo biên bản nhận nháp
- **Actor:** Nhân viên kho
- **Mô tả:** Tạo biên bản nhận hàng mới ở trạng thái DRAFT

#### 12.3.2 Xem danh sách biên bản nhận
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo số biên bản, đơn mua, kho, trạng thái, khoảng ngày nhận

#### 12.3.3 Xem biên bản theo đơn mua
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả biên bản nhận của một đơn mua

#### 12.3.4 Xem chi tiết biên bản nhận
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin biên bản nhận theo ID

#### 12.3.5 Cập nhật biên bản nhận nháp
- **Actor:** Nhân viên kho
- **Mô tả:** Chỉnh sửa biên bản nhận khi đang ở trạng thái DRAFT

#### 12.3.6 Xóa biên bản nhận nháp
- **Actor:** Nhân viên kho
- **Mô tả:** Xóa biên bản nhận nháp

#### 12.3.7 Xác nhận nhận hàng
- **Actor:** Nhân viên kho
- **Mô tả:** Xác nhận biên bản nhận → kích hoạt tăng tồn kho và tạo lô hàng

### 12.4 Quản lý dòng biên bản nhận (Inbound Receipt Lines)

#### 12.4.1 Thêm dòng biên bản nhận
- **Actor:** Nhân viên kho
- **Mô tả:** Thêm dòng hàng vào biên bản nhận (chỉ DRAFT). Bao gồm thông tin lô cho sản phẩm theo dõi lô

#### 12.4.2 Cập nhật dòng biên bản nhận
- **Actor:** Nhân viên kho
- **Mô tả:** Chỉnh sửa dòng hàng (chỉ DRAFT)

#### 12.4.3 Xóa dòng biên bản nhận
- **Actor:** Nhân viên kho
- **Mô tả:** Xóa dòng hàng khỏi biên bản nhận (chỉ DRAFT)

#### 12.4.4 Xem dòng theo biên bản
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả dòng hàng của một biên bản nhận

### Sơ đồ luồng nhập kho:
```
Đơn mua (DRAFT → CONFIRMED)
    → Biên bản nhận (DRAFT → CONFIRMED)
        → Tăng tồn kho + Tạo lô hàng
```

---

## 13. NGHIỆP VỤ XUẤT KHO

**Mô tả:** Quản lý quy trình xuất hàng từ đơn bán đến giao hàng cho khách.

### 13.1 Quản lý đơn bán (Sales Order)

#### 13.1.1 Tạo đơn bán nháp
- **Actor:** Nhân viên bán hàng
- **Mô tả:** Tạo đơn bán mới ở trạng thái DRAFT (không ảnh hưởng tồn kho)

#### 13.1.2 Xem danh sách đơn bán
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo số đơn, khách hàng, kho, trạng thái, khoảng ngày đặt, khoảng ngày yêu cầu giao

#### 13.1.3 Xem chi tiết đơn bán
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin đơn bán kèm dòng hàng

#### 13.1.4 Cập nhật đơn bán nháp
- **Actor:** Nhân viên bán hàng
- **Mô tả:** Chỉnh sửa đơn bán khi đang ở trạng thái DRAFT

#### 13.1.5 Xác nhận đơn bán
- **Actor:** Quản lý bán hàng
- **Mô tả:** Xác nhận đơn bán → giữ tồn kho (thất bại nếu không đủ hàng)

#### 13.1.6 Hủy đơn bán
- **Actor:** Quản lý bán hàng
- **Mô tả:** Hủy đơn bán → giải phóng hàng đã giữ (bị chặn nếu đang giao)

### 13.2 Quản lý dòng đơn bán (Sales Order Lines)

#### 13.2.1 Thêm dòng đơn bán
- **Actor:** Nhân viên bán hàng
- **Mô tả:** Thêm dòng hàng vào đơn bán (chỉ DRAFT). Tự động tính lineTotal và cập nhật tổng đơn

#### 13.2.2 Cập nhật dòng đơn bán
- **Actor:** Nhân viên bán hàng
- **Mô tả:** Chỉnh sửa dòng hàng (số lượng, giá, ghi chú) - chỉ DRAFT

#### 13.2.3 Xem dòng đơn bán
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả dòng hàng của một đơn bán

### 13.3 Quản lý phiếu xuất kho (Outbound Shipment)

#### 13.3.1 Tạo phiếu xuất nháp
- **Actor:** Nhân viên kho
- **Mô tả:** Tạo phiếu xuất kho mới ở trạng thái DRAFT

#### 13.3.2 Xem danh sách phiếu xuất
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả phiếu xuất

#### 13.3.3 Xem chi tiết phiếu xuất
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin phiếu xuất theo ID

#### 13.3.4 Cập nhật phiếu xuất
- **Actor:** Nhân viên kho
- **Mô tả:** Chỉnh sửa phiếu xuất (chỉ DRAFT)

#### 13.3.5 Bắt đầu lấy hàng (Pick)
- **Actor:** Nhân viên kho
- **Mô tả:** Chuyển trạng thái: DRAFT → PICKING. Bắt đầu quy trình lấy hàng

#### 13.3.6 Đánh dấu đã đóng gói
- **Actor:** Nhân viên kho
- **Mô tả:** Chuyển trạng thái: PICKING → PACKED. Xác nhận đã lấy và đóng gói xong

#### 13.3.7 Chuyển sang khu chờ giao
- **Actor:** Nhân viên kho
- **Mô tả:** Chuyển trạng thái: PACKED → STAGING. Chuẩn bị điều phối

#### 13.3.8 Xác nhận xuất kho
- **Actor:** Quản lý kho
- **Mô tả:** Chuyển trạng thái: STAGING → SHIPPED. Bước cuối: tiêu hao hàng đã giữ, ghi nhận di chuyển tồn kho (OUTBOUND), cập nhật số lượng đã giao của dòng đơn bán, cập nhật trạng thái đơn bán

#### 13.3.9 Hủy phiếu xuất
- **Actor:** Quản lý kho
- **Mô tả:** Hủy phiếu xuất (mọi trạng thái trừ SHIPPED). Giải phóng hàng đã giữ nếu có

### 13.4 Quản lý dòng phiếu xuất (Outbound Shipment Lines)

#### 13.4.1 Tạo dòng phiếu xuất
- **Actor:** Nhân viên kho
- **Mô tả:** Thêm dòng hàng vào phiếu xuất (chỉ định sản phẩm, vị trí, lô, số lượng). Tự động gán lineNumber

#### 13.4.2 Xem dòng theo phiếu xuất
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem tất cả dòng hàng của một phiếu xuất

#### 13.4.3 Xem chi tiết dòng phiếu xuất
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin dòng phiếu xuất theo ID

#### 13.4.4 Cập nhật dòng phiếu xuất
- **Actor:** Nhân viên kho
- **Mô tả:** Chỉnh sửa dòng phiếu xuất (chỉ DRAFT)

#### 13.4.5 Xóa dòng phiếu xuất
- **Actor:** Nhân viên kho
- **Mô tả:** Xóa dòng phiếu xuất (chỉ DRAFT)

### Sơ đồ luồng xuất kho:
```
Đơn bán (DRAFT → CONFIRMED, giữ hàng)
    → Phiếu xuất (DRAFT → PICKING → PACKED → STAGING → SHIPPED)
        → Giảm tồn kho + Ghi nhận di chuyển tồn kho
```

---

## 14. ĐIỀU CHỈNH TỒN KHO

**Mô tả:** Quản lý việc điều chỉnh số lượng tồn kho (kiểm kê, thất thoát, hỏng hóc).

### 14.1 Tạo điều chỉnh
- **Actor:** Quản lý kho
- **Mô tả:** Tạo yêu cầu điều chỉnh tồn kho (lý do, tham chiếu tồn kho, số lượng thay đổi)

### 14.2 Xem chi tiết điều chỉnh
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin điều chỉnh theo ID

### 14.3 Xem danh sách điều chỉnh
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang, lọc theo trạng thái, sản phẩm, kho, tồn kho, số điều chỉnh, khoảng ngày tạo

### 14.4 Phê duyệt điều chỉnh
- **Actor:** Quản lý cấp cao
- **Mô tả:** Phê duyệt điều chỉnh → áp dụng thay đổi số lượng vào tồn kho

### 14.5 Từ chối điều chỉnh
- **Actor:** Quản lý cấp cao
- **Mô tả:** Từ chối điều chỉnh (bắt buộc có lý do từ chối)

---

## 15. CHUYỂN KHO

**Mô tả:** Quản lý việc di chuyển hàng hóa giữa các vị trí / kho.

### 15.1 Tạo chuyển kho
- **Actor:** Quản lý kho
- **Mô tả:** Tạo yêu cầu chuyển kho giữa các vị trí / kho

### 15.2 Xem chi tiết chuyển kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin chuyển kho theo ID

### 15.3 Xem danh sách chuyển kho
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả chuyển kho

### 15.4 Gửi chuyển kho
- **Actor:** Quản lý kho
- **Mô tả:** Gửi chuyển kho để xử lý / phê duyệt

### 15.5 Hoàn thành chuyển kho
- **Actor:** Quản lý kho
- **Mô tả:** Hoàn thành chuyển kho → di chuyển hàng từ nguồn đến đích

### 15.6 Hủy chuyển kho
- **Actor:** Quản lý kho
- **Mô tả:** Hủy yêu cầu chuyển kho

---

## 16. LỊCH SỬ DI CHUYỂN TỒN KHO

**Mô tả:** Nhật ký kiểm toán mọi thay đổi tồn kho trong hệ thống.

### 16.1 Xem chi tiết di chuyển
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin bản ghi di chuyển tồn kho theo ID

### 16.2 Xem danh sách di chuyển
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tất cả di chuyển tồn kho (nhật ký kiểm toán)

### 16.3 Xem di chuyển theo tham chiếu
- **Actor:** Quản lý kho
- **Mô tả:** Xem di chuyển liên kết đến một tham chiếu cụ thể (điều chỉnh, chuyển kho, v.v.)

---

## 17. QUẢN LÝ TỆP TIN (MINIO)

**Mô tả:** Quản lý lưu trữ tệp tin trên MinIO (ảnh sản phẩm, báo cáo, v.v.).

### 17.1 Tải lên tệp đơn
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tải lên một tệp với thư mục tùy chọn. Trả về metadata và presigned URL

### 17.2 Tải lên nhiều tệp
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tải lên tối đa 10 tệp trong một yêu cầu

### 17.3 Lấy presigned URL
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Tạo URL truy cập tệp có thời hạn

### 17.4 Xóa tệp
- **Actor:** Quản trị viên
- **Mô tả:** Xóa tệp khỏi MinIO

### 17.5 Kiểm tra tệp tồn tại
- **Actor:** Hệ thống
- **Mô tả:** Kiểm tra đối tượng có tồn tại trong bucket MinIO không

---

## 18. QUẢN LÝ EMAIL

**Mô tả:** Quản lý gửi email và nhật ký email (hỗ trợ bất đồng bộ qua RabbitMQ).

### 18.1 Gửi email
- **Actor:** Hệ thống / Người dùng
- **Mô tả:** Gửi email đồng bộ hoặc bất đồng bộ (cờ `async`). Bất đồng bộ dùng RabbitMQ

### 18.2 Xem nhật ký email
- **Actor:** Quản trị viên
- **Mô tả:** Xem chi tiết nhật ký email theo ID

### 18.3 Xem danh sách nhật ký email
- **Actor:** Quản trị viên
- **Mô tả:** Xem danh sách phân trang tất cả nhật ký email

### 18.4 Xem email theo trạng thái
- **Actor:** Quản trị viên
- **Mô tả:** Lọc nhật ký email theo trạng thái (PENDING, SENT, FAILED, v.v.)

### 18.5 Xem email theo loại
- **Actor:** Quản trị viên
- **Mô tả:** Lọc nhật ký email theo loại (REGISTRATION, FORGOT_PASSWORD, v.v.)

### 18.6 Xem email theo người nhận
- **Actor:** Quản trị viên
- **Mô tả:** Lọc nhật ký email theo địa chỉ email người nhận

### 18.7 Gửi lại email thất bại
- **Actor:** Quản trị viên
- **Mô tả:** Gửi lại một email thất bại cụ thể

### 18.8 Xem thống kê email
- **Actor:** Quản trị viên
- **Mô tả:** Xem thống kê tổng hợp email (số lượng theo trạng thái)

### 18.9 Xử lý email chờ
- **Actor:** Quản trị viên
- **Mô tả:** Kích hoạt thủ công xử lý tất cả email đang chờ

### 18.10 Gửi lại email thất bại hàng loạt
- **Actor:** Quản trị viên
- **Mô tả:** Kích hoạt thủ công gửi lại tất cả email thất bại

---

## 19. BÁO CÁO & XUẤT DỮ LIỆU

**Mô tả:** Tạo và xuất báo cáo tồn kho dưới dạng PDF.

### 19.1 Xuất báo cáo tồn kho PDF
- **Actor:** Quản lý kho
- **Mô tả:** Tạo và tải xuống báo cáo PDF mức tồn kho hiện tại với bộ lọc tùy chọn

### Báo cáo dự kiến (chưa triển khai):
- Định giá tồn kho
- Di chuyển tồn kho
- Truy xuất nguồn gốc lô
- Cảnh báo tồn kho thấp
- Lô sắp hết hạn
- Báo cáo lên lịch gửi qua email
- Tạo báo cáo bất đồng bộ qua RabbitMQ cho tập dữ liệu lớn

---

## 20. DASHBOARD

**Mô tả:** Cung cấp cái nhìn tổng quan vận hành hệ thống.

### 20.1 Xem snapshot dashboard
- **Actor:** Quản lý / Quản trị viên
- **Mô tả:** Xem dữ liệu dashboard vận hành: lọc kho tùy chọn, khoảng ngày (mặc định 7 ngày), giới hạn hoạt động, giới hạn tác vụ nền

---

## 21. QUẢN LÝ TÁC VỤ NỀN

**Mô tả:** Quản lý các tác vụ chạy nền như tạo báo cáo, import/export dữ liệu lớn.

### 21.1 Xem tác vụ của tôi
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem danh sách phân trang tác vụ nền của người dùng hiện tại, có bộ lọc

### 21.2 Xem chi tiết tác vụ
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem thông tin chi tiết tác vụ nền

### 21.3 Xem trạng thái tác vụ
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Xem trạng thái hiện tại của tác vụ nền

### 21.4 Thử lại tác vụ
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Thử lại tác vụ nền thất bại hoặc bị hủy

### 21.5 Hủy tác vụ
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Hủy tác vụ nền đang chạy hoặc đang chờ

### 21.6 Lấy liên kết tải xuống
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Lấy liên kết tải file kết quả của tác vụ hoàn thành

---

## 22. CHATBOT AI

**Mô tả:** Trợ lý AI hỗ trợ người dùng tương tác với hệ thống.

### 22.1 Chat với bot
- **Actor:** Người dùng đã đăng nhập
- **Mô tả:** Gửi tin nhắn đến chatbot AI
- **Ràng buộc:** Giới hạn 5 lần/phút

---

## TỔNG KẾT

| Hạng mục | Số lượng |
|----------|----------|
| Nhóm nghiệp vụ chính | 22 |
| Tổng usecase chi tiết | 150+ |
| Controller | 34 |
| Entity | 33 |
| Loại quyền hạn | 80+ |

### Phân loại nhóm nghiệp vụ:

| Nhóm | Modules |
|------|---------|
| Nền tảng / Bảo mật | Xác thực, Phân quyền, Nhân viên |
| Dữ liệu chủ | Kho hàng, Vị trí, Sản phẩm, Danh mục, Đơn vị tính, Đối tác |
| Tồn kho & Lô | Tồn kho, Batch |
| Nhập kho | Đơn mua, Dòng đơn mua, Biên bản nhận, Dòng biên bản |
| Xuất kho | Đơn bán, Dòng đơn bán, Phiếu xuất, Dòng phiếu xuất |
| Vận hành kho | Điều chỉnh tồn, Chuyển kho, Lịch sử di chuyển |
| Hỗ trợ | Tệp tin, Email, Báo cáo, Dashboard, Tác vụ nền, Chatbot
