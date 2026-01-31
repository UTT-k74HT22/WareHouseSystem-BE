# 🚀 Quick Start - Frontend Integration với WMS Backend

## 📌 Backend Information

- **Base URL (Development):** `http://localhost:8080`
- **API Prefix:** `/api/v1`
- **Authentication:** JWT Bearer Token

## ✅ CORS Đã Được Cấu Hình Sẵn

Backend đã cấu hình CORS cho các frontend ports phổ biến:
- ✅ `http://localhost:3000` - React (CRA)
- ✅ `http://localhost:5173` - Vite
- ✅ `http://localhost:8080` - Backend Swagger UI

## 🔧 Setup Frontend

### 1. React/Next.js với Axios

#### Cài đặt
```bash
npm install axios
```

#### Cấu hình (src/api/axios.js)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true,  // ← QUAN TRỌNG: Bắt buộc phải có
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Tự động thêm JWT token vào mỗi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Xử lý lỗi và refresh token
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post(
          'http://localhost:8080/api/v1/auth/refresh',
          { refreshToken }
        );
        
        localStorage.setItem('accessToken', data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (err) {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

### 2. Vue.js với Axios

#### Cài đặt
```bash
npm install axios
```

#### Cấu hình (src/api/index.js)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true,
  timeout: 30000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

### 3. Angular với HttpClient

#### app.config.ts hoặc app.module.ts
```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    // ...
  ]
};
```

#### auth.interceptor.ts
```typescript
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('accessToken');
  
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      },
      withCredentials: true
    });
  }
  
  return next(req);
};
```

#### api.service.ts
```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  get<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${endpoint}`);
  }

  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, data);
  }
}
```

## 🔐 Authentication Flow

### 1. Login
```javascript
// POST /api/v1/auth/login
const response = await api.post('/auth/login', {
  username: 'admin',
  password: 'password123'
});

// Lưu tokens
localStorage.setItem('accessToken', response.data.accessToken);
localStorage.setItem('refreshToken', response.data.refreshToken);
```

### 2. Sử dụng API với Token
```javascript
// Axios tự động thêm token vào header nhờ interceptor
const users = await api.get('/users');
const product = await api.get('/products/1');
```

### 3. Logout
```javascript
// POST /api/v1/auth/logout
await api.post('/auth/logout');

// Xóa tokens
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
```

## 📡 API Endpoints Chính

### Authentication
- `POST /api/v1/auth/register` - Đăng ký
- `POST /api/v1/auth/login` - Đăng nhập
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Đăng xuất

### Users
- `GET /api/v1/users` - Danh sách users
- `GET /api/v1/users/{id}` - Chi tiết user
- `POST /api/v1/users` - Tạo user mới
- `PUT /api/v1/users/{id}` - Cập nhật user
- `DELETE /api/v1/users/{id}` - Xóa user

### Products (Module 2 - Master Data)
- `GET /api/v1/products` - Danh sách sản phẩm
- `GET /api/v1/products/{id}` - Chi tiết sản phẩm
- `POST /api/v1/products` - Tạo sản phẩm
- `PUT /api/v1/products/{id}` - Cập nhật sản phẩm
- `DELETE /api/v1/products/{id}` - Xóa sản phẩm

### Warehouses
- `GET /api/v1/warehouses` - Danh sách kho
- `GET /api/v1/warehouses/{id}` - Chi tiết kho

## 🎨 Example Components

### React Login Component
```jsx
import { useState } from 'react';
import api from './api/axios';

function Login() {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post('/auth/login', credentials);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      window.location.href = '/dashboard';
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Username"
        value={credentials.username}
        onChange={(e) => setCredentials({ ...credentials, username: e.target.value })}
      />
      <input
        type="password"
        placeholder="Password"
        value={credentials.password}
        onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
      />
      {error && <p className="error">{error}</p>}
      <button type="submit">Login</button>
    </form>
  );
}

export default Login;
```

### React Data Fetching Component
```jsx
import { useState, useEffect } from 'react';
import api from './api/axios';

function UserList() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const { data } = await api.get('/users');
      setUsers(data.data || data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>{user.username} - {user.email}</li>
      ))}
    </ul>
  );
}

export default UserList;
```

## 🐛 Troubleshooting

### ❌ CORS Error
```
Access to XMLHttpRequest at 'http://localhost:8080/api/v1/users' from origin 'http://localhost:3000' 
has been blocked by CORS policy
```

**Giải pháp:**
- Đảm bảo backend đang chạy
- Kiểm tra `withCredentials: true` trong axios config
- Frontend port phải nằm trong danh sách: 3000, 5173, 8080

### ❌ 401 Unauthorized
```
401 Unauthorized
```

**Giải pháp:**
- Kiểm tra token có trong localStorage không
- Token có đúng format `Bearer <token>` không
- Token có hết hạn chưa (access token: 1h, refresh token: 24h)

### ❌ Network Error
```
Network Error
```

**Giải pháp:**
- Kiểm tra backend có đang chạy không (`http://localhost:8080`)
- Kiểm tra firewall/antivirus
- Test với `curl http://localhost:8080/actuator/health`

## 📚 Thêm Tài Liệu

- **API Documentation:** `http://localhost:8080/swagger-ui/index.html`
- **CORS Guide:** `src/documents/CORS_CONFIGURATION_GUIDE.md`
- **Backend README:** `README.md`

## ✅ Checklist Frontend Setup

- [ ] Cài đặt axios hoặc setup HttpClient
- [ ] Cấu hình `baseURL` = `http://localhost:8080/api/v1`
- [ ] Cấu hình `withCredentials: true`
- [ ] Setup interceptor để thêm JWT token
- [ ] Lưu `accessToken` và `refreshToken` sau login
- [ ] Xóa tokens khi logout
- [ ] Xử lý refresh token khi 401
- [ ] Test login → call API → logout flow

---

**Happy Coding! 🎉**

Nếu gặp vấn đề, check:
1. Browser Console (F12) → xem error message
2. Network Tab → xem request/response headers
3. Backend logs → xem request có đến server không
