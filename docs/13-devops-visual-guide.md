# 13. DevOps Visual Guide

## 1. Mục tiêu

Tài liệu này giúp bạn nhìn toàn bộ phần DevOps đã triển khai theo cách trực quan nhất:

- hệ thống gồm những gì
- luồng build và deploy chạy ra sao
- server VMware dùng gì
- file nào phục vụ mục đích gì
- secret nằm ở đâu
- backup và restore đi theo trình tự nào

Tài liệu này không thay thế runbook chi tiết, mà đóng vai trò "bản đồ tổng" để bạn hiểu nhanh trước khi đi sâu vào từng bước.

## 2. Thành phần đã có trong repo

Các nhóm artifact DevOps hiện tại:

- Tài liệu:
  - `docs/09-devops-platform-blueprint.md`
  - `docs/10-devops-deployment-runbook.md`
  - `docs/11-devops-operations-checklist.md`
  - `docs/12-vmware-host-bootstrap-runbook.md`
- Triển khai production:
  - `deploy/docker-compose.prod.yml`
  - `deploy/.env.prod.example`
  - `deploy/nginx/nginx.conf`
  - `deploy/nginx/templates/whs.conf.template`
- Monitoring:
  - `deploy/docker-compose.monitoring.yml`
  - `deploy/monitoring/prometheus/prometheus.yml`
  - `deploy/monitoring/grafana/provisioning/datasources/datasource.yml`
- Backup:
  - `scripts/backup/*`
- Restore:
  - `scripts/restore/*`
- CI/CD:
  - `.github/workflows/ci.yml`
  - `.github/workflows/deploy-vmware.yml`
  - `scripts/deploy/deploy-vmware.sh`

## 3. Mindmap tổng thể

```mermaid
mindmap
  root((WHS DevOps))
    Docs
      09 Blueprint
      10 Deployment Runbook
      11 Ops Checklist
      12 VMware Bootstrap
      13 Visual Guide
    Deploy
      docker-compose.prod.yml
      .env riêng trên server
      Nginx
        nginx.conf
        whs.conf.template
      Data volumes
        mysql
        redis
        rabbitmq
        minio
        email-attachments
    Monitoring
      Prometheus
      Grafana
      Node Exporter
      cAdvisor
    Backup
      MySQL dump
      MinIO archive
      Config archive
      backup-all.sh
    Restore
      restore-mysql.sh
      restore-minio.sh
      restore-config.sh
      restore-all.sh
    CI CD
      GitHub Actions CI
        compile
        test
        build image
        push GHCR
      GitHub Actions CD
        rsync deploy assets
        SSH VMware
        docker login GHCR
        pull image
        docker compose up
        health check
    Runtime
      Public
        Nginx 80 443
      Private
        App 8080
        MySQL 3306
        Redis 6379
        RabbitMQ 5672
        MinIO 9000
```

## 4. Kiến trúc triển khai trên VMware

```mermaid
flowchart TD
    Internet[Internet hoặc mạng nội bộ] --> Nginx[Nginx reverse proxy]

    subgraph VMware["VMware Host /opt/whs"]
      Nginx --> App[whs-backend]

      App --> MySQL[(MySQL 8)]
      App --> Redis[(Redis 7)]
      App --> Rabbit[(RabbitMQ)]
      App --> MinIO[(MinIO)]

      Prom[Prometheus] --> App
      Prom --> NodeExp[node-exporter]
      Prom --> cAdvisor[cAdvisor]

      Grafana[Grafana] --> Prom
    end

    style Nginx fill:#f8f1d4,stroke:#b48a00
    style App fill:#d9ecff,stroke:#2166ac
    style MySQL fill:#e6f4ea,stroke:#2f855a
    style Redis fill:#fde2e1,stroke:#c53030
    style Rabbit fill:#fff1e6,stroke:#dd6b20
    style MinIO fill:#efe3ff,stroke:#6b46c1
    style Prom fill:#ffe5d9,stroke:#c05621
    style Grafana fill:#e9f7ef,stroke:#2f855a
```

