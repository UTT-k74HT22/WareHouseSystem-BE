# Rate Limiting Technical Documentation

## Overview

This document provides comprehensive technical documentation for the Rate Limiting implementation in the Java Spring Boot 3.x project using Bucket4j with Redis distributed backend.

### Purpose & Objectives

- **Prevent abuse**: Protect APIs from brute-force attacks and spam
- **Resource management**: Control system load and prevent overload
- **Fair usage**: Ensure equitable resource distribution among users
- **Distributed consistency**: Maintain rate limits across multiple application instances

### Scope

Rate limiting is applied to:
- Authentication endpoints (login, refresh token)
- Sensitive operations (password reset, account creation)
- Public APIs (to prevent abuse)
- User-specific operations (per-user quotas)

## Architecture

### System Flow Diagram

```
Client Request
    ↓
Spring Controller
    ↓
RateLimitInterceptor / RateLimitFilter
    ↓
RateLimitService.checkRateLimit()
    ↓
Bucket4j ProxyManager
    ↓
RedissonClient (Redis)
    ↓
RateLimitDTO Response
    ↓
Allow/Reject Request
```

### Core Components

1. **@RateLimit Annotation** - Declarative rate limiting configuration
2. **RateLimitService** - Core business logic for rate limit checking
3. **ProxyManager** - Bucket4j distributed proxy manager
4. **RedissonClient** - Redis connection and operations
5. **RateLimitDTO** - Response object with rate limit state

## Algorithm: Token Bucket

### Implementation Details

The system uses **Token Bucket** algorithm with **greedy refill** strategy:

```java
Bandwidth bandwidth = Bandwidth.classic(
    limit,                                    // bucket capacity
    Refill.greedy(limit, Duration.ofSeconds(duration)) // refill rate
);
```

**Key Characteristics:**
- **Capacity**: Maximum tokens allowed (configured limit)
- **Refill**: Tokens added greedily at constant rate
- **Consumption**: Each request consumes 1 token
- **Burst handling**: Allows burst traffic up to capacity

### Algorithm Comparison

| Algorithm | Pros | Cons | Use Case |
|-----------|------|------|----------|
| **Token Bucket** | Burst handling, simple implementation | Memory overhead | **Current implementation** |
| Leaky Bucket | Smooth output rate | No burst handling | Streaming APIs |
| Sliding Window | Precise rate limiting | Complex implementation | High-precision scenarios |
| Fixed Window | Simple, low overhead | Burst at boundaries | Simple rate limiting |

## Configuration & Dependencies

### Maven Dependencies (pom.xml)

```xml
<!-- Bucket4j core -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.3.0</version>
</dependency>

<!-- Bucket4j Redis extension -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.3.0</version>
</dependency>

<!-- Redisson (Redis client) -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.20.0</version>
</dependency>
```

**Important Notes:**
- Use consistent versions across Bucket4j modules
- Redisson 3.20.0 is compatible with Bucket4j 8.3.0
- Avoid mixing different Redis client libraries

### Application Configuration (application.yml)

```yaml
spring:
  data:
    redis:
      host: ${redis.host:localhost}
      port: ${redis.port:6379}
      password: ${redis.password:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
```

### Recommended Redis Settings

| Setting | Recommended Value | Reason |
|---------|-------------------|--------|
| max-active | 20 | Sufficient connections for moderate load |
| max-idle | 10 | Balance between resource usage and performance |
| timeout | 2000ms | Prevent hanging on Redis issues |
| Connection pool | Enabled | Reuse connections for better performance |

## Core Components Deep Dive

### RedisConfig.java

```java
@Configuration
public class RedisConfig {
    
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer().setAddress(address);
        
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }

    @Bean
    public ProxyManager<String> bucketProxyManager(RedissonClient redissonClient) {
        CommandAsyncExecutor executor = ((Redisson) redissonClient).getCommandExecutor();
        return RedissonBasedProxyManager.builderFor(executor).build();
    }
}
```

