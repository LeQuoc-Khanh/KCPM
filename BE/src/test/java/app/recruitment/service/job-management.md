# Báo cáo kiểm thử chức năng Job Management

**Chủ đề:** Phân hoạch lớp tương đương, phân tích giá trị biên, thiết kế test case và kiểm thử tự động  
**Môn học:** Kiểm chứng phần mềm  
**Dự án áp dụng:** Career Mate  
**Chức năng kiểm thử:** Job Management - Quản lý tin tuyển dụng  
**Phạm vi:** Backend Spring Boot, nhóm API `/api/recruiter/jobs`

---

## 1. Mục tiêu kiểm thử

Báo cáo này tập trung kiểm thử chức năng quản lý tin tuyển dụng dành cho nhà tuyển dụng. Các mục tiêu chính gồm:

1. Xác định điều kiện kiểm thử từ code hiện có của chức năng Job Management.
2. Áp dụng kỹ thuật **phân hoạch lớp tương đương** để chia miền dữ liệu đầu vào thành các lớp hợp lệ và không hợp lệ.
3. Áp dụng kỹ thuật **phân tích giá trị biên** cho trường có ràng buộc biên rõ ràng.
4. Thiết kế bộ test case black-box có input, expected outcome và tag bao phủ.
5. Triển khai unit test tự động dựa trên các lớp tương đương, giá trị biên và nhánh nghiệp vụ chính.

---

## 2. Cơ sở kiểm thử

Chức năng Job Management cho phép nhà tuyển dụng tạo, cập nhật, xóa mềm, xem danh sách và tìm kiếm tin tuyển dụng. Trong phạm vi báo cáo này, phần kiểm thử chính tập trung vào **chức năng tạo tin tuyển dụng**, vì đây là luồng có dữ liệu đầu vào rõ ràng nhất để áp dụng phân hoạch lớp tương đương và phân tích giá trị biên.

Các API chính:

| Chức năng | Method | Endpoint | Ghi chú |
|---|:---:|---|---|
| Tạo tin tuyển dụng | POST | `/api/recruiter/jobs` | Tạo job mới, service chỉ chấp nhận `RECRUITER` hoặc `RECRUITER_VIP` |
| Cập nhật tin tuyển dụng | PUT | `/api/recruiter/jobs/{id}` | Chỉ recruiter sở hữu job được cập nhật |
| Xóa tin tuyển dụng | DELETE | `/api/recruiter/jobs/{id}` | Xóa mềm bằng cách đổi status sang `DELETED` |
| Xem danh sách job của recruiter | GET | `/api/recruiter/jobs/me` | Không trả về job đã `DELETED` |
| Tìm kiếm job | GET | `/api/recruiter/jobs/search?keyword=...` | Keyword rỗng trả về top 10 job `PUBLISHED` |
| Xem chi tiết job public | GET | `/api/recruiter/jobs/public/{id}` | Public endpoint |

Request tạo job được định nghĩa bởi `JobPostingRequest`:

```java
public class JobPostingRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String requirements;

    private String salaryRange;

    @NotBlank
    private String location;

    @NotNull
    @FutureOrPresent
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private String status;
}
```

Body hợp lệ mẫu:

```json
{
  "title": "Java Backend Developer",
  "description": "Build and maintain REST APIs using Spring Boot.",
  "requirements": "Java, Spring Boot, PostgreSQL.",
  "salaryRange": "15000000-25000000",
  "location": "Ho Chi Minh City",
  "expiryDate": "hôm nay + 30"
}
```

Logic nghiệp vụ quan trọng trong `JobPostingServiceImpl`:

- User tạo job phải tồn tại.
- User tạo job phải có role `RECRUITER` hoặc `RECRUITER_VIP`.
- Recruiter phải có thông tin công ty trước khi đăng tin.
- Job tạo mới được tự động set status `PENDING`.

---

## 3. Giả định kiểm thử

1. Base URL khi kiểm thử API bằng Postman:

```text
https://careermate-backend-23e8.onrender.com
```

2. URL môi trường kiểm thử:

| Variable | Value |
|---|---|
| `api_base_url` | `https://careermate-backend-23e8.onrender.com` |
| `frontend_url` | `https://kcpm-blue.vercel.app/` |

3. Các token kiểm thử đã được chuẩn bị trước:

| Biến Postman | Ý nghĩa |
|---|---|
| `{{recruiter_token}}` | Token của recruiter hợp lệ, đã có company |
| `{{recruiter_vip_token}}` | Token của recruiter VIP hợp lệ |
| `{{candidate_token}}` | Token của candidate |
| `{{admin_token}}` | Token của admin |
| `{{other_recruiter_token}}` | Token của recruiter khác, không sở hữu job |

