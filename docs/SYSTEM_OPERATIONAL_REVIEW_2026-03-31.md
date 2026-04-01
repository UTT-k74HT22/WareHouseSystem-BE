# Review vận hành hệ thống WMS Backend

Ngày review: 2026-03-31

## 1. Phạm vi và phương pháp

- Đây là review tĩnh trên codebase và cấu hình hiện tại.
- Phạm vi đã rà soát: security filter chain, rate limiting, Redis/cache, transaction/locking, inventory flow, outbound/inbound flow, RabbitMQ/email/background jobs, MinIO, logging, observability.
- Chưa chạy benchmark tải thực tế, chaos test, failover test hoặc soak test. Vì vậy các nhận định về khả năng chịu tải là kết luận kiến trúc và implementation-level, chưa phải benchmark định lượng.

## 2. Kết luận điều hành

| Hạng mục | Đánh giá | Nhận định ngắn |
|---|---|---|
| Scale ngang | Trung bình yếu | Ứng dụng có nền tảng stateless JWT và dùng Redis cho distributed rate limit/lock, nhưng nút cổ chai vẫn dồn vào MySQL, Redis và RabbitMQ. |
| Scale dọc | Yếu | Pool DB nhỏ, log SQL/security quá chi tiết, chưa thấy backpressure tổng thể, chưa có profiling/metrics cho các điểm nóng. |
| Phòng thủ tuyến đầu | Yếu | Rate limiting mới áp dụng cục bộ theo annotation, chưa phủ hết endpoint nhạy cảm, chưa có circuit breaker/bulkhead/retry có hệ thống. |
| Cache để giảm tải DB | Yếu | Redis hiện chủ yếu dùng cho OTP, permission, rate limit và vài cache nhỏ; chưa có chiến lược cache cho các read-path nghiệp vụ nóng. |
| Consistency dữ liệu | Trung bình | Một số flow làm đúng với `@Transactional` và `PESSIMISTIC_WRITE`, nhưng còn lỗ hổng idempotency, khóa chưa đều và một số chỗ tự "heal" dữ liệu thay vì ngăn drift từ đầu. |
| Messaging và async consistency | Yếu | Có topology retry/DLQ nhưng chưa có outbox; callback `afterCommit` có thể làm mất việc nếu publish fail hoặc process chết sau commit. |
| Observability/production readiness | Yếu | Có actuator nhưng thiếu metrics nghiệp vụ quan trọng; logging hiện tại quá ồn cho production; lộ chi tiết health không nên để mặc định. |

Kết luận ngắn:

- Hệ thống hiện chưa sẵn sàng để gọi là "chịu tải tốt" ở mức production traffic lớn.
- Ứng dụng có một số nền tảng đúng hướng, nhưng các lớp phòng thủ thực chiến cho traffic lớn, replay request, cache offload, sự cố async và consistency liên node vẫn chưa hoàn thiện.
- Điểm rủi ro lớn nhất hiện tại không phải ở business rule thuần túy, mà ở khả năng hệ thống bị nhân đôi mutation, nghẽn shared resources và mất đồng bộ khi có retry/failure ngoài happy path.

## 3. Những phần đang làm tương đối đúng

- Ứng dụng dùng JWT stateless trong `SecurityConfig`, về mặt app node có thể scale ngang.
- Rate limit đang dùng Bucket4j + Redis distributed backend, tức là về mặt nguyên lý có thể dùng chung trạng thái giữa nhiều instance.
- Nhiều flow nghiệp vụ quan trọng đã dùng `@Transactional` kết hợp `PESSIMISTIC_WRITE`, đặc biệt ở:
  - `SalesOrdersRepository.findByIdForUpdate`
  - `InventoryRepository.findByIdForUpdate`
  - `InventoryRepository.findByDimensionForUpdate`
  - `InventoryRepository.findBestSuitableForUpdate`
  - `StockTransfersRepository.findByIdForUpdate`
  - `InboundReceiptsRepository.findByIdForUpdate`
