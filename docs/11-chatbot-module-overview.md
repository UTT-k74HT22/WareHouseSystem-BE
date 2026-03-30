# WHS ChatBot Module Overview

Tài liệu này giải thích chi tiết về module Chatbot tích hợp trí tuệ nhân tạo (Gemini AI) vào hệ thống quản lý kho (WareHouseSystem-BE).

## 1. Giới thiệu tổng quan
Module Chatbot cung cấp một giao diện hội thoại thông minh, cho phép nhân viên kho tra cứu thông tin sản phẩm, SKU và giải đáp thắc mắc nghiệp vụ một cách tự nhiên.

## 2. Cấu trúc File và Thư mục

### 2.1 Controller Layer
| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `ChatBotController.java` | `controller/ChatBotController.java` | REST endpoint xử lý request từ client. Định tuyến đến `/api/v1/chatbot/chat`. Áp dụng Rate Limit 5 requests/60 giây. |

### 2.2 Service Layer
| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `ChatBotService.java` | `service/ChatBotService.java` | Interface định nghĩa contract cho chatbot. |
| `ChatBotServiceImpl.java` | `service/impl/ChatBotServiceImpl.java` | Triển khai chính: điều phối luồng xử lý, gọi các service khác (Product, Inventory, Batch), tích hợp Gemini API, Redis caching, xử lý retry khi bị rate limit. |
| `ChatBotIntentResolver.java` | `service/chatbot/ChatBotIntentResolver.java` | Xác định ý định của người dùng (Intent) dựa trên rule-based. Trích xuất SKU/từ khóa sản phẩm bằng Regex. Chuẩn hóa tiếng Việt (loại bỏ dấu). |
| `ChatBotResponseFormatter.java` | `service/chatbot/ChatBotResponseFormatter.java` | Định dạng câu trả lời tiếng Việt có dấu cho từng loại intent. Bao gồm các phương thức greeting(), help(), productLookup(), inventorySummary(), batchExpiring(), v.v. |
| `ChatBotIntent.java` | `service/chatbot/ChatBotIntent.java` | Enum định nghĩa các loại ý định: GREETING, HELP, PRODUCT_LOOKUP, INVENTORY_SUMMARY, INVENTORY_BY_LOCATION, BATCH_EXPIRING, UNKNOWN. |
| `ChatBotCommand.java` | `service/chatbot/ChatBotCommand.java` | Record lưu trữ command sau khi phân tích: intent, originalMessage, normalizedMessage, subjectKeyword, thresholdDays. |

### 2.3 DTO Layer
| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `ChatBotRequest.java` | `entity/dto/request/chatbot/ChatBotRequest.java` | Request DTO chứa trường `message` từ người dùng. |
| `ChatBotResponse.java` | `entity/dto/response/chatbot/ChatBotResponse.java` | Response DTO chứa trường `reply` trả về cho người dùng. |
| `GeminiRequest.java` | `entity/dto/request/chatbot/GeminiRequest.java` | Request DTO để gọi Gemini API (contents, parts, text). |
| `GeminiResponse.java` | `entity/dto/response/chatbot/GeminiResponse.java` | Response DTO từ Gemini API (candidates, content, parts, text). |

### 2.4 Luồng dữ liệu giữa các tầng
```
Client Request → ChatBotController → ChatBotServiceImpl
                                           ↓
                            ChatBotIntentResolver (xác định intent)
                                           ↓
                            ┌──────────────┴──────────────┐
                            ↓                             ↓
              Xử lý local                  Gọi Gemini API (khi UNKNOWN)
              (ProductService,                       ↓
               InventoryService,           Redis Cache (10 phút)
               BatchService)                        ↓
                            └──────────────┬──────────────┘
                                           ↓
                            ChatBotResponseFormatter (format tiếng Việt)
                                           ↓
                                  Client Response
```

## 3. Kiến trúc xử lý (Multi-Layer Architecture)
Để tối ưu hóa hiệu suất và tiết kiệm hạn mức API (Rate Limit), module được xây dựng theo mô hình phân lớp:

