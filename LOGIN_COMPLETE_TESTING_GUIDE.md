# Email/Password Login - Complete Testing & Verification Guide

## Overview

This guide will help you verify that email/password login is working correctly and troubleshoot any issues.

## Part 1: Setup Test User

### Option A: Create User via Registration (Recommended)
1. Go to `http://localhost:3000/register`
2. Fill form:
   - Email: `test@example.com`
   - Full Name: `Test User`
   - Password: `TestPassword123`
   - Role: Select a role
   - Upload avatar (optional)
3. Click Register
4. Check email for verification code (or server logs)
5. Go to email verification page and enter code
6. User is now ACTIVE and can login

### Option B: Create User via SQL (For Testing)

```sql
-- First, generate a BCrypt hash for password "TestPassword123"
-- You can use: https://www.bcryptcalculator.com/ or your backend's password encoder

-- Password: TestPassword123
-- BCrypt Hash: $2a$10$slYQmyNdGzin7olVZiYm2OPST9/PgBkqquzi.Uh1AFAIj1mvp9eaC

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
  '$2a$10$slYQmyNdGzin7olVZiYm2OPST9/PgBkqquzi.Uh1AFAIj1mvp9eaC',
  'CANDIDATE',
  'LOCAL',
  'ACTIVE',
  true,
  NOW(),
  NOW()
);

-- Verify insertion
SELECT id, email, full_name, status, is_email_verified FROM users WHERE email = 'test@example.com';
```

## Part 2: Test Backend Login API

### Step 1: Check User Exists
```bash
curl -X GET "http://localhost:8080/api/auth/debug/user?email=test@example.com"
```

Expected output:
- User should have `status: "ACTIVE"`
- User should have `isEmailVerified: true`
- User should have their email and role

### Step 2: Test Login API Directly
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123"
  }'
```

Expected response (200 OK):
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
      "isEmailVerified": true,
      "profileImageUrl": "...",
      "createdAt": "2024-...",
      "lastLoginAt": "2024-...",
      "vipExpirationDate": null
    }
  }
}
```

### Step 3: Test with Wrong Password
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "WrongPassword"
  }'
```

Expected response (401 Unauthorized):
```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không chính xác",
  "data": null
}
```

### Step 4: Test with Non-Existent User
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com",
    "password": "AnyPassword"
  }'
```

Expected response (401 Unauthorized):
```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không chính xác",
  "data": null
}
```

### Step 5: Test with Unverified User

First, create a user with `status = 'PENDING_VERIFICATION'` and `is_email_verified = false`:

```sql
INSERT INTO users (email, full_name, password, user_role, auth_provider, status, is_email_verified, verification_code, created_at, updated_at) 
VALUES ('unverified@example.com', 'Unverified User', '$2a$10$slYQmyNdGzin7olVZiYm2OPST9/PgBkqquzi.Uh1AFAIj1mvp9eaC', 'CANDIDATE', 'LOCAL', 'PENDING_VERIFICATION', false, '123456', NOW(), NOW());
```

Then try to login:
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "unverified@example.com",
    "password": "TestPassword123"
  }'
```

Expected response (401 Unauthorized):
```json
{
  "success": false,
  "message": "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để xác nhân.",
  "data": null
}
```

## Part 3: Test Frontend Login

### Step 1: Open Login Page
Navigate to: `http://localhost:3000/login`

### Step 2: Enter Valid Credentials
- Email: `test@example.com`
- Password: `TestPassword123`
- Click "Đăng nhập"

### Step 3: Check Results
- Should see success toast: "Đăng nhập thành công!"
- Should be redirected to appropriate dashboard based on role:
  - ADMIN → `/admin/dashboard`
  - RECRUITER/RECRUITER_VIP → `/dashboard-recruiter`
  - CANDIDATE/CANDIDATE_VIP → `/dashboard-candidate`

### Step 4: Verify Browser Console
Open DevTools Console and look for logs like:
```
[LOGIN] Attempting login with email: test@example.com
[LOGIN] Login successful, received authData: {...}
[LOGIN] Setting user context: {...}
[LOGIN] Redirecting based on user role: CANDIDATE
```

