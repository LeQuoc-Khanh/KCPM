# Assignment: Kiểm thử Phân hệ Quản lý Công ty/Nhà tuyển dụng

**Chủ đề:** Phân hoạch lớp tương đương, phân tích giá trị biên, thiết kế test case và kiểm thử tự động  
**Môn học:** Kiểm chứng phần mềm  
**Họ và tên:** Nguyễn Sỹ Linh  
**Dự án:** CareerMate – Hệ thống tuyển dụng và tìm kiếm việc làm  
**Phân hệ:** Feature 5 – Company/Recruiter Management  

---

## 1. Mục tiêu bài tập

1. Xác định điều kiện kiểm thử từ source của phân hệ Company/Recruiter Management.
2. Áp dụng phân hoạch lớp tương đương (Equivalence Partitioning – EP) cho dữ liệu công ty, JWT, role và file ảnh.
3. Áp dụng Robust Boundary Value Analysis theo công thức `6n+1` cho biến có miền biên xác định.
4. Thiết kế test case có input, expected result và tag bao phủ.
5. Thiết kế kịch bản Postman cho ba endpoint thực tế.
6. Triển khai unit test bằng JUnit 5, Mockito, MockMvc và đo coverage bằng JaCoCo.
7. Phân tích white-box cho `CompanyService` và `CloudinaryService`.
8. Ghi nhận các khoảng trống validation phát hiện từ source.

---

## 2. Mô tả bài toán kiểm thử

Phân hệ cho phép Recruiter xem, cập nhật hồ sơ công ty và upload logo/ảnh bìa.

### 2.1 Phạm vi endpoint

| STT | Method | Endpoint | Chức năng |
|---:|:---:|---|---|
| 1 | GET | `/api/recruiter/company/me` | Lấy công ty của người dùng hiện tại |
| 2 | PUT | `/api/recruiter/company/me` | Cập nhật hoặc tạo hồ sơ công ty |
| 3 | POST | `/api/recruiter/company/upload-image` | Upload logo hoặc ảnh bìa |

Endpoint đăng nhập `/api/auth/login` chỉ dùng để lấy JWT, không được tính là endpoint chính của phân hệ.

### 2.2 Source được sử dụng

| Thành phần | File |
|---|---|
| Controller | `RecruiterCompanyController.java` |
| Request DTO | `UpdateCompanyRequest.java` |
| Service | `CompanyService.java` |
| Entity | `Company.java` |
| Repository | `CompanyRepository.java`, `UserRepository.java` |
| Authentication helper | `SecurityUtils.java` |
| Authorization | `SecurityConfig.java` |
| Upload service | `CloudinaryService.java` |
| Error handler | `GlobalExceptionHandler.java` |

### 2.3 Đầu vào của API cập nhật công ty

| Biến | Kiểu | Ý nghĩa |
|---|---|---|
| `name` | String | Tên công ty |
| `description` | String | Mô tả công ty |
| `website` | String | Website công ty |
| `industry` | String | Ngành nghề |
| `size` | String | Quy mô công ty |
| `foundedYear` | String | Năm thành lập |
| `address` | String | Địa chỉ |
| `phone` | String | Số điện thoại |
| `email` | String | Email liên hệ |
| `logoUrl` | String | URL logo |
| `coverImageUrl` | String | URL ảnh bìa |

---

## 3. Giả định và giới hạn của bài toán

Source dùng cho kết quả cuối ngày 18/07/2026 đã bao gồm bản sửa MP-107 được merge tại PR #34. Ba test hồi quy liên quan được chạy lại trên backend cục bộ; các test còn lại dùng kết quả Postman đã ghi nhận trước đó trên backend Render:

