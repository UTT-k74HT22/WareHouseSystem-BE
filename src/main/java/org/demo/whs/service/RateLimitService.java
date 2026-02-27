package org.demo.whs.service;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.utils.annotation.RateLimit;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service chịu trách nhiệm xử lý rate limiting sử dụng Bucket4j
 * với Redis distributed backend (thông qua ProxyManager).
 *
 * <p>
 * Kiến trúc:
 * Copect ntroller → As→ RateLimitService → Bucket4j → Redis
 * </p>
 *
 * <p>
 * Thuật toán sử dụng: Token Bucket (Refill.greedy)
 *
 * - Mỗi request sẽ consume 1 token
 * - Khi hết token → request bị reject
 * - Token sẽ được refill đều đặn theo duration cấu hình
 * </p>
 *
 * <p>
 * Redis đóng vai trò lưu trạng thái bucket để đảm bảo:
 * - Distributed consistency
 * - Multi-instance support
 * - Atomic operation
 * </p>
 *
 * <p>
 * Fallback behavior:
 * - failClosed = true  → Block request nếu Redis lỗi
 * - failClosed = false → Allow request nếu Redis lỗi
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    /**
     * ProxyManager từ Bucket4j Redis extension.
     *
     * ProxyManager chịu trách nhiệm:
     * - Tạo distributed bucket
     * - Lưu state vào Redis
     * - Đồng bộ giữa nhiều instance
     */
    private final ProxyManager<String> proxyManager;

    /**
     * Kiểm tra request có vượt quá rate limit hay không.
     *
     * @param rateLimitAnnotation cấu hình từ annotation @RateLimit
     * @param identifier          định danh (IP address / username / API route...)
     * @param routeKey            key của route (METHOD + pattern)
     * @return RateLimitDTO chứa trạng thái rate limit
     */
    public RateLimitDTO checkRateLimit(RateLimit rateLimitAnnotation,
                                       String identifier,
                                       String routeKey) {

        String key = buildKey(
                rateLimitAnnotation.type(),
                rateLimitAnnotation.key(),
                identifier,
                routeKey
        );

        int limit = rateLimitAnnotation.limit();
        int duration = rateLimitAnnotation.duration();
        boolean failClosed = rateLimitAnnotation.failClosed();

        try {

            /*
             * Cấu hình bandwidth:
             *
             * - capacity = limit
             * - refill strategy = greedy (refill toàn bộ capacity sau duration)
             */
            Bandwidth bandwidth = Bandwidth.classic(
                    limit,
                    Refill.greedy(limit, Duration.ofSeconds(duration))
            );

            BucketConfiguration configuration = Bucket4j.configurationBuilder()
                    .addLimit(bandwidth)
                    .build();

            /*
             * Tạo hoặc lấy bucket distributed từ Redis.
             *
             * Nếu bucket chưa tồn tại → sẽ được tạo tự động.
             */
            Bucket bucket = proxyManager.builder().build(key, configuration);

            /*
             * Consume 1 token và lấy thông tin remaining + thời gian refill.
             */
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            long now = System.currentTimeMillis() / 1000;

            /*
             * Nếu không consume được → vượt quá rate limit.
             */
            if (!probe.isConsumed()) {

                long waitSeconds =
                        probe.getNanosToWaitForRefill() / 1_000_000_000;

                long resetTime = now + waitSeconds;

                log.warn("Rate limit exceeded for key: {}", key);

                return new RateLimitDTO(
                        false,          // allowed
                        0,              // remaining
                        limit,          // limit
                        resetTime,      // resetTime (epoch seconds)
                        waitSeconds     // retryAfter
                );
            }

            /*
             * Nếu request được phép.
             */
            long remaining = probe.getRemainingTokens();

            return new RateLimitDTO(
                    true,               // allowed
                    (int) remaining,    // remaining tokens
                    limit,              // configured limit
                    now,                // resetTime (current)
                    null                // retryAfter (null vì không bị block)
            );

        } catch (Exception e) {

            log.error("Redis error for key: {}", key, e);

            /*
             * Nếu Redis lỗi:
             *
             * - failClosed = true  → block request
             * - failClosed = false → allow request
             */
            if (failClosed) {
                long now = System.currentTimeMillis() / 1000;
                return new RateLimitDTO(
                        false,
                        0,
                        limit,
                        now + duration,
                        (long) duration
                );
            }

            // fail-open mode
            long now = System.currentTimeMillis() / 1000;
            return new RateLimitDTO(
                    true,
                    limit,
                    limit,
                    now + duration,
                    null
            );
        }
    }

    /**
     * Xây dựng Redis key theo format:
     *
     * rate_limit:{type}:{key}:{routeKey}:{identifier}
     *
     * Ví dụ:
     * rate_limit:IP:login:POST:/api/v1/auth/login:192.168.1.1
     * rate_limit:USER:refresh-token:POST:/auth/refresh:john.doe
     *
     * @param type       loại rate limit strategy
     * @param key        key cấu hình từ annotation
     * @param identifier định danh (IP / USER / GLOBAL)
     * @param routeKey   route identifier
     * @return Redis key duy nhất cho bucket
     */
    private String buildKey(RateLimitType type,
                            String key,
                            String identifier,
                            String routeKey) {

        return String.format("%s:%s:%s:%s",
                type.name(),
                key,
                routeKey,
                identifier
        );
    }
}