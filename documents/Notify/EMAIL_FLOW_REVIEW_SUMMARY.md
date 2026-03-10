# 📧 Email Flow Review – Quick Summary

## 🎯 Tổng quan
Email feature của WMS backend được thiết kế **khá tốt** với architecture rõ ràng, hỗ trợ sync/async sending qua RabbitMQ. Tuy nhiên có **4 bugs nghiêm trọng** cần fix ngay.

---

## ✅ Điểm mạnh
1. **Architecture tốt**: Layered design, phân tách rõ ràng (Controller → Service → Producer/Consumer)
2. **Async pattern**: RabbitMQ + Consumer scale tốt
3. **Audit logging**: EmailLog entity track đầy đủ status, retry, error
4. **Test coverage**: Unit tests cho controller và service đầy đủ

---

## ❌ Bugs nghiêm trọng (MUST FIX)

### 🐛 Bug #1: Retry logic race condition
**File**: `EmailConsumerService.consumeEmail()` line ~65
**Vấn đề**: 
- Consumer set `status=RETRY` nhưng **không re-queue** message vào RabbitMQ
- Email failed sẽ không tự động retry
- Rely hoàn toàn vào scheduled task (chạy 30 phút 1 lần)

**Fix**:
```java
// Option 1: Remove auto re-queue logic, chỉ dùng scheduled task
if (emailLog.getRetryCount() < emailLog.getMaxRetry()) {
    emailLog.setStatus(EmailStatus.FAILED); // Not RETRY
    emailLogRepository.save(emailLog);
    // Let scheduled task handle retry after 30 minutes
}

// Option 2: Actually re-queue
if (emailLog.getRetryCount() < emailLog.getMaxRetry()) {
    emailLog.setStatus(EmailStatus.RETRY);
    emailLogRepository.save(emailLog);
    emailProducerService.sendEmailToQueue(emailLog); // ← ADD THIS
}
```

---

### 🐛 Bug #2: Scheduled email send ngay lập tức
**File**: `EmailConsumerService.consumeEmail()` line ~38
**Vấn đề**:
- Request có `scheduled_at: "2026-01-27T09:00:00"` (future time)
- Consumer vẫn send email **ngay lập tức** thay vì đợi đến scheduled time

**Fix**:
```java
@RabbitListener(queues = "#{emailProperties.queueName}")
@Transactional
public void consumeEmail(EmailLog emailLog) {
    log.info("Consuming email from queue for recipient: {}", emailLog.getRecipient());

    // ✅ ADD THIS CHECK
    if (emailLog.getScheduledAt() != null && 
        emailLog.getScheduledAt().isAfter(LocalDateTime.now())) {
        log.info("Email scheduled for future ({}), skipping for now", 
                 emailLog.getScheduledAt());
        return; // Let scheduled task handle it later
    }

    try {
        emailLog.setStatus(EmailStatus.SENDING);
        // ... existing code
    }
}
```

---

### 🐛 Bug #3: Producer error không update EmailLog status
**File**: `EmailProducerService.sendEmailToQueue()` line ~37
**Vấn đề**:
- Nếu RabbitMQ unavailable, producer throw exception
- EmailLog đã được save với `status=PENDING` nhưng message không vào queue
- Email stuck ở PENDING mãi mãi

**Fix**:
```java
public void sendEmailToQueue(EmailLog emailLog) {
    try {
        log.info("Sending email to queue...");
        rabbitTemplate.convertAndSend(
            emailProperties.getExchangeName(),
            emailProperties.getRoutingKey(),
            emailLog
        );
        log.info("Email successfully sent to queue");
        
    } catch (Exception e) {
        log.error("Failed to send email to queue for recipient: {}", 
                  emailLog.getRecipient(), e);
        
        // ✅ UPDATE EMAIL LOG STATUS
        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setErrorMessage("Queue error: " + e.getMessage());
        emailLogRepository.save(emailLog);
        
        // ❌ DON'T THROW – let scheduled task retry later
    }
}
```

**Lưu ý**: Cần inject `EmailLogRepository` vào `EmailProducerService`

---

### 🐛 Bug #4: N+1 query problem khi get email logs
**File**: `EmailServiceImpl.mapToResponse()` line ~389
**Vấn đề**:
```java
// Mỗi email log → 1 query để fetch username
if (emailLog.getTriggeredBy() != null) {
    triggeredByUsername = accountRepository.findById(emailLog.getTriggeredBy())
        .map(Account::getUsername)
        .orElse(null);
}
```
- Get 100 email logs → 100 queries để fetch username
- Very slow khi volume lớn