- `Inventory` có `@Version`, nghĩa là đã có nhận thức về optimistic locking, dù mới áp dụng rất hẹp.
- `StockTransfersServiceImpl` và `StockAdjustmentsServiceImpl` thể hiện tư duy consistency tốt hơn mặt bằng chung của codebase.
- RabbitMQ email/background job đã có queue retry và DLQ, tức là phần topology không phải viết từ đầu.

## 4. Invariants và aggregate roots cần bảo vệ

### 4.1 Inventory aggregate

Các bất biến cần luôn đúng:

- `onHandQuantity >= 0`
- `reservedQuantity >= 0`
- `available = onHand - quarantine - reserved >= 0`
- Một request mutation cùng idempotency key không được áp dụng hơn một lần.
- Một `orderLineId` không được reserve/unreserve/decrease theo cách tạo double effect.

### 4.2 Location aggregate

Các bất biến cần luôn đúng:

- `usedCapacity >= 0`
- `usedCapacity <= capacity` nếu business không chủ động định nghĩa transit location là "không giới hạn".
- Capacity update và inventory movement phải cùng thành bại trong cùng transaction.

### 4.3 Async email/background job aggregate

Các bất biến cần luôn đúng:

- Nếu bản ghi DB đã committed với trạng thái chờ xử lý, message phải được publish thành công hoặc phải có cơ chế recovery chắc chắn.
- Không được để trạng thái `PENDING/RETRY` mắc kẹt vô thời hạn chỉ vì callback sau commit bị lỗi.

### 4.4 Gateway/security aggregate

Các bất biến cần luôn đúng:

- Endpoint public nhạy cảm phải bị throttling ngay cả khi auth context chưa có.
- Khi Redis/rate-limit layer lỗi ở endpoint nhạy cảm, hệ thống phải fail theo chủ đích, không được vô tình fail-open do lỗi filter chung.

## 5. Phát hiện ưu tiên cao

## 5.1 P0 - Inventory mutation chưa có idempotency bền vững

Mức độ ảnh hưởng: Rất cao

Bằng chứng:

- `InventoryServiceImpl.increase(...)` và `InventoryServiceImpl.decrease(...)` đang bắt `DataIntegrityViolationException` như thể DB có unique constraint để chống duplicate request.
- `V20260313_09__Create_stock_movements.sql` không tạo unique constraint nào cho idempotency trên `stock_movements`.
- Lock Redisson hiện chỉ chặn đồng thời trong khoảng thời gian request đang chạy. Sau khi request thành công và lock được nhả, cùng request đó hoàn toàn có thể bị replay lại.

Rủi ro thực tế:

- Client timeout rồi retry.
- Load balancer/proxy retry.
- Consumer xử lý lặp.
- Cùng business event bị gửi lại từ upstream.
- Khi scale ngang nhiều instance, replay request sẽ không bị chặn nếu không có idempotency persisted ở DB.

Hậu quả:

- Tồn kho có thể tăng/giảm 2 lần.
- Audit trail ghi nhận nhiều movement hợp lệ về mặt schema nhưng sai về mặt nghiệp vụ.
- Sai lệch này khó phát hiện ngay vì code hiện thiên về "ghi movement thành công là coi như xong".

Nhận định:

- Đây là lỗ hổng nghiêm trọng nhất của lớp consistency.
- Redisson lock không thay thế cho idempotency persisted.

Khuyến nghị:

- Thêm idempotency key thực sự ở tầng DB cho mọi mutation làm thay đổi stock.
- Không nên suy luận uniqueness từ `reference_id` đơn thuần, vì một chứng từ có thể sinh nhiều movement line.
- Cách an toàn hơn là tạo `request_id` hoặc `mutation_key` riêng cho từng mutation call và unique index theo khóa đó.
- Lưu cả trạng thái xử lý và kết quả đã trả để retry có thể trả về kết quả cũ thay vì chạy lại mutation.

