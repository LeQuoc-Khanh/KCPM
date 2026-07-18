# Feature 7 Test Scenarios

This scenario list is based only on code that exists in the project. General documentation uses "Gamification" and "Payment"; code references keep actual names such as `LeaderboardController`, `LeaderboardService`, and `/api/payment/vip-upgrade`.

## Summary count

| Module | Unit Test | Integration Test | Total |
|---|---:|---:|---:|
| Notification | 18 | 3 | 21 |
| Review | 13 | 3 | 16 |
| Gamification | 24 | 2 | 26 |
| Payment | 7 | 4 | 11 |
| Cross-module | 0 | 1 | 1 |
| Total unique Feature 7 test methods | 62 | 12 | 74 |

Notes:

- `IT-F7-CROSS-001` is the same physical test method as the main review creation integration test; it is called out separately because it verifies Review -> Notification integration.
- Feature 7 active tests: 73. Disabled/cannot-test scenarios: 1 (`IT-F7-GAM-002`).
- Whole Maven suite after changes: 144 tests, 0 failures, 0 errors, 1 skipped.

## Unit test cases

| ID | Module | Level | Class/endpoint | Objective | Pre-condition | Test data | Steps | Expected result | Real dependency | Mocked dependency | Branch covered | Type | Priority | Status | Reason |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| UT-F7-NOT-001 | Notification | Unit | `NotificationService.getMyNotifications` | Return notifications for user | Repository returns list | userId=1 | Call service | Same list returned | none | `NotificationRepository` | happy path | Positive | P1 | Existing | Valid service unit |
| UT-F7-NOT-002 | Notification | Unit | `NotificationService.markAsRead` | Mark existing notification read | Notification exists | id=10 | Call service | `isRead=true`, saved | none | repository | found branch | Positive | P1 | Existing | Valid service unit |
| UT-F7-NOT-003 | Notification | Unit | `NotificationService.markAsRead` | Missing notification no-op | Repository empty | id=999 | Call service | no save | none | repository | not found branch | Error handling | P2 | Existing | Mirrors code behavior |
| UT-F7-NOT-004 | Notification | Unit | `NotificationService.markAllAsRead` | Delegates bulk update | userId=1 | userId | Call service | repo method called | none | repository | delegation | Positive | P2 | Existing | Valid |
| UT-F7-NOT-005 | Notification | Unit | `NotificationService.deleteNotification` | Delete own notification | Recipient id matches | notificationId=10 | Call service | repo delete called | none | repository/user mock | ownership success | Authorization | P1 | Existing | Valid |
| UT-F7-NOT-006 | Notification | Unit | `NotificationService.deleteNotification` | Missing notification throws | Repository empty | id=999 | Call service | `RuntimeException("Notification not found")` | none | repository | not found | Error handling | P1 | Existing | Valid |
| UT-F7-NOT-007 | Notification | Unit | `NotificationService.deleteNotification` | Other user delete blocked | Recipient id differs | owner=2,current=1 | Call service | exception, no delete | none | repository/user mock | ownership failure | Authorization | P1 | Existing | Valid |
| UT-F7-NOT-008 | Notification | Unit | `NotificationService.sendNotification` | Save and send websocket | Recipient exists | title/message/link | Call service | notification saved and sent | none | user repo, notification repo, messaging | success | Positive | P1 | Existing | Valid |
| UT-F7-NOT-009 | Notification | Unit | `NotificationService.sendNotification` | Recipient missing throws | User repo empty | recipient=999 | Call service | exception, no save/send | none | deps mocked | missing recipient | Error handling | P1 | Existing | Valid |
| UT-F7-NOT-010 | Notification | Unit | `NotificationController.getMyNotifications` | Controller uses current user id | SecurityUtils returns id | id=1 | Call method | 200 + list | none | service, security utils | controller path | Positive | P2 | Existing | Valid |
| UT-F7-NOT-011 | Notification | Unit | `NotificationController.markAsRead` | Delegates mark read | id=10 | id | Call method | 200, service called | none | service | controller path | Positive | P2 | Existing | Ownership not checked by code |
| UT-F7-NOT-012 | Notification | Unit | `NotificationController.markAllAsRead` | Uses current user | id=1 | id | Call method | 200, service called | none | service, security utils | controller path | Positive | P2 | Existing | Valid |
| UT-F7-NOT-013 | Notification | Unit | `NotificationController.deleteNotification` | Delete delegates with user id | id=10,user=1 | id | Call method | 200, service called | none | service, security utils | controller path | Positive | P2 | Existing | Valid |
| UT-F7-NOT-014 | Notification | Unit | `NotificationCleanupService.cleanupOldNotifications` | Deletes older than 30 days | none | now-30d | Call method | repo delete called with cutoff | none | repository | success | Positive | P3 | Existing | Valid |
| UT-F7-NOT-015 | Notification | Unit | `NotificationCleanupService.cleanupOldNotifications` | Repository error swallowed | repo throws | exception | Call method | no throw | none | repository | catch branch | Error handling | P3 | Existing | Valid |
| UT-F7-NOT-016 | Notification | Unit | `Notification` | Default read false | builder | notification | Build | `isRead=false` | none | none | builder default | Positive | P3 | Existing | Model test |
| UT-F7-NOT-017 | Notification | Unit | `Notification` | Default createdAt | builder | notification | Build | createdAt not null | none | none | builder default | Positive | P3 | Existing | Model test |
| UT-F7-NOT-018 | Notification | Unit | `Notification.onCreate` | PrePersist fills createdAt | createdAt null | notification | Call `onCreate` | createdAt set | none | none | lifecycle branch | Positive | P3 | Existing | Model test |
| UT-F7-REV-001 | Review | Unit | `ReviewService.addReview` | User missing | no user | user=999 | Call service | `User not found`, no save | none | repos | missing user | Error handling | P1 | Existing | Valid |
| UT-F7-REV-002 | Review | Unit | `ReviewService.addReview` | Company missing | user exists, company missing | company=999 | Call service | `Company not found`, no save | none | repos | missing company | Error handling | P1 | Existing | Valid |
| UT-F7-REV-003 | Review | Unit | `ReviewService.addReview` | Duplicate review blocked | existsBy true | user/company | Call service | exception, no save/notification | none | repos, notification | duplicate branch | Negative | P1 | Existing | Valid |
| UT-F7-REV-004 | Review | Unit | `ReviewService.addReview` | Save review and notify recruiter | user, company, recruiter exist | rating=5 | Call service | response mapped, save, notification | none | repos, notification | success with recruiter | Positive | P1 | Created | Added in this work |
| UT-F7-REV-005 | Review | Unit | `ReviewService.addReview` | Save review without recruiter notification | company recruiter null | rating=4 | Call service | saved, no notification | none | repos, notification | success no recruiter | Positive | P2 | Created | Added in this work |
| UT-F7-REV-006 | Review | Unit | `ReviewService.getReviewsByCompany` | Map entity to response | repo returns review | companyId=100 | Call service | DTO fields mapped | none | repo | mapping | Positive | P2 | Created | Added in this work |
| UT-F7-REV-007 | Review | Unit | `ReviewService.getAverageRating` | No reviews returns 0 | repo null | companyId | Call service | `0.0` | none | repo | null avg | Boundary | P2 | Existing | Valid |
| UT-F7-REV-008 | Review | Unit | `ReviewService.getAverageRating` | Round average | repo 4.26 | companyId | Call service | `4.3` | none | repo | rounding | Boundary | P2 | Existing | Valid |
| UT-F7-REV-009 | Review | Unit | `ReviewController.createReview` | Uses authenticated principal id | security context set | user=1 | Call controller | 200 + body | security context | service | controller success | Positive | P2 | Existing | Valid |
| UT-F7-REV-010 | Review | Unit | `ReviewController.getCompanyReviews` | Delegates list | companyId | companyId | Call controller | 200 + list | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-REV-011 | Review | Unit | `ReviewController.getAverageRating` | Delegates average | companyId | companyId | Call controller | 200 + double | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-REV-012 | Review | Unit | `ReviewRequest` | DTO getters/setters | none | fields | Set/get | values equal | none | none | DTO | Positive | P3 | Existing | Simple DTO |
| UT-F7-REV-013 | Review | Unit | `ReviewResponse` | Builder fields | none | fields | Build | values equal | none | none | DTO | Positive | P3 | Existing | Simple DTO |
| UT-F7-GAM-001 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Valid event saves log and updates all periods | under limit, no duplicate | APPLY | Call service | log saved, 4 upserts | none | repos | success | Positive/Transaction | P1 | Existing | Valid |
| UT-F7-GAM-002 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Admin ignored | role ADMIN | LOGIN | Call | no repo interactions | none | repos | admin return | Negative | P1 | Existing | Valid |
| UT-F7-GAM-003 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Unknown role ignored | role null | LOGIN | Call | no repo interactions | none | repos | unknown return | Negative | P1 | Existing | Valid |
| UT-F7-GAM-004 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Daily limit reached | count == limit | LOGIN | Call | no save/upsert | none | repos | limit branch | Boundary | P1 | Existing | Valid |
| UT-F7-GAM-005 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Duplicate ref ignored | ref exists | APPLY ref | Call | no save/upsert | none | repos | duplicate branch | Negative | P1 | Existing | Valid |
| UT-F7-GAM-006 | Gamification | Unit | `LeaderboardService.handlePointEvent` | Repo exception swallowed | repo throws | APPLY | Call | no throw | none | repos | catch branch | Error handling | P2 | Existing | Valid |
| UT-F7-GAM-007 | Gamification | Unit | `LeaderboardService.getTopUsers` | Normalize role and key | role recruiter | period month | Call | repo called with normalized values | none | score repo | normalization | Positive | P2 | Existing | Valid |
| UT-F7-GAM-008 | Gamification | Unit | `LeaderboardService.getTopUsers` | Resolve ALL_TIME key | null key | all_time | Call | key `ALL` | none | score repo | period default | Boundary | P2 | Existing | Valid |
| UT-F7-GAM-009 | Gamification | Unit | `LeaderboardService.getMyRank` | Normalize role and period | role candidate | week | Call | repo called | none | score repo | normalization | Positive | P2 | Existing | Valid |
| UT-F7-GAM-010 | Gamification | Unit | `LeaderboardService.getSystemLogs` | Return logs | repo list | limit=5 | Call | same list | none | log repo | delegation | Positive | P3 | Existing | Valid |
| UT-F7-GAM-011 | Gamification | Unit | `LeaderboardService.getMissions` | Candidate missions with progress | user id | count=1 | Call | 4 candidate missions | none | log repo | candidate branch | Positive | P2 | Existing | Valid |
| UT-F7-GAM-012 | Gamification | Unit | `LeaderboardService.getMissions` | Recruiter missions with progress | user id | count=0 | Call | 4 recruiter missions | none | log repo | recruiter branch | Positive | P2 | Existing | Valid |
| UT-F7-GAM-013 | Gamification | Unit | `LeaderboardService.getMissions` | Null user avoids progress query | user null | role candidate | Call | no count query | none | log repo | null user branch | Boundary | P3 | Existing | Valid |
| UT-F7-GAM-014 | Gamification | Unit | `LeaderboardController.getTop` | Returns success wrapper | rankings mocked | params | Call | 200 + data | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-GAM-015 | Gamification | Unit | `LeaderboardController.getMe` | Rank exists | rank mocked | params | Call | 200 + rank | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-GAM-016 | Gamification | Unit | `LeaderboardController.getMe` | Rank missing | service null | params | Call | 200 + empty map | none | service | null branch | Boundary | P2 | Existing | Valid |
| UT-F7-GAM-017 | Gamification | Unit | `LeaderboardController.getMissions` | Mission wrapper | missions mocked | params | Call | 200 + missions | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-GAM-018 | Gamification | Unit | `LeaderboardController.getLogs` | Logs wrapper | logs mocked | limit | Call | 200 + logs | none | service | controller success | Positive | P3 | Existing | Valid |
| UT-F7-GAM-019 to 024 | Gamification | Unit | `PointEvent`, `UserPointAction`, `LeaderboardPointsLog`, `LeaderboardScore`, `AddPointEventRequest` | Model/enum constructor, getter, default score checks | none | model data | Instantiate/set/get | expected values | none | none | model branches | Positive | P3 | Existing | Six simple model tests |
| UT-F7-PAY-001 | Payment | Unit | `PaymentController.upgradeToVip` | Admin rejected | auth admin | ADMIN | Call | 400, no save/token | security context | user repo, jwt | admin branch | Authorization | P1 | Existing | Valid |
| UT-F7-PAY-002 | Payment | Unit | `PaymentController.upgradeToVip` | Candidate -> Candidate VIP | auth candidate | CANDIDATE | Call | role set, expiration, tokens | security context | user repo, jwt | upgrade branch | Positive | P1 | Existing | Valid |
| UT-F7-PAY-003 | Payment | Unit | `PaymentController.upgradeToVip` | Recruiter -> Recruiter VIP | auth recruiter | RECRUITER | Call | role set, expiration, tokens | security context | user repo, jwt | upgrade branch | Positive | P1 | Existing | Valid |
| UT-F7-PAY-004 | Payment | Unit | `PaymentController.upgradeToVip` | Active Candidate VIP extension | expiration future | CANDIDATE_VIP | Call | expiration +30 days | security context | user repo, jwt | vip active branch | Boundary | P1 | Existing | Valid |
| UT-F7-PAY-005 | Payment | Unit | `PaymentController.upgradeToVip` | Active Recruiter VIP extension | expiration future | RECRUITER_VIP | Call | expiration +30 days | security context | user repo, jwt | vip active branch | Boundary | P1 | Existing | Valid |
| UT-F7-PAY-006 | Payment | Unit | `PaymentController.upgradeToVip` | Expired VIP renews from now | expiration past | CANDIDATE_VIP | Call | expiration around now+30 | security context | user repo, jwt | expired branch | Boundary | P1 | Existing | Valid |
| UT-F7-PAY-007 | Payment | Unit | `PaymentController.upgradeToVip` | User missing throws | repo empty | email | Call | `User not found` | security context | user repo | missing user | Error handling | P1 | Existing | Valid |