**Fix Option 1** – JOIN query:
```java
// EmailLogRepository.java
@Query("SELECT e FROM EmailLog e " +
       "LEFT JOIN FETCH e.triggeredByAccount " +
       "WHERE e.status = :status")
Page<EmailLog> findByStatusWithAccount(
    @Param("status") EmailStatus status, 
    Pageable pageable
);
```

**Fix Option 2** – Cache username:
```java
@Cacheable(value = "accountUsernames", key = "#accountId")
public String getUsername(String accountId) {
    return accountRepository.findById(accountId)
        .map(Account::getUsername)
        .orElse(null);
}
```

---

## ⚠️ Should Fix (Medium Priority)

### 1. Missing @Transactional in scheduled tasks
**File**: `EmailScheduledService.java`
```java
// ❌ Current
@Scheduled(fixedDelay = 300000)
public void processPendingEmails() { ... }

// ✅ Should be
@Transactional
@Scheduled(fixedDelay = 300000)
public void processPendingEmails() { ... }
```

---

### 2. Generic exception catching
**File**: `EmailServiceImpl.sendEmail()` line ~57
```java
// ❌ Current – catch tất cả
catch (Exception e) {
    emailLog.setStatus(EmailStatus.FAILED);
}

// ✅ Should distinguish transient vs permanent errors
catch (MessagingException e) {
    if (isTransientError(e)) { // Network timeout, SMTP busy
        emailLog.setStatus(EmailStatus.RETRY);
    } else { // Invalid email, auth failed
        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setRetryCount(emailLog.getMaxRetry()); // Don't retry
    }
}
```

---

### 3. Sensitive data logging (GDPR)
**File**: Multiple files
```java
// ❌ Current – log email addresses
log.error("Failed to send email to: {}", request.getRecipient(), e);

// ✅ Should log email ID only
log.error("Failed to send email with ID: {}", emailLog.getId(), e);
```

---

### 4. Missing HTML sanitization (XSS risk)
**File**: `EmailServiceImpl.sendEmail()`
```java
// ✅ ADD THIS
private String sanitizeHtmlContent(String content) {
    // Use OWASP Java HTML Sanitizer or Jsoup
    return Jsoup.clean(content, Safelist.relaxed());
}

// In sendEmail()
String sanitizedContent = sanitizeHtmlContent(request.getContent());
```

---

## 💡 Nice to Have (Low Priority)

1. **Rate limiting** cho `POST /api/v1/emails/send` (tránh email bombing)
2. **Dead Letter Queue (DLQ)** cho emails failed permanently
3. **Redis caching** cho `getEmailLog(id)`
4. **Integration tests** với real RabbitMQ (Testcontainers)
5. **Monitoring dashboard** (Grafana + Prometheus)

---

## 📊 Test Coverage Gaps

**Missing tests**:
- ❌ Integration test với RabbitMQ
- ❌ Test scheduled tasks execution
- ❌ Test concurrent sending (race conditions)
- ❌ Test template rendering errors
- ❌ Test SMTP authentication failure

---

## 🎯 Action Items

### Phase 1: Critical Fixes (Week 1)
- [ ] Fix Bug #1: Retry logic race condition
- [ ] Fix Bug #2: Scheduled email handling
- [ ] Fix Bug #3: Producer error handling
- [ ] Add @Transactional to scheduled tasks

### Phase 2: Security & Performance (Week 2)
- [ ] Fix Bug #4: N+1 query problem
- [ ] Add HTML sanitization
- [ ] Stop logging email addresses
- [ ] Add rate limiting

### Phase 3: Testing & Monitoring (Week 3)
- [ ] Add integration tests
- [ ] Add monitoring metrics
- [ ] Configure DLQ
- [ ] Performance testing

---

## 📈 Overall Score: 7.5/10

| Criteria | Score | Note |
|----------|-------|------|
| Architecture | 9/10 | Clean layered design ✅ |
| Code Quality | 8/10 | Good but có bugs |
| Security | 6/10 | Missing sanitization, GDPR issues |
| Performance | 7/10 | N+1 query problem |
| Testing | 8/10 | Good unit tests, missing integration |
| Documentation | 8/10 | Clear Javadoc, Swagger OK |

---

## ✅ Recommendation

**ACCEPT WITH MAJOR REVISIONS**

Email feature có architecture tốt và đầy đủ tính năng, nhưng cần fix **4 bugs nghiêm trọng** trước khi deploy production:
1. Retry logic không hoạt động
2. Scheduled email send sai timing
3. Producer error handling
4. N+1 query performance

Sau khi fix xong, feature này **sẵn sàng cho production**.

---

**Full detailed review**: Xem file `EMAIL_FLOW_REVIEW.md` (35 pages)

**Reviewer**: GitHub Copilot (UC Reviewer Mode)  
**Date**: January 26, 2026
