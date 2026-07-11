# Báo cáo kiểm tra White-box: Độ bao phủ kiến trúc và độ bao phủ nhánh

**Dự án:** CareerMate / KCPM  
**Phạm vi phân tích:** Backend Spring Boot trong thư mục `BE/src/main/java/app` và test trong `BE/src/test/java/app`  
**Ngày lập:** 10/07/2026  
**Loại kiểm thử:** White-box testing, tập trung vào Architecture Coverage và Branch Coverage  

---

## 1. Mục tiêu báo cáo

Báo cáo này được lập nhằm đánh giá mức độ bao phủ kiểm thử white-box của dự án CareerMate theo hai góc nhìn chính:

1. **Độ bao phủ kiến trúc (Architecture Coverage):** kiểm tra xem các tầng và module chính của hệ thống đã có test tương ứng hay chưa.
2. **Độ bao phủ nhánh (Branch Coverage):** kiểm tra mức độ các điều kiện rẽ nhánh quan trọng trong code đã được test qua các trường hợp đúng, sai, lỗi biên, thiếu quyền hoặc dữ liệu không hợp lệ.

Báo cáo không sử dụng công cụ đo coverage tự động như JaCoCo, mà dựa trên phân tích static code kết hợp với các test class hiện có trong dự án. Do đó, các số liệu nhánh là **ước lượng phục vụ báo cáo học phần**, không phải số liệu runtime tuyệt đối.

---

## 2. Tổng quan kiến trúc dự án

Backend của dự án được xây dựng theo kiến trúc nhiều tầng phổ biến của Spring Boot:

| Tầng kiến trúc | Vai trò chính | Số lớp ước lượng |
|---|---|---:|
| Controller | Nhận HTTP request, phân quyền endpoint, gọi service | 31 |
| Service | Xử lý nghiệp vụ chính của hệ thống | 47 |
| Repository | Truy vấn dữ liệu thông qua Spring Data JPA | 21 |
| Security | JWT, phân quyền, filter, authentication entry point | 8 |
| Model / Entity | Biểu diễn dữ liệu nghiệp vụ và bảng CSDL | 26 |
| DTO | Dữ liệu request/response giữa client và server | 47 |
| Config | Cấu hình Cloudinary, WebSocket, JPA auditing | 3 |
| Exception | Xử lý lỗi tập trung và custom exception | 9 |
| Other | Mapper, utility, main class và thành phần hỗ trợ khác | 6 |

Tổng số lớp production được quét: **198 lớp**.

---

## 3. Độ bao phủ kiến trúc

### 3.1. Thống kê test hiện có theo module

| Module | Lớp production | Lớp test | Test method | Nhận xét bao phủ |
|---|---:|---:|---:|---|
| Auth / Security | 42 | 2 | 41 | Bao phủ tốt các luồng đăng ký, đăng nhập, JWT, refresh token, logout, reset password, RBAC. |
| Candidate | 11 | 2 | 29 | Bao phủ khá tốt profile, CV, avatar, CV builder và các lỗi input. |
| Recruitment | 28 | 5 | 50 | Bao phủ tốt job posting, application, update status, recruiter flow. |
| Admin | 41 | 5 | 50 | Bao phủ tốt dashboard, user management, role, maintenance, report. |
| AI | 33 | 0 | 0 | Chưa có test trực tiếp cho service AI; chủ yếu phụ thuộc kiểm thử thủ công/integration. |
| Notification | 5 | 4 | 18 | Bao phủ tốt create, read, unread count, delete, pagination. |
| Review | 6 | 3 | 10 | Bao phủ các luồng review cơ bản, rating biên và quyền sở hữu. |
| Gamification | 12 | 3 | 24 | Bao phủ leaderboard, point log, rank, cộng điểm. |
| Payment | 1 | 1 | 7 | Có test controller/payment cơ bản. |
| Content | 11 | 0 | 0 | Chưa có test riêng cho article/company content. |
| Config / Utility / Exception | 13 | 0 | 0 | Chưa test trực tiếp, thường được bao phủ gián tiếp qua integration hoặc runtime. |
| Integration | - | 4 | 4 | Có test tích hợp cho review, payment, notification, gamification. |