Ý nghĩa:

- `Nginx` là điểm public duy nhất.
- `App` không public trực tiếp.
- `MySQL`, `Redis`, `RabbitMQ`, `MinIO` là private services.
- `Prometheus` kéo metrics từ app và host.
- `Grafana` đọc dữ liệu từ Prometheus để hiển thị dashboard.

## 5. Mô hình mạng và port

```mermaid
flowchart LR
    User[User / Browser / FE] -->|443| Nginx
    Admin[Ops / DevOps] -->|22| SSH

    subgraph Public
      Nginx
      SSH
    end

    subgraph Private Docker Network
      App8080[App 8080]
      MySQL3306[MySQL 3306]
      Redis6379[Redis 6379]
      Rabbit5672[RabbitMQ 5672]
      MinIO9000[MinIO 9000]
      Prom9090[Prometheus 9090 localhost bind]
      Graf3000[Grafana 3000 localhost bind]
      Node9100[node-exporter 9100 localhost bind]
      Cad8081[cAdvisor 8081 localhost bind]
    end

    Nginx --> App8080
    App8080 --> MySQL3306
    App8080 --> Redis6379
    App8080 --> Rabbit5672
    App8080 --> MinIO9000
    Prom9090 --> App8080
```

Điểm cần nhớ:

- Public thật sự chỉ nên có `22`, `80`, `443`.
- Các cổng monitoring hiện bind `127.0.0.1`, tức là chỉ truy cập cục bộ trên host.
- App chạy trong Docker network riêng, không nhận request public trực tiếp.

## 6. Vị trí của từng file trong luồng triển khai

```mermaid
flowchart TD
    A[.github/workflows/ci.yml] --> B[Build và push image]
    B --> C[GHCR image]
    D[.github/workflows/deploy-vmware.yml] --> E[rsync deploy/ và scripts/]
    E --> F[/opt/whs trên VMware]
    C --> G[scripts/deploy/deploy-vmware.sh]
    F --> G
    G --> H[deploy/docker-compose.prod.yml]
    H --> I[Nginx + App + MySQL + Redis + RabbitMQ + MinIO]
    J[deploy/docker-compose.monitoring.yml] --> K[Prometheus + Grafana + node-exporter + cAdvisor]
    K --> I
    L[scripts/backup/*] --> M[Backup]
    N[scripts/restore/*] --> O[Restore]
```

## 7. Sequence hoàn chỉnh từ lúc dev push code đến khi app chạy trên VMware

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GitHub as GitHub Repo
    participant CI as GitHub Actions CI
    participant GHCR as Container Registry
    participant CD as GitHub Actions CD
    participant VMware as VMware Host
    participant Compose as Docker Compose
    participant Nginx as Nginx
    participant App as WHS Backend

    Dev->>GitHub: Push code lên main hoặc chạy workflow_dispatch
    GitHub->>CI: Trigger workflow ci.yml
    CI->>CI: Setup JDK 17
    CI->>CI: mvn compile
    CI->>CI: mvn test
    CI->>CI: Build Docker image
    CI->>GHCR: Push image tag sha-<commit> và latest
    GitHub->>CD: Trigger deploy-vmware.yml sau khi CI success
    CD->>VMware: rsync deploy/ và scripts/
    CD->>VMware: SSH + docker login GHCR
    CD->>VMware: export APP_IMAGE=<sha-tag>
    VMware->>Compose: docker compose pull
    VMware->>Compose: docker compose up -d
    Compose->>App: Start app container
    Compose->>Nginx: Start nginx container
    CD->>App: Health check /actuator/health
    App-->>CD: Healthy
    CD-->>Dev: Deploy thành công
