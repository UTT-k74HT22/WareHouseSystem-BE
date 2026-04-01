# Deployment and Monitoring

This directory contains the production deployment assets for the WHS backend.

## Structure

- `docker-compose.prod.yml`: application and data services for production
- `docker-compose.monitoring.yml`: Prometheus, Grafana, and exporters
- `.env.prod.example`: template for the server runtime environment
- `nginx/`: edge reverse proxy and TLS-aware templates
- `monitoring/`: Prometheus config/data and Grafana persistent data

## Recommended layout on server

```text
/opt/whs
+-- deploy
?   +-- .env
?   +-- docker-compose.prod.yml
?   +-- docker-compose.monitoring.yml
?   +-- certs
?   +-- data
?   +-- logs
?   +-- nginx
?   +-- monitoring
+-- scripts
    +-- backup
    +-- deploy
    +-- restore
```

## First-time server setup

1. Copy `deploy/.env.prod.example` to `deploy/.env`.
2. Replace all placeholder passwords and secrets.
3. Set `APP_IMAGE` to the image published by CI, for example `ghcr.io/<owner>/<repo>:sha-<commit>`.
4. Put TLS certificate files in `deploy/certs/`.
5. Ensure Docker Engine and Docker Compose plugin are installed on the server.

## Deploy application stack

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --remove-orphans
```

## Enable monitoring stack

The monitoring services join the same private Docker network as the application stack, so Prometheus can scrape internal service names directly.

```bash
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.prod.yml \
  -f deploy/docker-compose.monitoring.yml \
  up -d --remove-orphans
```

## Endpoints

- App health: `https://<domain>/actuator/health`
- Prometheus: `http://<server>:9090`
- Grafana: `http://<server>:3000`
- Node Exporter: `http://<server>:9100/metrics`
- MySQL Exporter: `http://<server>:9104/metrics`
- Redis Exporter: `http://<server>:9121/metrics`

## Notes

- Keep `.github/workflows` at repository root. GitHub Actions requires that location.
- Keep the root `docker-compose.yml` for local development.
- Keep production assets under `deploy/` so operational files stay grouped without mixing local and CI concerns.
