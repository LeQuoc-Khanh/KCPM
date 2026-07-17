# Feature 2 - Candidate Profile & CV Final Testing Report & Evidence

**Project:** CareerMate  
**Feature:** Feature 2 - Candidate Profile & CV  
**Tester:** Phan Khanh Du  
**Test date:** 17/07/2026 - 18/07/2026  
**Backend:** Spring Boot 3.2.12  
**Unit test framework:** JUnit 5, Mockito, Maven Surefire  
**Coverage tool:** JaCoCo Maven Plugin 0.8.12  
**API test tool:** Postman  
**Postman environment:** KCPM  

---

## 1. Objective

Hoan thien tai lieu kiem thu Feature 2 - Candidate Profile & CV dua tren ket qua da chay thuc te:

- Code Audit cho Candidate Profile & CV.
- Unit Test white-box cho service, controller va model lien quan.
- Regression Test bang Maven.
- Coverage Report bang JaCoCo.
- Integration Test tren Postman.
- Black-box BVA tren Postman.
- Bug List va risk con ton tai.

---

## 2. Code Audit

| Module | Class | Method/Endpoint | Input | Output | Validation/Exception | Role | Dependency |
|---|---|---|---|---|---|---|---|
| Candidate Profile | `CandidateService` | `getProfileDTO()` | Current user email | Candidate profile DTO | Throw exception neu profile khong ton tai | Candidate | `CandidateProfileRepository` |
| Candidate Profile | `CandidateService` | `updateProfile()` | Profile update request | Updated profile | Chua thay validation chat che cho field rong/dai | Candidate | `UserRepository`, `ObjectMapper`, `CVAnalysisResultRepository` |
| CV Upload | `CandidateService` | `uploadAndAnalyzeCV()` | Multipart CV file | Updated profile sau khi AI phan tich CV | Phu thuoc AI key va Cloudinary | Candidate | `CloudinaryService`, `CVAnalysisService`, `ApplicationEventPublisher` |
| Avatar | `CandidateService` | `uploadAvatar()` | Multipart image file | Avatar URL | Chua dam bao validate file khong phai anh o service | Candidate | `CloudinaryService`, `UserRepository` |
| Profile API | `CandidateProfileController` | `GET /api/candidate/profile/me` | Bearer token | Current candidate profile | Protected API | Candidate | `CandidateService`, `UserRepository` |
| Profile API | `CandidateProfileController` | `PUT /api/candidate/profile/me` | Profile JSON | Updated profile | Service error tra 400 | Candidate | `CandidateService` |
| CV Upload API | `CandidateProfileController` | `POST /api/candidate/profile/upload-cv` | Multipart CV file | CV analysis/profile update | AI/config error co the lam upload fail | Candidate | `CandidateService` |
| Avatar API | `CandidateProfileController` | `POST /api/candidate/profile/avatar` | Multipart file | Avatar URL | Can validate file type | Candidate | `CandidateService` |
| CV Builder API | `CandidateCVController` | `POST /api/candidate/cv-builder/save` | CV Builder JSON | Saved CV | Payload rong co risk van duoc luu | Candidate | `CandidateCVRepository`, `UserRepository`, `SecurityUtils` |
| CV Builder API | `CandidateCVController` | `GET /api/candidate/cv-builder/my-cvs` | Bearer token | List CV cua current user | Protected API | Candidate | `CandidateCVRepository` |
| CV Builder API | `CandidateCVController` | `GET /api/candidate/cv-builder/{id}` | CV id | CV detail | Can kiem tra ownership | Candidate | `CandidateCVRepository` |
| Model | `CandidateCV` | `@PrePersist`, `@PreUpdate` | Entity lifecycle | Timestamp | Can verify timestamp | N/A | JPA |

---

## 3. Test Condition List