3. Với các trường chuỗi bắt buộc, `null`, `""` và `"   "` đều không hợp lệ do annotation `@NotBlank`.
4. Với `expiryDate`, giá trị hợp lệ là **hôm nay hoặc ngày trong tương lai** do annotation `@FutureOrPresent`.
5. Với `salaryRange`, code hiện tại chưa có annotation validate, nên chưa xác định lớp không hợp lệ rõ ràng ở tầng DTO.
6. Với `status`, API tạo job không sử dụng giá trị status từ request, nên trường này không được đưa vào bộ test case chính của chức năng tạo job.

---

## 4. Phân hoạch lớp tương đương

| Biến/điều kiện đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| `title` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V1 | `null` | X1 |
| `title` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V1 | `""` hoặc `"   "` | X2 |
| `description` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V2 | `null` | X3 |
| `description` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V2 | `""` hoặc `"   "` | X4 |
| `requirements` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V3 | `null` | X5 |
| `requirements` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V3 | `""` hoặc `"   "` | X6 |
| `location` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V4 | `null` | X7 |
| `location` | Chuỗi có ít nhất 1 ký tự khác khoảng trắng | V4 | `""` hoặc `"   "` | X8 |
| `expiryDate` | Ngày hôm nay hoặc tương lai, đúng format `yyyy-MM-dd` | V5 | `null` | X9 |
| `expiryDate` | Ngày hôm nay hoặc tương lai, đúng format `yyyy-MM-dd` | V5 | Ngày trong quá khứ | X10 |
| `expiryDate` | Ngày hôm nay hoặc tương lai, đúng format `yyyy-MM-dd` | V5 | Sai định dạng ngày | X11 |
| `salaryRange` | `null`, rỗng hoặc chuỗi bất kỳ | V6 | Chưa có lớp không hợp lệ rõ ràng trong code | - |
| Xác thực | Có JWT hợp lệ | V7 | Không có token hoặc token sai | X12 |
| Role tạo job | `RECRUITER` hoặc `RECRUITER_VIP` | V8 | `CANDIDATE`, `CANDIDATE_VIP`, `ADMIN` hoặc role khác | X13 |

---

## 5. Phân tích giá trị biên

Trong chức năng này, biến có biên rõ ràng nhất là `expiryDate`, vì được khai báo bằng `@FutureOrPresent`.

Miền hợp lệ:

```text
expiryDate >= hôm nay
```

Code hiện tại không quy định biên trên cho `expiryDate`, nên bảng BVA ghi đầy đủ các cột chuẩn, nhưng `max-`, `max`, `max+` không áp dụng được.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| `expiryDate` | Hôm nay - 1 ngày | Hôm nay | Hôm nay + 1 ngày | Hôm nay + 30 ngày | - | - | - | B1-B7 |

Ý nghĩa các tag biên:

| Tag | Giá trị | Kết quả mong đợi |
|---|---|---|
| B1 | Hôm nay - 1 ngày | Không hợp lệ vì nhỏ hơn biên dưới |
| B2 | Hôm nay | Hợp lệ vì đúng tại biên |
| B3 | Hôm nay + 1 ngày | Hợp lệ vì ngay trên biên |
| B4 | Hôm nay + 30 ngày | Hợp lệ, giá trị đại diện trong miền |
| B5 | `max-` | - |
| B6 | `max` | - |
| B7 | `max+` | - |

Ngoài ra, `expiryDate = null` không phải giá trị biên theo nghĩa số học, nhưng vẫn là ca kiểm thử bắt buộc vì trường này có `@NotNull`.

Các trường `title`, `description`, `requirements` và `location` chỉ có `@NotBlank`, chưa có `@Size(min, max)`, nên không đủ cơ sở để phân tích min/max theo BVA đầy đủ. Các trường này được kiểm thử bằng phân hoạch lớp tương đương.

---

## 6. Thiết kế test case

Bộ test case dưới đây là **test case black-box** ở mức API, dùng để chạy bằng Postman hoặc công cụ API testing tương tự.

Bộ test case được chia thành 3 nhóm:

| Nhóm test case | Số lượng | Căn cứ thiết kế | Phạm vi |
|---|---:|---|---|
| Phân hoạch lớp tương đương | 11 | Các lớp hợp lệ/không hợp lệ của input và role tạo job | Tạo job |
| Giá trị biên | 3 | Biên dưới của `expiryDate`: hôm nay - 1, hôm nay, hôm nay + 1 | Tạo job |
| Nghiệp vụ | 10 | Các luồng quản lý job còn lại: update, delete, search, public detail | Job Management |
| **Tổng** | **24** | EP + BVA + nghiệp vụ | API/Postman |

