# 11. Checklist Vận hành DevOps

## 1. Checklist trước khi bắt đầu DevOps

- [ ] Chốt domain naming cho lab, staging và production
- [ ] Chốt registry dùng để lưu Docker image
- [ ] Chốt Ubuntu LTS version
- [ ] Chốt JDK runtime version là 17
- [ ] Chốt chính sách secret management
- [ ] Chốt chiến lược backup và nơi lưu backup remote

## 2. Checklist readiness của repo

- [ ] Loại bỏ hardcode secret khỏi source code
- [ ] Loại bỏ hardcode DB credentials khỏi Maven plugin
- [ ] Chốt `application-prod.yml` cho production thật
- [ ] Chốt `application-dev.yml` đúng nghĩa cho local và dev
- [ ] Khóa Java 17 trong CI
- [ ] Có `docker-compose.prod.yml`
- [ ] Có Nginx config production
- [ ] Có backup và restore scripts

## 3. Checklist bootstrap VMware lab

- [ ] Tạo VM đúng sizing
- [ ] Đặt static IP
- [ ] Cài Docker Engine
- [ ] Cài Docker Compose plugin
- [ ] Tạo user `deploy`
- [ ] Bật SSH key login
- [ ] Tắt password login nếu có thể
- [ ] Mở firewall 22, 80, 443
- [ ] Đóng public các port service nội bộ
- [ ] Tạo thư mục `/opt/whs`

## 4. Checklist file và secret trên server

- [ ] Có file `.env` riêng cho lab
- [ ] Secret file chmod `600`
- [ ] Có `docker-compose.prod.yml`
- [ ] Có cấu hình `nginx`
- [ ] Có registry credentials nếu image private
- [ ] Có script backup DB
- [ ] Có script backup MinIO data

## 5. Checklist deploy lần đầu

- [ ] Pull được image từ registry
- [ ] Start được MySQL
- [ ] Start được Redis
- [ ] Start được RabbitMQ
- [ ] Start được MinIO
- [ ] Start được backend
- [ ] Start được Nginx
- [ ] `actuator/health` xanh
- [ ] Login API thành công
- [ ] Một API có auth chạy thành công
- [ ] Không có startup error nghiêm trọng trong logs

## 6. Checklist monitoring

- [ ] Có Prometheus
- [ ] Có Grafana
- [ ] Có dashboard host metrics
- [ ] Có dashboard application metrics
- [ ] Có alert app down
- [ ] Có alert disk usage
- [ ] Có alert DB down
- [ ] Có alert backup fail

## 7. Checklist backup và restore

- [ ] MySQL backup hằng ngày
- [ ] Backup được copy sang remote storage
- [ ] MinIO được backup
- [ ] Config files được backup
- [ ] Đã test restore trên môi trường khác
- [ ] Đã chốt RPO và RTO chấp nhận được

## 8. Checklist trước mỗi lần release

- [ ] Có image tag rõ ràng
- [ ] Có release note
- [ ] Có migration impact note
- [ ] Có backup gần nhất
- [ ] Còn đủ disk
- [ ] Secret thay đổi đã được cập nhật
- [ ] Dashboard đang xanh

## 9. Checklist sau deploy

- [ ] Health endpoint xanh
- [ ] Smoke test pass
- [ ] CPU và RAM bình thường
- [ ] Request 5xx không tăng bất thường
- [ ] Queue RabbitMQ không tồn đọng bất thường
- [ ] Error log không tăng đột biến

## 10. Checklist rollback readiness

- [ ] Biết image tag trước đó
- [ ] Có hướng dẫn rollback rõ ràng
- [ ] Có DB backup nếu release có migration rủi ro
- [ ] Đã xác định rollback owner
- [ ] Đã xác định tiêu chí rollback

## 11. Checklist trước khi cutover lên VPS thật

- [ ] Đã ổn định trên VMware qua nhiều chu kỳ deploy
- [ ] CI build image ổn định
- [ ] CD deploy lab ổn định
- [ ] Restore drill đã pass
- [ ] Rollback drill đã pass
- [ ] Domain, TLS và VPS firewall đã sẵn sàng
- [ ] Monitoring và alerting đã sẵn sàng
- [ ] Team nắm được runbook

## 12. Checklist production go-live

- [ ] DNS đã trỏ đúng VPS
- [ ] TLS certificate hợp lệ
- [ ] Chỉ mở 80, 443, 22
- [ ] Không expose 3306, 6379, 5672, 9000, 9001, 8080 ra public
- [ ] Swagger đã tắt hoặc bị giới hạn
- [ ] Actuator đã bị giới hạn
- [ ] Backup đã chạy thành công trước ngày go-live
- [ ] Có contact point xử lý sự cố

## 13. Thứ tự implementation đề xuất

Thứ tự nên làm tiếp trong repo này:

1. `docs`
2. `docker-compose.prod.yml`
3. Nginx production config
4. backup scripts
5. restore scripts
6. GitHub Actions CI
7. GitHub Actions CD
8. monitoring stack
9. hardening cho production