| ID | Area | Test condition |
|---|---|---|
| TC-F2-01 | Profile | Candidate xem profile hien tai |
| TC-F2-02 | Profile | Candidate cap nhat profile |
| TC-F2-03 | CV Upload | Upload CV DOCX hop le |
| TC-F2-04 | CV Upload | Upload CV PDF hop le |
| TC-F2-05 | CV Upload | Upload CV sai dinh dang |
| TC-F2-06 | Avatar | Upload avatar hop le/khong hop le |
| TC-F2-07 | CV Builder | Luu CV Builder |
| TC-F2-08 | CV Builder | Xem danh sach CV da luu |
| TC-F2-09 | CV Builder | Xem chi tiet CV |
| TC-F2-10 | Security | Protected API reject token thieu/sai |
| TC-F2-11 | CV Builder | Reject payload khong hop le |
| TC-F2-12 | BVA | Kiem tra bien rong, toi thieu, qua dai, sai file type |

---

## 4. Unit Test Case List

| Test group | File | Tests |
|---|---|---:|
| Candidate service | `BE/src/test/java/app/candidate/service/Feature2CandidateServiceTest.java` | 9 |
| Candidate profile controller | `BE/src/test/java/app/candidate/controller/Feature2CandidateProfileControllerTest.java` | 6 |
| Candidate CV controller | `BE/src/test/java/app/candidate/controller/Feature2CandidateCVControllerTest.java` | 6 |
| Candidate model | `BE/src/test/java/app/candidate/model/Feature2CandidateModelTest.java` | 3 |
| **Total** |  | **24** |

---

## 5. Unit Test Result

Command executed:

```powershell
cd C:\Users\ADMIN\OneDrive\Documents\GitHub\KCPM\BE
mvn "-Dtest=Feature2CandidateServiceTest,Feature2CandidateProfileControllerTest,Feature2CandidateCVControllerTest,Feature2CandidateModelTest" test
```

Actual result:

| Scope | Tests run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| Feature 2 Unit Tests | 24 | 0 | 0 | 0 | BUILD SUCCESS |

Evidence:

```text
BE/target/surefire-reports/app.candidate.service.Feature2CandidateServiceTest.txt
BE/target/surefire-reports/app.candidate.controller.Feature2CandidateProfileControllerTest.txt
BE/target/surefire-reports/app.candidate.controller.Feature2CandidateCVControllerTest.txt
BE/target/surefire-reports/app.candidate.model.Feature2CandidateModelTest.txt
```

---

## 6. Regression Test Result

Command executed:

```powershell
mvn test
```

Actual result:

| Scope | Tests run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| All backend tests after adding Feature 2 tests | 63 | 0 | 0 | 0 | BUILD SUCCESS |

Conclusion:

- Baseline before adding Feature 2 unit tests: 39 tests passed.
- After adding Feature 2 unit tests: 63 tests passed.
- Additional Feature 2 unit tests: 24.
- No regression failure was introduced.

---

## 7. Coverage Report

Command executed:

```powershell
mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report
```

Actual result:

| Scope | Tests run | Failures | Errors | Result |
|---|---:|---:|---:|---|
| JaCoCo with all backend tests | 63 | 0 | 0 | BUILD SUCCESS |

Evidence:

```text
BE/target/site/jacoco/index.html
BE/target/site/jacoco/jacoco.csv
BE/target/site/jacoco/jacoco.xml
```

Coverage from `jacoco.csv`:

| Package | Class | Instruction | Branch | Line | Covered lines | Missed lines |
|---|---|---:|---:|---:|---:|---:|
| `app.candidate.service` | `CandidateService` | 91.53% | 67.65% | 94.78% | 127 | 7 |
| `app.candidate.controller` | `CandidateProfileController` | 77.25% | N/A | 85.71% | 30 | 5 |
| `app.candidate.controller` | `CandidateCVController` | 95.73% | 100.00% | 100.00% | 20 | 0 |
| `app.candidate.model` | `CandidateCV` | 100.00% | N/A | 100.00% | 5 | 0 |

Note:

- `JobRecommendationService` va `RecommendationController` nam trong package `app.candidate` nhung khong thuoc pham vi Candidate Profile & CV cua lan test nay.

---

## 8. Postman Integration Test Result

Collection folder:

```text
[Feature 2] Integration Test for User/Profile Management
```

Postman Runner evidence provided:

- Environment: `KCPM`
- Iterations: `1`
- Duration: `41s 769ms`
- Errors: `0`
- Avg. response time: `3102 ms`
- Runner showed `All tests: 0` because Postman test scripts were not added at that moment.

Actual API status observed from Postman screenshots and responses:

| Test Case ID | Description | Expected Result | Actual Result | Result | Note |
|---|---|---|---|---|---|
| TC_2.1 | View Candidate Profile | 200 OK | 200 OK | Pass | Response received successfully. |
| TC_2.2 | Update Candidate Profile | 200 OK | 200 OK | Pass with note | Screenshot shows request method as GET, so request setup should be reviewed if this case is intended to update profile. |
| TC_2.3 | Upload Valid DOCX CV | 200 OK | 400 Bad Request | Fail | Backend AI processing failed because API key was reported as leaked. |
| TC_2.4 | Upload Valid PDF CV | 200 OK | 400 Bad Request | Fail | Same CV upload flow is blocked by AI/config issue. |
| TC_2.5 | Reject Invalid CV File | 400/415/422 | 400 Bad Request | Pass | Invalid CV file was rejected. |
| TC_2.6 | View_Saved_CV_List | Handled response | Not confirmed | Not confirmed | No clear evidence captured for this case. |
| TC_2.7 | Reject Invalid Avatar File | 400/415/422 | 400 Bad Request | Pass | Invalid avatar file was rejected in latest screenshot. |
| TC_2.8 | Save CV Builder | 200 OK | 200 OK | Pass | CV Builder save request succeeded. |
| TC_2.9 | View Saved CV List | 200 OK | 200 OK | Pass | Saved CV list returned successfully. |
| TC_2.10 | View CV Detail | 200 OK | 200 OK | Pass | CV detail returned for id `6`. |
| TC_2.11 | Protected APIs Reject Invalid/Missing Token | 401/403 | 200 OK | Fail | Request still had valid token or authorization was not disabled. Need retest with No Auth/invalid token. |
| TC_2.12 | Reject Invalid CV Builder Data | 400/422 | 400 Bad Request | Pass | Invalid CV Builder payload was rejected. |
| JIRA | Create Bug from Failed Test | 201 Created | 201 Created | Support request passed | Not counted as Feature 2 test case. |

Integration summary based on confirmed evidence:

| Metric | Count |
|---|---:|
| Confirmed Feature 2 integration cases | 11 |
| Passed | 7 |
| Failed | 3 |
| Not confirmed | 1 |
| Postman runner errors | 0 |

---

## 9. Black-box BVA Test Design And Result

BVA scope created for Feature 2:

```text
[Black-box BVA] Feature 2 Candidate Profile & CV
```

Planned BVA groups:

- Candidate Profile boundary values.
- CV Upload file boundaries.
- Avatar Upload file boundaries.
- CV Builder payload boundaries.

Detailed BVA test set:

| BVA ID | Area | Boundary condition | Expected Result | Actual Result | Result |
|---|---|---|---|---|---|
| BVA-F2-01 | Profile | Minimum valid profile data | 200 OK | Not run/Not provided | Pending |
| BVA-F2-02 | Profile | Empty `fullName` | 400/422 or handled validation | Not run/Not provided | Pending |
| BVA-F2-03 | Profile | Very long `fullName` | Not 500 | Not run/Not provided | Pending |
| BVA-F2-04 | Profile | Empty skills | 200/400/422 | Not run/Not provided | Pending |
| BVA-F2-05 | CV Upload | Valid DOCX file | 200 OK | 400 Bad Request - AI API key reported as leaked | Fail |
| BVA-F2-06 | CV Upload | Missing CV file | 400/422 | Not run/Not provided | Pending |
| BVA-F2-07 | CV Upload | Invalid CV file type | 400/415/422 | Not run/Not provided | Pending |
| BVA-F2-08 | Avatar | Valid image file | 200 OK | Not run/Not provided | Pending |
| BVA-F2-09 | Avatar | Missing avatar file | 400/422 | Not run/Not provided | Pending |
| BVA-F2-10 | Avatar | Invalid avatar file type | 400/415/422 | Not run/Not provided | Pending |
| BVA-F2-11 | CV Builder | Minimum valid CV Builder data | 200 OK | Not run/Not provided | Pending |
| BVA-F2-12 | CV Builder | Empty CV Builder payload | 400/422 | Not run/Not provided | Pending |