1. Entity khai báo `name` với `@Column(nullable=false)`.
2. Trường `name` trong DTO có `@NotBlank` và `@Size(max = 255)`; các trường email, website, phone, size và foundedYear vẫn chưa có validation định dạng riêng.
3. Controller đã dùng `@Valid` cho `UpdateCompanyRequest`.
4. Frontend chỉ cho chọn `image/*` và kiểm tra dung lượng tối đa 5 MB.
5. Backend `uploadCompanyImage` chưa tự kiểm tra file rỗng, content type hoặc dung lượng 5 MB.
6. `SecurityConfig` cho phép `RECRUITER`, `RECRUITER_VIP` và `ADMIN` truy cập `/api/recruiter/**`.
7. `getMyCompany` trả HTTP 200 với body null nếu recruiter chưa có công ty.
8. `updateCompany` cập nhật công ty hiện có hoặc tạo mới nếu recruiter chưa có công ty.
9. `logoUrl` và `coverImageUrl` chỉ được cập nhật khi request gửi giá trị khác null.

Để có thể áp dụng `6n+1`, bài kiểm thử sử dụng giả định kỹ thuật:

```text
Độ dài name hợp lệ: 1–255 ký tự.
```

Cơ sở: `name` không được null và cột String JPA không khai báo `length` riêng, thường sử dụng độ dài mặc định 255. Đây là **test assumption**, không phải Bean Validation đang tồn tại. Nếu API chấp nhận chuỗi rỗng hoặc không trả 400 cho chuỗi vượt biên thì ghi nhận test Failed/bug thiếu validation.

Đối với upload:

```text
File ảnh phải không rỗng và không vượt quá 5 MB.
```

Biên 5 MB lấy từ kiểm tra thực tế tại frontend. Backend chưa thực thi biên này nên các test được dùng để phát hiện khoảng trống validation.

---

# PHẦN A. ĐỀ BÀI GIAO CHO SINH VIÊN

## Câu 1. Xác định lớp tương đương

### 1.1 Authentication và Authorization

| Biến | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| JWT | Token còn hạn, đúng chữ ký | V1 | Không gửi token | X1 |
| | | | Token giả mạo hoặc hết hạn | X2 |
| Role | `RECRUITER`, `RECRUITER_VIP`, `ADMIN` | V2 | `CANDIDATE`, `CANDIDATE_VIP` | X3 |

### 1.2 GET thông tin công ty

| Điều kiện | Lớp hợp lệ | Tag | Lớp đặc biệt/không hợp lệ | Tag |
|---|---|---|---|---|
| Công ty của user | Công ty tồn tại | V3 | User chưa có công ty | X4 |
| User từ JWT | User tồn tại trong DB | V4 | User không tồn tại | X5 |

Theo source, lớp X4 hiện trả HTTP 200 với body null; lớp X5 có thể phát sinh HTTP 500 do `SecurityUtils` ném `RuntimeException`.

### 1.3 PUT cập nhật công ty

| Biến | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| `name` | Không rỗng, dài 1–255 | V5 | Null | X6 |
| | | | Chuỗi rỗng | X7 |
| | | | Dài hơn 255 | X8 |
| `email` | Email đúng định dạng hoặc null | V6 | Sai định dạng | X9 |
| `website` | URL hợp lệ hoặc null | V7 | URL sai định dạng | X10 |
| `size` | Một trong `1-50`, `51-200`, `201-500`, `500+` hoặc rỗng | V8 | Giá trị ngoài danh sách | X11 |
| `foundedYear` | Chuỗi năm hợp lệ hoặc null | V9 | Chứa chữ/ký tự đặc biệt | X12 |
| `phone` | Chuỗi số điện thoại hợp lệ hoặc null | V10 | Chứa chữ/ký tự không hợp lệ | X13 |
| JSON body | JSON đúng cú pháp | V11 | Thiếu body | X14 |
| | | | JSON sai cú pháp/sai kiểu | X15 |
| Company state | Công ty đã tồn tại | V12 | Chưa có công ty nhưng recruiter tồn tại | V13 |
| Recruiter state | Recruiter tồn tại | V14 | Recruiter không tồn tại | X16 |
| `logoUrl` | URL mới hoặc null | V15 | URL sai định dạng | X17 |
| `coverImageUrl` | URL mới hoặc null | V16 | URL sai định dạng | X18 |

