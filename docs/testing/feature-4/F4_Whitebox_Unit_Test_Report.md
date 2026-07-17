# BÁO CÁO HOÀN THÀNH KIỂM THỬ (TEST SUMMARY REPORT)
**Feature:** F4 - Application/CV Submission & Job Management

**Người thực hiện:** An

**Ngày hoàn thành:** 17/07/2026

---

## 1. Code Audit (Rà soát code thật)
Các module/class đã được tiến hành rà soát để chuẩn bị cho quá trình kiểm thử:
- `JobApplicationController` / `JobApplicationServiceImpl`
- `JobPostingController` / `JobPostingServiceImpl`
- `CandidateProfileController` / `CandidateService`
- `CandidateSearchServiceImpl`
- `RecruiterDashboardService`
- `CompanyService`
- `JobRecommendationService`

## 2. Test Condition List (Danh sách điều kiện kiểm thử)
Các điều kiện và tình huống nghiệp vụ cốt lõi đã được xác định:
- **Ứng tuyển (Job Application):** Job hợp lệ/hết hạn, Candidate nộp thiếu CV, AI phân tích điểm match.
- **Quản lý CV (Candidate):** Upload file đúng định dạng, file vượt quá dung lượng, lỗi Cloudinary, cập nhật profile một phần.
- **Quản lý Job (Recruiter):** Tạo Job mới, Update Job thuộc sở hữu, Update Job của người khác (phân quyền).
- **Thống kê & Tìm kiếm:** Dashboard hiển thị đúng số liệu pipeline, search đúng role.

## 3. Test Case List (Danh sách Test Case)
Tổng hợp kịch bản kiểm thử dựa trên Test Conditions (Đã review và Approved):
- Số lượng Unit Test Cases: > 300 test cases.
- *Ví dụ mẫu:*
  - `UT-F4-APP-001`: Nộp đơn với JobID hợp lệ -> Lưu thành công, kích hoạt sự kiện Gamification.
  - `UT-F4-APP-002`: Nộp đơn khi File CV quá lớn -> Ném lỗi RuntimeException (400 Bad Request).
  - `UT-F4-APP-003`: Recruiter xem Dashboard thống kê -> Trả về đúng số liệu ứng viên.

## 4. Unit Test Result (Kết quả Unit Test)
Kiểm thử độc lập với sự hỗ trợ của Mockito (Mock Dependency, không dùng DB thật):
- **Lệnh thực thi:** `mvn test`
- **Tổng số Tests run:** 330
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 1 (Thiết lập `@Disabled` có chủ đích)
- **Trạng thái:** `BUILD SUCCESS`

## 5. Integration Test Result (Kết quả Kiểm thử Tích hợp)
Kiểm thử giao tiếp giữa các thành phần thực tế (Service -> Repository -> H2 Database):
- **Script thực thi:** `JobApplicationIntegrationTest.java`
- **Kịch bản:** Tạo User/Candidate thật -> Lưu JobPosting thật -> Thực thi luồng `jobApplicationService.apply()` -> Truy vấn ngược lại bằng `findById()` để kiểm tra quan hệ khóa ngoại (Foreign Key).
- **Kết quả:** Pass 100%. Dữ liệu ghi nhận chính xác xuống in-memory DB (H2) kèm theo các Audit Log (`created_at`, `updated_at`).

## 6. Coverage Report (Báo cáo độ bao phủ)
Kết quả đo lường độ bao phủ bằng JaCoCo Plugin (`mvn clean verify -Pcoverage`):
- **RecruiterDashboardService:** 100% Instruction Coverage.
- **CandidateSearchServiceImpl:** 100% Instruction Coverage.
- **CompanyService:** 91% Instruction Coverage.
- **JobRecommendationService:** 91% Instruction Coverage.
- **JobApplicationServiceImpl:** 76% Instruction Coverage.
- **JobPostingServiceImpl:** 75% Instruction Coverage.
- **CandidateService:** 38% Instruction Coverage.

**Đánh giá tổng quan Package:** 
- Package `app.recruitment.service`: Đạt **80%**
- Package `app.candidate.service`: Đạt **55%**

### Minh chứng chạy Test và Coverage

*(Ảnh chụp màn hình Terminal báo Tests run: 330, Failures: 0, Errors: 0 và BUILD SUCCESS)*
![Minh chứng Terminal](https://cdn.phototourl.com/free/2026-07-16-7832df48-e5cf-4ede-bdb9-4b4dfaef380d.png)

*(Hình ảnh minh chứng báo cáo JaCoCo HTML)*
![Minh chứng JaCoCo Tổng quan](https://cdn.phototourl.com/free/2026-07-16-428f5026-1a43-4702-8060-582690cb8a58.png)
![Minh chứng JaCoCo Recruitment](https://cdn.phototourl.com/free/2026-07-16-df91f6a5-35cf-405d-b041-6a978bfef7ce.png)
![Minh chứng JaCoCo Candidate](https://cdn.phototourl.com/free/2026-07-16-9e0f40a9-b4e4-4d05-9d71-67dbf807de17.png)

## 7. Bug List (Danh sách Lỗi phát hiện trong quá trình test)
- **Bug 01:** `NullPointerException` khi `GeminiResponse.getContact()` trả về mảng rỗng lúc phân tích CV.
- **Bug 02:** Argument Mismatch trong Mockito khi truyền cứng `TEST_EMAIL` nhưng Spring Security dùng principal khác.
- **Bug 03:** `DataIntegrityViolationException` khi Entity `User` vi phạm ràng buộc `NOT NULL` của trường `auth_provider` và `password` lúc chạy Integration Test trên H2.

## 8. Retest/Regression Result (Kết quả Retest và Regression Test)
- Toàn bộ Bug liệt kê tại phần 7 đã được lập trình viên sửa mã trực tiếp trên Test Script và Production Code.
- **Retest:** Các luồng sinh lỗi đã Pass thành công (Bằng chứng: Đã inject thành công `AuthProvider.LOCAL` và `setPassword`).
- **Regression Test:** Các component xung quanh (Review, Notification, Gamification) không bị ảnh hưởng (0 Failures).

## 9. Test Summary (Tổng kết)
- **Đánh giá chung:** Feature 4 (Application & Job Management) đã đáp ứng hoàn toàn 100% tiêu chí hoàn thành (Completion Criteria) đề ra tại Giai đoạn 1. 
- **Chất lượng:** Mã nguồn xử lý mượt mà các ngoại lệ, phân quyền bảo mật hoạt động đúng kỳ vọng, và đảm bảo tính toàn vẹn dữ liệu. Sẵn sàng tích hợp toàn hệ thống (System Testing) và nghiệm thu (UAT).