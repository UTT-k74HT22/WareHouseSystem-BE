# Security Audit (API Surface)

Scope audited: controllers/endpoints, DTO validation use, exception handling, Spring Security + JWT config, authorization annotations, CORS, auth flows.

## 1) Implemented Security Controls (What Exists)

## 1.1 Authentication & Authorization Controls

- Stateless security with JWT:
  - `SessionCreationPolicy.STATELESS`, CSRF disabled for REST (`SecurityConfig.java:67-71`).
  - `JwtAuthFilter` installed before `UsernamePasswordAuthenticationFilter` (`SecurityConfig.java:106`).
- Global auth policy:
  - Public: `/api/v1/auth/**`, Swagger/docs, health/info (`SecurityConfig.java:88-95`).
  - All other endpoints require authenticated user (`SecurityConfig.java:101`).
- Method-level authorization enabled:
  - `@EnableMethodSecurity` (`SecurityConfig.java:35`).
  - `@PreAuthorize` used on `HomeController` and `EmailController`.

## 1.2 JWT Handling Controls

- Token signature + expiry validation via `JwtProvider.validateToken` (`JwtProvider.java:132-155`).
- Startup guard for key strength:
  - Secret decoded and required to be >= 64 bytes for HS512 (`JwtProvider.java:38-46`).
- Token claims include `roles`, `userId`, and `type` (`JwtProvider.java:66-83`).

## 1.3 Abuse Prevention (Rate Limit)

- Rate-limit filter in security chain before JWT auth filter (`SecurityConfig.java:108-110`).
- Annotation-driven limits (`@RateLimit`) on sensitive endpoints:
  - Login: 5 req / 5 min, fail-closed (`AuthController.java:42-50`).
  - Refresh token: 10 req / min (`AuthController.java:65-72`).
- Redis + Lua sliding window implementation (`RateLimitService.java:53-80`).

## 1.4 Validation & Error Handling Controls

- Global validation/exception handling via `@RestControllerAdvice` (`GlobalExceptionHandle.java:20`).
- Handles:
  - `MethodArgumentNotValidException` and `ConstraintViolationException` => 400 with field errors (`GlobalExceptionHandle.java:42-81`).
  - Access denied => 403 (`GlobalExceptionHandle.java:102-110`).
  - Generic exception => 500, hides internal details in response (`GlobalExceptionHandle.java:144-150`).
- Dedicated security handlers:
  - Unauthorized => JSON 401 (`JwtAuthenticationEntryPoint.java:42-55`).
  - Forbidden => JSON 403 (`JwtAccessDeniedHandler.java:40-53`).

## 1.5 CORS Controls

- CORS configured in `SecurityConfig` and backup WebMVC config:
  - Allowed origins/methods/headers from config properties (`SecurityConfig.java:44-51`, `WebMvcConfig.java:27-34`).
  - `allowCredentials(true)` in both places (`SecurityConfig.java:144`, `WebMvcConfig.java:63`).
  - Exposed headers include `Authorization`, rate-limit headers (`SecurityConfig.java:136-141`).

---

## 2) Security Findings & Gaps (Actionable)

## F-01 (High): Refresh token validation logic appears inverted

- Evidence: `AuthServiceImpl.validRefreshToken` throws unauthorized **when `validateToken` returns true** (`AuthServiceImpl.java:103-106`).
- Why this matters:
  - `validateToken == true` indicates valid signature/expiry.
  - Current logic rejects valid refresh token flow.
  - Potential impact: refresh flow broken (availability/auth flow defect).
- Action:
  - Change condition to reject when `!jwtProvider.validateToken(refreshToken)`.
  - Add unit tests for valid token, expired token, wrong type token.

## F-02 (High): Refresh tokens can be accepted by auth filter as bearer access tokens

- Evidence:
  - JWT filter validates token and authenticates without checking `type` claim (`JwtAuthFilter.java:43-62`).
  - Token type is present in JWT (`JwtProvider.java:69`, `JwtProvider.java:81`, `JwtProvider.java:205-212`) but not enforced in filter.
- Why this matters:
  - If a refresh token leaks, it could potentially authenticate API calls where access token is expected.
- Action:
  - In `JwtAuthFilter`, enforce `type == accessToken`.
  - Reject any non-access token at filter level.

## F-03 (Medium): Authorization annotation mismatch (`hasAuthority('ADMIN')` vs `ROLE_ADMIN`)

- Evidence:
  - `CustomUserDetails` maps roles to `ROLE_*` authorities (`CustomUserDetails.java:22-27`).
  - `EmailController` uses `@PreAuthorize("hasAuthority('ADMIN')")` on most endpoints (`EmailController.java:67`, `81`, `105`, `124`, `143`, `176`, `190`, `204`).
- Why this matters:
  - `hasAuthority('ADMIN')` does not match `ROLE_ADMIN` unless a separate `ADMIN` authority exists.
  - Likely causes unintended 403 for admins or inconsistent behavior.
