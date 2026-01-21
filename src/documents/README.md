# Warehouse Management System - Documentation Index
## 📚 Tài Liệu Hệ Thống Quản Lý Kho

---

## 🎯 Giới Thiệu

Chào mừng bạn đến với bộ tài liệu hoàn chỉnh của **Warehouse Management System (WMS) Version 1**. Đây là hệ thống quản lý kho hàng được xây dựng với Spring Boot, hỗ trợ quản lý toàn bộ quy trình nhập/xuất kho, theo dõi tồn kho real-time, và kiểm toán đầy đủ.

---

## 📖 Cấu Trúc Tài Liệu

### 1. [Project Overview](./01_PROJECT_OVERVIEW.md) 📋
**Tổng quan dự án - Dành cho: PM, BA, Stakeholders**

**Nội dung:**
- Giới thiệu và mục tiêu dự án
- Phạm vi Version 1 (In-scope & Out-scope)
- Công nghệ sử dụng (Tech Stack)
- Kiến trúc tổng quan
- Các vai trò người dùng (Roles)
- Quy trình nghiệp vụ chính
- KPIs và Timeline

**Đọc tài liệu này nếu bạn:**
- Là người mới tham gia dự án
- Cần hiểu tổng quan về hệ thống
- Muốn nắm bắt mục tiêu và phạm vi dự án

---

### 2. [System Architecture](./02_SYSTEM_ARCHITECTURE.md) 🏗️
**Kiến trúc hệ thống - Dành cho: Architects, Senior Developers**

**Nội dung:**
- High-level architecture diagram
- Technology stack chi tiết
- Architecture patterns (Layered, Module-based)
- Module design và dependencies
- Database design principles
- Security architecture
- Performance & scalability strategies

**Đọc tài liệu này nếu bạn:**
- Cần hiểu kiến trúc kỹ thuật
- Muốn thiết kế module mới
- Cần tối ưu hiệu năng

---

### 3. [API Documentation](./03_API_DOCUMENTATION.md) 🔌
**Tài liệu API - Dành cho: Frontend Developers, API Consumers**

**Nội dung:**
- API standards và conventions
- Authentication & authorization flow
- Error handling và response codes
- Tất cả API endpoints theo module:
  - Auth APIs
  - Master Data APIs (Warehouses, Products, Locations)
  - Inventory APIs
  - Inbound APIs (Purchase Orders, Receipts)
  - Outbound APIs (Sales Orders, Shipments)
  - Reporting APIs
  - Import APIs
- WebSocket notifications
- Request/Response examples

**Đọc tài liệu này nếu bạn:**
- Đang phát triển Frontend
- Cần tích hợp với API
- Muốn test API endpoints

**Quick Links:**
```
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

---

### 4. [Database Schema](./04_DATABASE_SCHEMA.md) 🗄️
**Thiết kế cơ sở dữ liệu - Dành cho: Database Administrators, Backend Developers**

**Nội dung:**
- ERD diagram đầy đủ
- Table definitions với SQL scripts
- Indexes và constraints
- Data types và validations
- Sample data cho testing
- Migration scripts

**Bao gồm các nhóm bảng:**
- Auth & Users (7 tables)
- Master Data (5 tables)
- Batch Management (1 table)
- Inventory (2 tables)
- Inbound Operations (4 tables)
- Outbound Operations (4 tables)
- Stock Movement Audit (1 table)
- Jobs & Reports (2 tables)

**Đọc tài liệu này nếu bạn:**
- Cần tạo hoặc sửa database schema
- Muốn hiểu data model
- Đang viết queries phức tạp

---

### 5. [Implementation Guide](./05_IMPLEMENTATION_GUIDE.md) 👨‍💻
**Hướng dẫn triển khai - Dành cho: Developers**

**Nội dung:**
- Roadmap implementation 14 tuần
- Phase-by-phase development guide
- Module implementation chi tiết với code examples:
  - Master Data Module
  - Inventory & Batch Module
  - Inbound Module
  - Outbound Module
  - Reporting & Import Module
  - Notification Module
- Best practices
- Testing strategy
- Code templates và patterns

**Đọc tài liệu này nếu bạn:**
- Đang implement các module
- Cần code examples
- Muốn follow best practices

---

### 6. [Deployment Guide](./06_DEPLOYMENT_GUIDE.md) 🚀
**Hướng dẫn triển khai & vận hành - Dành cho: DevOps, System Administrators**

**Nội dung:**
- Environment setup (Dev/Prod)
- Build & deployment procedures
- Docker & Kubernetes deployment
- Configuration management
- Monitoring & logging setup
- Backup & recovery procedures
- Troubleshooting common issues
- Emergency procedures

**Đọc tài liệu này nếu bạn:**
- Cần setup môi trường development
- Đang deploy lên production
- Gặp v��n đề về hệ thống

---

## 🚀 Quick Start

### Dành cho Developers
```bash
# 1. Clone project
git clone <repository-url>
cd whsBE