Tổng số test class được quét: **29 lớp**.  
Tổng số test method được quét: **233 test method**.

### 3.2. Đánh giá theo tầng kiến trúc

| Tầng | Mức bao phủ hiện tại | Nhận xét |
|---|---|---|
| Controller | Trung bình - Khá | Một số controller đã có test trực tiếp như Candidate, Notification, Review, Payment. Một số controller khác chủ yếu được kiểm thử qua Postman hoặc integration. |
| Service | Khá | Các service nghiệp vụ chính như Auth, Recruitment, Admin, Candidate, Notification, Review, Leaderboard đã có test. |
| Repository | Trung bình | Repository ít được test trực tiếp, nhưng được gọi gián tiếp qua service/integration test. Với dự án sinh viên nhỏ, mức này chấp nhận được. |
| Security | Khá | AuthServiceTest và JwtTokenProviderTest đã bao phủ nhiều nhánh JWT, đăng nhập, refresh, logout, RBAC. |
| DTO / Model | Trung bình | Một số DTO/model có test, nhưng phần lớn được kiểm tra gián tiếp qua request/response và service test. |
| External Services | Thấp - Trung bình | Cloudinary, Gemini AI, Email thường được mock hoặc kiểm thử thủ công, chưa có test tự động đầy đủ. |

### 3.3. Kết luận về Architecture Coverage

Dự án đã bao phủ tốt các module nghiệp vụ quan trọng nhất:

- Authentication & Authorization
- Candidate Profile & CV
- Job Search & Application
- Recruiter Job Management
- Admin Management
- Notification, Review, Leaderboard

Các khu vực còn yếu gồm:

- AI services và Gemini API client
- Content/Article management
- Cloudinary upload edge cases
- Một số configuration/filter đặc thù như WebSocket, Maintenance filter

Với quy mô nhóm sinh viên 7 người, nhóm nên ưu tiên test các module có rủi ro cao trước thay vì cố bao phủ toàn bộ class nhỏ.

---

## 4. Độ bao phủ nhánh

### 4.1. Phương pháp xác định nhánh

Nhánh được xác định thông qua các cấu trúc điều kiện và luồng rẽ trong code Java, bao gồm:

- `if`, `else if`
- `switch`, `case`
- `catch`
- vòng lặp `for`, `while`
- toán tử logic `&&`, `||`
- toán tử ba ngôi `? :`

Kết quả quét static cho thấy toàn backend có khoảng **590 điểm rẽ nhánh ước lượng**.

### 4.2. Thống kê nhánh theo module

| Module | Điểm rẽ nhánh ước lượng | Mức độ test hiện tại | Nhận xét |
|---|---:|---|---|
| AI | 161 | Thấp | Nhiều nhánh xử lý API Gemini, parse JSON, retry, file extraction nhưng chưa có test trực tiếp. |
| Auth | 96 | Khá cao | Đã có 41 test method, bao phủ nhiều nhánh login/register/token/RBAC. |
| Recruitment | 93 | Khá | Đã có nhiều test cho job posting và application, nhưng vẫn cần chú ý LazyInitialization và quyền sở hữu dữ liệu. |
| Admin | 79 | Khá | Có test core và edge/security cho user, role, maintenance, report. |
| Candidate | 67 | Khá | Có test profile/CV/avatar; cần tăng test upload file không hợp lệ và parsing lỗi. |
| Gamification | 32 | Khá | Có test cộng điểm, rank, log, daily limit. |
| Content | 18 | Thấp | Chưa có test riêng cho publish/unpublish, slug, article status. |
| Util | 13 | Thấp | SecurityUtils có nhiều nhánh auth/principal nhưng chưa test trực tiếp. |
| Config | 8 | Thấp | Chủ yếu được kiểm tra khi chạy app/integration. |
| Review | 7 | Khá | Ít nhánh nhưng đã có test rating, duplicate, permission. |
| Payment | 6 | Trung bình | Có controller test cơ bản. |
| Notification | 5 | Khá | Ít nhánh, đã có test service/controller/model. |
| Exception | 1 | Thấp | Global handler nên được test thêm ở mức controller/integration. |

