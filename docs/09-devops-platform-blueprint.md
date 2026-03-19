# 09. Kiến trúc Nền tảng DevOps

## 1. Mục tiêu

Tài liệu này chốt kiến trúc DevOps end-to-end cho `WareHouseSystem-BE` theo lộ trình:

1. Triển khai ổn định trong môi trường lab trên VMware.
2. Giữ nguyên nguyên lý vận hành khi chuyển sang VPS thật.
3. Tiến tới mô hình production-grade với CI/CD, giám sát, sao lưu, khôi phục và hardening.

Mục tiêu không phải là "chạy được một lần", mà là:

- triển khai lặp lại được
- kiểm soát được cấu hình
- rollback được
- theo dõi được
- sao lưu và phục hồi được
- giảm tối đa thao tác thủ công rủi ro cao

## 2. Phạm vi hệ thống

Hệ thống backend hiện tại gồm:

- Spring Boot API
- MySQL 8
- Redis 7
- RabbitMQ 3 Management
- MinIO
- Docker image cho backend
- Docker Compose cho local stack
- Actuator và Prometheus metrics

Trong thực tế vận hành, cần bổ sung:

- Nginx reverse proxy
- TLS certificate
- pipeline CI/CD
- log rotation và tập trung log
- backup jobs
- runbook và checklist vận hành

## 3. Nguyên tắc thiết kế

### 3.1. Immutable artifact

- Build image một lần tại CI.
- Mỗi môi trường chỉ pull đúng image đó.
- Không build source trực tiếp trên server VMware hoặc VPS.

### 3.2. Environment parity

- Lab VMware, staging và VPS production phải dùng cùng một nguyên lý topology.
- Khác nhau chủ yếu ở:
  - domain
  - secret
  - sizing
  - backup policy
  - mức độ cảnh báo

### 3.3. Externalized configuration

- Toàn bộ secret và biến môi trường runtime phải nằm ngoài source code.
- Repo chỉ nên giữ:
  - `.env.example`
  - template compose
  - template CI/CD
  - tài liệu và runbook

### 3.4. Zero-trust cơ bản cho internal services

- Không expose MySQL, Redis, RabbitMQ, MinIO ra Internet nếu không cần.
- Chỉ expose:
  - `80/443` cho Nginx
  - `22` cho SSH
- Các cổng nội bộ chỉ tồn tại trong Docker network hoặc private subnet.

### 3.5. Operability first

Mỗi thành phần phải có tối thiểu:

- health check
- logs
- metrics nếu có thể
- chiến lược backup nếu giữ dữ liệu
- chiến lược rollback hoặc recreate

## 4. Mục tiêu kiến trúc theo giai đoạn

### 4.1. Giai đoạn A: Lab trên VMware

Mục đích:

- học và xác thực flow triển khai
- kiểm thử deployment
- kiểm thử restore
- kiểm thử rollback
- xác định sizing tối thiểu

Topology đề xuất:

- 1 VM Ubuntu `wms-app-01`
- trên VM dùng Docker Compose hoặc Docker với compose plugin
- tất cả services chạy chung một host:
  - nginx
  - whs-backend
  - mysql
  - redis
  - rabbitmq
  - minio
  - prometheus
  - grafana

Ưu điểm:

- đơn giản
- dễ debug
- chi phí thấp
- phù hợp giai đoạn đầu

Hạn chế:

- có điểm lỗi đơn lẻ
- không có HA
- kỷ luật backup và restore là bắt buộc

### 4.2. Giai đoạn B: Pre-production trên VMware hoặc VPS nhỏ

Mục đích:

- tách biệt local và môi trường gần production
- tập quy trình release
- test backup, restore và incident handling

Topology đề xuất:

- 2 máy hoặc 2 VM
- node 1:
  - nginx
  - backend
  - prometheus
  - grafana
- node 2:
  - mysql
  - redis
  - rabbitmq
  - minio

Nếu chưa tách được node, vẫn có thể chạy 1 node nhưng phải:

- backup định kỳ
- snapshot VM
- test restore thường xuyên

### 4.3. Giai đoạn C: VPS thật

Mục đích:

- go-live thật
- có domain thật
- có TLS thật
- có backup ngoài máy chủ
- có monitoring, alerting và release policy rõ ràng

Topology tối thiểu:

- 1 VPS app
- 1 VPS data hoặc dịch vụ data managed nếu có ngân sách

Khuyến nghị:

- app node:
  - nginx
  - whs-backend
  - prometheus
  - grafana
- data node:
  - mysql
  - redis
  - rabbitmq
  - minio

Nếu ngân sách rất hạn chế:

- có thể chạy 1 VPS all-in-one
- nhưng phải chấp nhận rủi ro cao
- bắt buộc có backup remote và diễn tập restore

## 5. Topology đích chuẩn

```text
Internet
   |
   v
Nginx reverse proxy
   |
   +--> / -> whs-backend:8080
   +--> /actuator/health (nội bộ hoặc bị giới hạn)
   +--> /grafana (IP allow-list hoặc basic auth)
   |
Docker network private
   |
   +--> whs-backend
   +--> mysql
   +--> redis
   +--> rabbitmq
   +--> minio
   +--> prometheus
   +--> grafana
```

