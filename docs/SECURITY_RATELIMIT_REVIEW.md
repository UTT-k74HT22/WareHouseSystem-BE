# Báo Cáo Đánh Giá Bảo Mật & Rate Limiting - Warehouse Management System

**Ngày đánh giá**: 27/02/2026  
**Phạm vi**: Security configuration, JWT authentication, Rate limiting mechanism, Filter chain order  
**Reviewer**: GitHub Copilot (uc_reviewer mode)

---

## 1. TỔNG QUAN & KẾT LUẬN NHANH

### ✅ Điểm Mạnh

1. **Kiến trúc Rate Limiting đúng flow**: 
   - Filter-based architecture ở entry point (TRƯỚC Security Filter Chain)
   - Sử dụng Redis + Bucket4j distributed backend
   - Hardened implementation với proper TTL handling

2. **Security layer structure tốt**:
   - Stateless JWT authentication
   - Proper exception handlers (401, 403, 429)
   - CORS configuration với explicit origins
   - Method-level authorization enabled

3. **Best practices được follow**:
   - Constructor injection
   - Global exception handling
   - Semantic HTTP status codes
   - Rate limit headers chuẩn (X-RateLimit-*)

### 🔴 Vấn Đề Nghiêm Trọng (PHẢI SỬA)

1. **[CRITICAL] Rate Limit Filter Order SAI** - User-based rate limit không hoạt động đúng
2. **[HIGH] Token type không được validate** - Refresh token có thể dùng như access token
3. **[HIGH] Authorization annotation mismatch** - hasAuthority('ADMIN') vs ROLE_ADMIN
4. **[MEDIUM] Inverted refresh token validation** - Logic validation bị đảo ngược

---

## 2. ĐÁNH GIÁ CHI TIẾT RATE LIMITING MECHANISM

### 2.1 ✅ Điểm Tốt - Architecture & Implementation

#### a) Filter Order Đúng Flow (Một phần)

```java
// SecurityConfig.java:106-110
.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
```

**Phân tích**:
- ✅ RateLimitFilter chạy TRƯỚC JwtAuthFilter → đúng nguyên tắc "fail fast"
- ✅ Ngăn chặn abuse trước khi xử lý authentication (tiết kiệm resources)
- ✅ IP-based rate limit hoạt động hoàn hảo ở entry point

**Flow hiện tại**:
```
HTTP Request 
  → RateLimitFilter (check @RateLimit annotation)
    → JwtAuthFilter (parse & validate JWT)
      → SecurityContext (set authentication)
        → Controller
```

#### b) Trusted IP Extraction - Security Hardening

```java
// RateLimitFilter.java:239-260
private String getTrustedClientIp(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    
    // Chỉ tin X-Forwarded-For nếu request từ trusted proxy
    if (isTrustedProxy(remoteAddr)) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        // ... lấy IP đầu tiên
    }
    
    return remoteAddr; // Default: remote address
}
```

**Đánh giá**: ✅ EXCELLENT
- Chống IP spoofing attack
- Chỉ tin forwarded headers từ trusted proxy IPs
- Có fallback an toàn về remoteAddr
- **Recommendation**: Nên move TRUSTED_PROXIES sang application.yml để config theo môi trường

#### c) Route Pattern Key - Cardinality Explosion Prevention

```java
// RateLimitFilter.java:165-182
private String buildRouteKey(HttpServletRequest request, HandlerMethod handlerMethod) {
    String method = request.getMethod();
    String pattern = (String) request.getAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    
    return method + ":" + pattern; // VD: "GET:/api/v1/products/{id}"
}
```

**Đánh giá**: ✅ EXCELLENT
- Sử dụng pattern thay vì actual URI → tránh cardinality explosion trong Redis
- `/products/1`, `/products/2`, `/products/999` → cùng key `GET:/api/v1/products/{id}`
- Memory efficient, predictable Redis key count

#### d) Bucket4j + Redis Distributed Backend