**Key Points:**
- `destroyMethod = "shutdown"` ensures proper cleanup
- `CommandAsyncExecutor` provides async Redis operations
- `RedissonBasedProxyManager` handles distributed bucket state

### RateLimitService.java

#### Key Method: checkRateLimit()

```java
public RateLimitDTO checkRateLimit(RateLimit rateLimitAnnotation,
                                   String identifier,
                                   String routeKey) {
    
    // 1. Build unique key
    String key = buildKey(rateLimitAnnotation.type(), 
                         rateLimitAnnotation.key(), 
                         identifier, 
                         routeKey);
    
    // 2. Configure bandwidth
    Bandwidth bandwidth = Bandwidth.classic(
        rateLimitAnnotation.limit(),
        Refill.greedy(rateLimitAnnotation.limit(), 
                     Duration.ofSeconds(rateLimitAnnotation.duration()))
    );
    
    // 3. Create bucket configuration
    BucketConfiguration configuration = Bucket4j.configurationBuilder()
        .addLimit(bandwidth)
        .build();
    
    // 4. Get/create distributed bucket
    Bucket bucket = proxyManager.builder().build(key, configuration);
    
    // 5. Consume token and check result
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    
    // 6. Return appropriate response
    return buildRateLimitDTO(probe, rateLimitAnnotation);
}
```

#### Key Building: buildKey()

```java
private String buildKey(RateLimitType type, String key, 
                        String identifier, String routeKey) {
    return String.format("%s:%s:%s:%s",
        type.name(),    // IP, USER, API, GLOBAL
        key,            // login, register, etc.
        routeKey,       // POST:/api/v1/auth/login
        identifier      // 192.168.1.1 or username
    );
}
```

**Example Keys:**
- `IP:login:POST:/api/v1/auth/login:192.168.1.1`
- `USER:refresh-token:POST:/auth/refresh:john.doe`
- `GLOBAL:public-api:GET:/api/v1/products:GLOBAL`

### RateLimitDTO.java

```java
@Data
public class RateLimitDTO {
    private boolean allowed;      // Request allowed?
    private int remaining;        // Tokens remaining
    private int limit;           // Configured limit
    private long resetTime;      // Epoch seconds when window resets
    private Long retryAfter;     // Seconds to wait (null if allowed)
}
```

**Response Semantics:**
- **allowed=true**: `remaining >= 0`, `retryAfter = null`
- **allowed=false**: `remaining = 0`, `retryAfter > 0`

## Usage Examples

### Controller Level

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @PostMapping("/login")
    @RateLimit(key = "login", limit = 5, duration = 60, 
               type = RateLimitType.IP, failClosed = true)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Login logic
    }
    
    @PostMapping("/refresh")
    @RateLimit(key = "refresh-token", limit = 10, duration = 300, 
               type = RateLimitType.USER, failClosed = true)
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
        // Refresh token logic
    }
}
```

### Service Level (Programmatic)

```java
@Service
public class SomeService {
    
    private final RateLimitService rateLimitService;
    
    public void performOperation(String userId) {
        RateLimit annotation = createRateLimitAnnotation();
        RateLimitDTO result = rateLimitService.checkRateLimit(
            annotation, userId, "POST:/api/v1/operation"
        );
        
        if (!result.isAllowed()) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        
        // Perform operation
    }
}
```

## Fail-Open vs Fail-Closed Strategy

### Fail-Open (failClosed = false)

**When to use:**
- Read-only endpoints
- Non-critical operations
- Public APIs where availability is priority

**Behavior:**
- Redis errors → Allow requests
- Log error for monitoring
- Continue normal operation

### Fail-Closed (failClosed = true)

**When to use:**
- Authentication endpoints
- Sensitive operations (password change, delete)
- Financial operations
- Admin endpoints

**Behavior:**
- Redis errors → Block requests
- Return 429 or 503 status
- Prioritize security over availability

**Example Configuration:**
```java
@RateLimit(key = "login", limit = 5, duration = 60, 
           type = RateLimitType.IP, failClosed = true)  // Security critical
