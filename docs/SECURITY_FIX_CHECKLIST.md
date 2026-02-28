# Security & Rate Limiting - Fix Checklist

**Date**: 27/02/2026  
**Status**: 🔴 4 Critical Issues Found

---

## 🔴 CRITICAL - FIX NGAY (P0)

### ✅ Issue #1: Token Type Validation Missing

**Problem**: Refresh token có thể dùng như access token trong Authorization header

**Files to fix**:
- [ ] `src/main/java/org/demo/whs/security/JwtProvider.java`
- [ ] `src/main/java/org/demo/whs/security/JwtAuthFilter.java`
- [ ] `src/main/java/org/demo/whs/service/impl/AuthServiceImpl.java` (if exists)

**Changes**:
```java
// JwtProvider.java - Add method
public String getTokenType(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.get("type", String.class);
}

// JwtAuthFilter.java - Add validation after validateToken()
String tokenType = jwtProvider.getTokenType(token);
if (!"accessToken".equals(tokenType)) {
    log.warn("Invalid token type: {} (expected: accessToken)", tokenType);
    filterChain.doFilter(request, response);
    return;
}
```

**Time**: 1-2 hours  
**Test**: Create test with refresh token in Authorization header, expect 401

---

### ✅ Issue #2: Authorization Annotation Mismatch

**Problem**: `hasAuthority('ADMIN')` không match với `ROLE_ADMIN` authority

**Files to fix**:
- [ ] `src/main/java/org/demo/whs/controller/EmailController.java`
- [ ] Search all files with `hasAuthority('ADMIN')`

**Changes**:
```java
// Đổi TẤT CẢ
@PreAuthorize("hasAuthority('ADMIN')")  // ❌ SAI
↓
@PreAuthorize("hasRole('ADMIN')")      // ✅ ĐÚNG
```

**Command to find**:
```bash
grep -r "hasAuthority('ADMIN')" src/
```

**Time**: 2-3 hours (including tests)  
**Test**: Admin user access EmailController endpoints, expect 200 not 403

---

### ✅ Issue #3: Refresh Token Validation Logic Inverted

**Problem**: Logic validation bị đảo ngược - reject valid token

**Files to fix**:
- [ ] Search for `validRefreshToken` method in AuthService/AuthServiceImpl

**Changes**:
```java
// BEFORE (SAI)
if (jwtProvider.validateToken(refreshToken)) {
    throw new UnauthorizedException(...);
}

// AFTER (ĐÚNG)
if (!jwtProvider.validateToken(refreshToken)) {
    throw new UnauthorizedException(ErrorCode.AUTH_006);
}

// Add token type check
String tokenType = jwtProvider.getTokenType(refreshToken);
if (!"refreshToken".equals(tokenType)) {
    throw new UnauthorizedException(ErrorCode.AUTH_006);
}
```

**Time**: 30 minutes  
**Test**: Refresh token flow end-to-end

---

### ✅ Issue #4: USER-Based Rate Limit Not Working

**Problem**: RateLimitFilter chạy trước JwtAuthFilter → SecurityContext chưa có authentication → getUserIdentifier() luôn return "anonymous"

**Root Cause**:
```
RateLimitFilter (check @RateLimit USER) 
  → SecurityContext = NULL ❌
    → JwtAuthFilter (set authentication)
      → SecurityContext = POPULATED ✅
```

**Solution**: Dual-pass filter approach

**Files to create**:
- [ ] `src/main/java/org/demo/whs/security/RateLimitUserFilter.java` (NEW)

**Files to modify**:
- [ ] `src/main/java/org/demo/whs/configuration/SecurityConfig.java`

**Implementation**:

1. **Create new filter** `RateLimitUserFilter.java`:
```java
@Component
public class RateLimitUserFilter extends OncePerRequestFilter {
    // Copy from RateLimitFilter
    // But ONLY handle RateLimitType.USER
    // Skip if type is IP/API/GLOBAL
}
```

2. **Update SecurityConfig.java**:
```java
// Current (IP/GLOBAL/API work, USER broken)
.addFilterBefore(rateLimitFilter, JwtAuthFilter.class)

// Change to (All types work)
.addFilterBefore(rateLimitFilter, JwtAuthFilter.class)           // IP/GLOBAL/API
.addFilterAfter(rateLimitUserFilter, JwtAuthFilter.class)        // USER only
```