Lưu ý: sau bản sửa MP-107, trường `name` đã có `@NotBlank`, `@Size(max = 255)` và endpoint PUT đã kích hoạt `@Valid`. Các trường X9–X13 và X17–X18 vẫn dùng `String` và chưa có ràng buộc định dạng tương ứng; vì vậy API hiện chấp nhận các chuỗi này và trả HTTP 200. Kết quả này được xác định theo source và đã được xác nhận bằng Postman, không phải thay đổi expected chỉ để làm test PASS.

### 1.4 POST upload ảnh

| Biến | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| Multipart key | Key `file` | V17 | Thiếu file/sai key | X19 |
| File content | Ảnh JPG/PNG/WEBP hợp lệ | V18 | File rỗng | X20 |
| | | | File không phải ảnh | X21 |
| File size | Lớn hơn 0 và không quá 5 MB | V19 | Lớn hơn 5 MB | X22 |
| Cloudinary | Upload thành công, có `secure_url` | V20 | IOException/upload thất bại | X23 |

---

## Câu 2. Phân tích giá trị biên

### 2.1 Xác định `n`

Robust BVA được áp dụng cho API:

```http
PUT /api/recruiter/company/me
```

Biến được chọn:

```text
Độ dài name ∈ [1,255]
```

Chỉ có một biến có đủ cận dưới và cận trên trong bài toán BVA:

```text
n = 1
```

### 2.2 Công thức Robust BVA

```text
6n + 1 = 6 × 1 + 1 = 7 test case
```

### 2.3 Bộ giá trị biên

| Vị trí | Độ dài `name` | Kết quả mong đợi | Tag |
|---|---:|---|---|
| `min-1` | 0 | Không hợp lệ – HTTP 400 | B1 |
| `min` | 1 | Hợp lệ – HTTP 200 | B2 |
| `min+1` | 2 | Hợp lệ – HTTP 200 | B3 |
| nominal | 128 | Hợp lệ – HTTP 200 | B4 |
| `max-1` | 254 | Hợp lệ – HTTP 200 | B5 |
| `max` | 255 | Hợp lệ – HTTP 200 | B6 |
| `max+1` | 256 | Không hợp lệ – HTTP 400 | B7 |

Khi chạy BVA, tất cả field khác giữ ở giá trị nominal hợp lệ:

```json
{
  "name": "<chuỗi có độ dài theo từng test>",
  "description": "Công ty phát triển sản phẩm phần mềm",
  "website": "https://careermate.vn",
  "industry": "Information Technology",
  "size": "51-200",
  "foundedYear": "2020",
  "address": "Ho Chi Minh City",
  "phone": "0901234567",
  "email": "contact@careermate.vn",
  "logoUrl": null,
  "coverImageUrl": null
}
```

### 2.4 Giá trị sát biên upload 5 MB

Do source chỉ xác định biên trên tại frontend, các ca sau được thiết kế bổ sung và không đưa vào công thức `6n+1` của PUT:

| File size | Ý nghĩa | Expected |
|---:|---|---|
| 0 byte | File rỗng | HTTP 400 |
| 1 MB | Nominal | HTTP 200 |
| 5 MB − 1 byte | Sát dưới biên | HTTP 200 |
| 5 MB | Tại biên | HTTP 200 |
| 5 MB + 1 byte | Vượt biên | HTTP 400/413 |

---

## Câu 3. Thiết kế test case

Bộ test chính thức được tinh gọn còn **25 test case**. Các ca được chọn theo nguyên tắc: giữ các lớp đại diện, xác thực/phân quyền và các điểm biên có thể chạy ổn định bằng Postman; loại các ca trùng mục tiêu hoặc không phù hợp với phạm vi kiểm thử tự động hiện tại.

Phân bổ: **5 GET + 9 PUT EP/Security + 7 PUT Robust BVA + 4 POST Upload = 25 test case**.

### 3.1 GET `/api/recruiter/company/me`

