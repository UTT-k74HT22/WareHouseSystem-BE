# Email Service Implementation Guide

## 📧 Tổng quan

Hệ thống Email Service đã được implement theo chuẩn doanh nghiệp với các tính năng:

### ✨ Tính năng chính
- ✅ Gửi email đồng bộ (sync) và bất đồng bộ (async)
- ✅ Hỗ trợ HTML email với Thymeleaf templates
- ✅ Queue management với RabbitMQ
- ✅ Retry mechanism cho failed emails
- ✅ Email logging và tracking
- ✅ Scheduled tasks (cleanup, retry)
- ✅ Email statistics và monitoring
- ✅ Support attachments
- ✅ Priority-based sending

---

## 🏗️ Kiến trúc

```
Controller (REST API)
    ↓
Service Layer (Business Logic)
    ↓
EmailProducer → RabbitMQ → EmailConsumer
    ↓                           ↓
Repository              JavaMailSender
    ↓                           ↓
Database (email_logs)      SMTP Server
```

---

## 📦 Components

### 1. Entities
- `EmailLog` - Entity lưu lịch sử email
- `EmailStatus` - Enum (PENDING, SENDING, SENT, FAILED, RETRY)
- `EmailType` - Enum (WELCOME, PASSWORD_RESET, ORDER_CONFIRMATION, etc.)

### 2. DTOs
- `SendEmailRequest` - Request DTO để gửi email
- `EmailLogResponse` - Response DTO chứa thông tin email log

### 3. Repository
- `EmailLogRepository` - JPA repository với custom queries

### 4. Services
- `EmailService` (Interface) - Service interface
- `EmailServiceImpl` - Business logic implementation
- `EmailProducerService` - RabbitMQ producer
- `EmailConsumerService` - RabbitMQ consumer
- `EmailScheduledService` - Scheduled tasks

### 5. Controller
- `EmailController` - REST API endpoints

### 6. Configuration
- `EmailProperties` - Configuration properties
- `RabbitMQEmailConfig` - RabbitMQ setup

### 7. Templates (Thymeleaf)
- `welcome-email.html` - Welcome email template
- `password-reset.html` - Password reset template
- `inventory-alert.html` - Low stock alert template
- `order-confirmation.html` - Order confirmation template

---

## 🔧 Cấu hình

### 1. Environment Variables

Thêm vào file `.env` hoặc biến môi trường:

```bash
# Email Configuration
EMAIL_FROM=noreply@yourcompany.com
EMAIL_FROM_NAME=Your Company Name

# Gmail SMTP (nếu dùng Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Hoặc dùng SMTP server khác
# MAIL_HOST=smtp.sendgrid.net
# MAIL_PORT=587
# MAIL_USERNAME=apikey
# MAIL_PASSWORD=your-sendgrid-api-key
```

### 2. Gmail App Password

Nếu dùng Gmail, cần tạo App Password:
1. Truy cập: https://myaccount.google.com/security
2. Bật 2-Step Verification
3. Tạo App Password tại: https://myaccount.google.com/apppasswords
4. Sử dụng password này cho `MAIL_PASSWORD`

### 3. Application Properties

Đã được cấu hình trong `application.yml`:
```yaml
app:
  email:
    enabled: true
    from: ${EMAIL_FROM:noreply@warehouse.com}
    from-name: ${EMAIL_FROM_NAME:Warehouse Management System}
    max-retry: 3
    retry-delay-seconds: 60
    async-by-default: true
    queue-name: wms.email.queue
    exchange-name: wms.email.exchange
    routing-key: wms.email.send
```

---

## 🚀 Cách sử dụng

### 1. Gửi email đơn giản

```java
@Autowired
private EmailService emailService;

// Gửi text/HTML email
emailService.sendSimpleEmail(
    "user@example.com",
    "Welcome to WMS",
    "<h1>Welcome!</h1><p>Thank you for joining us.</p>",
    EmailType.WELCOME
);
```

### 2. Gửi email với template