### 4.3. Bao phủ nhánh theo các feature chính

#### Feature 1 - Authentication & Authorization

Các nhánh quan trọng đã được bao phủ:

- Đăng ký thành công / email trùng / role admin bị chặn
- Upload avatar khi đăng ký có file / không có file / file lỗi
- Verify email đúng code / sai code / email không tồn tại / account đã verify
- Login đúng credentials / sai password / email không tồn tại / email chưa verify
- Google login thành công / token sai / tạo user mới
- Refresh token hợp lệ / invalid / expired
- Logout khi authenticated / unauthenticated
- RBAC: token hợp lệ nhưng sai role trả 403; thiếu token hoặc token sai trả 401

Đánh giá: **bao phủ nhánh tốt** cho nghiệp vụ Auth/Security.

#### Feature 2 - Candidate Profile & CV

Các nhánh quan trọng đã được bao phủ:

- Xem profile thành công
- Cập nhật profile với dữ liệu hợp lệ
- Upload CV PDF/DOCX hợp lệ
- Upload file sai định dạng hoặc quá lớn
- Upload avatar hợp lệ / file không phải ảnh
- Lưu CV builder thành công / thiếu dữ liệu
- Xem danh sách CV / xem chi tiết CV / truy cập CV không thuộc owner

Đánh giá: **bao phủ khá**, nhưng cần chú ý xác nhận lại bug upload avatar non-image trên môi trường deploy.

#### Feature 3 - Job Search & Application

Các nhánh quan trọng đã được bao phủ:

- Tạo job hợp lệ / thiếu dữ liệu / deadline sai
- Xem danh sách job / tìm kiếm job / xem chi tiết job
- Cập nhật job hợp lệ / job không tồn tại / không thuộc recruiter
- Ứng tuyển job với CV upload / CV profile
- Chặn ứng tuyển trùng
- Gửi notification sau khi apply
- Xử lý job/application không tồn tại

Đánh giá: **bao phủ khá**, nhưng các lỗi LazyInitialization cần có regression test rõ ràng.

#### Feature 4 - Recruiter Job Management

Các nhánh quan trọng đã được bao phủ:

- Xem/cập nhật company profile
- Upload logo công ty hợp lệ / sai file / quá lớn
- Tạo/cập nhật/xóa job
- Xem danh sách ứng viên apply
- Cập nhật trạng thái đơn ứng tuyển
- Gửi notification khi trạng thái thay đổi

Đánh giá: **bao phủ khá**, phù hợp với các bug MP-70 và MP-95 đã phát hiện.

#### Feature 5 - AI Features

Các nhánh cần bao phủ:

- CV có text hợp lệ / CV rỗng / CV không đọc được
- Gemini trả kết quả hợp lệ / lỗi API / timeout / JSON sai format
- Có cache / không có cache
- Candidate hoặc job không tồn tại
- Match score cao / thấp / thiếu skill
- Interview session đang chạy / đã completed

Đánh giá: **bao phủ tự động còn thấp**. Đây là vùng rủi ro cao vì phụ thuộc external API.

#### Feature 6 - Admin Management

Các nhánh quan trọng đã được bao phủ:

- Admin xem dashboard, user list, report
- Lock/unlock user
- Cập nhật role hợp lệ / không hợp lệ
- Chặn tự khóa chính mình
- Chặn hạ quyền admin khác
- Bật/tắt maintenance mode
- Non-admin bị chặn khi maintenance

Đánh giá: **bao phủ khá tốt**.

#### Feature 7 - Notification, Review, Leaderboard & VIP

