# Feature 7 Code Audit

Scope reviewed: Notification, Review, Gamification (implemented as `Leaderboard*`), Payment (implemented as VIP role upgrade).

## Files read

- `BE/pom.xml`
- `BE/src/main/resources/application.properties`
- `BE/src/main/resources/db/`
- `scripts/render-postgres-migration/`
- `BE/src/main/java/app/auth/security/SecurityConfig.java`
- `BE/src/main/java/app/auth/security/JwtAuthenticationFilter.java`
- `BE/src/main/java/app/auth/security/JwtTokenProvider.java`
- `BE/src/main/java/app/auth/security/UserPrincipal.java`
- `BE/src/main/java/app/auth/security/CustomUserDetailsService.java`
- `BE/src/main/java/app/util/SecurityUtils.java`
- `BE/src/main/java/app/exception/GlobalExceptionHandler.java`
- `BE/src/main/java/app/auth/model/User.java`
- `BE/src/main/java/app/auth/model/enums/UserRole.java`
- `BE/src/main/java/app/auth/repository/UserRepository.java`
- `BE/src/main/java/app/content/model/Company.java`
- `BE/src/main/java/app/auth/repository/CompanyRepository.java`
- `BE/src/main/java/app/recruitment/service/CompanyService.java`
- `BE/src/main/java/app/notification/controller/NotificationController.java`
- `BE/src/main/java/app/notification/service/NotificationService.java`
- `BE/src/main/java/app/notification/service/NotificationCleanupService.java`
- `BE/src/main/java/app/notification/model/Notification.java`
- `BE/src/main/java/app/notification/repository/NotificationRepository.java`
- `BE/src/main/java/app/review/controller/ReviewController.java`
- `BE/src/main/java/app/review/service/ReviewService.java`
- `BE/src/main/java/app/review/dto/ReviewRequest.java`
- `BE/src/main/java/app/review/dto/ReviewResponse.java`
- `BE/src/main/java/app/review/entity/CompanyReview.java`
- `BE/src/main/java/app/review/repository/CompanyReviewRepository.java`
- `BE/src/main/java/app/gamification/controller/LeaderboardController.java`
- `BE/src/main/java/app/gamification/service/LeaderboardService.java`
- `BE/src/main/java/app/gamification/event/PointEvent.java`
- `BE/src/main/java/app/gamification/model/UserPointAction.java`
- `BE/src/main/java/app/gamification/model/LeaderboardScore.java`
- `BE/src/main/java/app/gamification/model/LeaderboardPointsLog.java`
- `BE/src/main/java/app/gamification/repository/LeaderboardScoreRepository.java`
- `BE/src/main/java/app/gamification/repository/LeaderboardPointsLogRepository.java`
- `BE/src/main/java/app/payment/controller/PaymentController.java`
- `BE/src/main/java/app/auth/dto/response/AuthResponse.java`
- `BE/src/main/java/app/auth/dto/response/UserResponse.java`
- Feature 7 tests under `BE/src/test/java/app/notification`, `app/review`, `app/gamification`, `app/payment`, `app/integration`.

## Project structure and config findings

- Build uses Maven with Spring Boot `3.2.12`, JUnit 5, Mockito, MockMvc, AssertJ, H2 test dependency, and `spring-security-test`.
- No `application-test.properties`, `application-test.yml`, or `application.yml` exists.
- Main DB config is PostgreSQL in `application.properties`; integration tests override datasource inline with H2 memory DB and `ddl-auto=create-drop`.
- `BE/src/main/resources/db/changelog` and `db/migration` exist but contain no migration files found by `rg --files`.
- `scripts/render-postgres-migration/careermate_backup.sql` was referenced by previous repo listing but was not present in `rg --files` during this audit; the migration helper folder contains README and PowerShell scripts.
- `SecurityConfig` requires authentication for `/api/payment/**` and `/api/leaderboard/**`; permits public `GET /api/reviews/company/**`; all other review POST and notification endpoints require authentication via fallback `.anyRequest().authenticated()`.
- JaCoCo was added as Maven profile `coverage` because the plugin is not cached locally and Maven Central access currently fails with a PKIX certificate error.

## Feature 7 Code Audit Table