@RateLimit(key = "public-data", limit = 100, duration = 60, 
           type = RateLimitType.IP, failClosed = false) // Availability critical
```

## Testing Strategy

### Unit Testing

#### Test Structure

```java
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    
    @Mock
    private ProxyManager<String> proxyManager;
    
    @Mock
    private RemoteBucketBuilder<String> builder;
    
    @Mock
    private BucketProxy bucket;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    @Test
    @DisplayName("Should allow request when token available")
    void shouldAllowRequest() {
        // Mock setup
        when(proxyManager.builder()).thenReturn(builder);
        when(builder.build(anyString(), any(BucketConfiguration.class)))
            .thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1))
            .thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);
        
        // Test execution
        RateLimitDTO result = rateLimitService.checkRateLimit(
            mockAnnotation, "192.168.1.1", "POST:/api/test"
        );
        
        // Assertions
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(9);
    }
    
    @Test
    @DisplayName("Should reject request when tokens exhausted")
    void shouldRejectRequest() {
        // Mock setup for rejection
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L);
        
        // Test and assertions
        RateLimitDTO result = rateLimitService.checkRateLimit(...);
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfter()).isEqualTo(30L);
    }
    
    @Test
    @DisplayName("Should fallback allow when Redis error and failOpen")
    void shouldFallbackAllow() {
        // Mock Redis failure
        when(builder.build(anyString(), any(BucketConfiguration.class)))
            .thenThrow(new RuntimeException("Redis down"));
        
        // Test fail-open behavior
        RateLimitDTO result = rateLimitService.checkRateLimit(...);
        assertThat(result.isAllowed()).isTrue();
    }
}
```

#### Test Scenarios to Cover

1. **Normal flow**: Allow/reject based on tokens
2. **Redis failure**: Fail-open vs fail-closed behavior
3. **Edge cases**: Zero limits, negative durations
4. **Key generation**: Different types and identifiers
5. **Concurrent access**: Multiple threads same key

### Integration Testing

#### Using Testcontainers

```java
@Testcontainers
class RateLimitIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }
    
    @Test
    void shouldEnforceRateLimitAcrossMultipleRequests() {
        // Test actual Redis behavior
        for (int i = 0; i < 15; i++) {
            RateLimitDTO result = rateLimitService.checkRateLimit(...);
            if (i < 10) {
                assertThat(result.isAllowed()).isTrue();
            } else {
                assertThat(result.isAllowed()).isFalse();
            }
        }
    }
}
```

#### Performance Testing

```java
@Test
void shouldHandleHighConcurrency() throws InterruptedException {
    int threadCount = 100;
    int requestsPerThread = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                for (int j = 0; j < requestsPerThread; j++) {
                    rateLimitService.checkRateLimit(...);
                }
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await(30, TimeUnit.SECONDS);
    // Verify rate limits still enforced
}
```

## Migration Guide

### Upgrading from Bucket4j 7.x to 8.x

#### Key Changes

| Aspect | 7.x | 8.x |
|--------|-----|-----|
| Group ID | `com.github.vladimir-bukhtoyarov` | `com.bucket4j` |
| ProxyManager Builder | `proxyManager.builderFor(key)` | `proxyManager.builder().build(key, config)` |
| Redis Integration | `bucket4j-jcache` | `bucket4j-redis` |
| Configuration API | `BucketConfiguration.builder()` | `Bucket4j.configurationBuilder()` |

#### Migration Steps

1. **Update Dependencies:**
```xml
<!-- Old -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- New -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.3.0</version>
</dependency>
```

2. **Update ProxyManager Usage:**
```java
// Old 7.x
Bucket bucket = proxyManager.builderFor(key).build();

