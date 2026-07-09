# Email/Password Login - Debug & Testing Guide

## Quick Test Steps

### 1. Check User in Database
Use the debug endpoint to check if a user exists and their status:
```bash
curl -X GET "http://localhost:8080/api/auth/debug/user?email=test@example.com"
```

Expected response:
```json
{
  "success": true,
  "message": "Debug: User info",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "user": {
      "id": 1,
      "email": "test@example.com",
      "fullName": "Test User",
      "status": "ACTIVE",
      "isEmailVerified": true,
      "userRole": "CANDIDATE",
      ...
    }
  }
}
```

### 2. Test Login Directly with Backend

#### Using cURL (Password: testpassword123)
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"testpassword123"}'
```

#### Using Postman
- Method: POST
- URL: http://localhost:8080/api/auth/login
- Headers: Content-Type: application/json
- Body:
```json
{
  "email": "test@example.com",
  "password": "testpassword123"
}
```

### 3. Expected Responses

#### Success (200 OK)
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 1800000,
    "user": {
      "id": 1,
      "email": "test@example.com",
      "fullName": "Test User",
      "userRole": "CANDIDATE",
      "status": "ACTIVE",
      "isEmailVerified": true,
      ...
    }
  }
}
```

#### Error Cases (401 Unauthorized)
```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không chính xác",
  "data": null
}
```

```json
{
  "success": false,
  "message": "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để xác nhận.",
  "data": null
}
```

```json
{
  "success": false,
  "message": "Tài khoản của bạn đã bị khoá. Liên hệ hỗ trợ để được giúp đỡ.",
  "data": null
}
```

## Troubleshooting

### Issue 1: User Not Found
**Symptom:** "Email hoặc mật khẩu không chính xác"

**Causes:**
1. User doesn't exist in database
2. Email typo in login form or database

**Solution:**
1. Check database: `SELECT * FROM users WHERE email = 'test@example.com';`
2. Create test user if doesn't exist
3. Use debug endpoint to verify

### Issue 2: Email Not Verified
**Symptom:** "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để xác nhận."

**Causes:**
1. User created but email verification not completed
2. Email verification code is invalid or expired
3. is_email_verified = false in database

**Solution:**
1. Use /api/auth/verify-email endpoint with correct code
2. Or manually update database: `UPDATE users SET status='ACTIVE', is_email_verified=true WHERE email='test@example.com';`

### Issue 3: Account Banned
**Symptom:** "Tài khoản của bạn đã bị khoá. Liên hệ hỗ trợ để được giúp đỡ."

**Causes:**
1. User status is BANNED

**Solution:**
1. Check user status: `SELECT status FROM users WHERE email='test@example.com';`
2. Update if needed: `UPDATE users SET status='ACTIVE' WHERE email='test@example.com';`

### Issue 4: Wrong Password
**Symptom:** "Email hoặc mật khẩu không chính xác" (same as not found)

**Solution:**
1. Password is hashed with BCrypt
2. Create new user with known password or reset password
3. Use reset-password endpoint

## Creating Test User

### SQL Command
```sql
INSERT INTO users (
  email, 
  full_name, 
  password,
  user_role, 
  auth_provider, 
  status, 
  is_email_verified,
  created_at, 
  updated_at
) VALUES (
  'test@example.com',
  'Test User',
  '$2a$10$E8Gq8Pp9.T5QvRCxJbGLi.DGWC5gZvYzgE1H8ZxVx3jI8YWvPxAKq', -- password: testpassword123
  'CANDIDATE',
  'LOCAL',
  'ACTIVE',
  true,
  NOW(),
  NOW()
);
```

### Using Frontend Registration
1. Go to /register
2. Fill in form with test credentials
3. Verify email with code from email (or check logs)
4. User should now be able to login

## Backend Logs to Check

Enable debug logging in application.properties:
```properties
logging.level.app.auth=DEBUG
logging.level.app.auth.security=DEBUG
```

Look for these log messages:
```
Login attempt with email: test@example.com
Authenticating user: test@example.com
Authentication successful for: test@example.com
User logged in successfully: test@example.com (CANDIDATE)
```

## Comparing Google Login vs Email/Password Login

### Google Login Flow
1. Frontend gets Google token
2. Sends POST /api/auth/google with token
3. Backend verifies with Google API
4. If user not found, creates new ACTIVE user
5. Returns AuthResponse with tokens

### Email/Password Login Flow
1. Frontend sends POST /api/auth/login with email + password
2. Backend authenticates via authenticationManager
3. CustomUserDetailsService loads user from database
4. Checks password with BCrypt
5. AuthService validates user status (ACTIVE, verified email)
6. Returns AuthResponse with tokens

### Key Differences
- Google login auto-verifies email and creates account if new
- Email/Password login requires pre-existing account with ACTIVE status
- Both return same AuthResponse structure

## Next Steps if Still Having Issues

1. Check browser DevTools Console for frontend errors
2. Check backend logs for detailed error messages
3. Verify database connection is working
4. Test with Postman directly to isolate frontend issues
5. Check JWT token generation with debug endpoint
6. Verify password encoding in database

## Files Modified

### Backend
- `AuthService.java` - Enhanced login error handling
- `CustomUserDetailsService.java` - Added logging
- `UserPrincipal.java` - Comments on status checks
- `GlobalExceptionHandler.java` - Better error responses
- `AuthController.java` - Added debug endpoint

### Frontend
- No changes needed (working as expected with backend fixes)

## Support

For additional debugging, use the debug endpoint:
- GET /api/auth/debug/user?email=<email>

This endpoint logs user information server-side and returns full user data for inspection.