## 5.2 P0 - Async publish sau commit có thể làm mất việc và tạo trạng thái treo

Mức độ ảnh hưởng: Rất cao

Bằng chứng:

- `EmailServiceImpl.sendEmailAsync(...)` tạo `EmailLog` trạng thái `PENDING`, rồi publish vào RabbitMQ trong callback `afterCommit()`.
- `BackgroundJobServiceImpl.dispatchAfterCommit(...)` cũng dùng mô hình tương tự.
- `EmailScheduledService` đang bị comment toàn bộ, tức là recovery job hiện không hoạt động.

Rủi ro thực tế:

- Transaction DB commit xong nhưng process chết trước khi chạy `afterCommit`.
- `afterCommit` chạy nhưng publish thất bại.
- Có lỗi mạng tạm thời với RabbitMQ sau khi DB đã commit.

Hậu quả:

- DB đã có bản ghi `PENDING` hoặc `RETRY` nhưng queue không nhận được message.
- Không có outbox worker hoặc scheduler recovery đảm bảo eventual delivery.
- Email hoặc background job có thể bị "mất việc" mà người dùng chỉ thấy trạng thái treo.

Khuyến nghị:

- Triển khai outbox pattern chuẩn.
- Nếu chưa làm outbox ngay, ít nhất cần:
  - bật lại cơ chế recovery quét `PENDING/RETRY`
  - có timeout để đánh dấu stuck jobs
  - có metric và cảnh báo cho số lượng bản ghi chờ xử lý quá lâu

## 5.3 P0 - Tuyến phòng thủ request đầu vào chưa hoàn chỉnh

Mức độ ảnh hưởng: Rất cao

Bằng chứng:

- Chỉ endpoint có `@RateLimit` mới bị bảo vệ.
- `AuthController.register(...)` không có `@RateLimit`.
- `AuthController.changePassWord(...)` không có `@RateLimit`.
- `OtpController.sendOtp(...)` và `OtpController.verifyOtp(...)` không có `@RateLimit`.
- `application.yml` có block `app.rate-limit`, nhưng không thấy class binding hoặc nơi nào đọc config này. Tức là phần lớn đang là config chết.
- Không có thư viện hay implementation circuit breaker/bulkhead/retry có hệ thống.

Rủi ro thực tế:

- Đăng ký tài khoản, spam OTP, verify OTP brute force và change-password spam vẫn có thể dồn tải trực tiếp vào DB/Redis/Mail.
- Không có cơ chế chặn tổng thể khi downstream như RabbitMQ, SMTP hoặc MinIO chậm/bất ổn.

Nhận định:

- Rate limiting hiện tại là "điểm vá" cho một số endpoint, chưa phải tầng điều phối request chuẩn production.
- Hệ thống chưa có lớp bulkhead/backpressure đủ tốt cho traffic lớn.

Khuyến nghị:

- Đưa throttling nhạy cảm ra gateway/ingress nếu có thể.
- Nếu tiếp tục làm ở application layer, cần phủ hết endpoint public nhạy cảm.
- Bỏ việc hard-code hoàn toàn trong annotation cho các giá trị cần tuning vận hành.
- Bổ sung circuit breaker, timeout, retry có kiểm soát, bulkhead cho các downstream call quan trọng.

## 5.4 P0 - Cache layer hiện chưa sẵn sàng để giảm tải DB cho traffic lớn

Mức độ ảnh hưởng: Rất cao

Bằng chứng:

- Cache nghiệp vụ hiện rất mỏng:
  - `UnitsOfMeasureImpl` dùng `@Cacheable/@CacheEvict`
  - `PermissionCacheServiceImpl` cache permission
  - OTP/rate-limit dùng Redis trực tiếp
