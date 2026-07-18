# Assignment: Kiểm thử Feature 2 - Candidate Profile & CV

**Chủ đề:** Code Audit, Unit Test, Regression Test và Coverage thực tế  
**Môn học:** Kiểm chứng phần mềm  
**Dự án áp dụng:** CareerMate  
**Feature:** F2 - Candidate Profile & CV  
**Người phụ trách:** Phan Khánh Du  
**Ngày chạy test:** 17/07/2026  
**Backend:** Spring Boot 3.2.12  
**Java:** 17  
**Test framework:** JUnit 5, Mockito, Maven Surefire  
**Coverage tool:** JaCoCo Maven Plugin 0.8.12  

> Báo cáo này cập nhật đến trước bước Postman Integration Test. Phần Postman chưa chạy đủ evidence nên chưa ghi Pass/Fail.

---

## 1. Test Scope

Feature 2 trong lượt kiểm thử này tập trung vào:

| Module | Class | Trạng thái |
|---|---|---|
| Candidate Profile Service | `CandidateService` | Đã audit, đã viết unit test, đã chạy |
| Candidate Profile API | `CandidateProfileController` | Đã audit, đã viết unit test, đã chạy |
| CV Builder API | `CandidateCVController` | Đã audit, đã viết unit test, đã chạy |
| Candidate Model | `CandidateCV`, `CandidateProfile` | Đã audit, đã viết unit test một phần, đã chạy |
| Integration API/Postman | Feature 2 endpoints | Chưa cập nhật kết quả vào báo cáo này |

---

## 2. Baseline Trước Khi Thêm Unit Test Feature 2

Lệnh đã chạy:

```powershell
cd C:\Users\ADMIN\OneDrive\Documents\GitHub\KCPM\BE
mvn test
```

Kết quả baseline thực tế:

| Scope | Tests run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| Backend trước khi thêm bộ test mới Feature 2 | 39 | 0 | 0 | 0 | BUILD SUCCESS |

**Finished at:** `2026-07-17T22:34:35+07:00`

---

## 3. Code Audit

| Module | Class | Method/Endpoint | Chức năng | Dependency | Rủi ro ghi nhận |
|---|---|---|---|---|---|
| Profile | `CandidateService` | `getProfileDTO()` | Lấy profile và map sang response DTO | `CandidateProfileRepository` | Profile không tồn tại sẽ throw exception |
| Profile | `CandidateService` | `updateProfile()` | Cập nhật profile, skills, experiences, educations | `UserRepository`, `ObjectMapper`, `CVAnalysisResultRepository` | Chưa có Bean Validation ở DTO |
| CV Upload | `CandidateService` | `uploadAndAnalyzeCV()` | Upload CV, gọi AI, map kết quả vào profile, publish point event | `CloudinaryService`, `CVAnalysisService`, `ApplicationEventPublisher` | Upload/AI/event dependency có thể lỗi |
| Avatar | `CandidateService` | `uploadAvatar()` | Upload avatar và cập nhật `profileImageUrl` | `CloudinaryService`, `UserRepository` | Chưa validate MIME type ảnh trong service |
| Profile API | `CandidateProfileController` | `GET /api/candidate/profile/me` | Xem hồ sơ candidate hiện tại | `CandidateService`, `UserRepository` | Profile missing trả 200 với data null |
| Profile API | `CandidateProfileController` | `PUT /api/candidate/profile/me` | Cập nhật hồ sơ candidate | `CandidateService`, `UserRepository` | Service lỗi trả 400 |
| Profile API | `CandidateProfileController` | `POST /api/candidate/profile/upload-cv` | Upload và phân tích CV | `CandidateService`, `UserRepository` | Phụ thuộc upload/AI |
| Profile API | `CandidateProfileController` | `POST /api/candidate/profile/avatar` | Upload avatar | `CandidateService`, `UserRepository` | Chưa thấy validation file ảnh rõ |
| CV Builder | `CandidateCVController` | `POST /api/candidate/cv-builder/save` | Tạo/cập nhật CV Builder | `CandidateCVRepository`, `UserRepository`, `SecurityUtils` | Payload rỗng vẫn có thể được lưu |
| CV Builder | `CandidateCVController` | `GET /api/candidate/cv-builder/my-cvs` | Lấy danh sách CV của current user | `CandidateCVRepository`, `UserRepository` | Phụ thuộc current login |
| CV Builder | `CandidateCVController` | `GET /api/candidate/cv-builder/{id}` | Lấy chi tiết CV theo id | `CandidateCVRepository` | Chưa kiểm tra owner |
| Model | `CandidateCV` | `@PrePersist`, `@PreUpdate` | Tự cập nhật timestamp | JPA lifecycle | Cần test lifecycle |

