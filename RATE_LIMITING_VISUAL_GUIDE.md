# Rate Limiting Architecture - Visual Guide

## 🏗️ Architecture Comparison

### ❌ OLD (v1.x) - Interceptor-based
```
┌──────────────────────────────────────────────────────────────┐
│                      HTTP REQUEST                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  Spring Security Filter Chain                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  JwtAuthFilter                                         │  │
│  │  - Extract & validate JWT                             │  │
│  │  - Set SecurityContext                                 │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  DispatcherServlet                                            │
│  - Route to controller                                        │
│  - Map to handler method                                      │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  RateLimitInterceptor ⚠️ (TOO LATE!)                         │
│  - Check @RateLimit annotation                                │
│  - Already authenticated & routed                             │
│  - Wasted resources if rate limited                           │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  Controller Method                                            │
└──────────────────────────────────────────────────────────────┘
```

### ✅ NEW (v2.0) - Filter-based
```
┌──────────────────────────────────────────────────────────────┐
│                      HTTP REQUEST                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  RateLimitFilter ✨ (ENTRY POINT!)                           │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  1. Get handler mapping                                │  │
│  │  2. Check @RateLimit annotation                        │  │
│  │  3. Build route key (METHOD:pattern)                   │  │
│  │  4. Extract TRUSTED client IP                          │  │
│  │  5. Call RateLimitService (Lua script)                 │  │
│  │  6. Return 429 immediately if exceeded                 │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                                │
│  ⚡ If blocked: No authentication, no routing, instant 429    │
└──────────────────────────────────────────────────────────────┘
                            ↓ (only if allowed)
┌──────────────────────────────────────────────────────────────┐
│  Spring Security Filter Chain                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  JwtAuthFilter                                         │  │
│  │  - Extract & validate JWT                             │  │
│  │  - Set SecurityContext                                 │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  DispatcherServlet → Controller                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔑 Redis Key Evolution

### ❌ OLD (v1.x) - Cardinality Explosion Risk
```
rate_limit:{TYPE}:{KEY}:{IDENTIFIER}

Examples:
✗ rate_limit:IP:login:192.168.1.1
✗ rate_limit:IP:login:192.168.1.2
✗ rate_limit:API:users:/api/v1/users/1      ⚠️
✗ rate_limit:API:users:/api/v1/users/2      ⚠️
✗ rate_limit:API:users:/api/v1/users/999    ⚠️
                                             └─> Infinite keys!
```

### ✅ NEW (v2.0) - Pattern-based Protection
```
rate_limit:{TYPE}:{KEY}:{ROUTE_KEY}:{IDENTIFIER}
                           └─> METHOD:pattern

Examples:
✓ rate_limit:IP:login:POST:/api/v1/auth/login:192.168.1.1
✓ rate_limit:USER:api:GET:/api/v1/products/{id}:john.doe
✓ rate_limit:API:users:GET:/api/v1/users/{id}:*
                       └─> Same pattern for /users/1, /users/2, /users/999!
```

---

## 📊 Lua Script Comparison

### ❌ OLD (v1.x) - Edge Cases & Extra Round-trip
```lua
local current = redis.call('get', KEYS[1])
if current and tonumber(current) >= tonumber(ARGV[1]) then
    return -1  -- Exceeded
end

current = redis.call('incr', KEYS[1])

-- ⚠️ PROBLEM: Only set TTL if counter == 1
if tonumber(current) == 1 then
    redis.call('expire', KEYS[1], ARGV[2])
end

return tonumber(ARGV[1]) - tonumber(current)
```

**Issues:**
1. ⚠️ If INCR fails halfway, key exists without TTL → memory leak
2. ⚠️ Need separate `TTL` call in Java → 2 round-trips
3. ⚠️ Edge case: key exists but TTL = -1

**Java code:**
```java
Long remaining = execute(script, ...);  // Round-trip 1
Long ttl = redisTemplate.getExpire();   // Round-trip 2 ⚠️
```

### ✅ NEW (v2.0) - Hardened & Single Round-trip
```lua
local current = redis.call('GET', KEYS[1])
local limit = tonumber(ARGV[1])
local duration = tonumber(ARGV[2])

-- Check exceeded BEFORE increment
if current and tonumber(current) >= limit then
    local ttl = redis.call('TTL', KEYS[1])
    if ttl < 0 then
        -- ✅ Fix edge case: key exists but no TTL
        redis.call('EXPIRE', KEYS[1], duration)
        ttl = duration
    end
    return {-1, ttl}  -- Return both values
end

current = redis.call('INCR', KEYS[1])

-- ✅ ALWAYS ensure TTL is set (hardened)
local ttl = redis.call('TTL', KEYS[1])
if ttl < 0 then
    redis.call('EXPIRE', KEYS[1], duration)
    ttl = duration
end