| STT | Nhóm | Tên test case | API | Input chính | Kết quả mong đợi | Tag được bao phủ |
|---:|---|---|---|---|---|---|
| 1 | EP | Tạo job hợp lệ chuẩn | POST `/recruiter/jobs` | Recruiter token hợp lệ; body hợp lệ; `expiryDate = hôm nay + 30 ngày` | `201 Created`, job được tạo, status `PENDING` | V1,V2,V3,V4,V5,V6,V7,V8,B4 |
| 2 | EP | Tạo job bằng recruiter VIP | POST `/recruiter/jobs` | Recruiter VIP token, body hợp lệ | `201 Created`, job được tạo, status `PENDING` | V8 |
| 3 | BVA | Deadline tại biên dưới | POST `/recruiter/jobs` | `expiryDate = hôm nay` | `201 Created` | B2 |
| 4 | BVA | Deadline ngay trên biên | POST `/recruiter/jobs` | `expiryDate = hôm nay + 1 ngày` | `201 Created` | B3 |
| 5 | BVA | Deadline dưới biên | POST `/recruiter/jobs` | `expiryDate = hôm nay - 1 ngày` | `400 Bad Request` | X10,B1 |
| 6 | EP | Title null | POST `/recruiter/jobs` | `title = null` | `400 Bad Request` | X1 |
| 7 | EP | Title rỗng | POST `/recruiter/jobs` | `title = ""` | `400 Bad Request` | X2 |
| 8 | EP | Title chỉ có khoảng trắng | POST `/recruiter/jobs` | `title = "   "` | `400 Bad Request` | X2 |
| 9 | EP | Description rỗng | POST `/recruiter/jobs` | `description = ""` | `400 Bad Request` | X4 |
| 10 | EP | Requirements rỗng | POST `/recruiter/jobs` | `requirements = ""` | `400 Bad Request` | X6 |
| 11 | EP | Location rỗng | POST `/recruiter/jobs` | `location = ""` | `400 Bad Request` | X8 |
| 12 | EP | Salary null | POST `/recruiter/jobs` | `salaryRange = null` | `201 Created` vì code chưa validate `salaryRange` | V6 |
| 13 | EP | Thiếu deadline | POST `/recruiter/jobs` | Không gửi `expiryDate` | `400 Bad Request` | X9 |
| 14 | EP | Candidate tạo job | POST `/recruiter/jobs` | Candidate token | `403 Forbidden` do Security chặn `/api/recruiter/**` | X13 |
| 15 | Nghiệp vụ | Cập nhật job hợp lệ | PUT `/recruiter/jobs/{id}` | Chủ job, body hợp lệ, `status = PUBLISHED` | `200 OK`, dữ liệu và status được cập nhật | - |
| 16 | Nghiệp vụ | Cập nhật status không hợp lệ | PUT `/recruiter/jobs/{id}` | Chủ job, `status = INVALID_STATUS` | `200 OK`, status cũ được giữ nguyên | - |
| 17 | Nghiệp vụ | Recruiter khác cập nhật job | PUT `/recruiter/jobs/{id}` | Token recruiter không phải chủ job | `400 Bad Request`, không có quyền sửa | - |
| 18 | Nghiệp vụ | Cập nhật job không tồn tại | PUT `/recruiter/jobs/999999` | Job id không tồn tại | `400 Bad Request`, `Job not found` | - |
| 19 | Nghiệp vụ | Xóa job hợp lệ | DELETE `/recruiter/jobs/{id}` | Chủ job xóa job của mình | `204 No Content`, status đổi sang `DELETED` | - |
| 20 | Nghiệp vụ | Recruiter khác xóa job | DELETE `/recruiter/jobs/{id}` | Token recruiter không phải chủ job | `400 Bad Request`, không có quyền xóa | - |
| 21 | Nghiệp vụ | Search keyword rỗng | GET `/recruiter/jobs/search?keyword=` | Keyword rỗng | `200 OK`, trả top 10 job `PUBLISHED` | - |
| 22 | Nghiệp vụ | Search keyword có khoảng trắng | GET `/recruiter/jobs/search?keyword= java ` | Keyword được trim thành `java` | `200 OK`, gọi search theo keyword | - |
| 23 | Nghiệp vụ | Public detail job tồn tại | GET `/recruiter/jobs/public/{id}` | Job id tồn tại | `200 OK`, trả detail kèm application count | - |
| 24 | Nghiệp vụ | Public detail job không tồn tại | GET `/recruiter/jobs/public/999999` | Job id không tồn tại | `400 Bad Request`, `Job not found` | - |

---