---

## 4. Test Condition List

| ID | Method/Area | Test condition |
|---|---|---|
| `TC-F2-01` | `getProfileDTO()` | Profile tồn tại và được map đúng sang response |
| `TC-F2-02` | `getProfileDTO()` | Profile không tồn tại |
| `TC-F2-03` | `updateProfile()` | Candidate chưa có profile, hệ thống tạo profile mới |
| `TC-F2-04` | `updateProfile()` | Experiences mới thay thế experiences cũ |
| `TC-F2-05` | `updateProfile()` | Educations được serialize thành JSON |
| `TC-F2-06` | `uploadAndAnalyzeCV()` | Upload CV, AI response được map vào profile |
| `TC-F2-07` | `uploadAndAnalyzeCV()` | Point event lỗi nhưng upload vẫn thành công |
| `TC-F2-08` | `uploadAvatar()` | Avatar upload thành công và cập nhật user image URL |
| `TC-F2-09` | `uploadAvatar()` | Profile không tồn tại |
| `TC-F2-10` | `getMyProfile()` | Profile tồn tại |
| `TC-F2-11` | `getMyProfile()` | Profile không tồn tại |
| `TC-F2-12` | `updateMyProfile()` | Update profile thành công |
| `TC-F2-13` | `updateMyProfile()` | Service lỗi khi update |
| `TC-F2-14` | `uploadCV()` | Upload CV thành công |
| `TC-F2-15` | `uploadAvatar()` | Upload avatar thành công |
| `TC-F2-16` | `saveCV()` | Tạo CV Builder mới |
| `TC-F2-17` | `saveCV()` | Cập nhật CV Builder đã tồn tại |
| `TC-F2-18` | `saveCV()` | Payload rỗng theo behavior hiện tại |
| `TC-F2-19` | `getMyCVs()` | Lấy danh sách CV của current user |
| `TC-F2-20` | `getCV(id)` | CV tồn tại |
| `TC-F2-21` | `getCV(id)` | CV không tồn tại |
| `TC-F2-22` | `CandidateCV` | `@PrePersist` set timestamp |
| `TC-F2-23` | `CandidateCV` | `@PreUpdate` update timestamp |
| `TC-F2-24` | `CandidateProfile` | Lưu skills, experiences và education JSON |

---

## 5. Unit Test Implementation

Các file Unit Test mới đã tạo:

| File | Số test |
|---|---:|
| `BE/src/test/java/app/candidate/service/Feature2CandidateServiceTest.java` | 9 |
| `BE/src/test/java/app/candidate/controller/Feature2CandidateProfileControllerTest.java` | 6 |
| `BE/src/test/java/app/candidate/controller/Feature2CandidateCVControllerTest.java` | 6 |
| `BE/src/test/java/app/candidate/model/Feature2CandidateModelTest.java` | 3 |
| **Tổng** | **24** |

---

## 6. Unit Test Case List

