# Rate Limiting Flow - Visual Analysis

## 🔴 VẤN ĐỀ: USER-Based Rate Limit Không Hoạt Động

### Current Flow (BROKEN for USER type)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST                                 │
│                   GET /api/v1/auth/refresh-token                    │
│                   Header: Authorization: Bearer <JWT>                │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  1️⃣  RateLimitFilter (BEFORE JwtAuthFilter)                        │
│                                                                       │
│  • Lấy @RateLimit annotation                                         │
│    → type = RateLimitType.USER                                       │
│    → limit = 10, duration = 60s                                      │
│                                                                       │
│  • Call getUserIdentifier()                                          │
│    ┌──────────────────────────────────────────────────────────┐     │
│    │ Authentication auth =                                     │     │
│    │   SecurityContextHolder.getContext().getAuthentication()│     │
│    │                                                           │     │
│    │ Result: auth = NULL ❌                                    │     │
│    │ Return: "anonymous"                                       │     │
│    └──────────────────────────────────────────────────────────┘     │
│                                                                       │
│  • Build Redis key:                                                  │
│    rate_limit:USER:refresh-token:POST:/api/v1/auth/refresh:anonymous│
│                                    ↑                          ↑      │
│                            route pattern              WRONG! Should  │
│                                                       be username    │
│                                                                       │
│  • Check Redis bucket for "anonymous"                                │
│    → All users share same bucket! ❌                                 │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  2️⃣  JwtAuthFilter (AFTER RateLimitFilter)                         │
│                                                                       │
│  • Extract JWT from Authorization header                             │
│  • Validate signature & expiry                                       │
│  • Extract username from token → "john.doe"                          │
│  • Load UserDetails                                                  │
│  • Create Authentication object                                      │
│  • Set to SecurityContext ✅                                         │
│    ┌──────────────────────────────────────────────────────────┐     │
│    │ SecurityContextHolder.getContext()                        │     │
│    │   .setAuthentication(authentication)                     │     │
│    │                                                           │     │
│    │ Now: auth.getName() = "john.doe" ✅                       │     │
│    └──────────────────────────────────────────────────────────┘     │
│                                                                       │
│  TOO LATE! Rate limit đã check xong ở step 1                        │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  3️⃣  Controller (AuthController.refreshToken)                      │
│                                                                       │
│  • Business logic executes                                           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Problem Illustration

```
User A (john.doe)     →  10 requests  →  Key: rate_limit:USER:...:anonymous
                                         Bucket: 10/10 consumed ✅

User B (jane.smith)   →  10 requests  →  Key: rate_limit:USER:...:anonymous
                                         Bucket: 20/10 EXCEEDED ❌ BLOCKED!

User C (bob.johnson)  →  10 requests  →  Key: rate_limit:USER:...:anonymous
                                         Bucket: 30/10 EXCEEDED ❌ BLOCKED!
```

**Result**: 
- ❌ Tất cả users chia sẻ cùng 1 bucket "anonymous"
- ❌ User A exhausts limit → User B & C bị block
- ❌ Không fair distribution per user
- ❌ Mục đích USER-based rate limit bị thất bại

---

## ✅ GIẢI PHÁP: Dual-Pass Filter Approach

### Fixed Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST                                 │
│                   GET /api/v1/auth/refresh-token                    │
│                   Header: Authorization: Bearer <JWT>                │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  1️⃣  RateLimitFilter (BEFORE JwtAuthFilter)                        │
│                                                                       │
│  • Lấy @RateLimit annotation                                         │
│    → type = RateLimitType.USER                                       │
│                                                                       │
│  • Check type:                                                       │
│    if (type == RateLimitType.USER) {                                │
│        // SKIP! Let RateLimitUserFilter handle it                   │
│        filterChain.doFilter(request, response);                     │
│        return;                                                       │
│    }                                                                 │
│                                                                       │
│  • Only handle IP, API, GLOBAL types here                           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  2️⃣  JwtAuthFilter                                                  │
│                                                                       │
│  • Parse & validate JWT                                              │
│  • Set authentication to SecurityContext ✅                          │
│    → Now: SecurityContext.getAuthentication() = john.doe            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  3️⃣  RateLimitUserFilter (NEW - AFTER JwtAuthFilter)               │
│                                                                       │
│  • Lấy @RateLimit annotation                                         │
│    → type = RateLimitType.USER                                       │
│                                                                       │
│  • ONLY process if type == USER, else skip                          │
│                                                                       │
│  • Call getUserIdentifier()                                          │
│    ┌──────────────────────────────────────────────────────────┐     │
│    │ Authentication auth =                                     │     │
│    │   SecurityContextHolder.getContext().getAuthentication()│     │
│    │                                                           │     │
│    │ Result: auth = POPULATED ✅                               │     │
│    │ Return: "john.doe"                                        │     │
│    └──────────────────────────────────────────────────────────┘     │
│                                                                       │
│  • Build Redis key:                                                  │
│    rate_limit:USER:refresh-token:POST:/api/v1/auth/refresh:john.doe │
│                                                               ↑      │
│                                                         CORRECT! ✅  │
│                                                                       │
│  • Check Redis bucket for "john.doe"                                 │
│    → Each user has separate bucket ✅                                │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│  4️⃣  Controller (AuthController.refreshToken)                      │
│                                                                       │
│  • Business logic executes                                           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Fixed Behavior

