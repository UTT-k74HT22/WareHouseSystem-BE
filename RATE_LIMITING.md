# Rate Limiting Feature Documentation

## Tổng Quan

Tính năng Rate Limiting được implement sử dụng Redis để giới hạn số lượng request đến các API endpoints, bảo vệ hệ thống khỏi abuse và brute force attacks.

## Kiến Trúc

### Components

1. **@RateLimit Annotation** - Custom annotation để đánh dấu endpoints cần rate limiting
2. **RateLimitService** - Service xử lý logic rate limiting với Redis
3. **RateLimitInterceptor** - Interceptor kiểm tra rate limit trước khi request được xử lý
4. **RateLimitExceededException** - Exception được throw khi vượt quá rate limit
5. **GlobalExceptionHandler** - Xử lý exception và trả về response 429

### Algorithm

Sử dụng **Sliding Window Counter** algorithm với Lua script trên Redis:
- Atomic operation: increment counter và check limit trong một transaction
- TTL tự động: Redis key tự động expire sau duration
- Fallback mechanism: Allow request nếu Redis có vấn đề để tránh block toàn bộ hệ thống

## Cách Sử Dụng

### 1. Áp dụng Rate Limit cho Method

```java
@PostMapping("/login")
@RateLimit(
    key = "login",
    limit = 5,
    duration = 300, // 5 phút
    type = RateLimitType.IP,
    message = "Too many login attempts. Please try again after 5 minutes."
)
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // ...
}
```

### 2. Áp dụng Rate Limit cho toàn bộ Controller

```java
@RestController
@RequestMapping("/api/v1/home")
@RateLimit(key = "home", limit = 100, duration = 60, type = RateLimitType.IP)
public class HomeController {
    // Tất cả methods trong controller này đều áp dụng rate limit
}
```

## Rate Limit Types

### 1. IP-based (RateLimitType.IP)
Rate limit dựa trên IP address của client. Phù hợp cho:
- Login endpoints (chống brute force)
- Public APIs
- Registration endpoints

```java
@RateLimit(type = RateLimitType.IP, limit = 5, duration = 300)
```

### 2. User-based (RateLimitType.USER)
Rate limit dựa trên authenticated user. Phù hợp cho:
- Protected APIs
- User-specific operations
- Token refresh endpoints

```java
@RateLimit(type = RateLimitType.USER, limit = 10, duration = 60)
```

### 3. API-based (RateLimitType.API)
Rate limit dựa trên endpoint URI. Phù hợp cho:
- Shared endpoints
- Resource-intensive operations

```java
@RateLimit(type = RateLimitType.API, limit = 100, duration = 60)
```

### 4. Global (RateLimitType.GLOBAL)
Rate limit toàn bộ hệ thống. Phù hợp cho:
- System-wide protection
- Emergency rate limiting

```java
@RateLimit(type = RateLimitType.GLOBAL, limit = 1000, duration = 60)
```

## Configuration

### application.yml

```yaml
app:
  rate-limit:
    enabled: true
    default:
      limit: 100
      duration: 60
    endpoints:
      login:
        limit: 5
        duration: 300
      register:
        limit: 3
        duration: 3600
```

### Redis Configuration

Sử dụng Redis configuration hiện có trong project:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
```

## Response Format

### Successful Request

Headers:
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1704067200
```

### Rate Limit Exceeded

Status: `429 Too Many Requests`