```java
Map<String, Object> variables = new HashMap<>();
variables.put("userName", "John Doe");
variables.put("username", "john.doe");
variables.put("email", "john@example.com");
variables.put("verificationLink", "https://example.com/verify?token=xxx");

emailService.sendTemplateEmail(
    "john@example.com",
    "Welcome to Warehouse System",
    "email/welcome-email",
    variables,
    EmailType.WELCOME
);
```

### 3. Gửi email với attachment

```java
File reportFile = new File("path/to/report.pdf");

emailService.sendEmailWithAttachment(
    "manager@example.com",
    "Monthly Report",
    "<p>Please find attached the monthly report.</p>",
    EmailType.REPORT_EXPORT,
    reportFile
);
```

### 4. Gửi email qua REST API

```bash
POST /api/v1/emails/send
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "recipient": "user@example.com",
  "subject": "Test Email",
  "content": "<h1>Hello World</h1>",
  "emailType": "NOTIFICATION",
  "async": true,
  "priority": 5
}
```

### 5. Gửi email với template qua API

```bash
POST /api/v1/emails/send
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "recipient": "user@example.com",
  "subject": "Password Reset",
  "templateName": "email/password-reset",
  "templateVariables": {
    "userName": "John Doe",
    "resetLink": "https://example.com/reset?token=xyz",
    "expiryMinutes": 30
  },
  "emailType": "PASSWORD_RESET",
  "async": true
}
```

---

## 📊 REST API Endpoints

### Send Email
```
POST /api/v1/emails/send
Authorization: ADMIN, MANAGER, SYSTEM
```

### Get Email Log
```
GET /api/v1/emails/{id}
Authorization: ADMIN, MANAGER
```

### Get All Email Logs (Paginated)
```
GET /api/v1/emails?page=0&size=10&sortBy=createdAt&sortDir=DESC
Authorization: ADMIN, MANAGER
```

### Get Emails by Status
```
GET /api/v1/emails/status/{status}?page=0&size=10
Authorization: ADMIN, MANAGER
Status: PENDING, SENDING, SENT, FAILED, RETRY
```

### Get Emails by Type
```
GET /api/v1/emails/type/{type}?page=0&size=10
Authorization: ADMIN, MANAGER
Type: WELCOME, PASSWORD_RESET, ORDER_CONFIRMATION, etc.
```

### Get Emails by Recipient
```
GET /api/v1/emails/recipient/{email}?page=0&size=10
Authorization: ADMIN, MANAGER
```

### Retry Failed Email
```
POST /api/v1/emails/{id}/retry
Authorization: ADMIN, MANAGER
```

### Get Email Statistics
```
GET /api/v1/emails/statistics
Authorization: ADMIN, MANAGER

Response:
{
  "total": 100,
  "pending": 10,
  "sent": 80,
  "failed": 5,
  "retry": 5
}
```

---

## 🔄 Scheduled Tasks

### 1. Process Pending Emails
- **Frequency**: Every 5 minutes
- **Purpose**: Process emails in queue
- **Method**: `processPendingEmails()`

### 2. Retry Failed Emails
- **Frequency**: Every 30 minutes
- **Purpose**: Retry failed emails (up to max retry)
- **Method**: `retryFailedEmails()`

### 3. Cleanup Old Logs
- **Frequency**: Daily at 2:00 AM
- **Purpose**: Delete logs older than retention period (90 days)
- **Method**: `cleanupOldLogs()`

---

## 📝 Tạo Template mới

### 1. Tạo file HTML trong `src/main/resources/templates/email/`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Your Template</title>
    <style>
        /* Your styles */
    </style>
</head>
<body>
    <h1 th:text="${title}">Default Title</h1>
    <p th:text="${message}">Default message</p>
</body>
</html>
```

### 2. Sử dụng template

```java
Map<String, Object> variables = new HashMap<>();
variables.put("title", "Custom Title");
variables.put("message", "Custom Message");

emailService.sendTemplateEmail(
    "recipient@example.com",
    "Email Subject",
    "email/your-template",
    variables,
    EmailType.NOTIFICATION
);
```

---

## 🔍 Monitoring và Debugging

### 1. Xem logs
```bash
# Check application logs
tail -f logs/application.log | grep Email
```

### 2. Check RabbitMQ
```bash
# Access RabbitMQ Management UI
http://localhost:15672
Username: guest
Password: guest