Nguyên tắc expose port:

- Public:
  - 80
  - 443
- Restricted by office IP/VPN only:
  - 22
  - 3000 nếu truy cập Grafana trực tiếp
  - 15672 nếu cần RabbitMQ UI, nhưng ưu tiên không expose
  - 9001 nếu cần MinIO console, nhưng ưu tiên không expose
- Internal only:
  - 3306
  - 6379
  - 5672
  - 9000
  - 8080

## 6. Kiến trúc môi trường

Cần tối thiểu 4 môi trường logic:

### 6.1. Local

- dành cho developer
- chạy bằng Docker Compose
- được phép debug SQL, show-sql
- có thể dùng fake mail server

### 6.2. Lab

- chạy trên VMware
- dùng để test deployment và operations
- secret khác local
- dữ liệu không quan trọng

### 6.3. Staging

- gần production nhất có thể
- build từ cùng image như production
- migration, smoke test, backup, restore phải được diễn tập ở đây

### 6.4. Production

- secret riêng
- TLS thật
- backup thật
- alert thật
- không mở Swagger, management và admin tools ra public nếu không cần

## 7. Kiến trúc CI/CD đề xuất

### 7.1. Nguồn kích hoạt

- `pull_request`
  - compile
  - unit test
  - lint và các kiểm tra cơ bản
- `main`
  - build artifact
  - build Docker image
  - push registry
  - deploy lab hoặc staging
- `tag` hoặc `release/*`
  - deploy production sau approval

### 7.2. Pipeline chuẩn

```text
Developer push code
   ->
GitHub Actions CI
   ->
Compile + Test + Security checks
   ->
Build Docker image
   ->
Push image registry
   ->
Deploy Lab/Staging
   ->
Smoke test
   ->
Manual approval
   ->
Deploy Production
   ->
Post-deploy verification
```

### 7.3. Các job tối thiểu nên có

- `ci-build`
  - setup JDK 17
  - cache Maven
  - `mvn -q -DskipTests compile`
  - `mvn test`
- `package-image`
  - build image
  - tag theo commit SHA
  - push registry
- `deploy-lab`
  - SSH vào VM
  - pull image mới
  - restart service có kiểm soát
  - health check
- `deploy-prod`
  - manual approval
  - backup metadata trước deploy
  - pull image
  - rollout
  - smoke test
  - rollback nếu fail

### 7.4. Registry

Có thể dùng:

- GitHub Container Registry
- Docker Hub private repo
- GitLab Container Registry
- Harbor nếu on-prem

Khuyến nghị giai đoạn đầu:

- GitHub Container Registry

Tag image:

- `ghcr.io/<org>/whs-backend:<git-sha>`
- `ghcr.io/<org>/whs-backend:staging`
- `ghcr.io/<org>/whs-backend:prod`

Không nên chỉ dùng tag `latest`.

## 8. Quản lý secret và cấu hình

### 8.1. Phân loại secret

Các secret cần quản lý:

- MySQL root password
- MySQL app password
- Redis password
- RabbitMQ username và password
- MinIO access key và secret key
- JWT secret
- SMTP password
- SSH private key dùng cho deploy
- TLS certificate nếu tự quản lý

### 8.2. Nguyên tắc

- Không đặt secret thật trong repo.
- Không hardcode trong `pom.xml`.
- Không dùng chung một secret cho mọi môi trường.
- Rotate secret theo chu kỳ.

### 8.3. Nơi lưu secret

Giai đoạn đầu:

- GitHub Secrets cho CI/CD
- file env trên server, khóa quyền chặt

Mức tốt hơn:

- 1Password
- Bitwarden
- Vault
- hoặc cloud secret manager

### 8.4. Cấu trúc config trên server

Đề xuất:

```text
/opt/whs/
  compose/
    docker-compose.prod.yml
    .env
  nginx/
    conf.d/
  backups/
  scripts/
  logs/
```

## 9. Networking và security baseline

### 9.1. Firewall

Mở:

- 22
- 80
- 443

Đóng public:

- 3306
- 6379
- 5672
- 15672
- 9000
- 9001
- 8080

### 9.2. SSH

- tắt password login nếu có thể
- chỉ dùng SSH key
- đổi port SSH nếu cần, nhưng không xem đó là biện pháp bảo mật chính
- giới hạn IP nếu được

### 9.3. Reverse proxy

Nginx đảm nhận:

- TLS termination
- redirect HTTP sang HTTPS
- proxy timeout hợp lý
- request body size phù hợp cho upload
- optional:
  - rate limit layer ngoài ứng dụng
  - giới hạn truy cập admin endpoints

### 9.4. Application security baseline

Production cần khóa:

- Swagger chỉ mở nội bộ hoặc tắt hẳn
- `actuator/health` dùng cho load balancer hoặc health probe
- `actuator/prometheus` chỉ nội bộ hoặc qua reverse proxy có auth
- JWT secret riêng và đủ dài
- CORS chỉ allow domain front-end thật

