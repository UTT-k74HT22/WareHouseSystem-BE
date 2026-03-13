# 🚀 Quick Start - Email Service

## Bước 1: Chạy Migration

Database migration sẽ tự động chạy khi start application (Flyway).

```bash
# Check migration status
mvn flyway:info

# Migration file đã được tạo:
# V20260313_03__Create_email_logs_table.sql
```

## Bước 2: Cấu hình Email

### Option 1: Sử dụng Gmail

1. **Tạo App Password**
   - Truy cập: https://myaccount.google.com/security
   - Enable 2-Step Verification
   - Tạo App Password: https://myaccount.google.com/apppasswords

2. **Cấu hình environment variables**

Tạo file `.env` hoặc set biến môi trường:

```bash
# Windows PowerShell
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-digit-app-password"
$env:EMAIL_FROM="your-email@gmail.com"
$env:EMAIL_FROM_NAME="Your Company Name"
```

### Option 2: Sử dụng SMTP Server khác

```bash
# SendGrid
$env:MAIL_HOST="smtp.sendgrid.net"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="apikey"
$env:MAIL_PASSWORD="your-sendgrid-api-key"

# Mailgun
$env:MAIL_HOST="smtp.mailgun.org"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="postmaster@yourdomain.mailgun.org"
$env:MAIL_PASSWORD="your-mailgun-password"
```

## Bước 3: Verify RabbitMQ

```bash
# Check RabbitMQ đang chạy
# Truy cập: http://localhost:15672
# Username: guest
# Password: guest

# Hoặc check bằng command
docker ps | grep rabbitmq
```

## Bước 4: Start Application

```bash
mvn spring-boot:run
```

## Bước 5: Test Email Service

### Method 1: Qua Code

```java
@Autowired
private EmailService emailService;

@Test
void testSendEmail() {
    emailService.sendSimpleEmail(
        "recipient@example.com",
        "Test Email",
        "<h1>Hello from WMS!</h1>",
        EmailType.NOTIFICATION
    );
}
```

### Method 2: Qua REST API

1. **Login để lấy JWT token**

```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

# Response:
{
  "accessToken": "eyJhbGc..."
}
```

2. **Gửi email**

```bash
POST http://localhost:8080/api/v1/emails/send
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "recipient": "test@example.com",
  "subject": "Test Email from WMS",
  "content": "<h1>Hello!</h1><p>This is a test email.</p>",
  "emailType": "NOTIFICATION",
  "async": true
}
```

3. **Check email log**

```bash
GET http://localhost:8080/api/v1/emails/statistics
Authorization: Bearer eyJhbGc...

# Response:
{
  "total": 1,
  "pending": 0,
  "sent": 1,
  "failed": 0,
  "retry": 0
}
```

## Bước 6: Verify Email đã gửi

### Check Database

```sql
SELECT * FROM email_logs ORDER BY created_at DESC LIMIT 5;
```

### Check RabbitMQ Queue

1. Truy cập: http://localhost:15672
2. Vào tab "Queues"
3. Tìm queue: `wms.email.queue`
4. Check messages

### Check Application Logs

```bash
# Check logs
tail -f logs/application.log | grep Email

# Hoặc trong console khi run
# Tìm log:
# "Email sent successfully to: test@example.com"
```

## 📧 Test với Template Email

### Welcome Email

```bash
POST http://localhost:8080/api/v1/emails/send
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "recipient": "newuser@example.com",
  "subject": "Welcome to Warehouse System",
  "templateName": "email/welcome-email",
  "templateVariables": {
    "userName": "John Doe",
    "username": "john.doe",
    "email": "newuser@example.com",
    "verificationLink": "http://localhost:8080/verify?token=abc123"
  },
  "emailType": "WELCOME",
  "async": true
}
```

### Password Reset Email

```bash
POST http://localhost:8080/api/v1/emails/send
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "recipient": "user@example.com",
  "subject": "Reset Your Password",
  "templateName": "email/password-reset",
  "templateVariables": {
    "userName": "John Doe",
    "resetLink": "http://localhost:8080/reset-password?token=xyz789",
    "expiryMinutes": 30
  },
  "emailType": "PASSWORD_RESET",
  "async": true
}
```

## 🔍 Troubleshooting

### Email không được gửi

1. **Check SMTP configuration**
```bash
# Verify environment variables
echo $env:MAIL_HOST
echo $env:MAIL_USERNAME
```

2. **Check application logs**
```bash
# Look for errors
grep "Failed to send email" logs/application.log
```

3. **Test SMTP connection manually**
```bash
# Using telnet
telnet smtp.gmail.com 587
```

### RabbitMQ connection failed

```bash
# Check RabbitMQ is running
docker ps | grep rabbitmq

# Restart if needed
docker restart <rabbitmq-container-id>
```

### Authentication failed (Gmail)

- Đảm bảo đang dùng **App Password**, không phải password thường
- Check 2-Step Verification đã enable
- Try tạo App Password mới

## ✅ Success Indicators

Nếu mọi thứ hoạt động đúng, bạn sẽ thấy:

1. ✅ Email log được tạo trong database với status = `SENT`
2. ✅ Email thực sự đến inbox
3. ✅ Log message: `"Email sent successfully to: ..."`
4. ✅ Statistics API trả về số liệu đúng
5. ✅ RabbitMQ queue empty (messages được consume)

## 📚 Next Steps

- Đọc full documentation: [EMAIL_SERVICE_GUIDE.md](EMAIL_SERVICE_GUIDE.md)
- Customize email templates
- Integrate vào business flows
- Setup monitoring và alerts

---

**Happy Coding! 🎉**