- Chưa thấy read cache trọng tâm cho inventory availability, dashboard, search, reporting hoặc catalog nóng.
- Global Spring Cache TTL đang để 1 giờ cho tất cả cache trong `RedisConfig`.
- Không có jitter/randomized TTL.
- Không có mutex/single-flight cho cache miss.
- Không có Bloom filter hoặc negative cache strategy mang tính hệ thống.
- Không có quy tắc kiểm soát large key/value, compression, key cardinality, key hotness.

Đánh giá theo từng vấn đề anh nêu:

- Cache avalanche: Chưa xử lý đúng. TTL đang cố định, không jitter.
- Cache breakdown/stampede: Chưa xử lý đúng. `@Cacheable` không dùng `sync = true`, Permission cache cũng không có single-flight khi miss.
- Cache penetration: Chưa xử lý đúng ở tầng tổng thể. Không thấy Bloom filter hoặc negative caching cho các lookup dễ bị quét.
- Hot keys: Chưa có chiến lược giám sát hoặc tách tải cho key nóng.
- Large key: Chưa thấy guardrail cho kích thước object, số field, hay quy tắc scan an toàn.

Nhận định quan trọng:

- Rủi ro hiện tại không phải "cache hỏng làm DB sập", mà là "chưa có cache read-side đủ mạnh nên DB vẫn là tuyến phục vụ chính".
- Nói cách khác, tầng cache hiện chưa phải tầng giảm tải cho DB ở các use case nóng.

Khuyến nghị:

- Thiết kế cache theo use case thay vì bật cache chung chung.
- Ưu tiên các API đọc nhiều và đổi ít.
- Mỗi cache cần định nghĩa rõ:
  - key design
  - TTL
  - jitter
  - negative caching
  - stampede protection
  - invalidation strategy
  - metric hit/miss/latency

## 5.5 P1 - Rate limiting hiện có lỗi ngữ nghĩa và lệch thời gian sống bucket

Mức độ ảnh hưởng: Cao

Bằng chứng:

- `RedisConfig.bucketProxyManager(...)` đang cấu hình TTL bucket dựa trên `Duration.ofMinutes(10)`.
- Trong khi đó có endpoint auth dùng window 15 phút.
- `RateLimitService.checkRateLimit(...)` trả `resetTime = now` cho request được phép, tức header `X-RateLimit-Reset` không phản ánh đúng thời điểm refill/reset thật.

Hậu quả:

- Bucket của endpoint 15 phút có thể hết hạn sớm hơn thời gian business mong muốn.
- Client nhận header reset không chính xác.
- Về vận hành sẽ rất khó debug hoặc dựa vào header để viết client behavior đúng.

Khuyến nghị:

- TTL bucket phải lớn hơn hoặc bằng duration dài nhất cộng safety margin.
- `resetTime` cần phản ánh thời điểm token khả dụng tiếp theo hoặc thời điểm cửa sổ reset thật.
- Nếu đã có config trong `application.yml`, cần bind thật thay vì để annotation hard-code và config chết.

## 5.6 P1 - DB pool và shared resource sizing chưa tương xứng với mô hình tải

Mức độ ảnh hưởng: Cao

Bằng chứng:

- `spring.datasource.hikari.maximum-pool-size = 10`
- `spring.data.redis.lettuce.pool.max-active = 8`
- Email consumer `max-consumers = 10`
- Background job consumer `max-consumers = 4`
- HTTP request threads mặc định có thể cao hơn nhiều so với pool DB hiện tại.

Hậu quả:

- Khi traffic tăng, điểm nghẽn đầu tiên nhiều khả năng là connection pool starvation.
- Worker async và web request đang tranh cùng pool DB.
- Hệ thống có thể biểu hiện thành timeout ngẫu nhiên, queue backlog tăng và độ trễ tail latency rất xấu.

Khuyến nghị:

- Sizing lại pool theo throughput mục tiêu và profile workload.
- Xem xét giới hạn concurrency của consumer dựa trên DB pool thực tế.
- Nếu workload async đủ lớn, cân nhắc tách datasource hoặc ít nhất tách quota concurrency.