1.  **Intent Resolver (Lớp phân giải ý định)**: Sử dụng các quy tắc (Rule-based) để xác định người dùng muốn gì (Chào hỏi, Tìm sản phẩm, Cần giúp đỡ) trước khi quyết định gọi AI.
2.  **Vietnamese Normalizer**: Chuẩn hóa tiếng Việt (loại bỏ dấu, viết thường) để việc tìm kiếm và nhận diện ý định chính xác hơn, bất kể người dùng gõ có dấu hay không.
3.  **Local Keyword Extraction**: Trích xuất SKU hoặc tên sản phẩm trực tiếp bằng Regex, giảm thiểu việc phải nhờ AI trích xuất (tiết kiệm 1 lần gọi API).
4.  **Redis Caching Layer**: Lưu trữ các câu trả lời của AI vào Redis với TTL 10 phút. Nếu có câu hỏi trùng lặp, hệ thống trả về ngay lập tức.
5.  **AI Integration (Gemini 1.5 Flash)**: Lớp cuối cùng xử lý các câu hỏi mở hoặc định dạng dữ liệu sản phẩm thành bảng Markdown chuyên nghiệp.

## 4. Cấu hình hệ thống
Các thông số cấu hình được quản lý trong file `.env` và `application.yml`:

*   `app.gemini.api-key`: API Key lấy từ Google AI Studio.
*   `app.gemini.model`: Mặc định sử dụng `gemini-1.5-flash` (tốc độ cao, chi phí thấp).
*   `app.gemini.base-url`: Endpoint chính thức của Google Generative AI.

## 5. Bảo mật và Giới hạn tốc độ (Rate Limiting)
Module tích hợp chặt chẽ với hạ tầng bảo mật sẵn có của dự án:

*   **Bucket4j & Redis**: Áp dụng giới hạn **5 requests / 60 giây** cho mỗi người dùng/IP để ngăn chặn spam và bảo vệ hạn mức Gemini API.
*   **Retry Logic**: Tự động thử lại (Retry) khi gặp lỗi `429 Too Many Requests` từ phía Google với cơ chế đợi (Backoff).

## 6. API Specification

### Endpoint: Chat với AI
*   **URL**: `POST /api/v1/chatbot/chat`
*   **Authentication**: Public (được bảo vệ bởi Rate Limit).
*   **Request Body**:
    ```json
    {
      "message": "Tìm cho tôi sản phẩm TV Samsung"
    }
    ```
*   **Response Body**:
    ```json
    {
      "reply": "Dưới đây là danh sách sản phẩm TV Samsung tôi tìm thấy trong kho: \n\n | SKU | Tên | Danh mục | Giá | \n |---|---|---|---| \n | SKU-001 | TV Samsung 4K | Điện tử | 15,000,000 | \n\n Bạn có muốn tôi hỗ trợ gì thêm không?"
    }
    ```

## 7. Luồng xử lý logic trong Code (`ChatBotServiceImpl`)
1.  Nhận tin nhắn -> Chuẩn hóa tiếng Việt.
2.  Kiểm tra ý định (Intent):
    *   Nếu là **Chào hỏi/Trợ giúp**: Trả lời ngay từ bộ nhớ.
    *   Nếu là **Tìm sản phẩm**: Trích xuất keyword -> Truy vấn `ProductService` -> Gửi dữ liệu vào Gemini để định dạng bảng Markdown -> Trả về.
    *   Nếu **Không rõ**: Gửi toàn bộ cho Gemini xử lý linh hoạt.
3.  Trước khi gọi Gemini: Kiểm tra Redis Cache.
4.  Sau khi nhận phản hồi Gemini: Lưu vào Redis Cache.

## 8. Các câu hỏi Chatbot hỗ trợ

Chatbot hỗ trợ nhận cả **tiếng Việt có dấu và không dấu** (hệ thống tự chuẩn hóa trước khi xử lý).

### 8.1 Chào hỏi (GREETING)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `xin chao` / `xin chào` | Lời chào khi bắt đầu |
| `chao` / `chào` | Chào đơn giản |
| `hello` | Chào tiếng Anh |
| `hi` | Chào tiếng Anh |
| `hey` | Chào thân mật |

**Phản hồi**: "Chào bạn. Tôi là WHS Assistant. Tôi có thể tra cứu sản phẩm, tồn kho, vị trí tồn và batch sắp hết hạn bằng dữ liệu thật từ hệ thống."

### 8.2 Trợ giúp (HELP)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `help` | Hướng dẫn tiếng Anh |
| `giup` / `giúp` | Xin giúp đỡ |
| `huong dan` / `hướng dẫn` | Yêu cầu hướng dẫn |
| `tro giup` / `trợ giúp` | Xin trợ giúp |
| `ban lam duoc gi` / `bạn làm được gì` | Hỏi khả năng chatbot |