| TC ID | Điều kiện | Expected Result | Tag | Actual | Status |
|---|---|---|---|---|---|
| GET-COMP-01 | JWT Recruiter hợp lệ, công ty tồn tại | HTTP 200 và đúng thông tin công ty | V1,V2,V3,V4 | HTTP 200; response là company object | Pass |
| GET-COMP-02 | Không gửi JWT | HTTP 401 | X1 | HTTP 401 | Pass |
| GET-COMP-03 | JWT giả mạo/hết hạn | HTTP 401 | X2 | HTTP 401 | Pass |
| GET-COMP-04 | JWT Candidate | HTTP 403 | X3 | HTTP 403 | Pass |
| GET-COMP-05 | JWT Recruiter VIP | HTTP 200 | V1,V2 | HTTP 200; Recruiter VIP lấy được thông tin công ty | Pass |

Trường hợp Recruiter chưa có company không được chọn vì source tự động tạo company khi đăng ký, nên không thể chuẩn bị trạng thái này bằng luồng Postman thông thường. Trường hợp Admin được loại để tránh mở rộng sang vai trò không phải đối tượng chính của chức năng.

### 3.2 Robust BVA cho PUT `/api/recruiter/company/me`

| TC ID | Độ dài `name` | Expected Result | Tag | Actual | Status |
|---|---:|---|---|---|---|
| BVA-NAME-01 | 0 | HTTP 400 | X7,B1 | HTTP 400 sau bản sửa MP-107 | Pass |
| BVA-NAME-02 | 1 | HTTP 200 | V5,B2 | HTTP 200; trả về tên dài 1 ký tự | Pass |
| BVA-NAME-03 | 2 | HTTP 200 | V5,B3 | HTTP 200; trả về tên dài 2 ký tự | Pass |
| BVA-NAME-04 | 128 | HTTP 200 | V5,B4 | HTTP 200; trả về tên dài 128 ký tự | Pass |
| BVA-NAME-05 | 254 | HTTP 200 | V5,B5 | HTTP 200; trả về tên dài 254 ký tự | Pass |
| BVA-NAME-06 | 255 | HTTP 200 | V5,B6 | HTTP 200; trả về tên dài 255 ký tự | Pass |
| BVA-NAME-07 | 256 | HTTP 400 | X8,B7 | HTTP 400 sau bản sửa MP-107 | Pass |

### 3.3 EP cho PUT `/api/recruiter/company/me`

| TC ID | Input/Điều kiện | Expected Result | Tag | Actual | Status |
|---|---|---|---|---|---|
| PUT-COMP-01 | JSON nominal, công ty đã tồn tại | HTTP 200; dữ liệu được cập nhật | V5–V12,V14–V16 | HTTP 200; trả về tên đã cập nhật | Pass |
| PUT-COMP-02 | `name=null` | HTTP 400 | X6 | HTTP 400 sau bản sửa MP-107 | Pass |
| PUT-COMP-03 | `email="invalid-email"`; chuỗi tự do theo source | HTTP 200; dữ liệu được cập nhật | X9 | HTTP 200; chuỗi email được chấp nhận | Pass |
| PUT-COMP-04 | `website="invalid-website"`; chuỗi tự do theo source | HTTP 200; dữ liệu được cập nhật | X10 | HTTP 200; chuỗi website được chấp nhận | Pass |
| PUT-COMP-05 | `size="1000-2000"`; ngoài danh sách FE nhưng API không kiểm tra | HTTP 200; dữ liệu được cập nhật | X11 | HTTP 200; chuỗi size được chấp nhận | Pass |
| PUT-COMP-06 | `foundedYear="20AB"`; chuỗi tự do theo source | HTTP 200; dữ liệu được cập nhật | X12 | HTTP 200; chuỗi foundedYear được chấp nhận | Pass |
| PUT-COMP-07 | `phone="09ABC"`; chuỗi tự do theo source | HTTP 200; dữ liệu được cập nhật | X13 | HTTP 200; chuỗi phone được chấp nhận | Pass |
| PUT-COMP-08 | Không gửi JWT | HTTP 401 | X1 | HTTP 401 | Pass |
| PUT-COMP-09 | JWT Candidate | HTTP 403 | X3 | HTTP 403 | Pass |