```
User A (john.doe)     →  10 requests  →  Key: rate_limit:USER:...:john.doe
                                         Bucket: 10/10 consumed ✅
                                         11th request: 429 ✅

User B (jane.smith)   →  10 requests  →  Key: rate_limit:USER:...:jane.smith
                                         Bucket: 10/10 consumed ✅
                                         11th request: 429 ✅

User C (bob.johnson)  →  10 requests  →  Key: rate_limit:USER:...:bob.johnson
                                         Bucket: 10/10 consumed ✅
                                         11th request: 429 ✅
```

**Result**: 
- ✅ Mỗi user có bucket riêng
- ✅ Fair distribution per user
- ✅ USER-based rate limit hoạt động đúng mục đích

---

## 📊 Filter Chain Order Comparison

### Current (Partial Working)

```
┌──────────────────────────────────────┐
│  Filter Order                        │
├──────────────────────────────────────┤
│  1. RateLimitFilter                  │  ← IP ✅, API ✅, GLOBAL ✅, USER ❌
│     (BEFORE JwtAuthFilter)           │
│                                      │
│  2. JwtAuthFilter                    │  ← Sets SecurityContext
│     (BEFORE UsernamePassword...)     │
│                                      │
│  3. UsernamePasswordAuth...Filter    │
│                                      │
│  4. Controllers                      │
└──────────────────────────────────────┘
```

### Fixed (All Working)

```
┌──────────────────────────────────────┐
│  Filter Order                        │
├──────────────────────────────────────┤
│  1. RateLimitFilter                  │  ← IP ✅, API ✅, GLOBAL ✅
│     (BEFORE JwtAuthFilter)           │     (skip USER type)
│                                      │
│  2. JwtAuthFilter                    │  ← Sets SecurityContext
│     (BEFORE UsernamePassword...)     │
│                                      │
│  2.5 RateLimitUserFilter (NEW)       │  ← USER ✅
│     (AFTER JwtAuthFilter)            │     (only USER type)
│                                      │
│  3. UsernamePasswordAuth...Filter    │
│                                      │
│  4. Controllers                      │
└──────────────────────────────────────┘
```

---

## 🔑 Key Insights

### Why This Matters

1. **Security Context Lifecycle**
   - SecurityContext is thread-local
   - Only populated AFTER authentication filter runs
   - Filters before auth filter see NULL authentication

2. **Rate Limit Type Requirements**
   ```
   IP-based    → No auth needed → Can run BEFORE auth ✅
   API-based   → No auth needed → Can run BEFORE auth ✅
   GLOBAL      → No auth needed → Can run BEFORE auth ✅
   USER-based  → Auth REQUIRED  → Must run AFTER auth ✅
   ```

3. **Fail-Fast Principle**
   - Rate limiting should be early in chain (save resources)
   - BUT: Only for types that don't need authentication
   - USER-based is exception: needs auth → must be after auth filter

### Trade-offs

| Approach | Pros | Cons |
|----------|------|------|
| **Current (1 filter before auth)** | ✅ Fail-fast for IP/API/GLOBAL<br>✅ Simple | ❌ USER-based broken |
| **All filters after auth** | ✅ All types work<br>✅ Simple | ❌ No fail-fast<br>❌ JWT parsing before rate limit<br>❌ Resource waste |
| **Dual-pass (RECOMMENDED)** | ✅ Fail-fast for IP/API/GLOBAL<br>✅ USER-based works<br>✅ Best of both | ⚠️ Two filter classes<br>⚠️ Slightly more complex |

---

## 💡 Implementation Notes

### Code Changes Summary

1. **Keep existing RateLimitFilter**
   - Add check: skip if type == USER
   - Handle IP, API, GLOBAL types only

2. **Create new RateLimitUserFilter**
   - Copy logic from RateLimitFilter
   - Add check: process ONLY if type == USER
   - Skip all other types

3. **Update SecurityConfig**
   ```java
   .addFilterBefore(rateLimitFilter, JwtAuthFilter.class)      // IP/API/GLOBAL
   .addFilterAfter(rateLimitUserFilter, JwtAuthFilter.class)   // USER
   ```

4. **No annotation changes needed**
   - `@RateLimit(type = RateLimitType.USER)` works as-is
   - Transparent to controller code

### Testing Strategy

```java
// Test 1: IP-based still works early (before auth)
@Test
void testIpRateLimit_NoAuth_ShouldWork() {
    // No Authorization header
    // Hit IP rate limit
    // Expect: 429 before authentication
}

// Test 2: USER-based now works (after auth)
@Test
void testUserRateLimit_WithAuth_ShouldUsUsername() {
    // With Authorization header for user "john.doe"
    // Hit USER rate limit
    // Verify Redis key contains "john.doe" not "anonymous"
}

// Test 3: Multiple users, separate buckets
@Test
void testUserRateLimit_DifferentUsers_SeparateBuckets() {
    // User A: 10 requests → OK
    // User B: 10 requests → OK (not affected by A)
    // User A: 11th request → 429 (A's bucket full)
    // User B: 11th request → 429 (B's bucket full)
}
```

---

## 📚 References

- Spring Security Filter Chain: https://docs.spring.io/spring-security/reference/servlet/architecture.html
- SecurityContext Threading: https://docs.spring.io/spring-security/reference/features/authentication/persistence.html
- Bucket4j Distributed: https://bucket4j.com/