# 2. Start infrastructure
docker-compose up -d

# 3. Run application
mvnw spring-boot:run

# 4. Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

**Đọc tiếp:**
1. [Project Overview](./01_PROJECT_OVERVIEW.md) - Hiểu tổng quan
2. [System Architecture](./02_SYSTEM_ARCHITECTURE.md) - Hiểu kiến trúc
3. [Implementation Guide](./05_IMPLEMENTATION_GUIDE.md) - Bắt đầu code

---

### Dành cho Frontend Developers
```bash
# 1. Start backend
docker-compose up -d

# 2. Access API documentation
open http://localhost:8080/swagger-ui.html
```

**Đọc tiếp:**
1. [API Documentation](./03_API_DOCUMENTATION.md) - Chi tiết tất cả APIs
2. [Project Overview](./01_PROJECT_OVERVIEW.md) - Hiểu business flows

---

### Dành cho DevOps
```bash
# 1. Setup production environment
# 2. Configure environment variables
# 3. Deploy with Docker Compose or Kubernetes
```

**Đọc tiếp:**
1. [Deployment Guide](./06_DEPLOYMENT_GUIDE.md) - Chi tiết deployment
2. [System Architecture](./02_SYSTEM_ARCHITECTURE.md) - Hiểu infrastructure

---

## 📊 Project Status

### Current Phase: Phase 2 - Master Data Module 🔄

| Module | Status | Completion |
|--------|--------|------------|
| Auth & RBAC | ✅ Completed | 100% |
| Master Data | 🔄 In Progress | 20% |
| Batch Management | ⏳ Pending | 0% |
| Inventory | ⏳ Pending | 0% |
| Inbound | ⏳ Pending | 0% |
| Outbound | ⏳ Pending | 0% |
| Reporting | ⏳ Pending | 0% |
| Import | ⏳ Pending | 0% |
| Notifications | ⏳ Pending | 0% |

### Completed Features ✅
- ✅ Project setup with Spring Boot 3.5.9
- ✅ Docker Compose infrastructure
- ✅ JWT Authentication
- ✅ Role-based Access Control (RBAC)
- ✅ Rate Limiting
- ✅ Database migration with Flyway
- ��� Unit & Integration Tests for Auth module
- ✅ API Documentation with Swagger

### Next Priorities 🎯
1. **Units of Measure (UOM)** - CRUD operations
2. **Warehouses** - với caching và audit
3. **Locations** - link to warehouses
4. **Products** - với full-text search
5. **Business Partners** - suppliers & customers

---

## 🛠️ Tech Stack Summary

```yaml
Backend:
  Language: Java 17
  Framework: Spring Boot 3.5.9
  Database: MySQL 8.0
  Cache: Redis 7.0
  Message Broker: RabbitMQ 3.x
  Build Tool: Maven 3.8+

Security:
  Authentication: JWT (jjwt 0.12.x)
  Authorization: Spring Security RBAC
  Rate Limiting: Redis-based

Testing:
  Unit Tests: JUnit 5 + Mockito
  Integration Tests: Spring Boot Test
  Test Database: H2 in-memory

DevOps:
  Containerization: Docker
  Orchestration: Docker Compose / Kubernetes
  CI/CD: (To be configured)
```

---

## 📞 Support & Contact