```java
// RedisConfig.java:133-147
@Bean
public ProxyManager<String> bucketProxyManager(RedissonClient redissonClient) {
    CommandAsyncExecutor executor = ((Redisson) redissonClient).getCommandExecutor();
    
    return RedissonBasedProxyManager
            .builderFor(executor)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
            )
            .build();
}
```

**Đánh giá**: ✅ GOOD
- Distributed rate limiting across multiple instances
- Token Bucket algorithm với greedy refill strategy
- Proper TTL strategy (10 phút after last write)
- Atomic operations via Lua script trong Bucket4j

#### e) Per-Endpoint Fallback Mode

```java
// @RateLimit annotation
boolean failClosed() default false;

// Usage:
@RateLimit(
    key = "login",
    limit = 5,
    duration = 300,
    type = RateLimitType.IP,
    failClosed = true  // CRITICAL endpoint: block nếu Redis down
)
```

**Đánh giá**: ✅ EXCELLENT
- Fail-open cho public endpoints (không block user khi Redis lỗi)
- Fail-closed cho sensitive endpoints (login, admin) → security-first
- Flexible per-endpoint configuration

#### f) Semantic Rate Limit Headers

```java
// RateLimitFilter.java:310-320
private void addRateLimitHeaders(HttpServletResponse response, RateLimitDTO dto) {
    response.setHeader("X-RateLimit-Limit", String.valueOf(dto.getLimit()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(dto.getRemaining()));
    response.setHeader("X-RateLimit-Reset", String.valueOf(dto.getResetTime()));
    
    // Retry-After CHỈ khi exceeded (429)
    if (!dto.isAllowed() && dto.getRetryAfter() != null) {
        response.setHeader("Retry-After", String.valueOf(dto.getRetryAfter()));
    }
}
```

**Đánh giá**: ✅ EXCELLENT
- Follow chuẩn HTTP rate limiting headers
- Semantic correctness: Retry-After chỉ khi 429
- Client-friendly (có đủ info để retry intelligently)

---

### 2.2 🔴 VẤN ĐỀ NGHIÊM TRỌNG #1: USER-BASED RATE LIMIT KHÔNG HOẠT ĐỘNG

#### Mô tả vấn đề

```java
// RateLimitFilter.java:291-302
private String getUserIdentifier() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication != null && authentication.isAuthenticated() 
        && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
    }
    
    return "anonymous";
}
```

**VẤN ĐỀ**: RateLimitFilter chạy **TRƯỚC** JwtAuthFilter → SecurityContext **CHƯA CÓ** authentication!

**Timeline thực tế**:
```
1. HTTP Request arrives
2. RateLimitFilter.doFilterInternal()
   ├─ Lấy @RateLimit annotation → type = USER
   ├─ Call getIdentifier(request, RateLimitType.USER)
   ├─ Call getUserIdentifier()
   └─ SecurityContextHolder.getContext().getAuthentication() → NULL! ❌
   
3. JwtAuthFilter.doFilterInternal() (chưa chạy đến đây)
   └─ Parse JWT và set authentication vào SecurityContext
```

**Hậu quả**:
- ✅ IP-based rate limit: Hoạt động OK
- ❌ USER-based rate limit: LUÔN trả về "anonymous" → KHÔNG hoạt động theo user thực
- ❌ API-based rate limit: OK (dùng URI)
- ✅ GLOBAL rate limit: OK (hardcoded "global")

**Impact**:
- Endpoint `/api/v1/auth/refresh-token` có `type = RateLimitType.USER` → KHÔNG rate limit đúng per-user
- Attacker có thể bypass bằng cách dùng nhiều access token khác nhau
- Mất tính distributed fairness per user

#### Root Cause Analysis

**Filter chain order**:
```
RateLimitFilter (position: BEFORE JwtAuthFilter)
  ↓
JwtAuthFilter (position: BEFORE UsernamePasswordAuthenticationFilter)
  ↓
UsernamePasswordAuthenticationFilter (Spring default)
```

**SecurityContext lifecycle**:
- SecurityContext CHƯA được populate ở thời điểm RateLimitFilter chạy
- JwtAuthFilter mới set authentication vào SecurityContext
- Do đó getUserIdentifier() luôn thấy authentication = null

