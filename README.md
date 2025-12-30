# Warehouse Management System (WHS)

## Mô tả
Hệ thống quản lý kho hàng (Warehouse Management System) được xây dựng bằng Spring Boot.

## Công nghệ sử dụng
- **Java 17**
- **Spring Boot 3.5.9**
- **MySQL 8.0** - Cơ sở dữ liệu chính
- **Redis 7** - Cache và quản lý session
- **RabbitMQ 3** - Message broker
- **Flyway** - Database migration
- **Spring Security** - Bảo mật
- **JWT** - Xác thực
- **Lombok** - Giảm boilerplate code
- **SpringDoc OpenAPI** - API documentation

## Yêu cầu hệ thống
- Java 17 trở lên
- Maven 3.6+
- Docker & Docker Compose

## Cài đặt và chạy

### 1. Clone dự án
```bash
git clone <repository-url>
cd whs
```

### 2. Cấu hình môi trường
Copy file `.env.example` thành `.env` và cập nhật các giá trị nếu cần:
```bash
copy .env.example .env
```

### 3. Khởi động các services với Docker Compose
```bash
docker-compose up -d
```

Lệnh này sẽ khởi động:
- **MySQL** tại `localhost:3306`
- **Redis** tại `localhost:6379`
- **RabbitMQ** tại `localhost:5672` (Management UI: `localhost:15672`)

### 4. Kiểm tra trạng thái services
```bash
docker-compose ps
```

### 5. Xem logs
```bash
# Tất cả services
docker-compose logs -f

# Một service cụ thể
docker-compose logs -f mysql
docker-compose logs -f redis
docker-compose logs -f rabbitmq
```

### 6. Chạy ứng dụng Spring Boot

#### Sử dụng Maven Wrapper (Windows)
```bash
mvnw.cmd spring-boot:run
```

#### Hoặc build và chạy
```bash
mvnw.cmd clean package
java -jar target/whs-0.0.1-SNAPSHOT.jar
```

## Truy cập các services

### Application
- **URL**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

### RabbitMQ Management
- **URL**: http://localhost:15672
- **Username**: admin
- **Password**: admin123

### Actuator Endpoints
- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Metrics**: http://localhost:8080/actuator/metrics

## Cấu hình Database

### MySQL Connection
```properties
Host: localhost
Port: 3306
Database: whs_db
Username: whs_user
Password: whs_password
```

### Redis Connection
```properties
Host: localhost
Port: 6379
Password: redis123
```

## Profiles

### Development (mặc định)
```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production
```bash
java -jar target/whs-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Dừng và xóa services

### Dừng services
```bash
docker-compose stop
```

### Dừng và xóa containers
```bash
docker-compose down
```

### Dừng và xóa cả volumes (dữ liệu)
```bash
docker-compose down -v
```

## Cấu trúc thư mục
```
whs/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/demo/whs/
│   │   │       ├── configuration/
│   │   │       ├── controller/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       ├── service/
│   │   │       └── utils/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
├── docker-compose.yml
├── .env
├── .env.example
└── pom.xml
```

## Troubleshooting

### Port đã được sử dụng
Nếu port 3306, 6379, 5672 hoặc 8080 đã được sử dụng, bạn có thể thay đổi trong file `.env`

### Lỗi kết nối MySQL
Đợi MySQL khởi động hoàn toàn (khoảng 30 giây) trước khi chạy ứng dụng

### Reset database
```bash
docker-compose down -v
docker-compose up -d
```

## License
[Chọn license phù hợp]

