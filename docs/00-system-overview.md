# WHS Backend — System Overview

## 1) Purpose and architecture style
- Backend is a single Spring Boot application (`org.demo.whs.WhsApplication`) using layered structure:
  - **Controller** (`org.demo.whs.controller`)
  - **Service / Service Impl** (`org.demo.whs.service`, `org.demo.whs.service.impl`)
  - **Repository** (`org.demo.whs.repository`)
  - **Entity + DTO + Mapper** (`org.demo.whs.entity`, `org.demo.whs.entity.dto`, `org.demo.whs.mapper`)
- Main runtime patterns:
  - REST APIs under `/api/v1/**`
  - JWT stateless authentication + role-based authorization
  - Redis for cache/session/rate-limit state
  - RabbitMQ for async email flow
  - Flyway migrations for schema evolution

## 2) Technology stack (from `pom.xml` and configs)
- Java 17, Spring Boot 3.5.9
- Spring Web, Validation, Data JPA, Security, Actuator
- MySQL (runtime), Flyway
- Redis + Spring Session + Spring Cache
- RabbitMQ (AMQP)
- JWT (`jjwt`)
- Thymeleaf + JavaMail (email templates and sending)
- OpenAPI (`springdoc-openapi-starter-webmvc-ui`)

## 3) Runtime and configuration
- Core config: `src/main/resources/application.yml`
  - MySQL datasource + JPA validate mode
  - Flyway enabled (`classpath:db/migration`)
  - Redis + session store type `redis`
  - RabbitMQ host/port/listener/template retry
  - App custom config blocks: `app.jwt`, `app.cors`, `app.rate-limit`, `app.email`
- App entry enables:
  - `@EnableScheduling`
  - `@EnableCaching`

## 4) Cross-cutting backend components
- **Security**
  - `SecurityConfig`: stateless JWT, CORS, endpoint authorization.
  - `JwtAuthFilter`, `JwtProvider`, `CustomUserDetailsService`.
- **Rate limiting**
  - `RateLimitFilter` checks `@RateLimit` annotations before controller logic.
  - `RateLimitService` uses Redis + Lua script for counter/TTL handling.
  - `RateLimitInterceptor` still exists but marked deprecated.
- **Error handling**
  - `GlobalExceptionHandle` maps domain/security/validation exceptions to API responses.
- **Base persistence model**
  - `BaseEntity` provides `id`, audit fields, and lifecycle hooks (`@PrePersist`, `@PreUpdate`).

## 5) Integration points
- **MySQL + Flyway**
  - Migrations define RBAC, warehouse/location/master data, batch, inventory, inbound, outbound, stock movement, email logs.
- **Redis**
  - `RedisConfig` configures `RedisTemplate` and `CacheManager`.
  - Used by rate limit logic and caching (example: UOM service).
- **RabbitMQ + Email**
  - `RabbitMQConfig` and `RabbitMQEmailConfig`.
  - Async flow: `EmailServiceImpl` -> `EmailProducerService` -> Rabbit queue -> `EmailConsumerService`.
  - Scheduled recovery/maintenance in `EmailScheduledService`.

## 6) Current implementation shape (important for planning)
- Fully implemented REST + business logic exists mainly for:
  - Auth, Product, Warehouse, Location, Units of Measure, User read APIs, Email module.
- Multiple domain modules currently have entity/repository/service/controller scaffolding but minimal business methods in service/controller (e.g., inbound/outbound/inventory/stock movement controllers are mostly route placeholders).

## 7) End-to-end control/data flow (implemented pattern)
1. Request enters `SecurityFilterChain` (rate limit + JWT/authz).
2. Controller validates input DTO and calls service.
3. Service applies business validations, loads related data via repositories.
4. Mapper converts entity <-> DTO.
5. Repository persists/queries JPA entities.
6. Global exception handler standardizes error response.