| Module | Package | Class/File | Method or Endpoint | Actual function | Input | Output | Validation | Exception | Allowed role | Repository/DB | Dependencies | Test status | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Notification | `app.notification.controller` | `NotificationController` | `GET /api/notifications` | Get current user's notifications | JWT | `List<Notification>` | Requires authenticated user via `SecurityUtils` | Runtime if no user in context | Any authenticated user | `notifications` | `SecurityUtils`, `NotificationService` | Existing + updated integration | Sorted by repository method desc createdAt |
| Notification | `app.notification.controller` | `NotificationController` | `PUT /api/notifications/{id}/read` | Mark notification read | Path `id` | `200 OK` | No ownership check in controller/service | None if id missing; silently ignores missing id | Any authenticated user | `notifications` | `NotificationService` | Existing + added ownership-gap integration | Production issue: another user can mark read |
| Notification | `app.notification.controller` | `NotificationController` | `PUT /api/notifications/read-all` | Mark all current user's notifications read | JWT | `200 OK` | Current user resolved | Runtime if unauthenticated | Any authenticated user | `notifications` | `SecurityUtils`, repo update query | Existing | Ownership scoped by user id |
| Notification | `app.notification.controller` | `NotificationController` | `DELETE /api/notifications/{id}` | Delete notification | Path `id`, JWT | `200 OK` | Service checks recipient id | Runtime: not found or no permission | Any authenticated user | `notifications` | `SecurityUtils`, `NotificationService` | Existing | Ownership enforced for delete |
| Notification | `app.notification.service` | `NotificationService` | `sendNotification` | Persist and WebSocket-send notification | recipientId, title, message, link | none | Recipient must exist | Runtime `User not found` | Service internal | `users`, `notifications` | `SimpMessagingTemplate` | Existing | Uses `REQUIRES_NEW` |
| Notification | `app.notification.service` | `NotificationCleanupService` | `cleanupOldNotifications` | Delete notifications older than 30 days | Scheduled cron | none | None | Catches repository errors | Scheduled internal | `notifications` | `NotificationRepository` | Existing | Error is logged, not thrown |
| Review | `app.review.controller` | `ReviewController` | `POST /api/reviews` | Create company review for current user | JWT, `ReviewRequest` | `ReviewResponse` | No DTO bean validation; principal cast to `UserPrincipal` | Runtime via service/global handler | Any authenticated user | `company_reviews`, `users`, `companies`, `notifications` | `ReviewService`, security context | Existing + updated integration | No role restriction to Candidate only |
| Review | `app.review.controller` | `ReviewController` | `GET /api/reviews/company/{companyId}` | List company reviews | companyId | `List<ReviewResponse>` | None | none | Public | `company_reviews` | `ReviewService` | Existing | Public endpoint |
| Review | `app.review.controller` | `ReviewController` | `GET /api/reviews/company/{companyId}/average` | Average rating | companyId | `Double` | None | none | Public | `company_reviews` | `ReviewService` | Existing | Returns `0.0` if no review |
| Review | `app.review.service` | `ReviewService` | `addReview` | Validate user/company, prevent duplicate, save review, notify recruiter | userId, request | `ReviewResponse` | User exists, company exists, duplicate by repo | Runtime for missing/duplicate | Internal | `users`, `companies`, `company_reviews`, `notifications` | `NotificationService` | Expanded | Rating range not validated |
| Gamification | `app.gamification.controller` | `LeaderboardController` | `GET /api/leaderboard` | Read top rankings | role, period, periodKey, limit | `{success,data}` | Authenticated by security | service exceptions | Any authenticated user | `leaderboard_scores`, `users` | `LeaderboardService` | Existing integration | General docs call this Gamification |
| Gamification | `app.gamification.controller` | `LeaderboardController` | `GET /api/leaderboard/me` | Read one user's rank | userId, role, period, periodKey | `{success,data}` | Authenticated; no ownership check | Returns empty map when no rank | Any authenticated user | `leaderboard_scores`, `users` | `LeaderboardService` | Existing integration | userId is client supplied |
| Gamification | `app.gamification.controller` | `LeaderboardController` | `GET /api/leaderboard/missions` | Read role missions/progress | role, optional userId | `{success,data}` | Authenticated; no ownership check for userId | none | Any authenticated user | `leaderboard_points_log` | `LeaderboardService` | Existing integration | Role normalized in service |
| Gamification | `app.gamification.controller` | `LeaderboardController` | `GET /api/leaderboard/logs` | Recent point logs | limit | `{success,data}` | Authenticated only, not admin-only | none | Any authenticated user | `leaderboard_points_log`, `users` | `LeaderboardService` | Existing integration | Comment says admin optional, security allows all auth |
| Gamification | `app.gamification.service` | `LeaderboardService` | `handlePointEvent` | Add points from `PointEvent` | event | DB log and scores | Role normalized; daily limit; duplicate refId | Catches exception, but tx can mark rollback-only | Internal event listener | `leaderboard_points_log`, `leaderboard_scores` | Spring events | Unit existing; integration disabled | H2 cannot execute PostgreSQL `ON CONFLICT` native query |
| Gamification | `app.gamification.repository` | `LeaderboardScoreRepository` | `upsertScore` | Native score upsert | user/role/period/points | DB write | Unique DB constraint | SQL exception | Internal | `leaderboard_scores` | PostgreSQL syntax | Unit mocked; integration disabled | Not portable to H2 current config |
| Payment | `app.payment.controller` | `PaymentController` | `POST /api/payment/vip-upgrade` | Upgrade/extend VIP by role | JWT | `AuthResponse` | Admin blocked; role must map to `_VIP` when non-VIP | Runtime user missing; bad request invalid role | Any authenticated non-admin role | `users` | `JwtTokenProvider`, `UserRepository` | Existing + updated integration | No payment transaction/entity exists |
| Payment | `app.auth.security` | `CustomUserDetailsService` | `loadUserByUsername` | Revert expired VIP role on login/load | email | `UserPrincipal` | Expired `vipExpirationDate` | Username not found, disabled if banned | Auth flow | `users` | `UserRepository` | Covered under auth tests, dependency noted | Feature 7 dependency, not in payment package |