| Test ID | Test class | Test method | Expected result |
|---|---|---|---|
| `UT-F2-001` | `Feature2CandidateServiceTest` | `getProfileDTO_shouldMapEntityToResponse_whenProfileExists` | Map đúng profile sang DTO |
| `UT-F2-002` | `Feature2CandidateServiceTest` | `getProfileDTO_shouldThrow_whenProfileDoesNotExist` | Throw exception |
| `UT-F2-003` | `Feature2CandidateServiceTest` | `updateProfile_shouldCreateProfileAndSaveFields_whenNoProfileExists` | Tạo profile mới, save user name, delete cache |
| `UT-F2-004` | `Feature2CandidateServiceTest` | `updateProfile_shouldReplaceExperiences_whenExperiencesProvided` | Experiences được thay thế |
| `UT-F2-005` | `Feature2CandidateServiceTest` | `updateProfile_shouldSerializeEducations_whenEducationsProvided` | `educationJson` được lưu |
| `UT-F2-006` | `Feature2CandidateServiceTest` | `uploadAndAnalyzeCV_shouldUploadAnalyzeMapAndPublishEvent` | Upload, AI map, delete cache, publish event |
| `UT-F2-007` | `Feature2CandidateServiceTest` | `uploadAndAnalyzeCV_shouldNotFail_whenPointEventFails` | Event lỗi nhưng vẫn return profile |
| `UT-F2-008` | `Feature2CandidateServiceTest` | `uploadAvatar_shouldUpdateUserImageUrl_whenProfileExists` | Cập nhật avatar URL |
| `UT-F2-009` | `Feature2CandidateServiceTest` | `uploadAvatar_shouldThrow_whenProfileMissing` | Throw exception |
| `UT-F2-010` | `Feature2CandidateProfileControllerTest` | `getMyProfile_shouldReturnProfile_whenProfileExists` | 200 OK, data profile |
| `UT-F2-011` | `Feature2CandidateProfileControllerTest` | `getMyProfile_shouldReturnNullData_whenProfileMissing` | 200 OK, data null |
| `UT-F2-012` | `Feature2CandidateProfileControllerTest` | `updateMyProfile_shouldReturnUpdatedProfile_whenServiceSucceeds` | 200 OK |
| `UT-F2-013` | `Feature2CandidateProfileControllerTest` | `updateMyProfile_shouldReturnBadRequest_whenServiceFails` | 400 Bad Request |
| `UT-F2-014` | `Feature2CandidateProfileControllerTest` | `uploadCV_shouldAnalyzeAndReturnUpdatedProfile` | 200 OK |
| `UT-F2-015` | `Feature2CandidateProfileControllerTest` | `uploadAvatar_shouldReturnAvatarUrl_whenServiceSucceeds` | 200 OK |
| `UT-F2-016` | `Feature2CandidateCVControllerTest` | `saveCV_shouldCreateNewCV_whenPayloadIsValid` | 200 OK, CV gắn current user |
| `UT-F2-017` | `Feature2CandidateCVControllerTest` | `saveCV_shouldUpdateExistingCV_whenIdExists` | CV được update |
| `UT-F2-018` | `Feature2CandidateCVControllerTest` | `saveCV_currentBehavior_allowsEmptyPayload` | Ghi nhận behavior hiện tại: payload rỗng vẫn save |
| `UT-F2-019` | `Feature2CandidateCVControllerTest` | `getMyCVs_shouldReturnCurrentUserCVs` | Trả list CV |
| `UT-F2-020` | `Feature2CandidateCVControllerTest` | `getCV_shouldReturnCV_whenIdExists` | Trả CV |
| `UT-F2-021` | `Feature2CandidateCVControllerTest` | `getCV_shouldThrow_whenIdDoesNotExist` | Throw `NoSuchElementException` |
| `UT-F2-022` | `Feature2CandidateModelTest` | `candidateCV_shouldSetTimestamps_onCreate` | Set `createdAt`, `updatedAt` |
| `UT-F2-023` | `Feature2CandidateModelTest` | `candidateCV_shouldRefreshUpdatedAt_onUpdate` | `updatedAt` thay đổi |
| `UT-F2-024` | `Feature2CandidateModelTest` | `candidateProfile_shouldKeepSkillsExperiencesAndEducationJson` | Lưu đúng skills, experiences, educationJson |

---

## 7. Unit Test Result

Lệnh đã chạy:

```powershell
mvn "-Dtest=Feature2CandidateServiceTest,Feature2CandidateProfileControllerTest,Feature2CandidateCVControllerTest,Feature2CandidateModelTest" test
```

Kết quả thực tế:

| Test class | Tests run | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `Feature2CandidateCVControllerTest` | 6 | 0 | 0 | 0 |
| `Feature2CandidateProfileControllerTest` | 6 | 0 | 0 | 0 |
| `Feature2CandidateModelTest` | 3 | 0 | 0 | 0 |
| `Feature2CandidateServiceTest` | 9 | 0 | 0 | 0 |
| **Tổng Feature 2** | **24** | **0** | **0** | **0** |

**Build result:** `BUILD SUCCESS`  
**Finished at:** `2026-07-17T22:37:40+07:00`

Evidence:

```text
BE/target/surefire-reports/app.candidate.service.Feature2CandidateServiceTest.txt
BE/target/surefire-reports/app.candidate.controller.Feature2CandidateProfileControllerTest.txt
BE/target/surefire-reports/app.candidate.controller.Feature2CandidateCVControllerTest.txt
BE/target/surefire-reports/app.candidate.model.Feature2CandidateModelTest.txt
```

---

## 8. Regression Test Result

Lệnh đã chạy:

```powershell
mvn test
```

