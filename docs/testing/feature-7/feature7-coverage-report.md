# Feature 7 Coverage Report

## Execution results

Commands run:

| Command | Result | Notes |
|---|---|---|
| `mvn test` | Pass | 144 tests, 0 failures, 0 errors, 1 skipped. |
| `mvn clean test` | Blocked | Maven clean cannot delete generated files under `BE/target`; Windows/filesystem returns access denied. |
| `mvn verify` | Tests pass, build fails in repackage | Surefire: 144 tests, 0 failures, 0 errors, 1 skipped. Spring Boot repackage cannot rename existing jar to `.jar.original`. |
| `mvn verify "-Dspring-boot.repackage.skip=true"` | Pass | Verify lifecycle through tests/package with repackage skipped. |
| `mvn verify -Pcoverage "-Dspring-boot.repackage.skip=true" -U` | Blocked | JaCoCo plugin download fails with PKIX certificate error from Maven Central. |

Latest Surefire totals:

| Tests run | Failures | Errors | Skipped |
|---:|---:|---:|---:|
| 144 | 0 | 0 | 1 |

Feature 7 method totals:

| Module | Unit tests | Integration tests | Skipped/Cannot-test |
|---|---:|---:|---:|
| Notification | 18 | 3 | 0 |
| Review | 13 | 3 | 0 |
| Gamification | 24 | 2 | 1 |
| Payment | 7 | 4 | 0 |
| Cross-module | 0 | 1 | 0 |

## JaCoCo status

JaCoCo was not present in `pom.xml`; a minimal Maven profile was added:

```bash
mvn verify -Pcoverage "-Dspring-boot.repackage.skip=true"
```

Coverage report generation is currently blocked because the machine cannot resolve:

```text
org.jacoco:jacoco-maven-plugin:0.8.12
PKIX path building failed: unable to find valid certification path to requested target
```

No `target/site/jacoco/index.html` could be generated. Therefore exact line, branch, method and class coverage percentages are not available and are not invented in this report.

## Coverage by code responsibility, based on test mapping

| Module | Covered code | Not covered / partially covered | Reason |
|---|---|---|---|
| Notification | `NotificationController` all methods; `NotificationService` read, mark read, read-all, delete, send; cleanup success/error; model defaults; integration DB read/update/delete and unauthenticated GET. | No pagination; no ownership enforcement for `markAsRead`. | Pagination does not exist. Ownership check missing in production. |
| Review | `ReviewController` all endpoints; `ReviewService` missing user/company, duplicate, success notify/no notify, list mapping, average null/rounding; integration create/read/average/notify, duplicate, unauthenticated POST. | Rating range validation; role restriction to Candidate; update/delete review. | These behaviors do not exist in code. |
| Gamification | `LeaderboardController` all read endpoints; `LeaderboardService` point processing unit branches, read methods, missions; models; read-side integration. | Active event-to-DB integration for score upsert. | H2 cannot run PostgreSQL native `ON CONFLICT` query. |
| Payment | `PaymentController` admin, candidate, recruiter, active VIP extension, expired VIP renewal, missing user; integration unauthenticated, admin rejected, candidate/recruiter DB upgrade. | Payment transaction persistence/failure/idempotency; response `vipExpirationDate`. | No payment entity/repository/failure flow. Response field exists but controller omits setting it. |

## Recommended additional coverage after blockers

1. Add Testcontainers PostgreSQL and enable `IT-F7-GAM-002` to cover `LeaderboardScoreRepository.upsertScore` with real PostgreSQL syntax.
2. After fixing `NotificationService.markAsRead`, add an integration test expecting 403/500 mapped access-denied for another user's notification.
3. After adding review rating validation, add negative tests for rating `<1`, `>5`, and null rating.
4. After setting `vipExpirationDate` in `PaymentController` response, assert response JSON includes it.
5. If a payment entity/gateway is introduced, add transaction rollback and idempotency tests.
