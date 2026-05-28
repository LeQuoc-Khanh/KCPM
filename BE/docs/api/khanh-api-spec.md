# API Spec - Member 5 (Gamification, Admin, Content)
Owner: Member 5
Base URL: /api/v1
Auth: JWT Bearer Token

---

## 1. Authentication & Roles
### 1.1 Header
Authorization: Bearer <access_token>

### 1.2 Roles
- USER
- ADMIN

---

## 2. Conventions
### 2.1 Success Response Format
{
  "success": true,
  "data": ...
}

### 2.2 Error Response Format
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "title is required",
    "details": [{"field":"title","reason":"must not be blank"}]
  }
}

### 2.3 Status Codes
- 200 OK
- 201 Created
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 500 Internal Server Error

---

## 3. Module: Content / Learning Hub (Articles)

### 3.1 GET /articles
Role: Public (or USER)
Query:
- page (default 1)
- size (default 10)
- q (optional)
- category (optional)
- sort (createdAt,desc)

Response 200:
{
  "success": true,
  "data": {
    "items": [
      {"id": 12, "title": "...", "category": "CV", "status": "PUBLISHED", "createdAt": "..." }
    ],
    "page": 1,
    "size": 10,
    "totalItems": 34,
    "totalPages": 4
  }
}

Errors:
- 400 invalid query
- 500

### 3.2 GET /articles/{id}
Role: Public (or USER)
Path:
- id (long)

Response 200:
{
  "success": true,
  "data": {
    "id": 12,
    "title": "...",
    "content": "...",
    "category": "CV",
    "status": "PUBLISHED",
    "createdAt": "...",
    "updatedAt": "..."
  }
}

Errors:
- 404 not found
- 403 if DRAFT and not ADMIN

### 3.3 POST /admin/articles
Role: ADMIN
Body:
{
  "title": "Kỹ năng phỏng vấn",
  "content": "Nội dung...",
  "category": "Interview",
  "status": "DRAFT"
}

Response 201:
{ "success": true, "data": { "id": 55 } }

Errors:
- 400 validation
- 401/403
- 500

### 3.4 PUT /admin/articles/{id}
Role: ADMIN
Body:
{
  "title": "...",
  "content": "...",
  "category": "...",
  "status": "PUBLISHED"
}

Response 200:
{ "success": true, "data": { "id": 55 } }

Errors:
- 400/401/403/404/500

### 3.5 DELETE /admin/articles/{id}
Role: ADMIN
Response 204 (no body)
Errors:
- 401/403/404/500

---

## 4. Module: Gamification

### 4.1 POST /gamification/events
Role: USER/ADMIN
Body:
{
  "eventType": "UPLOAD_CV",
  "refId": 12345
}

Allowed eventType:
- COMPLETE_PROFILE
- UPLOAD_CV
- APPLY_JOB
- UPDATE_CV

Response 201:
{
  "success": true,
  "data": {
    "addedPoints": 30,
    "totalPoints": 110,
    "newBadges": [{"code":"ACTIVE","name":"Active Candidate"}]
  }
}

Errors:
- 400 invalid eventType
- 401
- 409 duplicate event (optional rule)
- 500

### 4.2 GET /gamification/me
Role: USER/ADMIN
Response 200:
{
  "success": true,
  "data": {
    "totalPoints": 110,
    "badges": [
      {"code":"BEGINNER","name":"Beginner","achievedAt":"..."}
    ],
    "recentEvents": [
      {"eventType":"UPLOAD_CV","points":30,"createdAt":"..."}
    ]
  }
}

Errors:
- 401
- 500

### 4.3 GET /admin/leaderboard
Role: ADMIN
Query:
- limit (default 10)

Response 200:
{
  "success": true,
  "data": {
    "items": [
      {"rank": 1, "userId": 8, "displayName": "Nguyen A", "totalPoints": 450}
    ]
  }
}

Errors:
- 401/403
- 500

---

## 5. Module: Admin Dashboard

### 5.1 GET /admin/dashboard/summary
Role: ADMIN

Response 200:
{
  "success": true,
  "data": {
    "totalUsers": 1200,
    "totalJobs": 340,
    "totalApplications": 5200,
    "totalArticles": 42
  }
}

Errors:
- 401/403
- 500

### 5.2 GET /admin/dashboard/applications-by-day
Role: ADMIN
Query:
- days (default 7)

Response 200:
{
  "success": true,
  "data": {
    "days": 14,
    "series": [
      {"date":"2025-12-02","count":210}
    ]
  }
}

Errors:
- 400 invalid days
- 401/403
- 500