Headers:
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704067200
Retry-After: 30
```

Response Body:
```json
{
  "success": false,
  "error_code": "RATE_001",
  "message": "Too many login attempts. Please try again after 5 minutes.",
  "data": null,
  "retry_after": 300,
  "limit": 5,
  "remaining": 0,
  "reset_time": 1704067500,
  "timestamp": "2026-01-18T17:00:00"
}
```

## Redis Key Format

```
rate_limit:{TYPE}:{KEY}:{IDENTIFIER}
```

Examples:
- `rate_limit:IP:login:192.168.1.1`
- `rate_limit:USER:api:john.doe`
- `rate_limit:API:products:/api/v1/products`
- `rate_limit:GLOBAL:system:global`

## Best Practices

### 1. Chọn Rate Limit Type phù hợp

- **Login/Register**: Dùng `IP` type để chống brute force
- **Protected APIs**: Dùng `USER` type để fair usage
- **Public Read APIs**: Dùng `API` type để protect resources

### 2. Set Limits hợp lý

- **Sensitive operations**: Giới hạn thấp (3-5 requests per hour)
- **Normal operations**: Giới hạn trung bình (50-100 requests per minute)
- **Read-only operations**: Giới hạn cao (1000+ requests per minute)

### 3. Custom Messages

Luôn cung cấp message rõ ràng để user biết lý do và cách khắc phục:

```java
@RateLimit(
    message = "Too many password reset requests. Please try again in 1 hour."
)
```

### 4. Monitor và Adjust

- Log rate limit events
- Monitor Redis performance
- Adjust limits dựa trên usage patterns

## Testing

### Unit Tests

```java
// Test allowed request
@Test
void testCheckRateLimit_AllowedRequest() {
    RateLimitInfo result = rateLimitService.checkRateLimit(annotation, "192.168.1.1");
    assertThat(result.isAllowed()).isTrue();
}

// Test exceeded limit
@Test
void testCheckRateLimit_ExceededLimit() {
    RateLimitInfo result = rateLimitService.checkRateLimit(annotation, "192.168.1.1");
    assertThat(result.isAllowed()).isFalse();
}
```

### Integration Tests

```bash
# Run all rate limiting tests
./mvnw test -Dtest=RateLimitServiceTest,RateLimitInterceptorTest,RateLimitIntegrationTest
```

## Troubleshooting

### Redis Connection Issues

Nếu Redis không available, hệ thống sẽ **fallback** và allow requests để tránh block toàn bộ service.

### Rate Limit không hoạt động

1. Kiểm tra Redis đang chạy
2. Verify `@RateLimit` annotation đã được apply
3. Check `WebMvcConfig` đã register interceptor

### Reset Rate Limit (Development/Testing)

```java
rateLimitService.resetRateLimit(RateLimitType.IP, "login", "192.168.1.1");
```

Hoặc xóa trực tiếp trong Redis:

```bash
redis-cli DEL "rate_limit:IP:login:192.168.1.1"
```

## Performance

### Redis Operations

- **Read**: O(1) - Get current counter
- **Write**: O(1) - Increment counter with Lua script
- **Memory**: ~100 bytes per key

### Scalability

- Hỗ trợ horizontal scaling với Redis Cluster
- Stateless design - không cần sync giữa servers
- High throughput: 10,000+ requests/second per Redis instance

## Security Considerations

### IP Spoofing Prevention

Interceptor kiểm tra multiple headers để lấy real IP:
- X-Forwarded-For
- X-Real-IP
- Proxy-Client-IP
- ...

### Bypass Protection

- Exclude actuator và internal endpoints
- Swagger UI không bị rate limit
- Error pages không bị rate limit

## Migration Guide

### From No Rate Limiting

1. Add Redis dependency (đã có sẵn)
2. Deploy rate limiting code
3. Apply `@RateLimit` annotation to sensitive endpoints
4. Monitor và adjust limits

### Rollback Plan

1. Remove `@RateLimit` annotations
2. Comment out interceptor registration in `WebMvcConfig`
3. No data migration needed

## Roadmap

### Planned Features

- [ ] Rate limit dashboard/monitoring UI
- [ ] Dynamic rate limit adjustment via admin API
- [ ] Per-tenant rate limiting
- [ ] Advanced algorithms (Token Bucket, Leaky Bucket)
- [ ] Rate limit analytics và reporting

## Support

Nếu gặp vấn đề, kiểm tra:
1. [RateLimitService.java](src/main/java/org/demo/whs/service/RateLimitService.java) - Core logic
2. [RateLimitInterceptor.java](src/main/java/org/demo/whs/interceptor/RateLimitInterceptor.java) - Request interceptor
3. [Unit Tests](src/test/java/org/demo/whs/service/RateLimitServiceTest.java) - Test examples