---

### 2.3 🔴 GIẢI PHÁP CHO USER-BASED RATE LIMIT

Có **3 options** để fix:

#### Option 1: Dual-Pass Approach (RECOMMENDED) ⭐

**Ý tưởng**: RateLimitFilter chạy 2 lần check:
- Pass 1: Check IP-based và GLOBAL limits (BEFORE JwtAuthFilter)
- Pass 2: Check USER-based limits (AFTER JwtAuthFilter)

**Implementation**:
```java
// Tạo RateLimitFilter mới: RateLimitUserFilter
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Sau JwtAuthFilter
public class RateLimitUserFilter extends OncePerRequestFilter {
    // Chỉ xử lý USER-based rate limit
    // Lúc này SecurityContext đã có authentication
}

// SecurityConfig.java
.addFilterBefore(rateLimitFilter, JwtAuthFilter.class)           // IP/GLOBAL/API
.addFilterAfter(rateLimitUserFilter, JwtAuthFilter.class)        // USER
```

**Pros**:
- ✅ Giữ nguyên kiến trúc fail-fast cho IP-based
- ✅ USER-based rate limit hoạt động đúng
- ✅ Clear separation of concerns
- ✅ Không ảnh hưởng đến performance (filter only processes nếu có @RateLimit type=USER)

**Cons**:
- ❌ Thêm 1 filter class (nhưng code gần giống nhau)
- ❌ Phức tạp hơn một chút

#### Option 2: Parse JWT Manually in RateLimitFilter

**Ý tưởng**: RateLimitFilter tự parse JWT để lấy username, không đợi JwtAuthFilter

```java
// RateLimitFilter.java
private String getUserIdentifier(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7).trim();
        try {
            // Manual JWT parsing
            String username = jwtProvider.getUsernameFromToken(token);
            if (username != null) return username;
        } catch (Exception e) {
            // Invalid token → fallback to anonymous
        }
    }
    return "anonymous";
}
```

**Pros**:
- ✅ Không cần thêm filter
- ✅ USER-based rate limit hoạt động
- ✅ Vẫn fail-fast (1 filter pass)

**Cons**:
- ❌ Duplicate JWT parsing logic (JwtAuthFilter cũng parse)
- ❌ Performance overhead (parse JWT 2 lần)
- ❌ Tight coupling với JWT implementation

#### Option 3: Move RateLimitFilter AFTER JwtAuthFilter (NOT RECOMMENDED) ❌

**Ý tưởng**: Đổi order để RateLimitFilter chạy sau authentication

```java
// SecurityConfig.java
.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(rateLimitFilter, JwtAuthFilter.class)  // Changed: AFTER
```

**Pros**:
- ✅ USER-based rate limit hoạt động
- ✅ Đơn giản, không thay đổi code

**Cons**:
- ❌ KHÔNG CÒN FAIL-FAST: JWT parsing chạy trước rate limit
- ❌ Attacker có thể spam invalid JWT để tốn resources (JWT parsing/validation expensive)
- ❌ Vi phạm nguyên tắc security architecture (rate limit nên ở entry point)
- ❌ IP-based rate limit mất ý nghĩa (vì đã qua authentication rồi)

---

### 2.4 🟡 Vấn Đề Phụ - Rate Limiting

#### a) TRUSTED_PROXIES hardcoded

```java
// RateLimitFilter.java:77-84
private static final Set<String> TRUSTED_PROXIES = new HashSet<>(Arrays.asList(
    "127.0.0.1", "::1",
    "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
    // TODO: Add your actual load balancer/reverse proxy IPs
));
```

**Vấn đề**: 
- Hardcoded trong code, không config được theo environment
- Production cần add actual load balancer IPs

**Fix**:
```yaml
# application.yml
app:
  security:
    trusted-proxies:
      - 127.0.0.1
      - ::1
      - 10.240.0.0/16  # GCP Load Balancer range (example)
```

#### b) Rate Limit Config không centralized