// New 8.x
Bucket bucket = proxyManager.builder().build(key, configuration);
```

3. **Update Configuration Builder:**
```java
// Old 7.x
BucketConfiguration config = BucketConfiguration.builder()
    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
    .build();

// New 8.x
BucketConfiguration config = Bucket4j.configurationBuilder()
    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
    .build();
```

## Troubleshooting

### Common Errors & Solutions

#### 1. WrongTypeOfReturnValue

**Error:**
```
org.mockito.exceptions.misusing.WrongTypeOfReturnValue
```

**Cause:** Incorrect mock setup for `proxyManager.builder()`

**Solution:**
```java
// Wrong
when(proxyManager.builder()).thenReturn(bucket);  // Returns wrong type

// Correct
when(proxyManager.builder()).thenReturn(builder);
when(builder.build(anyString(), any(BucketConfiguration.class)))
    .thenReturn(bucket);
```

#### 2. UnnecessaryStubbingException

**Error:**
```
org.mockito.exceptions.misusing.UnnecessaryStubbingException
```

**Cause:** Stubbing methods that aren't called

**Solution:**
```java
// Remove unnecessary stubs or use lenient()
lenient().when(bucket.someMethod()).thenReturn(someValue);
```

#### 3. Redis Connection Issues

**Symptoms:**
- Timeouts
- Connection refused
- Authentication failures

**Solutions:**
```yaml
# Check Redis configuration
spring:
  data:
    redis:
      host: ${redis.host:localhost}
      port: ${redis.port:6379}
      password: ${redis.password:}
      timeout: 5000ms  # Increase timeout
      lettuce:
        pool:
          max-active: 50  # Increase pool size
```

#### 4. Key Explosion

**Symptoms:**
- Memory usage increasing
- Slow Redis operations

**Solutions:**
- Implement key TTL expiration
- Use key prefix consolidation
- Monitor key count

```java
// Add TTL to Redis keys (if needed)
@RateLimit(key = "short-lived", limit = 100, duration = 300)  // 5 minutes
```

#### 5. Performance Issues

**Symptoms:**
- High latency
- Redis timeouts under load

**Solutions:**
- Use Redis cluster for high QPS
- Implement local caching for frequently accessed keys
- Consider async operations

```java
// Async rate limiting (advanced)
CompletableFuture<RateLimitDTO> future = CompletableFuture
    .supplyAsync(() -> rateLimitService.checkRateLimit(...));
```

## Performance & Scaling

### Latency Considerations

| Operation | Expected Latency | Impact |
|-----------|------------------|--------|
| Local bucket check | < 1ms | Negligible |
| Redis round-trip | 5-20ms | Significant for high QPS |
| Redis cluster | 10-50ms | Higher but scalable |

### Optimization Strategies

#### 1. Connection Pooling

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10
```

#### 2. Key Prefix Optimization

```java
// Bad: Too specific
String key = "rate_limit:IP:login:POST:/api/v1/auth/login:192.168.1.1";

// Better: Consolidated
String key = "rl:ip:login:192.168.1.1";
```

#### 3. Batch Operations

```java
// For multiple rate limit checks
Map<String, RateLimitDTO> results = keys.parallelStream()
    .collect(Collectors.toMap(
        key -> key,
        key -> rateLimitService.checkRateLimit(...)
    ));
```

### High QPS Scenarios

#### Redis Cluster Configuration

```yaml
# Redis cluster for >10k QPS
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node1:6379
          - redis-node2:6379
          - redis-node3:6379
```

#### Lua Script Optimization

```lua
-- Advanced: Use Lua scripts for atomic operations
local current = redis.call('GET', KEYS[1])
if current and tonumber(current) > 0 then
    redis.call('DECR', KEYS[1])
    return 1
else
    return 0
end
```

## Security Considerations

### Brute Force Mitigation

#### Login Endpoint Protection