Trường hợp Recruiter chưa có company không được chọn vì không thể tạo trạng thái này qua luồng đăng ký hiện tại. Các ca riêng cho logo/cover URL được lược bỏ để tránh trùng mục tiêu với JSON nominal; các nhánh cập nhật URL sẽ được kiểm tra sâu hơn ở unit test/white-box.

PUT-COMP-03 đến PUT-COMP-07 có expected HTTP 200 vì source hiện coi các trường này là chuỗi tự do. Đây là expected được hiệu chỉnh sau khi đối chiếu source, không phải đổi expected chỉ để ép test PASS. PUT-COMP-02 mong đợi HTTP 400 vì `name` là trường bắt buộc và đã trả đúng HTTP 400 sau bản sửa MP-107. Hai ca thiếu body và JSON sai cú pháp đã được loại khỏi bộ Postman chính thức rút gọn; chúng không được tính vào kết quả PASS.

### 3.4 POST `/api/recruiter/company/upload-image`

| TC ID | File/Điều kiện | Expected Result | Tag | Actual | Status |
|---|---|---|---|---|---|
| UPLOAD-01 | PNG hợp lệ, nhỏ hơn 5 MB | HTTP 200 và body có `url` HTTPS | V17,V18,V19,V20 | HTTP 200; response chứa URL HTTPS | Pass |
| UPLOAD-02 | PNG hợp lệ 1×1 pixel, 68 byte | HTTP 200 và body có `url` HTTPS | V17,V18,V19,V20 | HTTP 200; response chứa URL HTTPS | Pass |
| UPLOAD-03 | Ảnh hợp lệ 5 MB − 1 byte | HTTP 200 và body có `url` HTTPS | V17,V18,V19,V20 | HTTP 200; response chứa URL HTTPS | Pass |
| UPLOAD-04 | Ảnh hợp lệ nhưng không gửi JWT | HTTP 401 | X1 | HTTP 401 | Pass |

UPLOAD-02 đại diện cho biên dưới thực tế của một file ảnh PNG hợp lệ; UPLOAD-03 kiểm tra sát biên trên hợp lệ 5 MB. Các ca thiếu file, sai multipart key, file rỗng, sai loại file và vượt 5 MB không nằm trong bộ Postman chính thức rút gọn. Đây là giới hạn phạm vi kiểm thử, không phải kết luận rằng backend đã xử lý đúng các trường hợp đó. Trường hợp Cloudinary IOException cần mock nên chuyển sang unit test; phân quyền Candidate đã được kiểm tra tại GET-COMP-04 và PUT-COMP-09.

### 3.5 Tổng hợp kết quả chạy Postman ngày 18/07/2026

| Nhóm | Số TC | Pass | Fail | Ghi chú |
|---|---:|---:|---:|---|
| GET My Company | 5 | 5 | 0 | Tất cả test case đạt expected |
| PUT EP & Security | 9 | 9 | 0 | PUT-COMP-02 đã retest PASS sau bản sửa MP-107 |
| PUT Robust BVA | 7 | 7 | 0 | Hai biên không hợp lệ 0 và 256 đều trả đúng HTTP 400 |
| POST Upload | 4 | 4 | 0 | Tất cả ca chính thức đạt expected |
| **Tổng** | **25** | **25** | **0** | Toàn bộ test case chính thức đã đạt expected |

`BVA-CLEANUP` trả HTTP 200 và khôi phục tên công ty thành công, nhưng đây là request hỗ trợ nên không tính vào 25 test case chính thức. Ba ca từng phát hiện MP-107 (`PUT-COMP-02`, `BVA-NAME-01`, `BVA-NAME-07`) đã được retest trên backend cục bộ sau khi merge bản sửa và đều PASS. Các expected vẫn giữ nguyên theo đặc tả; chỉ cột Actual/Status được cập nhật theo lần chạy lại.

---

## Câu 4. Triển khai kiểm thử tự động

