# BÁO CÁO KIỂM THỬ HỘP ĐEN & TÍCH HỢP API (BLACKBOX & API INTEGRATION TEST)
**Feature:** F4 - Application/CV Submission & Job Management
**Người thực hiện:** An
**Công cụ sử dụng:** Postman

---

## 1. Mục tiêu kiểm thử
- Kiểm chứng luồng nghiệp vụ Nộp hồ sơ ứng tuyển (Apply Job) thông qua các API Endpoints.
- Áp dụng kỹ thuật Phân hoạch lớp tương đương (EP) và Phân tích giá trị biên (BVA) để kiểm tra các ràng buộc đầu vào (Dung lượng CV, Định dạng file).
- Đảm bảo tính toàn vẹn dữ liệu và kiểm chứng các ngoại lệ nghiệp vụ (Ứng tuyển trùng, Job không tồn tại, Job đã bị xóa).

## 2. Phân tích Giá trị biên (BVA) & Lớp tương đương (EP) áp dụng
Dựa trên cấu hình hệ thống (`max-file-size=10MB`) và logic nghiệp vụ của Feature 4:

- **Biến Dung lượng CV (BVA):** 
  - Biên hợp lệ: `<= 10MB` (Kỳ vọng: 201 Created)
  - Vượt biên: `> 10MB` (Kỳ vọng: 413 Payload Too Large / 400 Bad Request) - *Áp dụng tại TC_4.9*
- **Biến Định dạng CV (EP):** 
  - Lớp hợp lệ: `.pdf`, `.docx` - *Áp dụng tại TC_4.1*
  - Lớp không hợp lệ: `.exe`, `.sh` - *Áp dụng tại TC_4.5*
- **Trạng thái Job (EP):**
  - Lớp hợp lệ: Job đang `ACTIVE`
  - Lớp không hợp lệ: Job `DELETED` hoặc `REJECTED` - *Áp dụng tại TC_4.10*

## 3. Kịch bản và Kết quả Kiểm thử (Test Execution)

*Dưới đây là bảng tổng hợp các test case thực thi qua Postman (Endpoint: `POST /api/applications/apply`)*

| TC ID | Tên kịch bản (Test Case) | Dữ liệu đầu vào / Điều kiện | Kết quả mong đợi (Expected) | Trạng thái (Status) |
| :--- | :--- | :--- | :--- | :---: |
| **TC_4.1** | Apply job successfully with a valid uploaded CV | valid jobId và cvUrl (.pdf/.docx) | 201 Created, file uploaded to storage successfully | ✅ Pass |
| **TC_4.2** | Apply job successfully using existing profile CV | valid jobId (No cvUrl attached) | 201 Created, system uses CV from Candidate Profile | ✅ Pass |
| **TC_4.3** | Apply job failed when Job ID does not exist | non-existent jobId (e.g., 999999) | 404 Not Found / 400 Bad Request | ✅ Pass |
| **TC_4.4** | Apply job failed when candidate has already applied | Job that candidate already applied | 409 Conflict / 400 Bad Request | ✅ Pass |
| **TC_4.5** | Apply job failed when CV file format is invalid | invalid cvUrl format (e.g., .exe, .sh) | 400 Bad Request | ✅ Pass |
| **TC_4.6** | Apply job failed without authentication token | Không có Authorization header | 401 Unauthorized | ✅ Pass |
| **TC_4.7** | Verify database relationship and data integrity | Kiểm tra DB sau TC_4.1 hoặc TC_4.2 | New record exists mapping correct candidate_id and job_id | ✅ Pass |
| **TC_4.8** | Verify Notification/Event publishing integration | Kiểm tra logs sau TC_4.1 hoặc TC_4.2 | ApplicationSubmittedEvent is fired / Notification sent | ✅ Pass |
| **TC_4.9** | Apply job failed when uploaded CV exceeds max size | CV file > 10MB | 413 Payload Too Large / 400 Bad Request | ✅ Pass |
| **TC_4.10** | Apply job failed when Job status is DELETED or REJECTED| jobId with status DELETED/REJECTED | 400 Bad Request | ❌ **Fail (Bug)** |

## 4. Ghi nhận Lỗi (Bug Report)
- **ID Lỗi:** BUG-F4-01 (Từ TC_4.10)
- **Mô tả:** Hệ thống cho phép ứng viên nộp CV thành công (trả về HTTP Status 201 Created) ngay cả khi trạng thái của Job Posting đó đã chuyển sang `DELETED` hoặc `REJECTED`. 
- **Giải pháp đề xuất:** Bổ sung logic kiểm tra `jobPosting.getStatus() == ACTIVE` tại tầng `JobApplicationServiceImpl` trước khi khởi tạo Application record.

## 5. Kết luận
- Hầu hết các luồng API cho Feature 4 hoạt động ổn định và xử lý tốt các ngoại lệ về xác thực, định dạng file và dung lượng vượt biên.
- Cần khắc phục gấp Bug liên quan đến trạng thái Job (TC_4.10) để đảm bảo tính logic của quy trình tuyển dụng trước khi release.

*(Hình ảnh minh chứng thực thi trên Postman)*
![Postman Collection](https://cdn.phototourl.com/free/2026-07-17-a4136c4e-7533-421c-ae73-072ea4bad2d7.png)