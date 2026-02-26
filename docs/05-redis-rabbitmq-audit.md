# Redis/RabbitMQ/Async Infrastructure Audit

## 1) Scope & Method
- **Scope requested:** Redis configuration/usage, RabbitMQ configuration/usage, async processing patterns (events/listeners/schedulers), and relevant application properties.
- **Codebase scanned:** `src/main/java`, `src/main/resources`, `docker-compose.yml`, `pom.xml`.
- **Focus:** Compare what is **configured** vs what is **actually used in code paths**.

---

## 2) Redis Audit

### 2.1 What is configured
- Dependencies present:
  - `spring-boot-starter-data-redis`
  - `spring-session-data-redis`
  - `jedis`
- Runtime properties in `application.yml`:
  - `spring.data.redis.*` (host/port/password/timeout/pool)
  - `spring.session.store-type=redis`, `spring.session.timeout=1800s`, `spring.session.redis.namespace=spring:session`
- Custom Redis beans (`RedisConfig`):
  - `LettuceConnectionFactory` (standalone)
  - `RedisTemplate<String,Object>` with JSON serializer
  - `RedisCacheManager` default TTL = **1 hour**
- Caching globally enabled: `@EnableCaching` in `WhsApplication`.

### 2.2 What is actually used
1. **Rate limiting state in Redis** (`RateLimitService`)
   - Lua script + `RedisTemplate.execute(...)`
   - Key pattern: `rate_limit:{type}:{key}:{routeKey}:{identifier}`
   - TTL pattern: TTL = annotation `duration` seconds, enforced in Lua (including self-healing if TTL missing)
   - Invalidation: explicit delete only via `resetRateLimit(...)`; otherwise natural TTL expiry.

2. **Application cache usage (Spring Cache + RedisCacheManager)**
   - Used in `UnitsOfMeasureImpl`:
     - `@Cacheable(cacheNames="uom_list", key="'all'")`
     - `@Cacheable(cacheNames="uom", key="#id")`
     - `@CacheEvict` on create/update/delete
   - TTL pattern: default **1h** for all caches (no per-cache override found).
   - Invalidation strategy:
     - Create evicts `uom_list:all`
     - Update/delete evict all entries in `uom` + `uom_list`

3. **Redis in tests**
   - `application-test.yml` excludes Redis autoconfig and disables session store.

### 2.3 Configured but not effectively used / ambiguous
- `spring.session.store-type=redis` is configured, but security is explicitly `SessionCreationPolicy.STATELESS` (JWT). This implies Redis-backed HTTP session is likely unnecessary for main API flow.
- `jedis` dependency exists, but implementation uses Lettuce/Spring Data Redis APIs; no explicit Jedis usage found.
- `app.rate-limit` block exists in config, but no `@ConfigurationProperties` binding/consumption found; active limits are hardcoded via `@RateLimit` annotations.

---

## 3) RabbitMQ Audit

### 3.1 What is configured
- Dependency present: `spring-boot-starter-amqp`.
- Runtime properties in `application.yml`:
  - `spring.rabbitmq.host/port/username/password`
  - listener tuning (`acknowledge-mode`, `concurrency`, `max-concurrency`, `prefetch`)
  - producer retry (`spring.rabbitmq.template.retry.*`)
- `RabbitMQConfig` defines:
  - `CachingConnectionFactory` with publisher confirm + returns enabled
  - JSON message converter
  - `RabbitTemplate`
  - `RabbitAdmin`
- Email messaging topology (`RabbitMQEmailConfig`, profile `!test`):
  - Durable queue: `app.email.queue-name`
  - Topic exchange: `app.email.exchange-name`
  - Binding by `app.email.routing-key`
  - Dedicated listener container factory (concurrency 3..10, prefetch 1)

### 3.2 What is actually used
- **Producer:** `EmailProducerService.sendEmailToQueue(...)` uses `RabbitTemplate.convertAndSend(exchange, routingKey, payload)`.
- **Consumer:** `EmailConsumerService.consumeEmail(...)` via `@RabbitListener`.
- **Publish timing pattern:** in `EmailServiceImpl.sendEmailAsync(...)`, queue publish happens in `TransactionSynchronization.afterCommit()` to avoid sending before DB commit.
- **Retry behavior (business level):** retries are tracked in DB fields (`retryCount`, `maxRetry`) and re-queued by service/scheduler logic.