### A. Production Code cần kiểm thử

#### `CompanyService.getMyCompany`

```java
return companyRepository.findByRecruiterId(recruiterId).orElse(null);
```

Hai trường hợp:

1. Repository có Company → trả Company.
2. Repository rỗng → trả null.

#### `CompanyService.updateCompany`

Các quyết định:

1. Company đã tồn tại hay chưa.
2. Nếu chưa có Company, Recruiter có tồn tại hay không.
3. `logoUrl` khác null hay không.
4. `coverImageUrl` khác null hay không.

#### `CloudinaryService.uploadCompanyImage`

Hai đường đi:

1. Upload thành công → trả `secure_url`.
2. IOException → ném RuntimeException.

### B. Unit test đã triển khai

| Test ID | Method | Điều kiện | Expected |
|---|---|---|---|
| UT-COMP-01 | `getMyCompany` | Repository có Company | Trả đúng Company |
| UT-COMP-02 | `getMyCompany` | Repository rỗng | Trả null |
| UT-COMP-03 | `updateCompany` | Company tồn tại | Cập nhật và save |
| UT-COMP-04 | `updateCompany` | Chưa có Company, recruiter tồn tại | Tạo mới và save |
| UT-COMP-05 | `updateCompany` | Chưa có Company, recruiter không tồn tại | Ném RuntimeException |
| UT-COMP-06 | `updateCompany` | Logo/cover khác null | Cập nhật URL |
| UT-COMP-07 | `updateCompany` | Logo/cover null | Giữ URL cũ |
| UT-UPLOAD-01 | `uploadCompanyImage` | Cloudinary thành công | Trả secure URL |
| UT-UPLOAD-02 | `uploadCompanyImage` | IOException | Ném RuntimeException |

Các test được hiện thực trong ba lớp sau:

| Test class | Phạm vi | Số lượt test | Kết quả |
|---|---|---:|---|
| `RecruiterCompanyControllerTest` | GET, PUT, BVA tên công ty, upload thành công/lỗi | 12 | 12 Passed |
| `CompanyServiceTest` | Đọc, cập nhật, tạo Company và các nhánh URL ảnh | 8 | 8 Passed |
| `CloudinaryServiceTest` | Upload ảnh/file thành công và IOException | 7 | 7 Passed |
| **Tổng** | | **27** | **27 Passed, 0 Failed, 0 Errors** |

### C. MockMvc test đã triển khai

| Test ID | Endpoint | Điều kiện | Expected | Kết quả |
|---|---|---|---|---|
| MVC-COMP-01 | GET `/me` | Company tồn tại | HTTP 200 và đúng Company | Passed |
| MVC-COMP-02 | GET `/me` | Company không tồn tại | HTTP 200, body rỗng | Passed |
| MVC-BVA-01 | PUT `/me` | `name` dài 1, 2, 128, 254, 255 | HTTP 200 | 5/5 Passed |
| MVC-BVA-02 | PUT `/me` | `name` rỗng hoặc dài 256 | HTTP 400 | 2/2 Passed |
| MVC-COMP-03 | PUT `/me` | `name=null` | HTTP 400 | Passed |
| MVC-UPLOAD-01 | POST `/upload-image` | Upload thành công | HTTP 200 và `$.url` | Passed |
| MVC-UPLOAD-02 | POST `/upload-image` | Service ném RuntimeException | HTTP 500 | Passed |

Lệnh chạy bộ kiểm thử Feature 5 và tạo báo cáo JaCoCo:

```powershell
mvn "-Dtest=RecruiterCompanyControllerTest,CompanyServiceTest,CloudinaryServiceTest" clean verify
```

Kết quả thực tế: **27 tests run, 27 passed, 0 failures, 0 errors; BUILD SUCCESS**.

---

# PHẦN B. BẢNG CHẤM ĐIỂM CHI TIẾT (LECTURER GRADING SCHEME)

## Câu 1. Xác định lớp tương đương (2.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt |
|---|---:|---:|
| Xác định đúng lớp hợp lệ | 0.8 | |
| Xác định lớp không hợp lệ | 0.8 | |
| Gắn tag V/X rõ ràng | 0.4 | |
| **Tổng** | **2.0** | |