3. **Update RateLimitFilter logic**:
```java
// Skip USER type in main filter
if (rateLimitAnnotation.type() == RateLimitType.USER) {
    filterChain.doFilter(request, response);
    return;
}
```

**Time**: 4-6 hours  
**Test**: Authenticated user hits refresh-token endpoint multiple times from same user, should hit rate limit per username not per IP

---

## 🟠 HIGH PRIORITY - FIX TRONG SPRINT (P1)

### ⬜ Move TRUSTED_PROXIES to Config

**Files**:
- [ ] `src/main/java/org/demo/whs/security/RateLimitFilter.java`
- [ ] `src/main/resources/application.yml`

**Changes**:
```yaml
# application.yml - Add new config
app:
  security:
    trusted-proxies:
      - 127.0.0.1
      - ::1
      - 10.0.0.0/8
      # Production: add actual load balancer IPs
```

```java
// RateLimitFilter.java
@Value("${app.security.trusted-proxies}")
private List<String> trustedProxies;
```

**Time**: 1 hour

---

### ⬜ JWT Secret Hardening

**Files**:
- [ ] `src/main/resources/application.yml`
- [ ] `src/main/java/org/demo/whs/security/JwtProvider.java`

**Changes**:
```yaml
# Remove default
app:
  jwt:
    secret: ${JWT_SECRET}  # No fallback - REQUIRED
```

```java
// JwtProvider.java @PostConstruct
if ("default_jwt_secret_key".equals(secret)) {
    throw new IllegalStateException("Cannot use default JWT secret in production!");
}
```

**Time**: 1 hour

---

## 🟡 MEDIUM PRIORITY - IMPROVEMENTS (P2)

### ⬜ Centralize Rate Limit Config

Move từ annotation → application.yml để dễ tune production

**Time**: 3-4 hours

---

### ⬜ CORS Startup Validation

Add @PostConstruct check: wildcard origin + credentials = reject

**Time**: 1 hour

---

### ⬜ PII Logging Protection

Mask username, IP, email trong application logs

**Time**: 2-3 hours

---

## 📊 PROGRESS TRACKER

- [x] Security review completed
- [ ] P0 Issue #1: Token type validation - **0%**
- [ ] P0 Issue #2: Authorization mismatch - **0%**
- [ ] P0 Issue #3: Refresh validation logic - **0%**
- [ ] P0 Issue #4: USER rate limit fix - **0%**
- [ ] P1: Trusted proxies config - **0%**
- [ ] P1: JWT secret hardening - **0%**

**Overall**: **0/6 Critical Items Fixed**

---

## 🧪 TESTING PLAN

### Critical Tests to Add

```java
// Test 1: Token type enforcement
@Test
void testJwtFilter_RefreshToken_ShouldReject() {
    // Given: refresh token in Authorization header
    // When: send request to protected endpoint
    // Then: 401 Unauthorized
}

// Test 2: Admin authorization
@Test
void testEmailController_AdminRole_ShouldAllow() {
    // Given: user with ROLE_ADMIN
    // When: GET /api/v1/emails
    // Then: 200 OK
}

// Test 3: Refresh token flow
@Test
void testRefreshToken_ValidToken_ShouldReturnNewAccessToken() {
    // Given: valid refresh token
    // When: POST /api/v1/auth/refresh-token
    // Then: 200 with new access token
}

// Test 4: USER-based rate limit
@Test
void testRateLimit_SameUser_DifferentIPs_ShouldHitLimit() {
    // Given: same user authenticated from 2 different IPs
    // When: both exceed USER-based limit
    // Then: both get 429 (same username = same rate limit bucket)
}
```

---

## 📞 QUESTIONS / BLOCKERS

- [ ] Xác nhận có AuthServiceImpl class không? (để fix issue #3)
- [ ] Môi trường production sử dụng load balancer nào? (để config trusted proxies)
- [ ] Team có test environment với Redis không? (để test rate limiting)

---

## 📚 RELATED DOCS

- Full review: `docs/SECURITY_RATELIMIT_REVIEW.md`
- Rate limiting guide: `documents/RateLimit/RATE_LIMITING.md`
- Security audit: `docs/03-security-audit.md`
