# Feature 7 Existing Tests Review

Review criteria: assertion quality, correct layer, duplication, data isolation, use of Spring context, repository/DB verification, security and ownership coverage.

| Existing test | Evaluation | Keep | Update | Delete as duplicate | Reason |
|---|---|---:|---:|---:|---|
| `NotificationServiceTest` | Good unit coverage of read, read-all, delete ownership, send notification and missing recipient. Has assertions and dependency verification. | Yes | No | No | Correct unit layer with mocked repositories and messaging. |
| `NotificationControllerTest` | Good thin controller unit tests. Does not test security because it is direct method invocation, which is acceptable for unit scope. | Yes | No | No | Complements integration tests. |
| `NotificationCleanupServiceTest` | Useful branch coverage for cleanup success and swallowed repository error. | Yes | No | No | Scheduled method tested as plain service. |
| `NotificationTest` | Simple model defaults. Low value but not duplicate. | Yes | No | No | Covers builder defaults and `onCreate`. |
| `NotificationIntegrationTest` | Good H2/MockMvc test for list/read/read-all/delete with DB assertions. Missing unauthenticated and mark-as-read ownership gap before this work. | Yes | Yes | No | Added `getNotifications_withoutToken_shouldReturnUnauthorized` and ownership-gap detector. |
| `ReviewServiceTest` | Had negative branches and average rounding, but missed successful save/notification and list mapping. | Yes | Yes | No | Added success with recruiter notification, no recruiter branch, and mapping test. |
| `ReviewControllerTest` | Good direct controller tests for principal id, list, and average. | Yes | No | No | Correct unit scope. |
| `ReviewDtoTest` | Simple DTO tests. Low risk but not harmful. | Yes | No | No | No duplicate with service/controller assertions. |
| `ReviewIntegrationTest` | Good cross-module create/read/average/notify test. Missing unauthenticated and duplicate review behavior before this work. | Yes | Yes | No | Added security and duplicate DB assertion. |
| `LeaderboardServiceTest` | Strong unit coverage: success, admin/unknown role, limits, duplicate ref, exception handling, read methods, missions. | Yes | No | No | Correct unit layer with mocked repositories. |
| `LeaderboardControllerTest` | Good direct controller wrapper tests including empty rank map. | Yes | No | No | No Spring context needed. |
| `LeaderboardModelTest` | Simple model/enum tests, includes empty `AddPointEventRequest`. | Yes | No | No | Low value, but not duplicate. |
| `GamificationIntegrationTest` | Good read-side integration for rankings, rank, missions and logs. Missing write-side event integration before this work. | Yes | Yes | No | Added disabled event integration documenting H2/PostgreSQL native query incompatibility. |
| `PaymentControllerTest` | Good unit coverage for admin, candidate/recruiter upgrade, VIP extension, expired VIP renewal, and missing user. | Yes | No | No | Correct unit layer; Payment has no service class. |
| `PaymentIntegrationTest` | Existing candidate upgrade DB check was good. Missing unauthenticated, admin rejected and recruiter upgrade. | Yes | Yes | No | Added those integration paths. |

## Key findings

- No Feature 7 test was deleted.
- Existing Feature 7 tests had real assertions and did not appear to be order-dependent.
- Integration tests use isolated H2 memory databases per class and `@AfterEach` cleanup.
- The main incorrect/insufficient coverage was not fake passing; it was missing security/ownership and happy-path service assertions.
- `NotificationService.markAsRead` ownership cannot be fixed in tests without production change; a passing integration test documents the current vulnerable behavior.
- Gamification write integration cannot be active under current H2 config due PostgreSQL native `ON CONFLICT`.