```java
// Hiện tại: mỗi endpoint config trong annotation
@RateLimit(key = "login", limit = 5, duration = 300, ...)
```

**Vấn đề**: 
- Config scattered across controllers
- Khó điều chỉnh centrally (cần rebuild để thay đổi limit)

**Better approach**:
```yaml
# application.yml
app:
  rate-limit:
    endpoints:
      login:
        limit: 5
        duration: 300
        type: IP
        fail-closed: true
      refresh-token:
        limit: 10
        duration: 60
        type: USER
```

Annotation chỉ reference key:
```java
@RateLimit(configKey = "login")
public ResponseEntity<?> login(...) { }
```

---

## 3. ĐÁNH GIÁ CHI TIẾT SECURITY CONFIGURATION

### 3.1 ✅ Điểm Tốt - Security Foundation

#### a) Stateless JWT Architecture

```java
// SecurityConfig.java:67-71
.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.csrf(AbstractHttpConfigurer::disable)
.formLogin(AbstractHttpConfigurer::disable)
.httpBasic(AbstractHttpConfigurer::disable)
```

**Đánh giá**: ✅ EXCELLENT
- Đúng cho REST API stateless
- Không overhead session management
- CSRF disabled hợp lý (vì không dùng cookies cho auth)

#### b) JWT Secret Key Validation at Startup

```java
// JwtProvider.java:33-53
@PostConstruct
public void init() {
    byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
    
    if (keyBytes.length < 64) {
        throw new IllegalArgumentException(
            "JWT secret key must be at least 512 bits (64 bytes) for HS512..."
        );
    }
}
```

**Đánh giá**: ✅ EXCELLENT
- Fail-fast at startup nếu secret key yếu
- Enforce minimum 512 bits cho HS512 algorithm
- Clear error message

#### c) Security Exception Handlers

```java
// JwtAuthenticationEntryPoint.java (401)
// JwtAccessDeniedHandler.java (403)
// GlobalExceptionHandle.java (429, 400, 500)
```

**Đánh giá**: ✅ GOOD
- Consistent JSON error responses
- Proper HTTP status codes
- Không expose stack traces
- Log security events để audit

---

### 3.2 🔴 VẤN ĐỀ NGHIÊM TRỌNG #2: TOKEN TYPE KHÔNG ĐƯỢC VALIDATE

#### Mô tả vấn đề

```java
// JwtAuthFilter.java:43-62
String token = header.substring(BEARER_PREFIX.length()).trim();

if (!jwtProvider.validateToken(token)) {
    filterChain.doFilter(request, response);
    return;
}

String username = jwtProvider.getUsernameFromToken(token);
var userDetails = customUserDetailsService.loadUserByUsername(username);

// Create authentication token
UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

SecurityContextHolder.getContext().setAuthentication(authentication);
```

**VẤN ĐỀ**: Filter chỉ validate signature & expiry, KHÔNG kiểm tra token type!

**Token structure**:
```java
// Access Token claims
{
  "sub": "username",
  "roles": ["ROLE_ADMIN"],
  "userId": 123,
  "type": "accessToken",  // ← Không được check!
  "exp": 1234567890
}

// Refresh Token claims
{
  "sub": "username",
  "userId": 123,
  "type": "refreshToken",  // ← Có thể dùng như access token!
  "exp": 1234567890
}
```

**Attack scenario**:
1. Attacker lấy được refresh token (thường có expiry dài hơn - 24h)
2. Dùng refresh token trong Authorization header thay vì access token
3. JwtAuthFilter chỉ check signature & expiry → **PASS** ❌
4. Attacker được authenticated như normal user

**Impact**:
- 🔴 Security vulnerability: Token leakage risk cao hơn
- 🔴 Refresh token có lifetime dài → attack window lớn
- 🔴 Vi phạm OAuth2 / JWT best practices

#### Fix ngay