## Production issues found during audit

1. `NotificationService.markAsRead(Long notificationId)` does not verify recipient ownership.
   - File: `BE/src/main/java/app/notification/service/NotificationService.java`
   - Method: `markAsRead`
   - Cause: accepts only notification id and saves read state for any found notification.
   - Test case: `IT-F7-NOT-003` / `NotificationIntegrationTest.markAsRead_notificationBelongsToAnotherUser_currentImplementationStillMarksAsRead`
   - Suggested fix: change service signature to include current user id, query by id and recipient id or compare recipient before save; update controller to pass `SecurityUtils.getCurrentUserId()`.

2. Review rating has no server-side validation.
   - File: `BE/src/main/java/app/review/dto/ReviewRequest.java`, `BE/src/main/java/app/review/service/ReviewService.java`
   - Method: `addReview`
   - Cause: no `@Min(1)`, `@Max(5)`, `@Valid`, or service guard.
   - Test case status: Not implemented as failing test; documented as cannot assert expected 400 because production does not support it.
   - Suggested fix: add bean validation to DTO and `@Valid` in controller or explicit service validation.

3. Duplicate review returns HTTP 500 instead of client error.
   - File: `BE/src/main/java/app/review/service/ReviewService.java`, `BE/src/main/java/app/exception/GlobalExceptionHandler.java`
   - Method: `addReview`
   - Cause: duplicate path throws generic `RuntimeException`; global fallback maps to 500.
   - Test case: `IT-F7-REV-003`
   - Suggested fix: introduce domain exception mapped to `409 Conflict` or `400 Bad Request`.

4. Gamification event integration cannot run on current H2 setup.
   - File: `BE/src/main/java/app/gamification/repository/LeaderboardScoreRepository.java`
   - Method: `upsertScore`
   - Cause: native PostgreSQL `ON CONFLICT` query fails in H2 2.2.224 despite PostgreSQL mode.
   - Test case: `IT-F7-GAM-002` disabled with reason.
   - Suggested fix: use Testcontainers PostgreSQL for this integration, add an H2-specific test repository path, or replace native upsert with database-portable logic.

5. Payment response omits `vipExpirationDate`.
   - File: `BE/src/main/java/app/payment/controller/PaymentController.java`
   - Method: `upgradeToVip`
   - Cause: `UserResponse` has `vipExpirationDate`, but builder line is commented out.
   - Test case status: DB persistence is tested; response field omission recorded as production issue.
   - Suggested fix: set `.vipExpirationDate(user.getVipExpirationDate())` in response.
