# 10. Runbook Triển khai DevOps

## 1. Mục tiêu

Runbook này mô tả cách triển khai thực tế cho `WareHouseSystem-BE` theo 2 chặng:

- chặng 1: VMware lab
- chặng 2: VPS thật

Nguyên tắc là chặng 2 phải tái sử dụng tối đa những gì đã được kiểm chứng ở chặng 1.

## 2. Invariants vận hành

Trước khi triển khai, cần chốt các bất biến:

1. Một image build ra phải deploy được cho nhiều môi trường.
2. Schema database chỉ được thay đổi qua Flyway.
3. Không sửa code trực tiếp trên server.
4. Không ghi đè secret thật vào repo.
5. Mỗi lần deploy đều có health check và đường rollback.

## 3. State transition của hệ thống vận hành

Các trạng thái vận hành cần hiểu rõ:

- `Provisioned`: server đã cài xong OS và network
- `Bootstrapped`: đã cài Docker, compose, nginx, user, firewall
- `Configured`: đã có `.env`, compose, nginx config, volumes
- `Deployed`: containers đã chạy
- `Verified`: smoke test và health check pass
- `Operational`: monitoring, backup, log rotation đã bật

Không được nhảy cóc từ `Provisioned` lên `Operational`.

## 4. Regression points cần cảnh giác

- JDK version sai trong CI
- DB volume mount sai
- expose nhầm port MySQL, Redis, RabbitMQ ra Internet
- JWT secret sai làm toàn bộ login fail
- CORS production sai domain
- Flyway migration lỗi làm app không lên
- MinIO bucket không tồn tại hoặc storage path không writable
- queue RabbitMQ tồn đọng nhưng không được monitor

## 5. Verification strategy tối thiểu

Mỗi lần deploy phải verify:

1. `actuator/health` xanh
2. login API thành công
3. một API đọc có auth thành công
4. DB migration không lỗi
5. Redis, RabbitMQ, MinIO reachable

## 6. Chuẩn bị VMware lab

### 6.1. VM specification

Khuyến nghị:

- Ubuntu Server 24.04 LTS
- 4 vCPU
- 8 GB RAM
- 120 GB SSD
- static IP trong mạng lab

### 6.2. OS bootstrap

Cần làm:

- tạo user deploy, không dùng root cho vận hành hằng ngày
- cập nhật package
- đặt timezone
- bật NTP
- cài `curl`, `git`, `ca-certificates`, `ufw`, `jq`

### 6.3. Docker bootstrap

Cần cài:

- Docker Engine
- Docker Compose plugin

Sau khi cài:

- thêm user deploy vào group docker
- bật Docker auto-start

### 6.4. Firewall

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

## 7. Cấu trúc thư mục trên server

```text
/opt/whs/
  app/
  compose/
  nginx/
  scripts/
  backups/
  monitoring/
  logs/
```

Khuyến nghị ownership:

- owner: `deploy:deploy`
- secret files chmod `600`

## 8. Các file cần có trước khi deploy

Trên server cần có:

- `docker-compose.prod.yml`
- `.env`
- `nginx.conf` hoặc `conf.d/whs.conf`
- backup scripts
- restore notes

Nếu có CI/CD thì các file này nên được:

- provision sẵn bằng script hoặc Ansible
hoặc
- đồng bộ qua SCP hoặc rsync ở bước bootstrap

## 9. Cấu hình `.env` production-like

Các nhóm biến cần có:

- Spring:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SERVER_PORT=8080`
- Database:
  - `DB_HOST`
  - `DB_PORT`
  - `MYSQL_DATABASE`
  - `MYSQL_USER`
  - `MYSQL_PASSWORD`
- Redis:
  - `REDIS_HOST`
  - `REDIS_PORT`
  - `REDIS_PASSWORD`
- RabbitMQ:
  - `RABBITMQ_HOST`
  - `RABBITMQ_PORT`
  - `RABBITMQ_DEFAULT_USER`
  - `RABBITMQ_DEFAULT_PASS`
- MinIO:
  - `MINIO_ENDPOINT`
  - `MINIO_ACCESS_KEY`
  - `MINIO_SECRET_KEY`
  - `MINIO_BUCKET_NAME`
- Security:
  - `JWT_SECRET`
  - `CORS_ALLOWED_ORIGINS`
- Mail:
  - `MAIL_HOST`
  - `MAIL_PORT`
  - `MAIL_USERNAME`
  - `MAIL_PASSWORD`

Nguyên tắc:

- mỗi môi trường một file `.env` riêng
- không copy `.env` local lên server

## 10. Docker Compose production guideline

File production compose cần tuân theo:

- image pull từ registry
- không dùng `build: .` trên production
- có restart policy
- có healthcheck
- có named volumes
- có network private
- mount logs và attachments rõ ràng

Compose production nên có thêm:

- nginx service
- prometheus
- grafana
- node-exporter hoặc cadvisor nếu cần

## 11. Nginx reverse proxy guideline

Nginx cần đảm nhận:

- redirect `80 -> 443`
- TLS certificate
- proxy sang `whs-backend:8080`
- timeout hợp lý cho upload file
- optional:
  - basic auth cho Grafana
  - IP allow-list cho `/actuator/prometheus`
  - deny Swagger trên production nếu không cần

## 12. Quy trình deploy lần đầu trên VMware

### 12.1. Provision

- tạo VM
- cài OS
- cài Docker
- cài Nginx nếu chạy host-level
- mở firewall

Exit criteria:

- SSH vào được
- Docker chạy ổn
- disk mount đúng

### 12.2. Bootstrap files

- tạo thư mục `/opt/whs`
- copy compose files
- copy `.env`
- copy nginx config
- tạo volumes và paths cần thiết

Exit criteria:

- file ownership đúng
- secret file đã khóa quyền

### 12.3. Pull image

Nếu đã có registry:

- docker login registry
- pull image theo tag

Exit criteria:

- image tồn tại trên host

### 12.4. Start stack

Khởi động theo thứ tự logic:

1. mysql
2. redis
3. rabbitmq
4. minio
5. backend
6. nginx
7. monitoring

Nếu dùng compose có dependency và health check, có thể start cả stack nhưng vẫn phải xác nhận trạng thái từng service.

Exit criteria:

- tất cả container `Up`
- các service có health check phải về trạng thái `healthy`

### 12.5. Verify sau deploy

Kiểm tra:

- `docker ps`
- `docker logs whs_be --tail 200`
- `curl http://localhost:8080/actuator/health`
- login API
- kết nối MySQL
- kết nối Redis
- RabbitMQ ping
- MinIO bucket tồn tại