**Phản hồi**: Hướng dẫn sử dụng với các mẫu câu hỏi được hỗ trợ.

### 8.3 Tra cứu sản phẩm (PRODUCT_LOOKUP)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `tim san pham TV Samsung` | Tìm sản phẩm theo tên |
| `thong tin SKU-001` | Xem thông tin theo SKU |
| `ma SKU-001` | Tra cứu theo mã |
| `san pham` / `sản phẩm` | Tìm sản phẩm |
| `SKU-001` | Tra cứu trực tiếp bằng SKU |
| `gia` / `giá` | Xem giá sản phẩm |
| `thong tin` / `thông tin` | Xem thông tin |

**Phản hồi**: Thông tin sản phẩm bao gồm SKU, Tên, Danh mục, Đơn vị, Giá bán, Trạng thái.

### 8.4 Tra cứu tồn kho (INVENTORY_SUMMARY)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `ton kho SKU-001` | Xem tồn kho theo SKU |
| `con bao nhieu` | Hỏi số lượng còn |
| `con hang khong` | Kiểm tra còn hàng không |
| `so luong ton` | Xem số lượng tồn |
| `so luong` | Xem số lượng |
| `con trong kho` | Kiểm tra trong kho |
| `trong kho con` | Kiểm tra trong kho còn |
| `hien tai trong kho` | Xem hiện tại trong kho |
| `kiem tra so luong` | Kiểm tra số lượng |
| `available` | Xem số lượng khả dụng |
| `kiem tra ton` | Kiểm tra tồn |

**Phản hồi**: Thông tin tồn kho bao gồm SKU, Tên, On hand, Reserved, Available, Số kho có hàng, Số vị trí có hàng.

### 8.5 Tra cứu tồn kho theo vị trí (INVENTORY_BY_LOCATION)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `SKU-001 o kho nao` | Xem sản phẩm ở kho nào |
| `o dau con` | Hỏi ở đâu còn |
| `ton theo kho` | Tồn theo kho |
| `ton theo vi tri` / `tồn theo vị trí` | Xem tồn theo vị trí |
| `vi tri nao con` | Hỏi vị trí nào còn |
| `kho nao con` | Hỏi kho nào còn |

**Phản hồi**: Danh sách tồn kho theo vị trí bao gồm Kho, Vị trí, Batch (nếu có), Available, On hand, Reserved.

### 8.6 Tra cứu batch sắp hết hạn (BATCH_EXPIRING)
| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `sap het han` / `sắp hết hạn` | Xem batch sắp hết hạn |
| `het han` / `hết hạn` | Xem batch hết hạn |
| `expiring batch` | Tiếng Anh |
| `lo sap het han` | Xem lô sắp hết hạn |
| `batch sap het han` | Xem batch sắp hết hạn |
| `batch sap het han 30 ngay` | Xem batch sắp hết hạn trong 30 ngày |

**Phản hồi**: Danh sách batch sắp hết hạn bao gồm Batch Number, Product SKU, Product Name, Ngày hết hạn, Số lượng khả dụng.

### 8.7 Câu hỏi không xác định (UNKNOWN)
Các câu hỏi không khớp với các mẫu trên sẽ được chuyển sang Gemini AI xử lý.

| Câu hỏi mẫu | Mô tả |
|-------------|-------|
| `tong hop bao cao` | Yêu cầu tổng hợp báo cáo |
| `tai sao ton kho giam` | Hỏi về nguyên nhân |
| `huong dan su dung` | Hướng dẫn sử dụng |

**Phản hồi**: Gemini AI sẽ trả lời dựa trên dữ liệu được cung cấp hoặc thông báo nếu không đủ dữ liệu.

## 9. Hướng dẫn Test nhanh (Postman)
1.  Đảm bảo đã khai báo `GEMINI_API_KEY` trong `.env`.
2.  Sử dụng Postman gửi POST tới `/api/v1/chatbot/chat`.
3.  Thử gửi câu hỏi `"hi"` để xem phản hồi tức thì.
4.  Thử gửi `"tìm sản phẩm [tên sản phẩm thật]"` để xem bảng Markdown.
5.  Gửi nhanh quá 5 lần/phút để xem thông báo chặn từ Bucket4j.