```java
// JwtAuthFilter.java - Thêm token type validation
if (!jwtProvider.validateToken(token)) {
    log.warn("Invalid JWT token");
    filterChain.doFilter(request, response);
    return;
}

// ✅ THÊM: Validate token type
String tokenType = jwtProvider.getTokenType(token);
if (!"accessToken".equals(tokenType)) {
    log.warn("Invalid token type for authentication: {} (expected: accessToken)", tokenType);
    filterChain.doFilter(request, response);
    return;
}

String username = jwtProvider.getUsernameFromToken(token);
// ... rest of authentication logic
```

```java
// JwtProvider.java - Thêm method
public String getTokenType(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.get("type", String.class);
}
```

---

### 3.3 🔴 VẤN ĐỀ NGHIÊM TRỌNG #3: AUTHORIZATION ANNOTATION MISMATCH

#### Mô tả vấn đề

```java
// CustomUserDetails.java:22-27
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .toList();
}
```

Role "ADMIN" → Authority "ROLE_ADMIN"

**NHƯNG**:

```java
// EmailController.java (nhiều chỗ)
@PreAuthorize("hasAuthority('ADMIN')")  // ❌ SAI! Tìm authority tên "ADMIN"
public ResponseEntity<?> getEmailLogById(...) { }

// HomeController.java
@PreAuthorize("hasRole('ADMIN')")  // ✅ ĐÚNG! Tìm authority "ROLE_ADMIN"
public ResponseEntity<?> adminHome() { }
```

**Vấn đề**:
- `hasAuthority('ADMIN')` tìm authority có tên **chính xác** là "ADMIN"
- Nhưng system tạo authority với prefix "ROLE_" → "ROLE_ADMIN"
- → Authorization check **LUÔN LUÔN FAIL** → 403 Forbidden

**Impact**:
- 🔴 Admin users không thể access EmailController endpoints
- 🔴 Security misconfiguration → business logic break
- 🔴 Test coverage gap (không có integration test cho admin access)

#### Fix Options

**Option A: Sửa annotation** (RECOMMENDED)
```java
// EmailController.java - Sửa TẤT CẢ
@PreAuthorize("hasRole('ADMIN')")  // hasRole tự động thêm prefix ROLE_
```

**Option B: Sửa CustomUserDetails**
```java
// CustomUserDetails.java - Bỏ prefix
.map(role -> new SimpleGrantedAuthority(role.getName()))  // "ADMIN" thay vì "ROLE_ADMIN"

// Và update tất cả annotation
@PreAuthorize("hasAuthority('ADMIN')")
```

**Option C: Standardize với hasAuthority**
```java
// CustomUserDetails.java - giữ nguyên prefix
// Annotation - dùng full authority name
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```

**Khuyến nghị**: **Option A** - Dùng `hasRole('ADMIN')` vì:
- ✅ Follow Spring Security convention
- ✅ Cleaner code (không phải viết "ROLE_" prefix)
- ✅ Ít thay đổi nhất

---

### 3.4 🟡 VẤN ĐỀ NGHIÊM TRỌNG #4: INVERTED REFRESH TOKEN VALIDATION

Vấn đề này đã được report trong security audit (F-01):

```java
// AuthServiceImpl.java:103-106 (giả định từ audit report)
private void validRefreshToken(String refreshToken) {
    if (jwtProvider.validateToken(refreshToken)) {  // ❌ SAI LOGIC!
        throw new UnauthorizedException(ErrorCode.AUTH_006);
    }
}
```

**Logic bị đảo ngược**: 
- `validateToken` trả về `true` = token hợp lệ
- Nhưng code throw exception khi `true` → reject valid token!

**Fix**:
```java
private void validRefreshToken(String refreshToken) {
    if (!jwtProvider.validateToken(refreshToken)) {  // ✅ ĐÚNG
        throw new UnauthorizedException(ErrorCode.AUTH_006);
    }
    
    // ✅ THÊM: Validate token type
    String tokenType = jwtProvider.getTokenType(refreshToken);
    if (!"refreshToken".equals(tokenType)) {
        throw new UnauthorizedException(ErrorCode.AUTH_006);
    }
}
```

---

### 3.5 🟡 Vấn Đề Phụ - Security