- Action:
  - Standardize on `hasRole('ADMIN')` OR `hasAuthority('ROLE_ADMIN')` consistently.
  - Add integration tests for admin endpoint access.

## F-04 (Medium): Email retry endpoint lacks explicit admin restriction

- Evidence:
  - `POST /api/v1/emails/{id}/retry` has no `@PreAuthorize` (`EmailController.java:161-167`).
  - Therefore only global authenticated check applies (`SecurityConfig.java:101`).
- Why this matters:
  - Non-admin authenticated users may trigger retry operations.
- Action:
  - Add `@PreAuthorize("hasRole('ADMIN')")` (or agreed RBAC rule).
  - Add access test for non-admin user (expect 403).

## F-05 (Medium): Sensitive/PII logging risk in request logs

- Evidence:
  - Logs full warehouse request object on create (`WareHouseController.java:40`).
  - Extensive auth logs with usernames and request metadata (`AuthController.java:52-55`, `JwtAuthenticationEntryPoint.java:36-40`).
- Why this matters:
  - May expose PII or operational details in logs.
- Action:
  - Mask/redact sensitive fields (email, phone, token-like values).
  - Use structured logging policy with allow-list fields.

## F-06 (Medium): Weak default JWT secret in application config fallback

- Evidence:
  - `app.jwt.secret: ${JWT_SECRET:default_jwt_secret_key}` (`src/main/resources/application.yml:130`).
- Why this matters:
  - If env var missing, app may start with weak/invalid fallback.
- Action:
  - Remove insecure default; require `JWT_SECRET` explicitly.
  - Fail startup when secret property absent.

## F-07 (Low/Medium): CORS + credentials can become unsafe under permissive env config

- Evidence:
  - `allowCredentials(true)` enabled (`SecurityConfig.java:144`, `WebMvcConfig.java:63`).
  - Origins/headers are env-driven (`SecurityConfig.java:44-51`); if misconfigured too wide, risk increases.
- Why this matters:
  - Credentialed cross-origin requests require strict origin allow-list.
- Action:
  - Enforce explicit origin allow-list per environment.
  - Add startup validation to reject wildcard origin with credentials.

## F-08 (Low): Validation coverage gaps on some input surfaces

- Evidence:
  - Product search request body has no `@Valid` and DTO fields unbounded (`ProductController.java:126-129`, `SearchProductRequest.java:16-52`).
  - Many path/query params have no explicit constraints.
- Why this matters:
  - Higher risk of malformed/oversized input reaching service/repository layers.
- Action:
  - Add request constraints where meaningful (length, enums, paging bounds).
  - Add centralized request-size and query-param validation standards.

## F-09 (Low): Error contract inconsistency between global handler and auth entry point

- Evidence:
  - `JwtAuthenticationEntryPoint` hardcodes errorCode `AUTH_401` + plain message (`JwtAuthenticationEntryPoint.java:47-49`).
  - Global handlers use `ErrorCode` enum and `BaseResponse.error(...)` pattern (`GlobalExceptionHandle.java:30-35`).
- Why this matters:
  - Inconsistent API error contracts complicate client handling and monitoring.
- Action:
  - Align auth entry point to shared `ErrorCode` catalog and standard response formatting.

---

## 3) Auth Flow Review (Login / Refresh)

## Login (`POST /api/v1/auth/login`)

- Controls present:
  - Public endpoint with `@Valid` request DTO.
  - Brute-force protection via rate limit (IP, fail-closed).
  - Password checked via `PasswordEncoder.matches`.
  - Inactive account blocked (`AccountStatus.INACTIVE`).
- Observed risk:
  - Logging usernames at info level may increase user enumeration signal in logs.

## Refresh (`POST /api/v1/auth/refresh-token`)

- Controls intended:
  - Public endpoint, `@Valid`, rate-limited by user.
  - Should validate token signature/expiry and ensure token type is refresh.
- Critical issue:
  - Validation condition appears inverted (see F-01), likely rejecting valid refresh flow.

---

## 4) Exception Handling & Security Behavior Summary

- Positive:
  - Security-specific 401/403 JSON handlers are implemented.
  - Generic exception response does not expose stack traces to clients.
- Watch-outs:
  - Several components still log raw exception messages (`JwtProvider.validateToken`, `GlobalExceptionHandle` malformed JSON branch), which should be reviewed against secure logging policy.

---

## 5) Prioritized Remediation Plan

1. **Fix refresh-token validation logic** and add unit/integration tests (F-01).
2. **Enforce token type in JWT filter** (access token only for API auth) (F-02).
3. **Normalize role/authority expressions** across all `@PreAuthorize` checks (F-03).
4. **Lock down email retry endpoint** to admin role (F-04).
5. **Harden config/logging**:
   - Remove weak JWT default secret fallback (F-06).
   - Implement PII-safe log masking (F-05).
6. **Strengthen input and error consistency** (F-08, F-09).

