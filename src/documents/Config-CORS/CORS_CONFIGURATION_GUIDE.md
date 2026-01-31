# CORS Configuration Guide - WMS Backend

## 📋 Tổng Quan

CORS (Cross-Origin Resource Sharing) cho phép Frontend (chạy trên domain khác) gọi API của Backend một cách an toàn.

### Kiến Trúc CORS Trong Project

```
Request Flow:
Frontend (http://localhost:3000)
    ↓
    ↓ Preflight Request (OPTIONS)
    ↓
Backend (http://localhost:8080)
    ↓
    ↓ CORS Filter (SecurityConfig) ← Primary Layer
    ↓
    ↓ Rate Limit Filter
    ↓
    ↓ JWT Authentication Filter
    ↓
    ↓ WebMvcConfig ← Backup Layer
    ↓
Controller
```

## 🔧 Cấu Hình Hiện Tại

### 1. Primary CORS Configuration (SecurityConfig.java)

**Vị trí:** `src/main/java/org/demo/whs/configuration/SecurityConfig.java`

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // Allowed Origins từ application.yml
    config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    
    // Allowed Methods
    config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    
    // Allowed Headers
    config.addAllowedHeader("*");
    
    // Exposed Headers - cho phép FE đọc các header này
    config.setExposedHeaders(List.of(
        "Authorization",           // JWT token
        "X-Total-Count",          // Pagination
        "X-RateLimit-Remaining",  // Rate limit info
        "X-RateLimit-Reset"       // Rate limit reset time
    ));
    
    // Allow credentials (cookies, JWT)
    config.setAllowCredentials(true);
    
    // Cache preflight request for 1 hour
    config.setMaxAge(3600L);
    
    return source;
}
```

### 2. Backup CORS Configuration (WebMvcConfig.java)

**Vị trí:** `src/main/java/org/demo/whs/configuration/WebMvcConfig.java`

Cấu hình backup cho các endpoint không đi qua Security Filter Chain (nếu có).

### 3. Configuration trong application.yml

```yaml
app:
  cors:
    # Development
    allowed-origins: http://localhost:3000,http://localhost:5173,http://localhost:8080
    
    # Production (ví dụ)
    # allowed-origins: https://warehouse.yourdomain.com
    
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: "*"
    max-age: 3600
```

## 🚀 Hướng Dẫn Sử Dụng

### Development Environment

1. **Khởi động Backend:**
   ```bash
   mvn spring-boot:run
   ```
   Backend chạy tại: `http://localhost:8080`

2. **Khởi động Frontend:**
   - React (Create React App): `http://localhost:3000`
   - Vite: `http://localhost:5173`
   - Angular: `http://localhost:4200`

3. **Cấu hình Frontend để gọi API:**

   **React/Next.js:**
   ```javascript
   // Sử dụng axios
   import axios from 'axios';
   
   const api = axios.create({
     baseURL: 'http://localhost:8080/api/v1',
     withCredentials: true,  // QUAN TRỌNG: gửi cookies/JWT
     headers: {
       'Content-Type': 'application/json'
     }
   });
   
   // Thêm JWT token vào mỗi request
   api.interceptors.request.use((config) => {
     const token = localStorage.getItem('accessToken');
     if (token) {
       config.headers.Authorization = `Bearer ${token}`;
     }
     return config;
   });
   
   // Sử dụng
   const response = await api.get('/users');
   ```

   **Fetch API:**
   ```javascript
   const token = localStorage.getItem('accessToken');
   
   const response = await fetch('http://localhost:8080/api/v1/users', {
     method: 'GET',
     credentials: 'include',  // QUAN TRỌNG
     headers: {
       'Content-Type': 'application/json',
       'Authorization': `Bearer ${token}`
     }
   });
   ```

### Production Environment

#### 1. Cập nhật CORS Origins

**Trong .env hoặc environment variables:**
```bash
# Single domain
CORS_ALLOWED_ORIGINS=https://warehouse.yourdomain.com

# Multiple domains
CORS_ALLOWED_ORIGINS=https://warehouse.yourdomain.com,https://www.warehouse.yourdomain.com,https://admin.warehouse.yourdomain.com
```

**Hoặc trong application.yml:**
```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:https://warehouse.yourdomain.com}
```

#### 2. Security Checklist

- ✅ **KHÔNG dùng wildcard** `*` cho origins khi `allowCredentials: true`
- ✅ **Chỉ cho phép HTTPS** trong production (trừ localhost khi dev)
- ✅ **Giới hạn origins** - chỉ domain thực tế của bạn
- ✅ **Kiểm tra JWT** được gửi đúng trong header `Authorization`
- ✅ **Enable SSL/TLS** cho MySQL, Redis, RabbitMQ