## Integration test cases

| ID | Module | Level | Class/endpoint | Objective | Pre-condition | Test data | Steps | Expected result | Real dependency | Mocked dependency | Branch covered | Type | Priority | Status | Reason |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| IT-F7-NOT-001 | Notification | Integration | `GET/PUT/DELETE /api/notifications` | Read/update/read-all/delete persisted notifications | Users and notifications in H2 | candidate, other user | Call APIs with JWT | only current user's list; read states updated; delete removes row | Spring Security, MockMvc, repositories, H2 | none | controller-service-repository | Positive/Transaction | P1 | Existing | Valid |
| IT-F7-NOT-002 | Notification | Integration | `GET /api/notifications` | Unauthenticated blocked | no token | none | Call endpoint | 401 | Security filter chain | none | auth failure | Security | P1 | Created | Added in this work |
| IT-F7-NOT-003 | Notification | Integration | `PUT /api/notifications/{id}/read` | Detect current ownership gap | User A and User B notification | other notification id | User A marks User B notification | 200 and DB read=true | Security, service, repository, H2 | none | current vulnerable path | Authorization | P0 | Created | Passes to document production issue |
| IT-F7-REV-001 | Review | Integration | `POST /api/reviews`, `GET /api/reviews/company`, `GET /average` | Create review, read it, average, notify recruiter | Candidate, recruiter, company | rating=5 | Candidate posts review, public reads, check DB notifications | review saved, average 5.0, notification persisted | Security, MockMvc, services, repos, H2 | none | success path | Positive/Cross-module | P1 | Existing | Valid |
| IT-F7-REV-002 | Review | Integration | `POST /api/reviews` | Unauthenticated create blocked | no token | body | Call POST | 401 | Security filter chain | none | auth failure | Security | P1 | Created | Added in this work |
| IT-F7-REV-003 | Review | Integration | `POST /api/reviews` | Duplicate review keeps one row | Candidate already reviewed company | same body twice | POST twice | first 200, second 500, DB one review | Security, service, unique check, H2 | none | duplicate branch | Negative/Transaction | P1 | Created | Reveals status mapping issue |
| IT-F7-GAM-001 | Gamification | Integration | `GET /api/leaderboard`, `/me`, `/missions`, `/logs` | Read persisted rankings, rank, missions, logs | Scores/log inserted in H2 | 2 users | Call endpoints | expected ranks/logs/mission progress | Security, MockMvc, repos, H2 | none | read APIs | Positive | P1 | Existing | Valid |
| IT-F7-GAM-002 | Gamification | Integration | `PointEvent -> LeaderboardService -> repositories` | Verify event creates log and score rows | Candidate user | APPLY event | Publish event, inspect DB | Should create 1 log + 4 scores | Spring event, service, repos, H2 | none | write transaction | Transaction | P1 | Cannot test | Disabled because H2 fails PostgreSQL `ON CONFLICT` native query |
| IT-F7-PAY-001 | Payment | Integration | `POST /api/payment/vip-upgrade` | Candidate upgrade persists role/expiration | Candidate in H2 | CANDIDATE | POST with JWT | 200, tokens, DB CANDIDATE_VIP, expiration ~30 days | Security, controller, repository, JWT, H2 | none | upgrade branch | Positive/Transaction | P1 | Existing | Valid |
| IT-F7-PAY-002 | Payment | Integration | `POST /api/payment/vip-upgrade` | Unauthenticated blocked | no token | none | POST | 401 | Security filter chain | none | auth failure | Security | P1 | Created | Added in this work |
| IT-F7-PAY-003 | Payment | Integration | `POST /api/payment/vip-upgrade` | Admin rejected and unchanged | Admin in H2 | ADMIN | POST with JWT | 400, DB role unchanged, no expiration | Security, controller, repository, H2 | none | admin branch | Authorization | P1 | Created | Added in this work |
| IT-F7-PAY-004 | Payment | Integration | `POST /api/payment/vip-upgrade` | Recruiter upgrade persists role/expiration | Recruiter in H2 | RECRUITER | POST with JWT | 200, DB RECRUITER_VIP | Security, controller, repository, H2 | none | recruiter branch | Positive/Transaction | P1 | Created | Added in this work |
| IT-F7-CROSS-001 | Cross-module | Integration | Review -> Notification | Review triggers recruiter notification | Candidate, recruiter-owned company | review payload | POST review | review row and notification row created | ReviewService, NotificationService, repositories | none | cross-module notification | Transaction/Cross-module | P1 | Existing | Same physical test as IT-F7-REV-001 |

## Cannot-test or not-implemented scenarios

| Scenario | Reason |
|---|---|
| Payment creation/confirmation/failure/transaction ID/idempotency | No payment entity, repository, external gateway, transaction id, or failure endpoint exists. |
| Review update/delete/ownership | No update/delete review endpoint exists. |
| Review rating validation expected 400 | No validation exists in DTO/controller/service. |
| Notification pagination | Repository returns full list; no pageable endpoint exists. |
| Gamification add-point API | `AddPointEventRequest` is empty and there is no controller endpoint for adding points. |
| Gamification event integration on H2 | `LeaderboardScoreRepository.upsertScore` uses PostgreSQL native `ON CONFLICT`, not accepted by current H2 setup. |
| Leaderboard log admin-only authorization | Code allows any authenticated user for `/api/leaderboard/logs`; no admin-only mapping exists. |
| Payment rollback on DB/payment failure | No service transaction boundary or payment persistence exists; controller directly updates user. |