```

## 8. Sequence vận hành request thực tế

```mermaid
sequenceDiagram
    participant User as User / Frontend
    participant Nginx as Nginx
    participant App as Spring Boot App
    participant DB as MySQL
    participant Cache as Redis
    participant MQ as RabbitMQ
    participant Object as MinIO

    User->>Nginx: HTTPS request
    Nginx->>App: Proxy request
    App->>Cache: Đọc cache / token / OTP / rate limit
    App->>DB: Query hoặc mutation
    App->>MQ: Publish async event nếu cần
    App->>Object: Upload hoặc download file nếu cần
    App-->>Nginx: HTTP response
    Nginx-->>User: HTTPS response
```

## 9. Sequence backup

```mermaid
sequenceDiagram
    participant Cron as Cron / Operator
    participant Backup as backup-all.sh
    participant MySQL as backup-mysql.sh
    participant MinIO as backup-minio.sh
    participant Config as backup-config.sh
    participant Storage as Backup Folder / Remote Storage

    Cron->>Backup: Chạy backup-all.sh
    Backup->>MySQL: Dump DB
    MySQL->>Storage: Lưu file .sql.gz
    Backup->>MinIO: Archive data MinIO
    MinIO->>Storage: Lưu file .tar.gz
    Backup->>Config: Archive compose/env/nginx/scripts
    Config->>Storage: Lưu file .tar.gz
```

## 10. Sequence restore

```mermaid
sequenceDiagram
    participant Ops as Operator
    participant Config as restore-config.sh
    participant MinIO as restore-minio.sh
    participant MySQL as restore-mysql.sh
    participant Compose as Docker Compose

    Ops->>Config: Restore config backup
    Config-->>Ops: Config restored
    Ops->>MinIO: Restore MinIO archive
    MinIO->>Compose: Stop MinIO service
    MinIO->>Compose: Start MinIO service
    MinIO-->>Ops: MinIO restored
    Ops->>MySQL: Restore .sql.gz
    MySQL-->>Ops: Database restored
```

## 11. Secret nằm ở đâu

```mermaid
flowchart TD
    Repo[Git Repository] -->|Không chứa secret thật| Docs[Docs / Templates]
    Repo --> EnvExample[deploy/.env.prod.example]

    GitHubSecrets[GitHub Secrets] --> CISecrets[CI/CD workflows]
    GitHubSecrets --> SSHSecrets[VMWARE_* / GHCR_*]

    ServerEnv[/opt/whs/deploy/.env] --> Compose[Docker Compose]
    ServerCerts[/opt/whs/deploy/certs] --> Nginx[Nginx TLS]

    CISecrets --> DeployWorkflow[deploy-vmware.yml]
    DeployWorkflow --> VMwareHost[VMware Host]
    VMwareHost --> ServerEnv
