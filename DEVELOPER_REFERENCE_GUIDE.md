# Email/Password Login - Developer Reference Guide

## Architecture Overview

```
Frontend (Next.js) 
    ↓ [POST /api/auth/login]
    ↓
Backend (Spring Boot)
    ├─ AuthController (REST endpoint)
    ├─ AuthService (Business logic)
    ├─ AuthenticationManager (Spring Security)
    ├─ CustomUserDetailsService (User loading)
    ├─ GlobalExceptionHandler (Error handling)
    └─ JwtTokenProvider (Token generation)
    ↓
PostgreSQL Database (User data)
```

## Request/Response Flow

### 1. Frontend: User Submits Login Form
```typescript
// fe-nextjs/src/app/(auth)/login/page.tsx
const handleLogin = async (e: FormEvent) => {
  const authData = await login({ email, password });
  // login() calls authService.ts
};
```

### 2. Frontend: Axios Call via authService
```typescript
// fe-nextjs/src/services/authService.ts
export const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  // Extracts authData from response.data.data
  return response.data.data;
};
```

### 3. Backend: REST Controller Receives Request
```java
// BE/src/main/java/app/auth/controller/AuthController.java
@PostMapping("/login")
public ResponseEntity<MessageResponse> login(@RequestBody LoginRequest request) {
    AuthResponse authResponse = authService.login(request.getEmail(), request.getPassword());
    return ResponseEntity.ok(MessageResponse.success("Đăng nhập thành công", authResponse));
}
```

### 4. Backend: Service Handles Authentication (7 Steps)
```java
// BE/src/main/java/app/auth/service/AuthService.java
public AuthResponse login(String email, String password) {
    // Step 1: Authenticate credentials with Spring Security
    Authentication authentication = authenticationManager.authenticate(...);
    
    // Step 2: Load user from database
    User user = userRepository.findByEmail(email).orElse(null);
    
    // Step 3: Validate user status (ACTIVE/PENDING_VERIFICATION/BANNED)
    if (user.getStatus() != UserStatus.ACTIVE)
        throw new BadCredentialsException("Account not active");
    
    // Step 4: Validate email verification
    if (!user.getIsEmailVerified())
        throw new BadCredentialsException("Email not verified");
    
    // Step 5: Check maintenance mode
    if (systemSetting.isMaintenanceMode())
        throw new MaintenanceException();
    
    // Step 6: Update last login timestamp
    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);
    
    // Step 7: Generate tokens and return response
    String accessToken = jwtTokenProvider.generateAccessToken(email, user.getUserRole());
    String refreshToken = refreshTokenService.generateRefreshToken(user.getId());
    return new AuthResponse(accessToken, refreshToken, user);
}
```

### 5. Backend: User Details Loading (During Authentication)
```java
// BE/src/main/java/app/auth/security/CustomUserDetailsService.java
@Override
public UserDetails loadUserByUsername(String email) {
    // This is called by Spring Security's AuthenticationManager
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Check if user is banned
    if (user.getStatus() == UserStatus.BANNED)
        return UserPrincipal.create(user, false); // isEnabled = false
    
    // Check if VIP expired
    if (user.isVipExpired())
        downgradeSqlVipRole();
    
    return UserPrincipal.create(user, true);
}
```

### 6. Backend: Exception Handling
```java
// BE/src/main/java/app/exception/GlobalExceptionHandler.java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<MessageResponse> handleBadCredentials() {
    // Returns 401 Unauthorized with generic message
    return ResponseEntity.status(401)
        .body(MessageResponse.error("Email hoặc mật khẩu không chính xác"));
}

@ExceptionHandler(DisabledException.class)
public ResponseEntity<MessageResponse> handleDisabled() {
    // User is banned or disabled
    return ResponseEntity.status(401)
        .body(MessageResponse.error("Tài khoản đã bị khoá"));
}
```