## 6. Phát hiện ưu tiên trung bình nhưng cần xử lý sớm

## 6.1 P1 - `unreserve` khóa chưa đồng đều với `reserve`

Mức độ ảnh hưởng: Cao

Bằng chứng:

- `reserve(...)` dùng Redisson lock theo `orderLineId`.
- `unreserve(...)` không dùng lock phân tán tương ứng.
- `unreserve(...)` lấy reservation bằng `findByOrderLineId(...)` nhưng không khóa pessimistic trên reservation row.
- `decrease(... consumeReserved=true ...)` cũng thao tác trên reservation mà không khóa row reservation một cách nhất quán.

Rủi ro:

- Cancel order, ship order, retry request hoặc event song song có thể cùng đụng `orderLineId`.
- `reservedQuantity` có nguy cơ drift rồi sau đó code phải "heal" bằng phép cộng lại từ reservation table.

Nhận định:

- Việc phải "healing reserved aggregate" là tín hiệu cho thấy hệ thống đã chấp nhận drift xảy ra rồi mới chỉnh lại, thay vì ngăn nó từ đầu.

Khuyến nghị:

- Chuẩn hóa locking strategy theo cùng một khóa logic cho cả reserve, unreserve và consumeReserved.
- Nếu aggregate chính là `orderLineId`, thì mọi mutation liên quan nên serialize theo `orderLineId`.

## 6.2 P1 - Capacity của transit location có thể bị vượt vì dùng `forceUpdate`

Mức độ ảnh hưởng: Cao

Bằng chứng:

- `LocationServiceImpl.increaseUsedCapacity(...)` với `PICKING/PACKING/STAGING` đọc row rồi gọi `forceUpdateUsedCapacity(...)`.
- `LocationRepository.forceUpdateUsedCapacity(...)` chỉ set giá trị mới, không validate `newUsed <= capacity`.

Hậu quả:

- `usedCapacity` của transit location có thể vượt `capacity` mà không bị DB chặn.
- Nếu business muốn transit location là "không giới hạn", logic này cần được mô hình hóa rõ thay vì lách qua validation.

Khuyến nghị:

- Hoặc enforce capacity cho transit location như storage location.
- Hoặc khai báo rõ transit location không dùng capacity cứng và bỏ invariant đó một cách minh bạch.

## 6.3 P1 - `RateLimitFilter` còn một số điểm chưa đủ production-grade

Mức độ ảnh hưởng: Cao

Bằng chứng:

- Filter dùng `handlerMapping.getHandler(request)` trên mọi request, tức là path resolution bị làm thêm một lần trước cả DispatcherServlet.
- Nếu có exception trong filter, outer catch đang fail-open cho toàn bộ request.
- Trusted proxy matching hiện là string/prefix match đơn giản, và mới cover `172.16.*` chứ không cover đầy đủ dải `172.16.0.0/12`.

Hậu quả:

- Tăng CPU cost ở hot path.
- `failClosed = true` ở endpoint nhạy cảm vẫn có thể bị vô hiệu hóa nếu lỗi xảy ra ở filter layer khác với Redis check.
- Proxy trust có thể sai hành vi khi triển khai trên nhiều subnet private thật.

Khuyến nghị:

- Dùng CIDR matcher chuẩn.
- Tránh double route resolution trong filter.
- Chỉ fail-open ở nơi thật sự có chủ đích, không catch-all cho mọi lỗi.

## 6.4 P2 - Redis có thao tác `KEYS`, không an toàn khi keyspace lớn

Mức độ ảnh hưởng: Trung bình

Bằng chứng:

- `RedisServiceImpl.getAllKeys(...)` gọi `redisTemplate.keys(pattern)`.
- `PermissionCacheServiceImpl.evictAllUsers()` dùng API này với pattern wildcard.