```java
@RateLimit(
    key = "login", 
    limit = 5, 
    duration = 300,  // 5 minutes
    type = RateLimitType.IP,
    failClosed = true  // Block if Redis down
)
```

#### Account Lockout Integration

```java
public void handleFailedLogin(String username, String ip) {
    // Check rate limit
    RateLimitDTO ipLimit = rateLimitService.checkRateLimit(
        loginAnnotation, ip, "POST:/auth/login"
    );
    
    RateLimitDTO userLimit = rateLimitService.checkRateLimit(
        userLoginAnnotation, username, "POST:/auth/login"
    );
    
    // Implement progressive delays
    if (!ipLimit.isAllowed() || !userLimit.isAllowed()) {
        lockAccount(username, Duration.ofMinutes(30));
    }
}
```

### Key Security Practices

#### 1. Avoid Information Leakage

```java
// Don't expose internal keys in responses
@RateLimit(key = "internal-api-key", limit = 100, duration = 60)
```

#### 2. Implement Key Rotation

```java
// Periodic key rotation strategy
@Scheduled(fixedRate = 24 * 60 * 60 * 1000) // Daily
public void rotateRateLimitKeys() {
    // Implement key rotation logic
}
```

#### 3. Monitor Anomalies

```java
@Component
public class RateLimitMonitor {
    
    @EventListener
    public void handleRateLimitExceeded(RateLimitExceededEvent event) {
        // Alert on suspicious patterns
        if (event.getAttempts() > 100) {
            alertService.sendSecurityAlert(event);
        }
    }
}
```

## Monitoring & Observability

### Metrics to Collect

| Metric | Type | Description |
|--------|------|-------------|
| `rate_limit.requests.total` | Counter | Total rate limit checks |
| `rate_limit.allowed.total` | Counter | Allowed requests |
| `rate_limit.rejected.total` | Counter | Rejected requests |
| `rate_limit.redis.errors` | Counter | Redis operation failures |
| `rate_limit.latency` | Histogram | Check duration |

### Micrometer Integration

```java
@Component
public class RateLimitMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter requestsCounter;
    private final Counter rejectedCounter;
    
    public RateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestsCounter = Counter.builder("rate_limit.requests.total")
            .description("Total rate limit checks")
            .register(meterRegistry);
        this.rejectedCounter = Counter.builder("rate_limit.rejected.total")
            .description("Rejected requests")
            .register(meterRegistry);
    }
    
    public void recordCheck(RateLimitDTO result) {
        requestsCounter.increment();
        if (!result.isAllowed()) {
            rejectedCounter.increment();
        }
    }
}
```

### Health Checks

```java
@Component
public class RateLimitHealthIndicator implements HealthIndicator {
    
    private final RateLimitService rateLimitService;
    
    @Override
    public Health health() {
        try {
            // Test Redis connectivity
            RateLimitDTO result = rateLimitService.checkRateLimit(
                healthCheckAnnotation, "health-check", "GET:/health"
            );
            return Health.up()
                .withDetail("status", "operational")
                .build();
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("status", "redis-unavailable")
                .build();
        }
    }
}
```

## FAQ

### Q1: What happens if Redis goes down?

**A:** Depends on `failClosed` parameter:
- `failClosed = false` (default): Requests are allowed (fail-open)
- `failClosed = true`: Requests are blocked (fail-closed)

### Q2: Can I use multiple rate limits on the same endpoint?

**A:** Yes, but they must have different `key` values. Each key creates a separate bucket.

### Q3: How are keys expired from Redis?

**A:** Bucket4j automatically manages TTL based on the duration parameter. Keys expire after the refill period.

### Q4: Can I change rate limits without restarting?

**A:** No, current implementation uses annotation-based configuration. For dynamic limits, consider using database configuration.

### Q5: What's the performance impact?

**A:** Each check requires a Redis round-trip (5-20ms typical). For high QPS, consider Redis clustering or local caching.

### Q6: How do I test rate limits in development?