## 10. Quản lý dữ liệu

### 10.1. MySQL

Cần có:

- volume riêng
- backup logical hằng ngày
- retention policy rõ ràng
- restore test định kỳ

Khuyến nghị:

- giai đoạn đầu dùng `mysqldump`
- về sau cân nhắc `xtrabackup` nếu dữ liệu lớn

### 10.2. Redis

Redis trong hệ thống này chủ yếu phục vụ:

- cache
- OTP
- token hoặc session related
- rate limit

Redis có thể mất dữ liệu tạm thời mà không được làm hỏng dữ liệu lõi, nhưng vẫn cần:

- password
- persistence phù hợp
- restart policy

### 10.3. RabbitMQ

Cần quan tâm:

- durable queue
- DLQ cho email hoặc xử lý async nếu cần
- queue depth monitoring
- alert nếu consumer dừng lâu

### 10.4. MinIO

Cần có:

- volume persistent
- bucket policy rõ ràng
- backup object storage
- không expose public console nếu không cần

## 11. Observability baseline

### 11.1. Metrics

Backend đã có Actuator và Prometheus, cần tận dụng để theo dõi:

- JVM memory
- CPU
- request latency
- error rate 4xx và 5xx
- DB connection pool
- Rabbit queue depth
- Redis availability
- disk usage

Công cụ đề xuất:

- Prometheus
- Grafana
- Node Exporter
- cAdvisor nếu monitor container

### 11.2. Logs

Giai đoạn đầu:

- logs stdout và docker logs
- log rotate trên host

Mức tốt hơn:

- Loki + Promtail + Grafana
hoặc
- ELK/OpenSearch

### 11.3. Alerting

Cần tối thiểu:

- app health fail
- host disk > 80%
- memory pressure
- DB down
- Rabbit queue tăng bất thường
- backup fail
- TLS sắp hết hạn

## 12. Backup, restore, rollback

### 12.1. Backup policy đề xuất

- MySQL:
  - full logical backup hằng ngày
  - giữ 7 ngày local
  - copy thêm 1 bản remote
- MinIO:
  - sync hằng ngày sang storage backup
- `.env`, nginx config, compose files:
  - backup mỗi khi thay đổi

### 12.2. Restore drill

Bắt buộc diễn tập:

- restore MySQL vào VM mới
- khởi động lại stack bằng backup config
- xác minh login, API health, file storage

### 12.3. Rollback ứng dụng

Rollback app phải dựa trên image tag:

1. xác định image tag trước đó
2. sửa compose hoặc env về image tag cũ
3. restart app
4. verify health

Lưu ý:

- rollback code không đồng nghĩa rollback schema
- migration Flyway nên theo nguyên tắc forward-compatible nếu muốn an toàn

## 13. Release strategy

### 13.1. Chuẩn release

Mỗi release cần có:

- changelog
- image tag
- danh sách migration
- impact assessment
- rollback note

### 13.2. Cách release đề xuất

Giai đoạn đầu:

- rolling theo kiểu single-node
- pull image mới
- recreate container
- chạy smoke test

Mức tốt hơn:

- blue/green bằng 2 app containers phía sau Nginx

## 14. Sizing tối thiểu đề xuất

### 14.1. Lab VMware

- 4 vCPU
- 8 GB RAM
- 120 GB SSD

Nếu chạy thêm Prometheus, Grafana và monitoring đầy đủ:

- 6 vCPU
- 12 GB RAM

### 14.2. VPS tối thiểu

Phương án all-in-one:

- 4 vCPU
- 8 GB RAM
- 160 GB SSD

Phương án tách app và data:

- app node: 2 vCPU, 4 GB RAM
- data node: 4 vCPU, 8 GB RAM

## 15. Các khoảng trống hiện tại cần xử lý trước khi go-live

Những điểm cần fix trong repo để đi đúng blueprint này:

- khóa JDK 17 trong CI/CD và build runtime
- bỏ hardcode secret khỏi source và Maven plugin
- tách local compose và production compose
- siết Swagger và Actuator cho production
- làm rõ session strategy Redis với stateless JWT
- chốt backup scripts và restore test
- bổ sung GitHub Actions

## 16. Deliverables DevOps cần có

Bộ tài liệu và artifacts cần chuẩn bị:

- platform blueprint
- deployment runbook
- operations checklist
- production compose file
- nginx config
- backup scripts
- restore scripts
- GitHub Actions CI
- GitHub Actions CD
- monitoring stack

## 17. Trình tự thực hiện đề xuất

Thứ tự nên làm:

1. Chốt blueprint và naming convention.
2. Hoàn thiện tài liệu production-grade và runbook.
3. Chuẩn hóa compose cho VMware lab.
4. Thêm Nginx, TLS và monitoring.
5. Thêm CI để build image.
6. Thêm CD để deploy vào VMware.
7. Diễn tập backup, restore, rollback.
8. Sau khi ổn định, clone nguyên flow sang VPS thật.

Tài liệu này là bản vẽ tổng. Các bước thao tác cụ thể nằm trong runbook tiếp theo.