BVA-F2-05 actual response:

```json
{
  "success": false,
  "message": "Lỗi xử lý CV: AI Error: Đã thử 3 key khác nhau nhưng vẫn thất bại. Lỗi: 403 Forbidden: Your API key was reported as leaked. Please use another API key.",
  "data": null
}
```

---

## 10. Bug List

| Bug ID | Source | Title | Expected | Actual | Impact | Status |
|---|---|---|---|---|---|---|
| BUG-F2-01 | TC_2.3, TC_2.4, BVA-F2-05 | Upload valid CV fails because AI API key is blocked | Valid DOCX/PDF CV returns 200 OK and is analyzed successfully | 400 Bad Request, AI provider returns 403 `PERMISSION_DENIED`, API key reported as leaked | Candidate cannot upload/analyze valid CV | Open |
| BUG-F2-02 | TC_2.11 | Protected API test returned 200 when invalid/missing token was expected | 401 Unauthorized or 403 Forbidden | 200 OK | Test setup likely still used valid token; retest required with No Auth/invalid token | Retest Required |
| RISK-F2-01 | Code audit + unit test behavior | CV Builder may allow empty payload in code path | Empty/invalid payload should be rejected | Unit test recorded risk in existing behavior | Empty CV data may be saved if backend validation is incomplete | Open/Risk |
| RISK-F2-02 | Code audit | CV detail endpoint should verify CV ownership | User cannot view CV of another candidate | Code audit found ownership check risk | Data privacy risk if ownership is not checked | Open/Risk |

---

## 11. Retest And Regression Plan

Retest required:

| ID | Item | Retest action | Expected retest result |
|---|---|---|---|
| RT-F2-01 | BUG-F2-01 | Replace/reconfigure AI API key in backend environment and rerun TC_2.3, TC_2.4, BVA-F2-05 | Upload valid CV returns 200 OK |
| RT-F2-02 | BUG-F2-02 | Rerun TC_2.11 with `No Auth` or token value `invalid_token` | API returns 401/403 |
| RT-F2-03 | Postman scripts | Add `pm.test(...)` scripts to all Feature 2 requests and rerun folder | Runner shows All tests > 0, Passed/Failed counts visible |
| RT-F2-04 | BVA remaining cases | Run BVA-F2-01 to BVA-F2-12 completely | Full BVA result table can be finalized |

Regression already completed:

| Regression item | Result |
|---|---|
| Full backend Maven test after Feature 2 unit tests | 63 passed, 0 failed |
| JaCoCo test run | 63 passed, 0 failed |

---

## 12. Final Test Summary

| Category | Result |
|---|---:|
| Unit Test implemented for Feature 2 | 24 |
| Unit Test passed | 24 |
| Unit Test failed/errors | 0 |
| Backend regression tests | 63 passed, 0 failed |
| JaCoCo report | Generated |
| Confirmed Postman integration cases | 11 |
| Confirmed Postman integration pass | 7 |
| Confirmed Postman integration fail | 3 |
| Confirmed Postman integration not confirmed | 1 |
| BVA cases designed | 12 |
| BVA cases with confirmed actual result | 1 |
| Open bugs/risks | 4 |

Conclusion:

Feature 2 has completed white-box Unit Test, backend Regression Test, and JaCoCo coverage measurement with successful Maven results. Postman Integration Test has been executed partially with real API responses captured. The main confirmed blocker is the CV upload flow, where valid CV upload fails because the backend AI API key is blocked/reported as leaked. The protected API negative case also requires retest because the request still returned 200 OK, likely due to a valid token still being attached.

The final report should be updated again after:

- AI API key is replaced and CV upload is retested.
- TC_2.11 is rerun with No Auth or invalid token.
- BVA folder is fully executed with Postman test scripts.