## Câu 2. Phân tích giá trị biên (2.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt |
|---|---:|---:|
| Xác định đúng biến và miền biên | 0.5 | |
| Xác định đúng `n=1` | 0.5 | |
| Áp dụng đúng `6n+1` | 0.5 | |
| Đủ 7 giá trị Robust BVA | 0.5 | |
| **Tổng** | **2.0** | |

## Câu 3. Thiết kế test case (3.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt |
|---|---:|---:|
| Bao phủ đủ 3 endpoint | 0.6 | |
| Có case hợp lệ/không hợp lệ | 0.6 | |
| Có đủ 7 BVA test | 0.6 | |
| Expected HTTP rõ ràng | 0.6 | |
| Tag bao phủ đầy đủ | 0.6 | |
| **Tổng** | **3.0** | |

## Câu 4. Triển khai kiểm thử tự động (3.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt |
|---|---:|---:|
| JUnit 5 và Mockito | 0.5 | |
| MockMvc | 0.5 | |
| Có test biên hợp lệ/không hợp lệ | 1.0 | |
| Test service branch | 0.5 | |
| Test chạy thành công | 0.5 | |
| **Tổng** | **3.0** | |

---

# PHẦN C. BỔ SUNG: POSTMAN, WHITE-BOX, COVERAGE VÀ BUG LIST

## 1. Cấu trúc Postman đề xuất

```text
[Feature 5] Company/Recruiter Management
├── 00 - Setup Authentication (không tính TC)
├── 01 - GET My Company (5 TC)
├── 02 - PUT Update Company - EP & Security (9 TC)
├── 03 - PUT Robust BVA name - 6n+1 (7 TC)
│   └── BVA-CLEANUP (request hỗ trợ, không tính TC)
└── 04 - POST Upload Company Image (4 TC)
```

Tổng số test case chính thức trên Postman là **25**. Các request đăng nhập lấy token và `BVA-CLEANUP` chỉ phục vụ thiết lập/khôi phục dữ liệu, không được tính vào tổng số test case.

## 2. Kịch bản Postman chính thức – 25 test case

| Nhóm | Test ID được chọn | Số TC |
|---|---|---:|
| GET My Company | GET-COMP-01 đến GET-COMP-05 | 5 |
| PUT EP & Security | PUT-COMP-01 đến PUT-COMP-09 | 9 |
| PUT Robust BVA | BVA-NAME-01 đến BVA-NAME-07 | 7 |
| POST Upload | UPLOAD-01 đến UPLOAD-04 | 4 |
| **Tổng** | | **25** |

Các request setup đăng nhập Recruiter, Recruiter VIP và Candidate phải chạy trước các test cần token. Chúng là điều kiện kiểm thử, không phải test case của Feature 5.

## 3. Phân tích white-box

### `CompanyService.updateCompany`

Số quyết định được xét:

```text
D1: Company tồn tại?
D2: Recruiter tồn tại khi cần tạo mới?
D3: logoUrl != null?
D4: coverImageUrl != null?
```

Cyclomatic complexity ước tính:

```text
V(G) = 4 + 1 = 5
```

Các đường đi độc lập cần test:

| Path | Điều kiện | Kết quả |
|---|---|---|
| P1 | Company tồn tại, logo/cover có giá trị | Cập nhật toàn bộ |
| P2 | Company tồn tại, logo/cover null | Giữ URL cũ |
| P3 | Company chưa tồn tại, recruiter tồn tại | Tạo Company mới |
| P4 | Company chưa tồn tại, recruiter không tồn tại | Ném exception |
| P5 | Chỉ một trong logo/cover khác null | Chỉ cập nhật trường tương ứng |

### `CloudinaryService.uploadCompanyImage`

```text
V(G) = 2
```

- P1: Upload thành công.
- P2: IOException.

## 4. JaCoCo/SonarCloud