#### 3. Testing CORS

**Test CORS với curl:**
```bash
# Preflight request (OPTIONS)
curl -X OPTIONS http://localhost:8080/api/v1/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v

# Actual request
curl -X GET http://localhost:8080/api/v1/users \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -v
```

**Kiểm tra response headers:**
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Expose-Headers: Authorization, X-Total-Count, X-RateLimit-Remaining, X-RateLimit-Reset
Access-Control-Max-Age: 3600
```

## 🐛 Troubleshooting

### Lỗi: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Nguyên nhân:**
- Frontend domain chưa được thêm vào `allowed-origins`

**Giải pháp:**
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
```

### Lỗi: "CORS policy: Response to preflight request doesn't pass"

**Nguyên nhân:**
- Method không được allow (thiếu OPTIONS)
- Headers không được allow

**Giải pháp:**
```yaml
app:
  cors:
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: "*"
```

### Lỗi: "CORS policy: Credentials flag is 'true', but the 'Access-Control-Allow-Credentials' header is ''"

**Nguyên nhân:**
- Frontend gửi `credentials: 'include'` hoặc `withCredentials: true`
- Backend chưa set `allowCredentials: true`

**Giải pháp:**
Backend đã cấu hình `allowCredentials(true)` rồi. Đảm bảo không dùng wildcard `*` cho origins.

### Lỗi 401 Unauthorized ngay cả khi CORS OK

**Nguyên nhân:**
- JWT token không được gửi hoặc đã hết hạn
- Token không đúng format `Bearer <token>`

**Giải pháp:**
```javascript
// Đảm bảo gửi token đúng format
headers: {
  'Authorization': `Bearer ${token}`  // Có space sau "Bearer"
}
```

## 📊 CORS Headers Explained

| Header | Giá trị | Ý nghĩa |
|--------|---------|---------|
| `Access-Control-Allow-Origin` | `http://localhost:3000` | Domain được phép gọi API |
| `Access-Control-Allow-Methods` | `GET, POST, PUT, DELETE` | HTTP methods được phép |
| `Access-Control-Allow-Headers` | `*` hoặc `Authorization, Content-Type` | Headers được phép gửi |
| `Access-Control-Allow-Credentials` | `true` | Cho phép gửi cookies/JWT |
| `Access-Control-Expose-Headers` | `Authorization, X-Total-Count` | Headers mà FE được đọc từ response |
| `Access-Control-Max-Age` | `3600` | Cache preflight request (giây) |

## 🔐 Best Practices

### Development
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: "*"
    max-age: 3600
```

### Production
```yaml
app:
  cors:
    # CHỈ domain thực tế
    allowed-origins: https://warehouse.yourdomain.com
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    # Có thể giới hạn headers cụ thể thay vì "*"
    allowed-headers: Authorization,Content-Type,X-Requested-With
    max-age: 86400  # 24 hours
```

### Multiple Environments

Sử dụng Spring Profiles:

**application-dev.yml:**
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
```

**application-prod.yml:**
```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

## 📝 Common Frontend Axios Setup

```javascript
// api.js
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1',
  withCredentials: true,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - thêm JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - xử lý lỗi
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Nếu 401 và chưa retry, thử refresh token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(
          'http://localhost:8080/api/v1/auth/refresh',
          { refreshToken }
        );
        
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh token hết hạn, đăng xuất
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

## 🎯 Summary

1. **CORS đã được cấu hình đầy đủ** trong SecurityConfig và WebMvcConfig
2. **Frontend chỉ cần:**
   - Gửi request với `withCredentials: true` (axios) hoặc `credentials: 'include'` (fetch)
   - Thêm `Authorization: Bearer <token>` vào headers
3. **Production:**
   - Cập nhật `CORS_ALLOWED_ORIGINS` trong environment variables
   - Chỉ cho phép HTTPS domains
4. **Testing:**
   - Sử dụng curl hoặc Postman để test CORS headers
   - Kiểm tra browser console nếu có lỗi CORS

## 📞 Support

Nếu gặp vấn đề về CORS:
1. Kiểm tra browser console để xem error message cụ thể
2. Kiểm tra backend logs (Spring Security DEBUG level)
3. Sử dụng browser DevTools → Network tab → xem request/response headers
4. Test với curl để loại trừ vấn đề từ frontend code

---

**Last Updated:** January 30, 2026
**Version:** 1.0
