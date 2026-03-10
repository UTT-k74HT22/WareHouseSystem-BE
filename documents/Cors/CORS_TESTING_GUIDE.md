# CORS Testing Scripts

## Test với curl (Command Line)

### 1. Test Preflight Request (OPTIONS)

```bash
# Test CORS preflight từ localhost:3000
curl -X OPTIONS http://localhost:8080/api/v1/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type" \
  -v

# Expected Response Headers:
# HTTP/1.1 200
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
# Access-Control-Allow-Headers: Authorization,Content-Type
# Access-Control-Allow-Credentials: true
# Access-Control-Max-Age: 3600
```

### 2. Test Actual Request (GET)

```bash
# Test GET request với JWT token
curl -X GET http://localhost:8080/api/v1/users \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -v

# Expected Response Headers:
# HTTP/1.1 200
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Credentials: true
# Access-Control-Expose-Headers: Authorization, X-Total-Count, X-RateLimit-Remaining, X-RateLimit-Reset
```

### 3. Test Public Endpoint (Login)

```bash
# Test login endpoint (không cần token)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123"
  }' \
  -v

# Expected: Trả về accessToken và refreshToken
```

## Test với PowerShell (Windows)

### 1. Test Preflight Request

```powershell
$headers = @{
    "Origin" = "http://localhost:3000"
    "Access-Control-Request-Method" = "GET"
    "Access-Control-Request-Headers" = "Authorization,Content-Type"
}

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/users" `
    -Method OPTIONS `
    -Headers $headers `
    -Verbose

# Kiểm tra response headers
$response.Headers
```

### 2. Test Actual Request

```powershell
$headers = @{
    "Origin" = "http://localhost:3000"
    "Authorization" = "Bearer YOUR_JWT_TOKEN_HERE"
    "Content-Type" = "application/json"
}

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/users" `
    -Method GET `
    -Headers $headers `
    -Verbose
```

### 3. Test Login

```powershell
$body = @{
    username = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$headers = @{
    "Origin" = "http://localhost:3000"
    "Content-Type" = "application/json"
}

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST `
    -Headers $headers `
    -Body $body

# Xem response
$response

# Lấy access token
$accessToken = $response.accessToken
Write-Host "Access Token: $accessToken"
```

## Test với JavaScript (Browser Console)

### 1. Test với Fetch API

```javascript
// Test login
fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin',
    password: 'Admin@123'
  })
})
.then(res => res.json())
.then(data => {
  console.log('Login success:', data);
  localStorage.setItem('accessToken', data.accessToken);
})
.catch(err => console.error('Login failed:', err));

// Test GET with token
fetch('http://localhost:8080/api/v1/users', {
  method: 'GET',
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
})
.then(res => res.json())
.then(data => console.log('Users:', data))
.catch(err => console.error('Fetch failed:', err));
```

### 2. Test với Axios

```javascript
// Import axios trong browser console
// Hoặc test trong React app

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Test login
api.post('/auth/login', {
  username: 'admin',
  password: 'Admin@123'
})
.then(response => {
  console.log('Login success:', response.data);
  localStorage.setItem('accessToken', response.data.accessToken);
})
.catch(error => {
  console.error('Login failed:', error);
});

// Test GET with token
api.get('/users', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
})
.then(response => {
  console.log('Users:', response.data);
})
.catch(error => {
  console.error('Fetch failed:', error);
});
```

## Test với Postman

### 1. Setup Environment

Tạo environment variables:
- `BASE_URL`: `http://localhost:8080/api/v1`
- `ACCESS_TOKEN`: (sẽ set tự động sau login)

### 2. Test Login Request

```
POST {{BASE_URL}}/auth/login
Headers:
  Content-Type: application/json
  Origin: http://localhost:3000
Body (JSON):
{
  "username": "admin",
  "password": "Admin@123"
}

Tests (Script):
if (pm.response.code === 200) {
  const data = pm.response.json();
  pm.environment.set("ACCESS_TOKEN", data.accessToken);
}
```

### 3. Test Protected Endpoint

```
GET {{BASE_URL}}/users
Headers:
  Authorization: Bearer {{ACCESS_TOKEN}}
  Origin: http://localhost:3000
```

### 4. Kiểm tra CORS Headers

Trong Postman, sau khi send request, kiểm tra tab "Headers" trong response:
- ✅ `Access-Control-Allow-Origin`: http://localhost:3000
- ✅ `Access-Control-Allow-Credentials`: true
- ✅ `Access-Control-Expose-Headers`: Authorization, X-Total-Count, X-RateLimit-Remaining, X-RateLimit-Reset

## Automated Test Script (Node.js)

Create `test-cors.js`:

```javascript
const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api/v1';
const ORIGIN = 'http://localhost:3000';

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: {
    'Origin': ORIGIN,
    'Content-Type': 'application/json'
  }
});

async function testCORS() {
  console.log('🧪 Testing CORS Configuration...\n');

  try {
    // Test 1: Login
    console.log('1️⃣ Testing Login...');
    const loginResponse = await api.post('/auth/login', {
      username: 'admin',
      password: 'Admin@123'
    });

    if (loginResponse.status === 200) {
      console.log('✅ Login successful');
      const accessToken = loginResponse.data.accessToken;
      
      // Test 2: GET with token
      console.log('\n2️⃣ Testing GET with JWT token...');
      api.defaults.headers['Authorization'] = `Bearer ${accessToken}`;
      
      const usersResponse = await api.get('/users');
      console.log('✅ GET request successful');
      console.log(`   Users count: ${usersResponse.data.data?.length || 0}`);
      
      // Test 3: Check CORS headers
      console.log('\n3️⃣ Checking CORS headers...');
      const corsHeaders = [
        'access-control-allow-origin',
        'access-control-allow-credentials',
        'access-control-expose-headers'
      ];
      
      corsHeaders.forEach(header => {
        const value = usersResponse.headers[header];
        if (value) {
          console.log(`✅ ${header}: ${value}`);
        } else {
          console.log(`❌ ${header}: NOT FOUND`);
        }
      });
      
      console.log('\n🎉 All CORS tests passed!');
    }
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    if (error.response) {
      console.error('   Status:', error.response.status);
      console.error('   Data:', error.response.data);
    }
  }
}

testCORS();
```

Run:
```bash
npm install axios
node test-cors.js
```

## Expected Results Checklist

### ✅ Successful CORS Response

```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
Access-Control-Allow-Headers: Authorization,Content-Type
Access-Control-Expose-Headers: Authorization, X-Total-Count, X-RateLimit-Remaining, X-RateLimit-Reset
Access-Control-Max-Age: 3600
Content-Type: application/json
```

### ❌ Common Error Responses

#### CORS Error - Origin not allowed
```
Access-Control-Allow-Origin: null (hoặc không có header này)
```
→ Thêm origin vào `application.yml`

#### Credentials Error
```
Access-Control-Allow-Credentials: false (hoặc không có)
```
→ Kiểm tra `allowCredentials(true)` trong SecurityConfig

#### 401 Unauthorized
```
HTTP/1.1 401 Unauthorized
```
→ Token hết hạn hoặc không đúng format

## Continuous Testing

### Run before deploying:

```bash
# 1. Start backend
mvn spring-boot:run

# 2. Wait for startup
sleep 10

# 3. Run CORS tests
node test-cors.js

# 4. If all pass, deploy
```

---

**Last Updated:** January 30, 2026