```

Ý nghĩa:

- Repo chỉ chứa template, không chứa secret thật.
- GitHub Secrets chứa secret phục vụ CI/CD.
- File `.env` thật nằm trên server.
- TLS cert cũng nằm trên server, không nằm trong repo.

### 11.1. GitHub Secrets tối thiểu để CD chạy được

- `VMWARE_HOST`: IP hoặc DNS của VMware host, phải reachable từ GitHub runner
- `VMWARE_PORT`: thường là `22`
- `VMWARE_USER`: user deploy trên host, ví dụ `deploy`
- `VMWARE_SSH_KEY`: private key dùng cho GitHub Actions SSH vào host
- `VMWARE_DEPLOY_PATH`: thường là `/opt/whs`
- `GHCR_USERNAME`: tài khoản GitHub có quyền pull image từ GHCR
- `GHCR_TOKEN`: PAT có tối thiểu quyền `read:packages`
- `VMWARE_SSH_KNOWN_HOSTS`: tùy chọn, nên cấu hình để pin host key thay vì phụ thuộc `ssh-keyscan`

### 11.2. Lưu ý quan trọng về kết nối

- Việc bạn SSH được từ máy cá nhân chưa đủ để CD chạy được.
- GitHub-hosted runner cũng phải SSH được vào `VMWARE_HOST`.
- Nếu VMware chỉ nằm trong mạng nội bộ lab, bạn cần một trong các cách sau:
  - public IP hoặc NAT port `22`
  - VPN/tunnel mà GitHub runner dùng được
  - self-hosted runner đặt cùng mạng với VMware

### 11.3. Image tag mà workflow sẽ dùng

- `ci.yml` publish:
  - `ghcr.io/<owner>/<repo>:sha-<commit_sha>`
  - `ghcr.io/<owner>/<repo>:latest` trên branch mặc định
- `deploy-vmware.yml` auto deploy image `sha-<commit_sha>` tương ứng với commit CI vừa pass
- `workflow_dispatch` cho phép nhập tay `image_tag` nếu muốn rollback hoặc redeploy

## 12. Ý nghĩa của từng lớp trong hệ thống

### 12.1. Lớp CI

Nơi kiểm tra chất lượng code:

- compile
- test
- build image

### 12.2. Lớp CD

Nơi đưa artifact đã build lên server:

- đồng bộ file deploy
- SSH vào host
- pull image
- rollout
- verify health

### 12.3. Lớp Runtime

Nơi ứng dụng thực sự phục vụ request:

- Nginx
- App
- DB
- Redis
- RabbitMQ
- MinIO

### 12.4. Lớp Observability

Nơi theo dõi tình trạng hệ thống:

- Prometheus
- Grafana
- node-exporter
- cAdvisor

### 12.5. Lớp Data Protection

Nơi bảo vệ khả năng phục hồi:

- backup scripts
- restore scripts
- runbook

## 13. Bạn nên hiểu theo thứ tự nào

Để nắm nhanh nhất, nên đọc theo thứ tự:

1. Mindmap ở mục 3
2. Kiến trúc VMware ở mục 4
3. Sequence deploy ở mục 7
4. Sequence request ở mục 8
5. Sequence backup/restore ở mục 9 và 10
6. Sau đó mới quay lại đọc:
   - `docs/09-devops-platform-blueprint.md`
   - `docs/10-devops-deployment-runbook.md`
   - `docs/12-vmware-host-bootstrap-runbook.md`

## 14. Những gì đã sẵn sàng và những gì còn phát triển tiếp

### 14.1. Đã sẵn sàng

- production compose
- monitoring compose
- nginx production config
- backup scripts
- restore scripts
- CI workflow
- CD workflow cho VMware
- runbook tổng và bootstrap host

### 14.2. Có thể phát triển tiếp

- route Grafana qua Nginx
- dashboard Grafana chi tiết
- Prometheus alert rules
- backup tự động bằng cron/systemd timer
- rollback strategy nâng cao
- tách app node và data node khi lên VPS thật

### 14.3. Cách demo nhanh sau khi push workflow

1. Thêm đủ GitHub Secrets ở mục `11.1`.
2. Đảm bảo VMware host reachable từ GitHub runner.
3. Trên host đã có sẵn:
   - `/opt/whs/deploy/.env`
   - `/opt/whs/deploy/certs/fullchain.pem`
   - `/opt/whs/deploy/certs/privkey.pem`
4. Push code lên `main` để chạy full CI -> CD tự động.
5. Nếu muốn deploy tay:
   - vào GitHub Actions
   - chạy `Deploy VMware`
   - nhập `image_tag` như `sha-<commit_sha>` hoặc `latest`
6. Sau deploy, verify tối thiểu:
   - `https://<domain>/healthz`
   - `https://<domain>/actuator/health`
   - `docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml ps`

## 15. Kết luận ngắn

Toàn bộ phần DevOps hiện tại đã tạo ra một "xương sống" hoàn chỉnh:

- code được kiểm tra ở CI
- image được build một lần
- VMware chỉ pull image và chạy
- Nginx đứng trước app
- monitoring theo dõi app và host
- backup và restore có script
- docs đã mô tả đầy đủ để phát triển tiếp

Nếu bạn muốn, bước tiếp theo tôi có thể làm thêm một tài liệu thứ hai mang tính "học nhanh trong 15 phút":

- chỉ 1 trang
- ít chữ
- chỉ giữ sơ đồ
- tóm tắt luồng DevOps như slide cho người mới nhìn là hiểu.