Kết quả thực tế sau khi thêm Unit Test Feature 2:

| Scope | Tests run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| All backend tests | 63 | 0 | 0 | 0 | BUILD SUCCESS |

**Finished at:** `2026-07-17T22:38:19+07:00`

Kết luận regression:

- Baseline trước khi thêm Feature 2 test: 39 pass.
- Sau khi thêm Feature 2 test: 63 pass.
- Số test tăng thêm: 24.
- Không phát sinh failure/error mới.

---

## 9. Coverage Report

Lệnh đã chạy:

```powershell
mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report
```

Kết quả thực tế:

| Scope | Tests run | Failures | Errors | Result |
|---|---:|---:|---:|---|
| JaCoCo run with all backend tests | 63 | 0 | 0 | BUILD SUCCESS |

**Finished at:** `2026-07-17T22:39:14+07:00`

Evidence:

```text
BE/target/site/jacoco/index.html
BE/target/site/jacoco/jacoco.csv
BE/target/site/jacoco/jacoco.xml
```

Coverage lấy trực tiếp từ `jacoco.csv`:

| Package | Class | Instruction | Branch | Line | Covered lines | Missed lines |
|---|---|---:|---:|---:|---:|---:|
| `app.candidate.service` | `CandidateService` | 91.53% | 67.65% | 94.78% | 127 | 7 |
| `app.candidate.controller` | `CandidateProfileController` | 77.25% | N/A | 85.71% | 30 | 5 |
| `app.candidate.controller` | `CandidateCVController` | 95.73% | 100.00% | 100.00% | 20 | 0 |
| `app.candidate.model` | `CandidateCV` | 100.00% | N/A | 100.00% | 5 | 0 |

Các class trong package `app.candidate` nhưng không thuộc phạm vi Candidate Profile & CV:

| Class | Line coverage | Ghi chú |
|---|---:|---|
| `JobRecommendationService` | 0.00% | Ngoài phạm vi kiểm thử Feature 2 lần này |
| `RecommendationController` | 0.00% | Ngoài phạm vi kiểm thử Feature 2 lần này |

---

## 10. Integration Test Result

Postman Integration Test chưa được cập nhật trong báo cáo này.

| Scope | Status | Ghi chú |
|---|---|---|
| Feature 2 Postman Integration Test | Pending | Chưa có evidence Postman trong lượt cập nhật này |
| Integration Pass/Fail | Not claimed | Không ghi kết quả khi chưa chạy/xác nhận |

---

## 11. Bug/Risk List Từ Code Audit Và Unit Test Behavior

| ID | Nguồn | Mô tả | Evidence hiện có | Trạng thái |
|---|---|---|---|---|
| `RISK-F2-01` | Code audit + unit test behavior | `saveCV()` cho phép payload rỗng và vẫn lưu CV với field null | `saveCV_currentBehavior_allowsEmptyPayload` | Open |
| `RISK-F2-02` | Code audit | `getCV(id)` chưa kiểm tra CV có thuộc current user không | Code hiện tại chỉ dùng `cvRepository.findById(id)` | Open |
| `RISK-F2-03` | Code audit + unit test behavior | `getCV(id)` với id không tồn tại throw `NoSuchElementException`, chưa map rõ 404 | `getCV_shouldThrow_whenIdDoesNotExist` | Open |
| `RISK-F2-04` | Code audit | `uploadAvatar()` chưa validate MIME type ảnh trong service | Chưa test validation vì production code chưa có logic validate | Open |

---

## 12. Test Summary Đến Trước Postman

| Hạng mục | Kết quả |
|---|---:|
| Baseline backend trước khi thêm Feature 2 test | 39 pass, 0 fail |
| Unit Test Feature 2 đã implement | 24 |
| Unit Test Feature 2 pass | 24 |
| Unit Test Feature 2 fail/error | 0 |
| Regression toàn backend sau khi thêm Feature 2 test | 63 pass, 0 fail |
| JaCoCo report | Generated |
| Postman Integration Test | Pending |
| Bugs/Risks còn mở | 4 |

Kết luận tạm thời: Feature 2 đã có Unit Test mới chạy thực tế cho `CandidateService`, `CandidateProfileController`, `CandidateCVController` và model liên quan. Kết quả Unit Test và Regression đều pass. Coverage đã đo bằng JaCoCo và có evidence trong `target/site/jacoco`. Phần Postman Integration Test sẽ được bổ sung sau khi có evidence chạy thực tế.