Hậu quả:

- `KEYS` là lệnh blocking, rất nguy hiểm khi số key tăng.
- Dưới tải lớn, Redis latency có thể tăng đột biến.

Khuyến nghị:

- Dùng `SCAN`.
- Tách namespace invalidate bằng versioned key hoặc per-user explicit evict thay vì wildcard global sweep.

## 6.5 P2 - Logging và cấu hình production hiện quá "dev mode"

Mức độ ảnh hưởng: Trung bình

Bằng chứng:

- `spring.jpa.show-sql = true`
- `org.hibernate.SQL = DEBUG`
- `org.hibernate.type.descriptor.sql.BasicBinder = TRACE`
- `org.springframework.security = DEBUG`
- `org.demo.whs = DEBUG`
- `management.endpoint.health.show-details = always`

Hậu quả:

- Giảm throughput.
- Tăng I/O log.
- Dễ lộ cấu trúc query, binding values và chi tiết nội bộ.

Khuyến nghị:

- Tách profile production rõ ràng.
- Tắt SQL trace/binder trace ở production.
- Chỉ expose health detail khi có auth nội bộ phù hợp.

## 6.6 P2 - Cấu hình session Redis đang lệch với mô hình stateless JWT

Mức độ ảnh hưởng: Trung bình

Bằng chứng:

- `SecurityConfig` dùng `SessionCreationPolicy.STATELESS`.
- Nhưng `application.yml` vẫn bật `spring.session.store-type = redis`.

Hậu quả:

- Tăng độ phức tạp vận hành mà không đem lại giá trị rõ ràng.
- Dễ gây hiểu nhầm về kiến trúc state management.

Khuyến nghị:

- Nếu hệ thống thực sự stateless theo JWT, nên bỏ HTTP session Redis nếu không có use case riêng bắt buộc.

## 7. Đánh giá riêng theo các câu hỏi anh nêu

## 7.1 Hệ thống đã scale ngang tốt chưa?

Chưa.

Những gì đang hỗ trợ scale ngang:

- JWT stateless ở tầng auth.
- Rate limit và distributed lock dùng Redis nên có thể dùng chung giữa nhiều node.

Những gì đang cản scale ngang:

- Shared bottleneck tập trung ở MySQL, Redis, RabbitMQ.
- Không có outbox/idempotency đủ tốt nên khi scale nhiều node, replay request càng nguy hiểm.
- Chưa có first-line backpressure đủ mạnh cho traffic public.
- Chưa có cache read-side đủ tốt để tách tải khỏi DB.

Kết luận:

- App node có thể nhân bản, nhưng toàn hệ thống chưa đạt mức scale ngang "an toàn khi tăng traffic".

## 7.2 Hệ thống đã scale dọc tốt chưa?

Chưa.

Điểm nghẽn rõ nhất:

- DB pool nhỏ.
- Redis pool nhỏ.
- Logging quá nặng.
- Chưa thấy profiling, batching, query budget, concurrency budget và metric theo resource.

Kết luận:

- Tăng CPU/RAM cho một node chưa chắc giúp nhiều nếu pool và log config không đổi.

## 7.3 Cache avalanche, breakdown, penetration, hot key, large key đã xử lý tốt chưa?

Chưa.

Đánh giá ngắn:

- Avalanche: Chưa có jitter TTL.
- Breakdown: Chưa có mutex/single-flight chuẩn.
- Penetration: Chưa có Bloom filter/negative caching ở tầng tổng thể.
- Hot key: Chưa có chiến lược rõ.
- Large key: Chưa có governance; còn dùng cả `KEYS`.

Điểm quan trọng nhất:

- Hệ thống hiện còn chưa có tầng cache read-side đủ mạnh để gọi là lớp giảm tải chính cho DB.

## 7.4 Rate limiting, circuit breaker, request coordination đã đủ chưa?

Chưa.