### 7. Backend: Response Structure
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800000,
    "user": {
      "id": 1,
      "email": "test@example.com",
      "fullName": "Test User",
      "userRole": "CANDIDATE",
      "status": "ACTIVE",
      "isEmailVerified": true
    }
  }
}
```

### 8. Frontend: Response Processing
```typescript
// fe-nextjs/src/app/(auth)/login/page.tsx
if (authData && authData.user) {
  // Save tokens
  setToken(authData.accessToken);
  setRefreshToken(authData.refreshToken);
  
  // Save user context
  setAuthUser(authData.user);
  
  // Redirect based on role
  if (authData.user.userRole === 'ADMIN') {
    router.push('/admin/dashboard');
  } else if (includes(authData.user.userRole, 'RECRUITER')) {
    router.push('/dashboard-recruiter');
  } else {
    router.push('/dashboard-candidate');
  }
}
```

## Error Scenarios

### Scenario 1: Wrong Password
```
Request: { email: "test@example.com", password: "WrongPassword" }
↓
AuthenticationManager attempts to authenticate
↓
BCrypt comparison fails
↓
BadCredentialsException thrown
↓
GlobalExceptionHandler catches it
↓
Response: 401 "Email hoặc mật khẩu không chính xác"
```

### Scenario 2: User Not Found
```
Request: { email: "nonexistent@example.com", password: "AnyPassword" }
↓
CustomUserDetailsService loads user by email
↓
User not found in database
↓
UsernameNotFoundException thrown
↓
AuthenticationManager catches it
↓
GlobalExceptionHandler handles it
↓
Response: 401 "Email hoặc mật khẩu không chính xác"
```

### Scenario 3: Email Not Verified
```
Request: { email: "unverified@example.com", password: "CorrectPassword" }
↓
AuthenticationManager succeeds (password is correct)
↓
AuthService.login() loads user
↓
Checks: user.getIsEmailVerified() == false
↓
Throws: BadCredentialsException("Email not verified")
↓
GlobalExceptionHandler catches it
↓
Response: 401 "Tài khoản chưa được xác thực"
```

### Scenario 4: Account Banned
```
Request: { email: "banned@example.com", password: "CorrectPassword" }
↓
CustomUserDetailsService loads user
↓
Checks: user.getStatus() == BANNED
↓
Returns: UserPrincipal with isEnabled=false
↓
AuthenticationManager encounters DisabledException
↓
GlobalExceptionHandler catches it
↓
Response: 401 "Tài khoản đã bị khoá"
```

### Scenario 5: Network Error
```
Frontend tries: axios.post('http://localhost:8080/api/auth/login')
↓
Backend not reachable / Network timeout
↓
axios throws AxiosError with err.response = undefined
↓
Frontend catch block checks: !err.response
↓
Shows: "Không thể kết nối đến server"
```

## Token Management

### Access Token (JWT)
- **Lifetime:** 30 minutes (1800000 ms)
- **Storage:** localStorage → `access_token`
- **Usage:** Sent in Authorization header for API calls
- **Format:** `Authorization: Bearer <access_token>`
- **Generated by:** JwtTokenProvider.generateAccessToken()

```typescript
// Frontend sends in every authenticated request
const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Refresh Token
- **Lifetime:** 10 hours
- **Storage:** localStorage → `refresh_token`
- **Usage:** Obtained new access_token when expired
- **Endpoint:** `POST /api/auth/refresh-token`
- **Generated by:** RefreshTokenService.generateRefreshToken()

```typescript
// Automatically called when 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Try to refresh token
      const refreshToken = localStorage.getItem('refresh_token');
      const newAccessToken = await api.post('/auth/refresh-token', { refreshToken });
      // Retry original request with new token
    }
  }
);
```

## Debugging Tools

### 1. Backend Debug Endpoint
```bash
# Get user information without modifying database
curl -X GET "http://localhost:8080/api/auth/debug/user?email=test@example.com"

Response:
{
  "success": true,
  "message": "User information retrieved",
  "data": {
    "id": 1,
    "email": "test@example.com",
    "fullName": "Test User",
    "status": "ACTIVE",
    "isEmailVerified": true,
    "userRole": "CANDIDATE",
    "createdAt": "2024-06-03T10:00:00",
    "lastLoginAt": "2024-06-03T15:30:00"
  }
}
```

### 2. Backend Logging
Enable DEBUG level in application.properties:
```properties
logging.level.app.auth=DEBUG
logging.level.org.springframework.security=DEBUG
```

Look for logs:
```
[DEBUG] Login attempt with email: test@example.com
[DEBUG] Authenticating user: test@example.com
[DEBUG] User loaded successfully: test@example.com (status: ACTIVE)
[DEBUG] Checking email verification: true
[INFO] User logged in successfully: test@example.com (CANDIDATE)
```

### 3. Frontend Console Logging
Open browser DevTools (F12) and look for:
```javascript
[LOGIN] Attempting login with email: test@example.com
[LOGIN] Login successful, received authData: {...}
[LOGIN] Setting user context: {...}
[LOGIN] Redirecting based on user role: CANDIDATE
```

### 4. Network Inspection
1. Open DevTools → Network tab
2. Filter by "auth"
3. Click on POST `/api/auth/login` request
4. Check:
   - Request body: credentials
   - Response status: 200 or 401
   - Response body: error or token data
   - Headers: CORS headers, Content-Type

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| 404 Not Found | Backend not running | `java -jar target/app.jar` |
| 401 Always | User has PENDING_VERIFICATION status | Update DB: `UPDATE users SET status='ACTIVE' WHERE ...` |
| No token | Response parsed incorrectly | Check response structure: `response.data.data.accessToken` |
| Silent failure | Frontend missing error logging | Check console for [LOGIN] logs |
| Redirect loop | isAuthenticated check failing | Verify localStorage has tokens |

## Security Considerations

1. **Password Hashing:** BCrypt with random salt (10 rounds minimum)
2. **Token Expiration:** Access tokens expire, refresh tokens are single-use
3. **HTTPS Only:** In production, set Secure flag on tokens
4. **CORS:** Only allow frontend domain (localhost:3000 in dev)
5. **Error Messages:** Generic to prevent user enumeration
6. **Input Validation:** Email format + non-empty password required

## Performance Notes

- **Typical Login Time:** 200-500ms
- **Database Query:** Indexed on email for O(1) lookup
- **Token Generation:** < 10ms using HMAC-SHA256
- **Password Verification:** ~100ms (intentional, for security)

---

**Document Purpose:** Technical reference for developers
**Last Updated:** June 3, 2026
**Audience:** Backend developers, frontend developers, DevOps engineers