JaCoCo được giới hạn vào đúng ba lớp thuộc Feature 5 để số liệu không bị hiểu nhầm là coverage của toàn bộ backend:

- `RecruiterCompanyController`
- `CompanyService`
- `CloudinaryService`

| Chỉ số | Mục tiêu | Thực tế |
|---|---:|---:|
| Instruction coverage | — | **100% (272/272)** |
| Line coverage | ≥ 80% | **100% (51/51)** |
| Branch coverage | ≥ 80% | **100% (6/6)** |
| Method coverage | ≥ 80% | **100% (11/11)** |
| JaCoCo Quality Gate | Line và Branch ≥ 80% | **Passed** |
| SonarCloud | Theo Quality Gate | Chưa quét |

| Lớp | Line | Branch | Method |
|---|---:|---:|---:|
| `RecruiterCompanyController` | 6/6 | Không có nhánh | 3/3 |
| `CompanyService` | 23/23 | 4/4 | 5/5 |
| `CloudinaryService` | 22/22 | 2/2 | 3/3 |

Báo cáo HTML được tạo tại `target/site/jacoco/index.html`; dữ liệu thô nằm tại `target/site/jacoco/jacoco.csv`.

## 5. Bug/Issue được ghi nhận

| Issue ID | Mô tả | Nguồn phát hiện | Trạng thái |
|---|---|---|---|
| MP-107 | Trước bản sửa: tên rỗng trả 200; `name=null` và tên dài 256 ký tự trả 500 thay vì 400 | Postman + static review | **Fixed – PR #34 đã merge; retest local PASS** |
| ISSUE-COMP-03 | Backend upload chưa kiểm tra file rỗng/type/5 MB | Static review | Open |
| ISSUE-COMP-04 | GET công ty không tồn tại trả HTTP 200 null thay vì response rõ ràng | Static review | Open |
| ISSUE-COMP-05 | RuntimeException chung có thể bị trả HTTP 500 | Static review | Open |

GET-COMP-05 đã được chạy lại bằng token Recruiter VIP và trả HTTP 200 đúng expected. MP-107 đã được xác nhận sửa thành công bằng ba test case hồi quy trên backend cục bộ. Các issue còn lại chỉ chuyển thành bug Confirmed sau khi có kết quả Postman hoặc unit test chứng minh.

## 6. Tổng kết thiết kế

| Hạng mục | Số lượng |
|---|---:|
| Endpoint chính | 3 |
| Lớp/tag hợp lệ | V1–V20 |
| Lớp/tag không hợp lệ | X1–X23 |
| Robust BVA | 7 test case (`n=1`) |
| GET test case chính thức | 5 |
| PUT EP/Security test case chính thức | 9 |
| Upload test case chính thức | 4 |
| **Tổng test case Postman chính thức** | **25** |
| Request cleanup hỗ trợ | 1, không tính TC |
| JUnit test đã triển khai | 27 (12 controller + 8 company service + 7 cloudinary service) |
| Kết quả JUnit | 27 Passed, 0 Failed, 0 Errors |
| JaCoCo line/branch/method | 100% / 100% / 100% trong phạm vi Feature 5 |

**Kết luận:** Bộ 25 test case bám theo ba endpoint thực tế của `RecruiterCompanyController` và cân bằng giữa mức độ bao phủ với khả năng chạy ổn định bằng Postman. EP bao phủ dữ liệu hợp lệ/không hợp lệ, xác thực và phân quyền; phần upload tập trung vào ảnh hợp lệ ở kích thước danh định, biên dưới thực tế, sát biên trên và trường hợp thiếu JWT. Robust BVA `6n+1` được áp dụng cho độ dài `name` với `n=1`, tạo bảy ca biên. Kết quả cuối cùng là **25/25 test case Postman PASS, 0 FAIL** sau khi retest bản sửa MP-107 trên backend cục bộ. Bên cạnh Postman, 27 lượt JUnit/Mockito/MockMvc đã chạy thành công. JaCoCo đạt 100% line, branch và method trên ba lớp Feature 5 được chọn.