## 7. Kiểm thử tự động

Unit test được triển khai ở tầng service/DTO để kiểm tra logic nghiệp vụ và validation. Trong đó, 15 unit test đầu đồng bộ với 15 test case Postman cho chức năng tạo job; 10 unit test bổ sung được dùng để kiểm thử white-box các nhánh còn lại của `JobPostingServiceImpl` và cải thiện coverage.

### 7.1. Công cụ sử dụng

| Công cụ | Mục đích |
|---|---|
| JUnit 5 | Viết và chạy unit test |
| Mockito | Mock repository, mapper, AI service và event publisher |
| Jakarta Validator | Kiểm tra annotation validation của DTO |

### 7.2. File unit test

```text
BE/src/test/java/app/recruitment/service/JobManagementWhiteBoxTest.java
```

### 7.3. Danh sách unit test đã triển khai

| Unit test | Nội dung | Tag liên quan |
|---|---|---|
| `UT01_create_success_withValidRecruiter_shouldSavePendingJob` | Tạo job thành công, status `PENDING` | V1,V2,V3,V4,V5,V6,V7,V8,B4 |
| `UT02_create_success_withRecruiterVip_shouldSavePendingJob` | Recruiter VIP được tạo job | V8 |
| `UT03_create_shouldAcceptExpiryDateToday` | `expiryDate = hôm nay` hợp lệ | B2 |
| `UT04_create_shouldAcceptExpiryDateTomorrow` | `expiryDate = hôm nay + 1 ngày` hợp lệ | B3 |
| `UT05_create_shouldRejectExpiryDateYesterday` | `expiryDate = hôm nay - 1 ngày` không hợp lệ | X10,B1 |
| `UT06_create_shouldRejectNullTitle` | Reject title null | X1 |
| `UT07_create_shouldRejectBlankTitle` | Reject title rỗng | X2 |
| `UT08_create_shouldRejectWhitespaceTitle` | Reject title chỉ gồm khoảng trắng | X2 |
| `UT09_create_shouldRejectBlankDescription` | Reject description rỗng | X4 |
| `UT10_create_shouldRejectBlankRequirements` | Reject requirements rỗng | X6 |
| `UT11_create_shouldRejectBlankLocation` | Reject location rỗng | X8 |
| `UT12_create_shouldAcceptNullSalaryRange` | Chấp nhận `salaryRange = null` | V6 |
| `UT13_create_shouldRejectNullExpiryDate` | Reject `expiryDate = null` | X9 |
| `UT14_create_shouldThrowWhenUserIsCandidate` | Candidate không được tạo job | X13 |
| `UT15_update_success_shouldUpdateFieldsAndStatus` | Cập nhật job và status hợp lệ | - |
| `UT16_update_shouldIgnoreInvalidStatus` | Status không hợp lệ bị bỏ qua | - |
| `UT17_update_shouldThrowWhenNotOwner` | Recruiter khác không được cập nhật job | - |
| `UT18_update_shouldThrowWhenJobNotFound` | Job không tồn tại khi update | - |
| `UT19_delete_success_shouldMarkJobDeleted` | Xóa mềm job thành `DELETED` | - |
| `UT20_delete_shouldThrowWhenNotOwner` | Recruiter khác không được xóa job | - |
| `UT21_searchJobs_shouldReturnTop10WhenKeywordBlank` | Keyword rỗng trả top 10 job published | - |
| `UT22_searchJobs_shouldCallSearchRepositoryWhenKeywordPresent` | Keyword có giá trị được trim rồi gọi search | - |
| `UT23_getJobDetailPublic_success_shouldReturnApplicationCount` | Public detail thành công | - |
| `UT24_getJobDetailPublic_shouldThrowWhenJobNotFound` | Public detail job không tồn tại | - |

### 7.4. Kết quả thực thi

```text
Tests run: 24
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Lệnh chạy riêng file unit test:

```bash
mvn -Dtest=JobManagementWhiteBoxTest -Pcoverage verify
```

---

## 8. Tổng kết kết quả kiểm thử

| Hạng mục | Kết quả |
|---|---|
| Phân hoạch lớp tương đương | Đã xác định lớp hợp lệ/không hợp lệ cho input, quyền và nghiệp vụ |
| Phân tích giá trị biên | Đã áp dụng cho `expiryDate` với biên dưới là hôm nay |
| Test case black-box | 24 test case Postman/API: 11 EP, 3 BVA, 10 nghiệp vụ |
| Kết quả chạy unit test | 24/24 passed |
| Coverage `JobPostingServiceImpl` | Line coverage: 81.6% (71/87 lines), branch coverage: 81.3% (13/16 branches), instruction coverage: 78% |