**A:** Use Testcontainers for Redis or configure a local Redis instance. The unit tests mock Redis dependencies.

### Q7: Can I use this with Spring Security?

**A:** Yes, integrate with Spring Security filters or method security annotations.

### Q8: What's the difference between IP and USER rate limiting?

**A:** 
- `IP`: Limits by client IP address
- `USER`: Limits by authenticated user ID
- `API`: Limits by API endpoint
- `GLOBAL`: System-wide limits

### Q9: How do I handle time zones?

**A:** All timestamps use UTC epoch seconds. Convert to local time only for display purposes.

### Q10: Can I implement custom rate limit strategies?

**A:** Yes, extend `RateLimitType` enum and implement custom logic in `buildKey()` method.

## Deployment Checklist

### Pre-Deployment

- [ ] Redis cluster is configured and tested
- [ ] Connection pool settings are optimized for expected load
- [ ] Rate limit configurations are reviewed for each endpoint
- [ ] Fail-open/fail-closed strategy is defined per endpoint
- [ ] Monitoring and alerting are configured
- [ ] Health checks include rate limit service

### Configuration Review

- [ ] Redis connection parameters are correct
- [ ] Rate limit values are appropriate for traffic expectations
- [ ] Key naming conventions are consistent
- [ ] TTL settings match business requirements

### Testing Validation

- [ ] Unit tests pass with >90% coverage
- [ ] Integration tests validate Redis behavior
- [ ] Load tests confirm performance under expected QPS
- [ ] Failover scenarios are tested (Redis downtime)

### Production Deployment

- [ ] Redis monitoring is active
- [ ] Rate limit metrics are collected
- [ ] Alert thresholds are configured
- [ ] Documentation is updated with current configurations
- [ ] Team is trained on troubleshooting procedures

### Post-Deployment

- [ ] Monitor rate limit effectiveness
- [ ] Check for false positives/negatives
- [ ] Adjust limits based on actual usage patterns
- [ ] Review performance impact
- [ ] Update documentation with lessons learned

## Release Notes Template

### Version X.X.X - Rate Limiting Enhancement

**Changes:**
- Implemented distributed rate limiting using Bucket4j and Redis
- Added @RateLimit annotation for declarative configuration
- Configured fail-open/fail-closed strategies per endpoint
- Enhanced security for authentication endpoints

**Impact:**
- Login endpoints limited to 5 attempts per 5 minutes per IP
- Public APIs limited to 100 requests per minute per IP
- User operations limited to 1000 requests per hour per user
- System maintains rate limits across multiple application instances

**Monitoring:**
- New metrics: rate_limit_requests_total, rate_limit_rejected_total
- Health check includes Redis connectivity
- Alerts configured for high rejection rates

**Rollback Plan:**
- Remove @RateLimit annotations if issues occur
- Disable rate limiting via configuration
- Monitor system behavior during rollback period

## Commands & Utilities

### Running Tests

```bash
# Run all rate limit tests
mvn test -Dtest="*RateLimit*"

# Run with coverage
mvn test jacoco:report -Dtest="*RateLimit*"

# Run integration tests
mvn test -Dtest="*RateLimitIntegration*"
```

### Redis Monitoring

```bash
# Monitor rate limit keys
redis-cli --scan --pattern "rate_limit:*"

# Check key expiration
redis-cli ttl "rate_limit:IP:login:POST:/api/v1/auth/login:192.168.1.1"

# Monitor Redis performance
redis-cli info stats
```

### Docker Test Setup

```bash
# Start Redis for testing
docker run -d -p 6379:6379 --name redis-test redis:7-alpine

# Run tests with Redis
mvn test -Dspring.data.redis.host=localhost -Dspring.data.redis.port=6379

# Clean up
docker stop redis-test && docker rm redis-test
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-02-26  
**Maintainer:** Backend Team  
**Review Required:** Yes (before production deployment)
