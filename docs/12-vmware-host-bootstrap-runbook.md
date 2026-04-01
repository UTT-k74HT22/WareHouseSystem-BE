# 12. Runbook Bootstrap Host VMware

## 1. M?c tiêu

Tài li?u này mô t? cách chu?n b? m?t host VMware m?i d? nh?n deployment t? workflow CI/CD hi?n t?i.

M?c tiêu c?a host:

- nh?n file `deploy/` và `scripts/` qua `rsync`
- ch?y `docker compose` cho production stack
- pull image t? GHCR
- cho phép workflow `deploy-vmware.yml` SSH vào và rollout an toàn

## 2. Yêu c?u d?u vào

Tru?c khi b?t d?u c?n có:

- 1 VM Ubuntu Server 24.04 LTS
- static IP ho?c hostname n?i b? ?n d?nh
- user có quy?n `sudo`
- SSH key s? dùng cho GitHub Actions
- domain ho?c subdomain n?u mu?n g?n TLS th?t

Sizing t?i thi?u d? xu?t:

- 4 vCPU
- 8 GB RAM
- 120 GB SSD

## 3. Chu?n b? OS

Ðang nh?p b?ng user có quy?n `sudo`, sau dó:

1. c?p nh?t package
2. d?t timezone dúng
3. b?t d?ng b? th?i gian
4. cài các tool v?n hành co b?n

Gói t?i thi?u nên có:

- `curl`
- `git`
- `ca-certificates`
- `jq`
- `ufw`
- `rsync`

## 4. T?o user deploy

T?o user chuyên trách:

- username: `deploy`
- shell: `/bin/bash`
- home: `/home/deploy`

Phân quy?n:

- thêm vào group `docker` sau khi Docker du?c cài
- ch? c?p `sudo` n?u b?n th?c s? c?n bootstrap b?ng user này

## 5. Cài Docker và Compose plugin

Host ph?i có:

- Docker Engine
- Docker Compose plugin

Sau khi cài:

- b?t Docker service
- ki?m tra `docker compose version`
- dang xu?t r?i dang nh?p l?i n?u v?a thêm user vào group docker

## 6. C?u hình firewall

Ch? m? public:

- `22`
- `80`
- `443`

Không m? public:

- `3306`
- `6379`
- `5672`
- `15672`
- `9000`
- `9001`
- `8080`
- `9090`
- `9100`
- `3000`
- `8081`

N?u Grafana ho?c Prometheus c?n truy c?p ngoài, hãy di qua Nginx ho?c VPN, không nên m? tr?c ti?p.

## 7. T?o c?u trúc thu m?c chu?n

Trên host t?o:

```text
/opt/whs/
  deploy/
    certs/
    logs/
    data/
    monitoring/
  scripts/
```

Trong `deploy/data/` c?n có:

- `mysql/`
- `redis/`
- `rabbitmq/`
- `minio/`
- `email-attachments/`

Trong `deploy/logs/` c?n có:

- `app/`
- `nginx/`

Trong `deploy/monitoring/` c?n có:

- `prometheus/data/`
- `grafana/data/`

## 8. Chép file ban d?u lên host

Workflow CD s? t? d?ng b? `deploy/` và `scripts/`, nhung l?n d?u host v?n c?n:

- `/opt/whs/deploy/.env`
- `/opt/whs/deploy/certs/fullchain.pem`
- `/opt/whs/deploy/certs/privkey.pem`

Không commit các file này vào repo.

## 9. T?o file `.env` th?t

T? `deploy/.env.prod.example`, t?o `/opt/whs/deploy/.env` và c?p nh?t:

- `APP_IMAGE`
- `DOMAIN_NAME`
- `TLS_CERT_PATH`
- `TLS_KEY_PATH`
- toàn b? password
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`
- `GRAFANA_ROOT_URL`

Phân quy?n:

- owner: `deploy`
- permission: `600`

## 10. C?p quy?n cho thu m?c v?n hành

Ch?y trên host:

- owner `deploy:deploy` cho `/opt/whs`
- d?m b?o Docker có th? ghi vào:
  - `deploy/data/*`
  - `deploy/logs/*`
  - `deploy/monitoring/*`

## 11. C?u hình SSH cho GitHub Actions

Trên host:

- t?o `~deploy/.ssh/authorized_keys`
- thêm public key tuong ?ng v?i secret `VMWARE_SSH_KEY`

Trên GitHub repository secrets:

- `VMWARE_HOST`
- `VMWARE_PORT`
- `VMWARE_USER`
- `VMWARE_SSH_KEY`
- `VMWARE_DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

Khuy?n ngh?:

- `VMWARE_DEPLOY_PATH=/opt/whs`

## 12. Ðang nh?p GHCR l?n d?u trên host

Workflow CD dã có bu?c `docker login` t? xa, nhung b?n v?n nên test tay m?t l?n:

1. SSH vào host
2. ch?y `docker login ghcr.io`
3. xác nh?n có th? pull image

Ði?u này giúp tách l?i m?ng kh?i l?i workflow.

## 13. Deploy l?n d?u b?ng tay

Tru?c khi b?t auto deploy, nên test th? công:

1. copy `deploy/` và `scripts/` lên `/opt/whs`
2. t?o `/opt/whs/deploy/.env`
3. set `APP_IMAGE` vào `.env`
4. ch?y:

```bash
cd /opt/whs
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml -f deploy/docker-compose.monitoring.yml up -d
```

5. ki?m tra:
   - `docker compose ... ps`
   - `curl http://127.0.0.1:8080/actuator/health`
   - `curl http://127.0.0.1/healthz`

## 14. Xác minh monitoring

Sau khi b?t monitoring stack, ki?m tra:

- Prometheus: `http://127.0.0.1:9090`
- Grafana: `http://127.0.0.1:3000`
- Node exporter: `http://127.0.0.1:9100/metrics`
- cAdvisor: `http://127.0.0.1:8081/metrics`

Các c?ng này nên ch? bind `127.0.0.1` ho?c di qua reverse proxy n?i b?.

## 15. B?t workflow CD

Sau khi manual deploy ?n:

1. push workflow lên GitHub
2. c?u hình d? secrets
3. ch?y `workflow_dispatch` v?i tag image c? th?
4. khi pass r?i m?i d? auto-deploy theo `main`

## 16. Checklist hoàn t?t bootstrap

Host du?c xem là s?n sàng khi:

- SSH b?ng deploy key ho?t d?ng
- Docker và Compose plugin ho?t d?ng
- `/opt/whs/deploy/.env` t?n t?i
- TLS cert t?n t?i
- pull du?c image t? GHCR
- production compose lên du?c
- monitoring compose lên du?c
- workflow CD có th? k?t n?i host