#### a) Weak Default JWT Secret

```yaml
# application.yml:130
app:
  jwt:
    secret: ${JWT_SECRET:default_jwt_secret_key}  # ❌ Weak default
```

**Vấn đề**: 
- Nếu env var JWT_SECRET không set → dùng default yếu
- App có thể start trong production với secret key không an toàn

**Fix**:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # No default - REQUIRED env var
```

```java
// JwtProvider.java @PostConstruct - Thêm check
if ("default_jwt_secret_key".equals(secret)) {
    throw new IllegalStateException(
        "Cannot start with default JWT secret! Set JWT_SECRET environment variable."
    );
}
```

#### b) CORS + Credentials Without Strict Origins

```java
// SecurityConfig.java:144
config.setAllowCredentials(true);

// Origins từ env var
@Value("${app.cors.allowed-origins}")
private String allowedOrigins;  // http://localhost:4200,http://localhost:5173,...
```

**Vấn đề**:
- `allowCredentials(true)` + wildcard origin = security risk
- Hiện tại origins từ config → OK, nhưng cần validation

**Fix - Add startup validation**:
```java
@PostConstruct
public void validateCorsConfig() {
    if ("*".equals(allowedOrigins) && corsConfig.isAllowCredentials()) {
        throw new IllegalStateException(
            "Cannot use wildcard CORS origin with credentials enabled!"
        );
    }
}
```

#### c) Sensitive Data Logging

```java
// AuthController.java:52-55
log.debug("Login attempt for username: {}", request.getUsername());
log.debug("Client IP: {}", clientIp);
log.info("User logged in successfully: {} from IP: {}", request.getUsername(), clientIp);
```

**Vấn đề**:
- Username + IP trong logs → PII exposure risk
- Logs có thể được aggregate vào central logging system

**Fix**: 
- Mask PII trong logs
- Hoặc chỉ log hashed/obfuscated username
- Use audit trail table thay vì application logs cho sensitive events

---

## 4. KIỂM TRA THÊM - TEST COVERAGE

### Test Files Review

```
✅ RateLimitIntegrationTest.java - Mock-based test
✅ RateLimitServiceTest.java - Service unit test
✅ RateLimitInterceptorTest.java - Interceptor test (legacy)
```

**Gap phát hiện**:
- ❌ Không có test cho USER-based rate limit scenario
- ❌ Không có test cho refresh token type validation
- ❌ Không có test cho admin authorization mismatch

**Cần thêm test**:
```java
@Test
void testRateLimit_UserBased_ShouldUseAuthenticatedUser() {
    // Test USER-based rate limit với authentication
    // Verify rate limit key sử dụng username thay vì "anonymous"
}

@Test
void testJwtFilter_RefreshToken_ShouldBeRejected() {
    // Test refresh token không thể dùng như access token
    // Expected: filter không set authentication vào SecurityContext
}