local remaining = limit - current
return {remaining, ttl}  -- ✅ Return both in one call
```

**Improvements:**
1. ✅ TTL ALWAYS set, no edge cases
2. ✅ Return `{remaining, ttl}` → single round-trip
3. ✅ Fix orphaned keys automatically

**Java code:**
```java
List<Long> result = execute(script, ...);  // Single round-trip! ✅
long remaining = result.get(0);
long ttl = result.get(1);
```

---

## 🔒 IP Trust Model

### ❌ OLD (v1.x) - Trust All (IP Spoofing Vulnerable)
```java
String[] headerNames = {
    "X-Forwarded-For",
    "X-Real-IP",
    "Proxy-Client-IP",
    // ... trust everything ⚠️
};

for (String header : headerNames) {
    String ip = request.getHeader(header);
    if (ip != null) {
        return ip;  // ⚠️ Attacker can spoof this!
    }
}
```

**Attack:**
```http
GET /api/v1/auth/login
X-Forwarded-For: 1.2.3.4  ← Attacker sets fake IP
                             Rate limit bypassed! ⚠️
```

### ✅ NEW (v2.0) - Trust Only Known Proxies
```java
String remoteAddr = request.getRemoteAddr();

// ✅ ONLY trust forwarded headers if request from known proxy
if (isTrustedProxy(remoteAddr)) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null) {
        return forwardedFor.split(",")[0].trim();
    }
} else {
    log.debug("Request not from trusted proxy, ignoring headers");
}

return remoteAddr;  // Default: actual connection IP
```

**Attack Prevented:**
```http
Direct Client → Server
X-Forwarded-For: 1.2.3.4  ← Ignored! Not from trusted proxy ✅
Using: remoteAddr (actual IP)

Trusted LB → Server
X-Forwarded-For: 1.2.3.4  ← Trusted! ✅
Using: 1.2.3.4 (client IP from LB)
```

---

## 🛡️ Fallback Mode Comparison

### ❌ OLD (v1.x) - Always Fail-Open
```java
// Redis down → ALWAYS allow requests
catch (Exception e) {
    log.error("Redis error");
    return new RateLimitDTO(true, ...);  // ⚠️ Allow even for login!
}
```

**Problem:**
- Login endpoint allows unlimited requests when Redis down
- Brute force attack possible during Redis outage ⚠️

### ✅ NEW (v2.0) - Per-Endpoint Control
```java
@PostMapping("/login")
@RateLimit(
    key = "login",
    limit = 5,
    duration = 300,
    failClosed = true  // ✅ Block when Redis down
)
```

```java
@GetMapping("/products")
@RateLimit(
    key = "products",
    limit = 100,
    duration = 60,
    failClosed = false  // ✅ Allow when Redis down (read-only)
)
```

**Service logic:**
```java
private RateLimitDTO handleRedisFallback(boolean failClosed) {
    if (failClosed) {
        // Sensitive endpoints: BLOCK
        return new RateLimitDTO(false, 0, limit, ...);
    } else {
        // Read endpoints: ALLOW
        return new RateLimitDTO(true, limit, limit, ...);
    }
}
```

---

## 📡 Response Headers - Semantic Correctness

### ❌ OLD (v1.x) - Always Send Retry-After
```http
HTTP/1.1 200 OK  ← Request allowed
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1704067200
Retry-After: 45  ⚠️ Why retry if allowed?
```

**Problem:** Client confused - should I retry or not?

### ✅ NEW (v2.0) - Retry-After Only on 429
```http
HTTP/1.1 200 OK  ← Request allowed
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1704067200
                 ✅ No Retry-After header
```

```http
HTTP/1.1 429 Too Many Requests  ← Blocked
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704067200
Retry-After: 45  ✅ Clear: retry after 45 seconds
```

**DTO Structure:**
```java
public class RateLimitDTO {
    private boolean allowed;
    private int remaining;
    private int limit;
    private long resetTime;      // Always present
    private Long retryAfter;     // ✅ null when allowed, value when blocked
}
```

---

## 🎯 Summary of Improvements

| Aspect | v1.x | v2.0 | Benefit |
|--------|------|------|---------|
| **Architecture** | Interceptor (late) | Filter (entry point) | ⚡ Early blocking |
| **Redis Calls** | 2 per request | 1 per request | ⚡ 50% faster |
| **TTL Guarantee** | ⚠️ Edge cases | ✅ Always set | 🛡️ No memory leaks |
| **Cardinality** | ⚠️ URI-based | ✅ Pattern-based | 💾 Memory efficient |
| **IP Trust** | ⚠️ Trust all | ✅ Trust proxies only | 🔒 Anti-spoofing |
| **Fallback** | ⚠️ Always allow | ✅ Per-endpoint | 🔒 Secure login |
| **Headers** | ⚠️ Always Retry-After | ✅ Only on 429 | 📡 Semantic |
| **JWT Logic** | ⚠️ Inverted | ✅ Correct | 🔒 Auth works |

**Result: Production-Ready Rate Limiting System!** 🎉
