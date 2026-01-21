# Deployment & Operations Guide - Warehouse Management System
## Hướng Dẫn Triển Khai & Vận Hành

---

## 📋 Mục Lục
1. [Environment Setup](#environment-setup)
2. [Build & Deployment](#build--deployment)
3. [Configuration Management](#configuration-management)
4. [Monitoring & Logging](#monitoring--logging)
5. [Backup & Recovery](#backup--recovery)
6. [Troubleshooting](#troubleshooting)

---

## 🔧 Environment Setup

### Development Environment

#### Prerequisites
```bash
# Required Software
- Java 17 JDK
- Maven 3.8+
- Docker Desktop
- Git
- IDE (IntelliJ IDEA / Eclipse / VS Code)

# Verify installations
java -version    # Should show Java 17
mvn -version     # Should show Maven 3.8+
docker --version # Should show Docker 20+
```

#### Clone & Setup
```bash
# 1. Clone repository
git clone https://github.com/your-org/warehouse-system.git
cd warehouse-system/whsBE

# 2. Start infrastructure services
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# Expected output:
# NAME                STATUS              PORTS
# mysql_whs          running            0.0.0.0:3306->3306/tcp
# redis_whs          running            0.0.0.0:6379->6379/tcp
# rabbitmq_whs       running            0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp

# 4. Build application
mvn clean install

# 5. Run application
mvn spring-boot:run
```

#### Access Points
```yaml
Application:
  URL: http://localhost:8080
  Swagger UI: http://localhost:8080/swagger-ui.html
  API Docs: http://localhost:8080/v3/api-docs

RabbitMQ Management:
  URL: http://localhost:15672
  Username: admin
  Password: admin123

Actuator:
  Health: http://localhost:8080/actuator/health
  Info: http://localhost:8080/actuator/info
  Metrics: http://localhost:8080/actuator/metrics
```

---

## 🏗️ Build & Deployment

### Build Process

#### Maven Build
```bash
# Clean build
mvn clean install

# Skip tests (faster build)
mvn clean install -DskipTests

# Build with specific profile
mvn clean install -Pproduction

# Build Docker image
mvn clean package
docker build -t whs-backend:1.0.0 .
```

#### Docker Build
```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/whs-*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2048m"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```bash
# Build Docker image
docker build -t whs-backend:1.0.0 .

# Tag for registry
docker tag whs-backend:1.0.0 registry.example.com/whs-backend:1.0.0

# Push to registry
docker push registry.example.com/whs-backend:1.0.0
```

---

### Deployment Strategies

#### 1. Local Development
```bash
# Using Maven
mvn spring-boot:run

# Using Java
java -jar target/whs-0.0.1-SNAPSHOT.jar

# With custom profile
java -jar target/whs-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### 2. Docker Compose Deployment
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  whs-backend:
    image: whs-backend:1.0.0
    container_name: whs_app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/whs_db
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_RABBITMQ_HOST=rabbitmq
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - mysql
      - redis
      - rabbitmq
    restart: unless-stopped
    networks:
      - whs-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  mysql:
    image: mysql:8.0
    container_name: mysql_whs
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=whs_db
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - whs-network
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: redis_whs
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - whs-network
    restart: unless-stopped
    command: redis-server --appendonly yes

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: rabbitmq_whs
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - whs-network
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:

networks:
  whs-network:
    driver: bridge
```

```bash
# Deploy with Docker Compose
docker-compose -f docker-compose.prod.yml up -d

# Check logs
docker-compose -f docker-compose.prod.yml logs -f whs-backend

# Stop services
docker-compose -f docker-compose.prod.yml down
```

#### 3. Kubernetes Deployment
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: whs-backend
  namespace: warehouse-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: whs-backend
  template:
    metadata:
      labels:
        app: whs-backend
        version: v1.0.0
    spec:
      containers:
      - name: whs-backend
        image: registry.example.com/whs-backend:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: whs-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: whs-secrets
              key: db-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: whs-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: whs-backend-service
  namespace: warehouse-system
spec:
  type: LoadBalancer
  selector:
    app: whs-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

```bash
# Apply Kubernetes resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Check deployment status
kubectl get pods -n warehouse-system
kubectl get svc -n warehouse-system

# View logs
kubectl logs -f deployment/whs-backend -n warehouse-system

# Scale deployment
kubectl scale deployment whs-backend --replicas=5 -n warehouse-system
```

---

## ⚙️ Configuration Management

### Application Profiles

#### application.yml (Common)
```yaml
spring:
  application:
    name: warehouse-system
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
    show-sql: false

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

server:
  port: 8080
  compression:
    enabled: true
  http2:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    org.demo.whs: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

#### application-dev.yml (Development)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/whs_db?useSSL=false
    username: whs_user
    password: whs_password
  
  redis:
    host: localhost
    port: 6379
  
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123

  jpa:
    show-sql: true

logging:
  level:
    org.demo.whs: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

jwt:
  secret: dev-secret-key-change-in-production
  expiration: 3600000  # 1 hour
  refresh-expiration: 86400000  # 24 hours
```

#### application-prod.yml (Production)
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

  jpa:
    show-sql: false

logging:
  level:
    org.demo.whs: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
  file:
    name: /var/log/whs/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000
  refresh-expiration: 86400000
```

### Environment Variables
```bash
# .env file for production
DB_URL=jdbc:mysql://production-db-host:3306/whs_db
DB_USERNAME=whs_prod_user
DB_PASSWORD=<strong-password>

REDIS_HOST=production-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>

RABBITMQ_HOST=production-rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=whs_user
RABBITMQ_PASSWORD=<rabbitmq-password>

JWT_SECRET=<generate-strong-secret-key-256-bits>

MYSQL_ROOT_PASSWORD=<mysql-root-password>
```

---

## 📊 Monitoring & Logging

### Application Monitoring

#### Actuator Endpoints
```yaml
# Enabled endpoints
/actuator/health          # Health check
/actuator/info            # Application info
/actuator/metrics         # Metrics
/actuator/prometheus      # Prometheus metrics
/actuator/loggers         # Log levels
/actuator/env             # Environment properties
```

#### Health Checks
```java
// Custom health indicator
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            return Health.up()
                .withDetail("database", "MySQL")
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### Metrics Collection
```yaml
# Prometheus configuration
management:
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### Logging Configuration

#### Logback Configuration
```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/whs/application.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/whs/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- JSON Appender for ELK -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/whs/application.json</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/whs/application-%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### Monitoring Dashboard (Prometheus + Grafana)

#### docker-compose.monitoring.yml
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:

networks:
  monitoring:
    driver: bridge
```

---

## 💾 Backup & Recovery

### Database Backup

#### Automated Backup Script
```bash
#!/bin/bash
# backup-db.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/mysql"
DB_NAME="whs_db"
DB_USER="whs_user"
DB_PASS="whs_password"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME | gzip > $BACKUP_DIR/whs_db_$DATE.sql.gz

# Keep only last 30 days
find $BACKUP_DIR -name "whs_db_*.sql.gz" -mtime +30 -delete

echo "Backup completed: whs_db_$DATE.sql.gz"
```

#### Cron Job Setup
```bash
# Add to crontab
crontab -e

# Run backup daily at 2 AM
0 2 * * * /opt/scripts/backup-db.sh >> /var/log/whs/backup.log 2>&1
```

#### Restore Database
```bash
# Restore from backup
gunzip < /backup/mysql/whs_db_20260121_020000.sql.gz | mysql -u whs_user -p whs_db
```

### Redis Backup
```bash
# Enable AOF persistence in redis.conf
appendonly yes
appendfsync everysec

# Manual backup
redis-cli BGSAVE

# Backup RDB file
cp /var/lib/redis/dump.rdb /backup/redis/dump_$(date +%Y%m%d).rdb
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Application Won't Start
```bash
# Check logs
docker logs whs_app

# Common causes:
# - Database connection failed
# - Port already in use
# - Missing environment variables

# Solutions:
# Check database is running
docker-compose ps mysql

# Check port availability
netstat -ano | findstr :8080

# Verify environment variables
docker exec whs_app env | grep SPRING
```

#### 2. Database Connection Issues
```bash
# Test MySQL connection
docker exec -it mysql_whs mysql -u whs_user -p

# Check MySQL logs
docker logs mysql_whs

# Verify network connectivity
docker exec whs_app ping mysql
```

#### 3. Redis Connection Issues
```bash
# Test Redis connection
docker exec -it redis_whs redis-cli ping

# Check Redis logs
docker logs redis_whs

# Clear Redis cache
docker exec redis_whs redis-cli FLUSHALL
```

#### 4. RabbitMQ Issues
```bash
# Check RabbitMQ status
docker exec rabbitmq_whs rabbitmqctl status

# View queues
docker exec rabbitmq_whs rabbitmqctl list_queues

# Purge queue
docker exec rabbitmq_whs rabbitmqctl purge_queue report-generation-queue
```

#### 5. High Memory Usage
```bash
# Check Java heap usage
docker exec whs_app jstat -gc 1

# Adjust JVM options
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Monitor memory in real-time
docker stats whs_app
```

#### 6. Slow Performance
```sql
-- Check slow queries in MySQL
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;

-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
```

```bash
# Check Redis cache hit rate
docker exec redis_whs redis-cli INFO stats | grep hit

# Monitor API response times
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/products"
```

---

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] Code review completed
- [ ] All tests passing
- [ ] Database migrations tested
- [ ] Environment variables configured
- [ ] Secrets properly stored
- [ ] Backup strategy in place

### Deployment
- [ ] Build application
- [ ] Tag Docker image
- [ ] Push to registry
- [ ] Update deployment configuration
- [ ] Deploy to staging
- [ ] Run smoke tests
- [ ] Deploy to production
- [ ] Verify health checks

### Post-Deployment
- [ ] Monitor logs for errors
- [ ] Check application metrics
- [ ] Verify database connections
- [ ] Test critical endpoints
- [ ] Notify team of deployment
- [ ] Update documentation

---

## 🚨 Emergency Procedures

### Rollback Procedure
```bash
# Docker Compose
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --no-deps whs-backend

# Kubernetes
kubectl rollout undo deployment/whs-backend -n warehouse-system
kubectl rollout status deployment/whs-backend -n warehouse-system
```

### Disaster Recovery
```bash
# 1. Stop application
docker-compose down

# 2. Restore database
gunzip < /backup/mysql/latest.sql.gz | mysql -u root -p whs_db

# 3. Restore Redis (if needed)
cp /backup/redis/latest.rdb /var/lib/redis/dump.rdb

# 4. Restart services
docker-compose up -d
```

---

**Cập nhật lần cuối:** 21/01/2026  
**Version:** 1.0