# Check queue: wms.email.queue
```

### 3. Check Database
```sql
-- Check recent emails
SELECT * FROM email_logs ORDER BY created_at DESC LIMIT 10;

-- Check failed emails
SELECT * FROM email_logs WHERE status = 'FAILED';

-- Check statistics
SELECT status, COUNT(*) as count 
FROM email_logs 
GROUP BY status;
```

---

## ⚠️ Troubleshooting

### Email không được gửi

1. **Check SMTP configuration**
   ```yaml
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-app-password
   ```

2. **Check email enabled**
   ```yaml
   app.email.enabled=true
   ```

3. **Check RabbitMQ connection**
   - Đảm bảo RabbitMQ đang chạy
   - Check logs: `Connection refused`

4. **Check email logs trong database**
   ```sql
   SELECT * FROM email_logs WHERE status = 'FAILED' ORDER BY created_at DESC;
   ```

### Gmail authentication failed

- Sử dụng **App Password**, không phải password thường
- Enable "Less secure app access" (không khuyến nghị)
- Hoặc dùng service khác: SendGrid, AWS SES, Mailgun

### RabbitMQ queue not processing

1. Check consumer đang chạy:
   ```bash
   # Check logs
   grep "Consuming email from queue" logs/application.log
   ```

2. Check RabbitMQ Management UI
   - Queue có messages không?
   - Consumer có đang active không?

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test -Dtest=EmailServiceImplTest
mvn test -Dtest=EmailControllerTest
```

### Manual Testing với cURL

```bash
# 1. Get JWT token first
TOKEN="your-jwt-token"

# 2. Send email
curl -X POST http://localhost:8080/api/v1/emails/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "subject": "Test Email",
    "content": "<h1>Test</h1>",
    "emailType": "NOTIFICATION",
    "async": true
  }'

# 3. Check statistics
curl -X GET http://localhost:8080/api/v1/emails/statistics \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📚 Best Practices

### 1. Luôn dùng async cho bulk emails
```java
SendEmailRequest request = SendEmailRequest.builder()
    .async(true)  // Important for performance
    .build();
```

### 2. Sử dụng templates cho emails thường xuyên
- Dễ maintain
- Consistent design
- Support multiple languages

### 3. Set priority hợp lý
- 1 = Highest (critical alerts)
- 5 = Normal (default)
- 10 = Lowest (marketing emails)

### 4. Monitor email logs
- Regularly check failed emails
- Set up alerts for high failure rate
- Clean up old logs

### 5. Use proper email types
- Giúp tracking và statistics
- Có thể apply rules khác nhau cho mỗi type

---

## 🔐 Security

### 1. Email credentials
- **KHÔNG** hardcode trong code
- Sử dụng environment variables
- Store securely (AWS Secrets Manager, Azure Key Vault)

### 2. Authorization
- Chỉ ADMIN/MANAGER có thể gửi email
- Users thường chỉ xem được emails của mình

### 3. Rate limiting
- Prevent email spam
- Implement rate limiting ở controller level

### 4. Content validation
- Validate email addresses
- Sanitize HTML content
- Prevent XSS attacks

---

## 🎯 Next Steps

### Có thể mở rộng thêm:

1. **Email Templates Builder** - UI để tạo templates
2. **Email Analytics** - Track open rate, click rate
3. **Email Scheduling** - Schedule emails for future
4. **Email Campaigns** - Bulk email campaigns
5. **Email Webhooks** - Callbacks from email providers
6. **Multi-language Support** - i18n cho templates
7. **Email Verification** - Verify email addresses
8. **Unsubscribe Management** - Manage subscriptions

---

## 📞 Support

Nếu có vấn đề, liên hệ:
- Check documentation tại: `/src/documents/`
- Review code comments
- Check test cases để hiểu cách sử dụng

---

**Chúc bạn implement thành công! 🎉**