- Rate limiting mới là partial coverage.
- Không thấy circuit breaker, bulkhead, retry policy đồng bộ.
- Filter hiện có một số lỗi semantics và fallback behavior chưa chặt.

## 7.5 Pessimistic lock, optimistic lock, transaction đã triển khai đúng mọi case chưa?

Chưa.

Điểm làm tốt:

- Nhiều flow update đã dùng `PESSIMISTIC_WRITE`.
- `Inventory` có `@Version`.
- Một số service xử lý transaction khá cẩn thận.

Điểm chưa tốt:

- Optimistic lock chỉ xuất hiện rất hẹp, chưa thành chiến lược nhất quán.
- `unreserve` và consumeReserved chưa khóa đồng đều.
- Idempotency của stock mutation chưa có DB guarantee.
- Có chỗ phải "heal" dữ liệu thay vì ngăn inconsistency từ đầu.

## 8. Khoảng trống test và xác minh

Hiện tôi chưa thấy test đủ mạnh cho các case concurrency và failure mode trọng yếu sau:

- Hai request `increase` cùng idempotency key.
- Hai request `decrease` cùng idempotency key.
- `reserve` và `unreserve` chạy song song trên cùng `orderLineId`.
- `cancel order` và `confirm dispatch` chạy song song.
- App crash ngay sau DB commit nhưng trước `afterCommit` publish.
- Redis unavailable với endpoint `failClosed`.
- RabbitMQ unavailable kéo dài.
- Cache stampede ở key nóng.

Điểm test hiện có nhưng chưa đủ:

- Có `InventoryOptimisticLockIntegrationTest`, nhưng mới kiểm tra stale update ở mức entity/SQL, chưa chứng minh đúng hành vi service-level khi concurrency xảy ra trong flow nghiệp vụ thật.

## 9. Roadmap khuyến nghị

## 9.1 Ưu tiên ngay

- Bổ sung durable idempotency cho toàn bộ inventory mutation API.
- Triển khai outbox hoặc recovery chắc chắn cho Email và BackgroundJob.
- Phủ rate limit cho `register`, `change-password`, `otp/send`, `otp/verify`.
- Sửa bucket TTL và `X-RateLimit-Reset`.
- Giảm log level production, tắt SQL binder trace, ẩn health detail.

## 9.2 Ưu tiên ngắn hạn

- Chuẩn hóa locking strategy quanh `orderLineId`.
- Thiết kế read cache cho các API đọc nóng, có jitter và stampede protection.
- Tuning lại pool DB/Redis/Rabbit consumer dựa trên load target.
- Bổ sung circuit breaker/timeout/retry/bulkhead cho SMTP, Rabbit publisher, MinIO.

## 9.3 Ưu tiên trung hạn

- Đưa rate limiting và request shaping ra ingress/API gateway.
- Bổ sung metrics cho:
  - DB pool exhaustion
  - Redis latency
  - cache hit ratio
  - rate limit rejects
  - lock contention
  - Rabbit publish failures
  - stuck pending jobs
- Thiết lập load test, soak test, failover test và chaos test thành chuẩn release.

## 10. Kết luận cuối cùng

Nhìn dưới góc độ vận hành thực chiến, hệ thống hiện tại mới ở mức "có nền tảng đúng ở một số chỗ", nhưng chưa đủ độ chặt để chịu traffic lớn một cách an toàn và ổn định.

Vấn đề quan trọng nhất cần xử lý trước không phải là tối ưu vi mô, mà là ba trụ cột sau:

- idempotency bền vững cho mutation
- async consistency với outbox/recovery
- request defense và cache/read-model đủ mạnh để không dồn mọi thứ vào DB

Nếu ba trụ cột này chưa được xử lý, việc tăng thêm node ứng dụng chủ yếu chỉ làm hệ thống xử lý nhanh hơn trên happy path, nhưng không làm nó an toàn hơn khi gặp retry, burst traffic, partial failure và race condition.