### Step 5: Test Error Cases
Try different error scenarios and verify correct error messages:
- **Wrong password**: "Email hoặc mật khẩu không chính xác"
- **Non-existent user**: "Email hoặc mật khẩu không chính xác"
- **Unverified email**: "Tài khoản chưa được xác thực..."
- **Banned account**: "Tài khoản của bạn đã bị khoá..."

## Part 4: Compare with Google Login

### Google Login Test
1. Go to login page
2. Click "Đăng nhập với Google"
3. Select account or login
4. Should be redirected to dashboard

### Differences to Note
- **Google login**: Creates account if new, auto-verifies email
- **Email/password login**: Requires pre-existing ACTIVE account with verified email

Both should:
- Save tokens to localStorage
- Redirect to appropriate dashboard
- Show user info in header

## Part 5: Verify Token Handling

### Check Token Storage
Open DevTools Storage tab, check localStorage for:
- `access_token` - JWT token for authenticated requests
- `refresh_token` - Token to get new access token
- `user_role` - User's role

### Verify Token in Network Tab
1. Go to login page
2. Open Network tab
3. Login with valid credentials
4. Look for POST request to `/api/auth/login`
5. Check Response tab for complete AuthResponse with tokens

## Part 6: Backend Logs

To see detailed login flow, check backend logs for messages like:

```
[DEBUG] Login attempt with email: test@example.com
[DEBUG] Authenticating user: test@example.com
[DEBUG] Authentication successful for: test@example.com
[DEBUG] Loading user details: test@example.com (status: ACTIVE)
[DEBUG] User loaded successfully: test@example.com with role: CANDIDATE
[INFO] User logged in successfully: test@example.com (CANDIDATE)
```

## Troubleshooting

### Issue: Login page appears but login doesn't work
**Check:**
1. Browser console for errors
2. Network tab for failed requests
3. Backend is running on `http://localhost:8080`
4. Frontend can reach backend (check CORS)

### Issue: "Email hoặc mật khẩu không chính xác" always shows
**Check:**
1. User exists in database: `SELECT * FROM users WHERE email='test@example.com';`
2. User status is ACTIVE: `SELECT status FROM users WHERE email='test@example.com';`
3. User email is verified: `SELECT is_email_verified FROM users WHERE email='test@example.com';`
4. Password hash is correct (compare with test password BCrypt)

### Issue: "Tài khoản chưa được xác thực" message
**Fix:**
```sql
UPDATE users SET status='ACTIVE', is_email_verified=true WHERE email='test@example.com';
```

### Issue: Redirects to /login after login success
**Check:**
1. Tokens are saved correctly (check localStorage)
2. User context is set (check in Authcontext)
3. Backend returned user role in response
4. Frontend is checking user role correctly

## Files Modified for Login Fix

### Backend
- ✅ `BE/src/main/java/app/auth/service/AuthService.java` - Enhanced login error handling
- ✅ `BE/src/main/java/app/auth/security/CustomUserDetailsService.java` - Added validation logging  
- ✅ `BE/src/main/java/app/auth/security/UserPrincipal.java` - Clarified status checks
- ✅ `BE/src/main/java/app/exception/GlobalExceptionHandler.java` - Better error responses
- ✅ `BE/src/main/java/app/auth/controller/AuthController.java` - Added debug endpoint

### Frontend
- ✅ `FE/src/app/(auth)/login/page.tsx` - Enhanced error handling and logging

## Next Steps

1. **Deploy Changes**
   - Rebuild backend: `mvn clean package`
   - Restart backend service
   - Clear frontend cache and reload browser

2. **Test Thoroughly**
   - Follow Part 1-5 of this guide
   - Test all error scenarios
   - Compare Google vs email/password login

3. **Monitor**
   - Check backend logs for errors
   - Monitor user experience
   - Collect feedback from users

## Support

For additional help:
1. Use debug endpoint: `GET /api/auth/debug/user?email=<email>`
2. Check backend logs with DEBUG level enabled
3. Use browser DevTools to inspect network requests and console logs
4. Check this guide for specific error scenarios

---

**Last Updated:** June 3, 2026
**Backend Fixes:** 7 files modified
**Frontend Fixes:** 1 file enhanced
**Status:** Ready for testing