@Test
void testAdminEndpoint_WithAdminRole_ShouldAllow() {
    // Test @PreAuthorize với ADMIN role
    // Verify admin có thể access EmailController endpoints
}
```

---

## 5. KHUYẾN NGHỊ ƯU TIÊN - ACTION ITEMS

### 🔴 P0 - CRITICAL (Fix ngay - Security risks)

1. **Fix Token Type Validation** (1-2 giờ)
   - [ ] Thêm `getTokenType()` method vào JwtProvider
   - [ ] Validate token type trong JwtAuthFilter
   - [ ] Validate token type trong refresh token flow
   - [ ] Thêm unit tests
   - **Impact**: Prevent refresh token abuse

2. **Fix Authorization Annotation Mismatch** (2-3 giờ)
   - [ ] Đổi tất cả `hasAuthority('ADMIN')` → `hasRole('ADMIN')` trong EmailController
   - [ ] Verify tất cả @PreAuthorize annotation trong project
   - [ ] Thêm integration tests cho admin endpoints
   - **Impact**: Admin endpoints hoạt động đúng

3. **Fix Refresh Token Validation Logic** (30 phút)
   - [ ] Đảo ngược logic trong `validRefreshToken()`
   - [ ] Thêm token type check
   - [ ] Test refresh token flow end-to-end
   - **Impact**: Refresh token flow hoạt động

### 🟠 P1 - HIGH (Fix trong sprint này - Functionality issues)

4. **Fix USER-Based Rate Limit** (4-6 giờ)
   - [ ] Implement Option 1: Dual-pass filter approach
   - [ ] Tạo RateLimitUserFilter (chạy sau JwtAuthFilter)
   - [ ] Update SecurityConfig filter chain
   - [ ] Test USER-based rate limit với authenticated users
   - [ ] Update documentation
   - **Impact**: Per-user rate limit hoạt động đúng

5. **Move TRUSTED_PROXIES to Config** (1 giờ)
   - [ ] Thêm config vào application.yml
   - [ ] Update RateLimitFilter để đọc từ @Value
   - [ ] Document production proxy IPs
   - **Impact**: Flexible proxy configuration per environment

### 🟡 P2 - MEDIUM (Fix trong 2 sprint tới - Improvements)

6. **Centralize Rate Limit Configuration** (3-4 giờ)
   - [ ] Move rate limit configs vào application.yml
   - [ ] Update @RateLimit annotation để support configKey
   - [ ] Create configuration loader service
   - [ ] Maintain backward compatibility

7. **JWT Secret Validation Hardening** (1 giờ)
   - [ ] Remove default fallback trong application.yml
   - [ ] Add startup check cho default secret
   - [ ] Update deployment documentation

8. **CORS Validation at Startup** (1 giờ)
   - [ ] Add @PostConstruct validation
   - [ ] Reject wildcard origin + credentials
   - [ ] Add environment-specific origin validation

### 🔵 P3 - LOW (Nice to have - Best practices)

9. **Improve Logging Security** (2-3 giờ)
   - [ ] Implement PII masking utility
   - [ ] Update log statements để mask sensitive data
   - [ ] Create audit trail table cho security events

10. **Expand Test Coverage** (4-6 giờ)
    - [ ] Add USER-based rate limit integration tests
    - [ ] Add token type validation tests
    - [ ] Add authorization tests cho tất cả admin endpoints
    - [ ] Add security regression test suite

---

## 6. TỔNG KẾT ĐÁNH GIÁ

### Score Card

| Category | Score | Comment |
|----------|-------|---------|
| **Rate Limiting Architecture** | 7/10 | Excellent design, nhưng USER-based không hoạt động |
| **JWT Implementation** | 6/10 | Good foundation, thiếu token type validation |
| **Security Configuration** | 7/10 | Solid structure, authorization mismatch cần fix |
| **Error Handling** | 8/10 | Consistent & semantic |
| **CORS & Headers** | 8/10 | Well configured |
| **Test Coverage** | 6/10 | Có tests nhưng thiếu critical scenarios |
| **Documentation** | 9/10 | Excellent documentation |
| **Overall** | **7.3/10** | **Good foundation, cần fix critical issues** |

### Final Verdict

**Hệ thống có foundation tốt về security & rate limiting**, nhưng có **4 vấn đề nghiêm trọng** cần fix ngay:

1. 🔴 **USER-based rate limit không hoạt động** (do filter order vs SecurityContext lifecycle)
2. 🔴 **Token type không được validate** (refresh token có thể abuse)
3. 🔴 **Authorization annotation mismatch** (admin endpoints broken)
4. 🟡 **Refresh token validation logic bị đảo ngược** (flow broken)

**Sau khi fix 4 issues trên**, hệ thống sẽ đạt **security production-ready standard**.

---

## 7. REFERENCES

- [Spring Security Filter Chain Order](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-filterchain)
- [Bucket4j Documentation](https://bucket4j.com/)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- Project docs:
  - `documents/RateLimit/RATE_LIMITING.md`
  - `documents/RateLimit/RATE_LIMITING_TECHNICAL_GUIDE.md`
  - `docs/03-security-audit.md`

---

**END OF REVIEW**