Các nhánh quan trọng đã được bao phủ:

- Notification có dữ liệu / rỗng
- Mark read / mark all read / delete
- Chặn thao tác notification của user khác
- Review rating 1/5 hợp lệ, rating ngoài biên bị chặn
- Chặn review trùng
- Leaderboard có điểm / chưa có điểm / user cùng điểm
- VIP candidate/recruiter/admin/unauthorized

Đánh giá: **bao phủ trung bình - khá**, nhưng còn nhiều pending test ở integration report.

---

## 5. Nhận xét về khoảng trống kiểm thử

Các khoảng trống chính hiện tại:

1. **AI module có nhiều nhánh nhưng chưa có test tự động tương ứng.**  
   Đây là module có nhiều xử lý lỗi, retry, parsing và phụ thuộc API ngoài nên cần mock Gemini để test ổn định.

2. **Content module chưa có test riêng.**  
   Nên bổ sung test cho article create/update/publish/unpublish, slug và trạng thái bài viết.

3. **Một số bug production chỉ xuất hiện trên Render.**  
   Ví dụ LazyInitializationException do khác cấu hình `open-in-view`. Cần thêm regression test hoặc kiểm thử Postman trên môi trường deploy.

4. **Repository và mapper được test gián tiếp nhiều hơn trực tiếp.**  
   Với dự án nhỏ thì chấp nhận được, nhưng các mapper có truy cập entity lazy nên nên có test riêng hoặc integration test.

5. **Một số nhánh security/filter chưa có test độc lập.**  
   Ví dụ WebSocket auth, maintenance mode filter, SecurityUtils.

---

## 6. Đề xuất cải thiện

### 6.1. Ưu tiên ngắn hạn

Nhóm nên ưu tiên bổ sung test cho các phần sau:

| Ưu tiên | Khu vực | Lý do |
|---|---|---|
| Cao | AI service | Nhiều nhánh, phụ thuộc external API, dễ lỗi runtime. |
| Cao | Recruitment mapper/repository | Đã phát sinh bug LazyInitializationException. |
| Cao | Candidate avatar/CV upload | Đã phát sinh bug nhận file không hợp lệ. |
| Trung bình | Content module | Chưa có test tự động. |
| Trung bình | SecurityUtils/Maintenance filter | Ảnh hưởng phân quyền toàn hệ thống. |

### 6.2. Cách triển khai phù hợp với nhóm sinh viên

Không cần cố đạt 100% coverage. Nhóm nên đặt mục tiêu thực tế:

- Mỗi feature có test core flow và edge/security flow.
- Các bug đã fix phải có regression test hoặc bằng chứng Postman retest.
- Với module nhiều external API như AI, ưu tiên mock response thay vì gọi API thật.
- Với lỗi LazyInitialization, nên test các API đọc dữ liệu có quan hệ entity như Job - Recruiter - Company.

---

## 7. Kết luận

Dựa trên cấu trúc code và test hiện tại, dự án CareerMate đã có mức bao phủ white-box tương đối tốt ở các module nghiệp vụ chính như Auth, Recruitment, Admin, Candidate, Notification, Review và Gamification.

Tuy nhiên, độ bao phủ chưa đồng đều. Một số module có nhiều nhánh xử lý nhưng ít hoặc chưa có test tự động, đặc biệt là AI, Content, Config/Utility và các luồng liên quan external service. Với quy mô nhóm sinh viên, nhóm nên tập trung vào các nhánh có rủi ro cao, các bug đã từng phát sinh và các luồng ảnh hưởng trực tiếp đến người dùng.

**Kết luận tổng quát:**

- Architecture Coverage: **Khá** đối với các feature chính, nhưng còn thiếu ở AI và Content.
- Branch Coverage: **Trung bình - Khá**, tốt ở Auth/Admin/Recruitment/Candidate, yếu ở AI/Content/Config.
- Hướng cải thiện: bổ sung regression test cho bug thực tế và mock test cho external service.