### 3.3 Queue/Exchange/Routing/Retry/DLQ patterns observed
- Queue: durable, single logical queue for email send.
- Exchange: topic exchange, single routing key default (`wms.email.send`).
- Routing: static property-based routing key.
- Retry:
  - Producer-side retry configured in Spring Rabbit template.
  - Business retry through DB status transitions (`FAILED`/`RETRY`) + scheduled/manual requeue.
- Dead-letter:
  - **No DLX/DLQ queue arguments** (`x-dead-letter-exchange`, `x-dead-letter-routing-key`) found.
  - No message TTL on queue/message for delayed retry pipeline.

### 3.4 Configured but not effectively used / partial
- Publisher confirms/returns are enabled in connection factory, but no explicit confirm/return callback handling found in producer flow (operational visibility may be limited).

---

## 4) Async Processing Patterns Audit

### 4.1 Patterns used
1. **Message-driven async**
   - RabbitMQ producer/consumer for email sending.
2. **Scheduler-driven async** (`@EnableScheduling` + `EmailScheduledService`)
   - Process pending emails every 5 minutes.
   - Retry failed emails every 30 minutes.
   - Cleanup old email logs daily at 02:00.
3. **After-commit trigger**
   - Uses transaction synchronization callback to enqueue only after successful commit.

### 4.2 Patterns not found
- No `@Async` method-based async execution.
- No domain event publishing/listening (`ApplicationEventPublisher`, `@EventListener`, `@TransactionalEventListener`) for this infra area.
- No explicit outbox table/pattern for guaranteed publish consistency.

---

## 5) Risks

1. **Session config mismatch risk**
   - Redis session store configured while security is stateless JWT; can cause confusion and operational overhead.

2. **No DLQ/dead-letter policy for email queue**
   - Poison messages or repeated processing failures are not isolated at broker level.

3. **At-least-once side effects without idempotency guard**
   - Requeue + scheduler/manual retries can produce duplicate sends unless downstream behavior is carefully guarded.

4. **Potential message loss window after DB commit**
   - Publish is after commit (good), but without outbox/relay there is still a failure window between commit and successful broker publish.

5. **Limited publisher observability**
   - Confirms/returns enabled but callback-based monitoring/alerting not evident.

6. **Cache eviction granularity trade-off**
   - `allEntries=true` on update/delete is safe but can create extra cache churn.

7. **Unused/partially used config debt**
   - `app.rate-limit` properties and `jedis` dependency appear unused; increases maintenance ambiguity.

---

## 6) Practical Recommendations

### High priority
1. **Decide session strategy explicitly**
   - If JWT-stateless only: remove/disable Redis session config and `spring-session-data-redis`.
   - If session needed for specific modules: document exact session use cases and keep config intentionally.

2. **Add DLQ pattern for email queue**
   - Define DLX + DLQ bindings and routing key conventions.
   - Route irrecoverable messages to DLQ for triage instead of repeated silent failures.

3. **Introduce outbox pattern for reliable publish**
   - Persist outbound events in DB, relay asynchronously to RabbitMQ, mark delivered on confirm.
   - Reduces post-commit publish loss window.

4. **Add idempotency key/guard for email send**
   - Ensure duplicate deliveries are prevented or harmless (e.g., dedupe by email_log_id/message_id).

### Medium priority
5. **Wire publisher confirm/return callbacks**
   - Log/metric unsuccessful routing and nack confirms with alerting.

6. **Externalize scheduler intervals**
   - Move fixed delays/cron to config properties for environment tuning.

7. **Refine cache strategy**
   - Add per-cache TTLs where needed and review whether broad `allEntries=true` can be narrowed.

8. **Clean configuration debt**
   - Remove `jedis` if not needed.
   - Either implement binding/use of `app.rate-limit` or remove stale block.

---

## 7) Quick Config-vs-Use Summary

| Area | Configured | Actually used | Gap |
|---|---|---|---|
| Redis connection | Yes | Yes | None |
| Redis cache manager (TTL 1h) | Yes | Yes (`uom`, `uom_list`) | No per-cache TTL tuning |
| Redis rate-limit keys | N/A (code-level) | Yes | No issue; strong Lua TTL handling |
| Redis HTTP session | Yes | Not evident in stateless JWT flow | Likely unnecessary unless specific session use case |
| RabbitMQ connection/template | Yes | Yes | Confirm/return callbacks not evident |
| Email queue/exchange/binding | Yes | Yes | Single-lane topology only |
| RabbitMQ DLQ/DLX | No | No | Missing failure isolation |
| Async scheduler | Yes (`@EnableScheduling`) | Yes | Intervals hardcoded in code |
| Event-driven app events | No | No | Optional capability not used |
| Outbox pattern | No | No | Reliability gap for post-commit publish |