Exit criteria:

- app phục vụ request
- không có startup error nghiêm trọng

## 13. Quy trình deploy update thường kỳ

### 13.1. Trước deploy

Cần kiểm tra:

- release note
- migration impact
- image tag
- secret diff nếu có
- free disk
- backup DB mới nhất

### 13.2. Deploy

Flow:

1. pull image mới
2. cập nhật image tag trong compose hoặc env
3. recreate backend container
4. chờ health xanh
5. chạy smoke test

### 13.3. Sau deploy

Kiểm tra:

- error logs trong 15 phút đầu
- CPU và RAM
- response time
- login flow
- queue backlog

## 14. Rollback runbook

Rollback khi:

- app không lên
- migration còn tương thích nhưng code mới lỗi
- tỷ lệ 5xx tăng mạnh
- login hoặc business flow chính fail

Các bước:

1. xác định image tag trước đó
2. đổi compose về image cũ
3. recreate backend
4. verify health
5. tiếp tục monitor

Nếu migration schema không backward-compatible:

- rollback sẽ phức tạp hơn
- có thể phải restore DB backup hoặc dùng migration repair

Vì vậy release nên ưu tiên migration forward-compatible.

## 15. Backup runbook

### 15.1. MySQL

Hằng ngày:

- dump DB ra file timestamp
- gzip backup
- copy sang storage remote

Hằng tuần:

- test restore trên môi trường lab

### 15.2. MinIO

Hằng ngày:

- sync object sang thư mục backup hoặc bucket backup

### 15.3. Backup cấu hình

Sau mỗi thay đổi:

- backup:
  - compose files
  - `.env`
  - nginx config
  - script files

## 16. Restore runbook

Các scenario cần diễn tập:

### 16.1. Mất app container

- pull lại image
- recreate container

### 16.2. Mất host nhưng còn backup

- tạo VM mới
- bootstrap Docker và folders
- restore config
- restore MySQL
- restore MinIO data
- start stack
- verify

### 16.3. Mất một phần dữ liệu

- restore DB dump gần nhất
- đánh giá RPO chấp nhận được

## 17. Monitoring runbook

Tối thiểu cần có dashboard cho:

- host CPU, RAM, disk
- container CPU, RAM, restart count
- JVM heap và non-heap
- HTTP request count và latency
- error 4xx và 5xx
- Hikari pool usage
- MySQL uptime
- Redis uptime
- Rabbit queue depth

Alert tối thiểu:

- app down > 1 phút
- disk > 80%
- RAM > 90%
- DB down
- backup fail

## 18. Chuyển từ VMware sang VPS thật

### 18.1. Những gì giữ nguyên

- image registry
- compose structure
- env naming
- health checks
- smoke tests
- backup scripts
- monitoring stack

### 18.2. Những gì thay đổi

- IP và domain
- TLS certificate thật
- firewall thật
- backup remote thật
- sizing
- khả năng tách app node và data node

### 18.3. Điều kiện được phép cutover

Chỉ cutover sang VPS khi:

- đã deploy ổn định trên VMware qua vài chu kỳ release
- đã diễn tập restore
- đã test rollback
- đã có dashboard và alert cơ bản
- đã có CI build image ổn định

## 19. Definition of done cho vận hành

Hệ thống được xem là sẵn sàng vận hành khi:

- build image từ CI thành công
- deploy không cần sửa code trên server
- backup và restore đã test
- có dashboard metrics
- có đường log review rõ ràng
- có rollback checklist
- secret đã tách khỏi repo
- app và dependencies không expose thừa cổng public

## 20. Bước tiếp theo sau tài liệu này

Sau runbook này, thứ tự nên làm là:

1. Tạo `docker-compose.prod.yml`.
2. Tạo Nginx config production.
3. Tạo backup scripts.
4. Tạo GitHub Actions CI.
5. Tạo GitHub Actions CD để deploy vào VMware.
6. Tạo monitoring stack compose.
7. Diễn tập deployment đầu tiên.