### Team Structure
```
Project Manager: [Tên PM]
Tech Lead: [Tên Tech Lead]
Backend Team: [3-4 developers]
DevOps: [1 engineer]
QA: [1-2 testers]
```

### Communication Channels
- **Documentation**: Wiki / Confluence
- **Code Repository**: GitHub / GitLab
- **Issue Tracking**: Jira / GitHub Issues
- **Chat**: Slack / Microsoft Teams
- **Email**: team@whs.com

---

## 📝 Document Conventions

### Icons Used
- ✅ Completed / Đã hoàn thành
- 🔄 In Progress / Đang thực hiện
- ⏳ Pending / Chưa bắt đầu
- ⚠️ Warning / Cảnh báo
- 💡 Tip / Gợi ý
- 🔐 Security / Bảo mật
- 🎯 Goal / Mục tiêu
- 📊 Data / Dữ liệu
- 🔧 Configuration / Cấu hình

### Code Block Types
```java
// Java code examples
```

```sql
-- SQL queries
```

```yaml
# YAML configuration
```

```bash
# Shell commands
```

```json
// JSON data
```

---

## 🔄 Document Updates

| Date | Document | Changes | Author |
|------|----------|---------|--------|
| 2026-01-21 | All | Initial creation of complete documentation set | System |
| 2026-01-21 | 01_PROJECT_OVERVIEW | Created comprehensive project overview | System |
| 2026-01-21 | 02_SYSTEM_ARCHITECTURE | Detailed architecture documentation | System |
| 2026-01-21 | 03_API_DOCUMENTATION | Complete API reference with examples | System |
| 2026-01-21 | 04_DATABASE_SCHEMA | Full database schema with SQL scripts | System |
| 2026-01-21 | 05_IMPLEMENTATION_GUIDE | Step-by-step implementation guide | System |
| 2026-01-21 | 06_DEPLOYMENT_GUIDE | Deployment and operations manual | System |

---

## 📚 Additional Resources

### External Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [MySQL 8.0 Documentation](https://dev.mysql.com/doc/refman/8.0/en/)
- [Redis Documentation](https://redis.io/documentation)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

### Related Documents
- `README.md` - Project setup instructions
- `RATE_LIMITING.md` - Rate limiting implementation details
- `docker-compose.yml` - Infrastructure configuration

---

## 🎓 Learning Path

### For New Developers
1. **Day 1-2**: Read [Project Overview](./01_PROJECT_OVERVIEW.md)
2. **Day 3**: Setup development environment using [Deployment Guide](./06_DEPLOYMENT_GUIDE.md)
3. **Day 4-5**: Study [System Architecture](./02_SYSTEM_ARCHITECTURE.md)
4. **Week 2**: Review existing code (Auth module)
5. **Week 3+**: Start implementing modules following [Implementation Guide](./05_IMPLEMENTATION_GUIDE.md)

### For Frontend Developers
1. Read [Project Overview](./01_PROJECT_OVERVIEW.md) - Understand business flows
2. Study [API Documentation](./03_API_DOCUMENTATION.md) - Learn all endpoints
3. Setup backend locally for testing
4. Start integration with frontend

### For DevOps Engineers
1. Read [System Architecture](./02_SYSTEM_ARCHITECTURE.md) - Understand infrastructure
2. Study [Deployment Guide](./06_DEPLOYMENT_GUIDE.md) - Learn deployment procedures
3. Setup CI/CD pipeline
4. Configure monitoring and alerting

---

## ✅ Documentation Checklist

- [x] Project Overview completed
- [x] System Architecture documented
- [x] API Documentation with examples
- [x] Database Schema with SQL scripts
- [x] Implementation Guide with code samples
- [x] Deployment Guide with procedures
- [x] Index document created
- [ ] Video tutorials (Future)
- [ ] Postman collection (Future)
- [ ] Architecture Decision Records (ADRs) (Future)

---

**Version:** 1.0  
**Last Updated:** 21/01/2026  
**Maintained by:** Development Team  
**Status:** 🟢 Active Development

---

## 📄 License

Copyright © 2026 Warehouse Management System Team. All rights reserved.

---

**Happy Coding! 🚀**

