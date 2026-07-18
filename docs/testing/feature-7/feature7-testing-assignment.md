# Assignment: Kiểm thử Phân hệ Feature 7 - Notification, Review, Gamification & VIP

**Chủ đề:** Phân hoạch lớp tương đương, phân tích giá trị biên, thiết kế test case và kiểm thử tự động  
**Môn học:** Kiểm chứng phần mềm  
**Họ và tên:** _Điền họ tên sinh viên_  
**MSSV:** _Điền mã số sinh viên_  
**Dự án áp dụng:** KCPM-CareerMate  
**Baseline đã xác minh:** Branch `test/extra-features`, commit `0839171557f45294f4351f061875da757688067f`  
**Thời điểm báo cáo test/coverage:** 14/07/2026, khoảng 10:35 (UTC+7)

---

## 1. Mục tiêu bài tập

1. Xác định được **điều kiện kiểm thử** từ source code và các API của Notification, Review, Gamification/Leaderboard và Payment/VIP.
2. Áp dụng **phân hoạch lớp tương đương** để chia dữ liệu đầu vào thành lớp hợp lệ và không hợp lệ.
3. Áp dụng **phân tích giá trị biên (Boundary Value Analysis - BVA)** cho daily limit và ngày hết hạn VIP.
4. Thiết kế bảng **test case** có input, expected outcome và tag bao phủ.
5. Viết và đánh giá mã kiểm thử tự động bằng **JUnit 5, Mockito, Spring Test và Postman**.
6. Đo lường coverage bằng **JaCoCo** và chỉ công bố kết quả có bằng chứng thực tế.

---

## 2. Mô tả bài toán kiểm thử

Feature 7 của CareerMate gồm bốn nhóm chức năng backend:

| Module | Chức năng | Package chính |
|---|---|---|
| Notification | Lấy danh sách, mark-read, read-all, xóa, gửi và cleanup notification | `app.notification.*` |
| Company Review | Tạo review, lấy review theo company, tính rating trung bình | `app.review.*` |
| Gamification | Ghi điểm, daily mission, leaderboard, rank và system log | `app.gamification.*` |
| Payment/VIP | Nâng cấp hoặc gia hạn VIP trực tiếp | `app.payment.*` |

Các biến và điều kiện đầu vào đã xác nhận từ source:

| Biến đầu vào | Ý nghĩa | Kiểu dữ liệu | Miền/điều kiện đã xác nhận |
|---|---|---|---|
| `notificationId` | ID notification cần đọc/xóa | `Long` | ID tồn tại; thao tác xóa yêu cầu thuộc user hiện tại |
| `companyId` | Công ty được review | `Long` | Company phải tồn tại |
| `rating` | Điểm đánh giá | `Integer` | DTO/service hiện chưa có validation |
| `comment` | Nội dung review | `String` | Chưa khai báo giới hạn độ dài |
| `role` | Nhóm leaderboard | `String` | Candidate/Recruiter; hậu tố `_VIP` được chuẩn hóa |
| `period` | Kỳ xếp hạng | `String` | `WEEK`, `MONTH`, `YEAR`, `ALL_TIME` được service xử lý |
| `limit` | Số dòng leaderboard/log | `int` | Có default nhưng chưa có min/max validation |
| `currentCount` | Số lần thực hiện action trong ngày | `long` | So sánh với `dailyLimit` của action |
| `refId` | ID tham chiếu point event | `Long` | Có thể `null`; nếu đã tồn tại thì event bị bỏ qua |
| `vipExpirationDate` | Ngày hết hạn VIP | `LocalDateTime` | Có thể `null`, hết hạn hoặc còn hạn |

Các API hiện có:

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/notifications` | Lấy notification của user hiện tại |
| PUT | `/api/notifications/{id}/read` | Đánh dấu một notification đã đọc |
| PUT | `/api/notifications/read-all` | Đánh dấu tất cả đã đọc |
| DELETE | `/api/notifications/{id}` | Xóa notification |
| POST | `/api/reviews` | Tạo review |
| GET | `/api/reviews/company/{companyId}` | Danh sách review công ty |
| GET | `/api/reviews/company/{companyId}/average` | Rating trung bình |
| GET | `/api/leaderboard` | Bảng xếp hạng |
| GET | `/api/leaderboard/me` | Rank theo `userId` |
| GET | `/api/leaderboard/missions` | Mission theo role/user |
| GET | `/api/leaderboard/logs` | System point logs |
| POST | `/api/payment/vip-upgrade` | Nâng cấp/gia hạn VIP trực tiếp |

Hệ thống hiện trả về:

- `200 OK` cho các controller path thành công.
- `401 Unauthorized` hoặc `403 Forbidden` khi API bảo vệ không nhận được xác thực hợp lệ.
- `400 Bad Request` khi Admin gọi nâng cấp VIP.
- Một số exception nghiệp vụ chưa có mapping riêng có thể trả `500 Internal Server Error`, ví dụ duplicate review.

---

## 3. Giả định của bài toán

Để tránh hiểu nhầm, bài tập sử dụng các nguyên tắc sau:

1. Expected result được lấy từ source hiện tại hoặc contract đã được xác nhận.
2. `rating=1..5`, `comment<=2000` và `limit=1..100` là các miền thường được đề xuất nhưng chưa được production code thực thi; không xem là yêu cầu chính thức.
3. `Designed` không đồng nghĩa `Executed` hoặc `Passed`.
4. Characterization test pass chỉ có nghĩa hành vi hiện tại được tái hiện, không khẳng định hành vi đó đúng nghiệp vụ.
5. Kết quả Surefire và JaCoCo trong tài liệu được tổng hợp trực tiếp từ report của baseline nêu trên.
6. Payment hiện chưa có transaction, gateway, chữ ký callback hoặc idempotent webhook.

---

# PHẦN A. ĐỀ BÀI GIAO CHO SINH VIÊN

---

## Câu 1. Xác định lớp tương đương

Hãy xác định các lớp tương đương hợp lệ và không hợp lệ:

| Biến/điều kiện | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| **Notification khi xóa** | ID tồn tại và thuộc current user | **V1** | ID không tồn tại | **X1** |
| | | | ID thuộc user khác | **X2** |
| **Notification khi mark-read** | ID tồn tại | **V2** | ID không tồn tại | **X3** |
| **User tạo review** | User tồn tại | **V3** | User không tồn tại | **X4** |
| **Company được review** | Company tồn tại | **V4** | Company không tồn tại | **X5** |
| **Duplicate review** | Chưa review company | **V5** | Đã review cùng company | **X6** |
| **Rating** | DTO/DB chấp nhận | **V6** | Chưa xác định chính xác do thiếu validation | **X7** |
| **Role nhận điểm** | Candidate/Recruiter, gồm VIP | **V7** | `ADMIN`, `UNKNOWN` | **X8** |
| **Daily action** | `currentCount < dailyLimit` | **V8** | `currentCount >= dailyLimit` | **X9** |
| **refId** | `null` hoặc chưa tồn tại | **V9** | Khác `null` và đã tồn tại | **X10** |
| **VIP role** | Candidate/Recruiter hoặc role `_VIP` | **V10** | `ADMIN` hoặc role không ánh xạ được | **X11** |
| **JWT Access Token** | Token hợp lệ, còn hạn | **V11** | Không có token | **X12** |
| | | | Token sai/hết hạn | **X13** |

---

## Câu 2. Phân tích giá trị biên

### A. Daily limit

| Action | Điểm | Daily limit |
|---|---:|---:|
| `LOGIN_DAILY` | 5 | 1 |
| `APPLY` | 10 | 3 |
| `JOB_POST_APPROVED` | 20 | 2 |
| `INTERVIEW_PRACTICE` | 15 | 1 |
| `UPLOAD_CV` | 20 | 1 |
| `REVIEW_CV` | 5 | 10 |
| `HIRED` | 50 | 5 |

Với một action có daily limit `L`:

| Giá trị | Vị trí biên | Expected theo source | Tag |
|---:|---|---|---|
| `L-1` | Ngay dưới biên | Event tiếp theo được ghi log và cập nhật điểm | **B1** |
| `L` | Tại biên | Không save log, không upsert score | **B2** |
| `L+1` | Trên biên | Không save log, không upsert score | **B3** |

### B. VIP expiration

| Trạng thái trước khi gọi | Expected theo source | Tag |
|---|---|---|
| Role thường | Đổi sang `_VIP`, expiration xấp xỉ `now+30 ngày` | **B4** |
| VIP, expiration `null` | Giữ role VIP, expiration xấp xỉ `now+30 ngày` | **B5** |
| VIP, expiration trước `now` | Tính lại từ thời điểm hiện tại | **B6** |
| VIP, expiration bằng `now` | Nhánh `isAfter(now)` không thỏa; tính lại từ thời điểm xử lý | **B7** |
| VIP, expiration sau `now` | Cộng 30 ngày vào expiration hiện tại | **B8** |

*Ghi chú:* Rating, comment length và limit chưa có biên chính thức trong production code nên chưa đưa vào expected HTTP bắt buộc.

---

## Câu 3. Thiết kế test case

| STT | Tên test case | Input/Precondition | Kết quả mong đợi | Tag bao phủ |
|---:|---|---|---|---|
| 1 | **NOT-01:** Lấy notification có token | Candidate token hợp lệ | `200`, response là array | V11 |
| 2 | **NOT-02:** Lấy notification không token | Không có Authorization | `401/403` | X12 |
| 3 | **NOT-03:** Xóa notification của mình | ID tồn tại, đúng owner | `200`, record bị xóa | V1 |
| 4 | **NOT-04:** Xóa notification user khác | ID thuộc user B, token A | Exception, không xóa | X2 |
| 5 | **NOT-05:** Mark-read ID tồn tại | Notification unread | `200`, `isRead=true` | V2 |
| 6 | **NOT-06:** Mark-read ID không tồn tại | ID Long không có trong DB | Hiện trả `200`, không save | X3 |
| 7 | **REV-01:** Tạo review | User/company tồn tại, chưa duplicate | `200`, lưu review | V3,V4,V5 |
| 8 | **REV-02:** User không tồn tại | User ID không có | Exception, không save | X4 |
| 9 | **REV-03:** Company không tồn tại | Company ID không có | Exception, không save | X5 |
| 10 | **REV-04:** Duplicate review | Cặp user-company đã tồn tại | Hiện trả `500`, DB giữ một review | X6 |
| 11 | **GAM-01:** Dưới daily limit | `currentCount=L-1` | Save log, upsert 4 period | B1,V8 |
| 12 | **GAM-02:** Tại daily limit | `currentCount=L` | Không save/upsert | B2,X9 |
| 13 | **GAM-03:** Trên daily limit | `currentCount=L+1` | Không save/upsert | B3,X9 |
| 14 | **GAM-04:** Duplicate refId | Ref đã tồn tại | Không save/upsert | X10 |
| 15 | **GAM-05:** Role Admin | `ADMIN` | Không ghi điểm | X8 |
| 16 | **VIP-01:** Nâng cấp Candidate | Candidate token hợp lệ | `200`, role thành `CANDIDATE_VIP` | B4,V10,V11 |
| 17 | **VIP-02:** Gia hạn VIP còn hạn | Expiration sau `now` | Expiration cũ cộng 30 ngày | B8,V10 |
| 18 | **VIP-03:** Gia hạn VIP hết hạn | Expiration trước `now` | Expiration mới khoảng `now+30 ngày` | B6,V10 |
| 19 | **VIP-04:** Admin upgrade | Admin token hợp lệ | `400`, không đổi role | X11 |
| 20 | **VIP-05:** Không token | Không Authorization | `401/403` | X12 |

---

## Câu 4. Triển khai kiểm thử tự động

### A. Cấu trúc logic nghiệp vụ phân hệ (Production Code)

```java
// LeaderboardService.java
long currentCount = logRepo.countActionsToday(userId, action.name(), startOfToday);
if (currentCount >= action.getDailyLimit()) {
    return;
}

if (refId != null
        && logRepo.existsByUserIdAndActionTypeAndRefId(userId, action.name(), refId)) {
    return;
}
```

```java
// ReviewService.java
if (reviewRepository.existsByUserIdAndCompanyId(userId, request.getCompanyId())) {
    throw new RuntimeException("Bạn đã đánh giá công ty này rồi!");
}
```

```java
// NotificationService.java
public void markAsRead(Long notificationId) {
    Notification notif = notificationRepository.findById(notificationId).orElse(null);
    if (notif != null) {
        notif.setRead(true);
        notificationRepository.save(notif);
    }
}
```

`markAsRead` không nhận current user ID nên chưa có ownership check cho thao tác này.

```java
// PaymentController.java - tóm tắt nhánh expiration
if (currentRoleName.endsWith("_VIP")) {
    if (user.getVipExpirationDate() != null
            && user.getVipExpirationDate().isAfter(now)) {
        newExpirationDate = user.getVipExpirationDate().plusDays(30);
    } else {
        newExpirationDate = now.plusDays(30);
    }
} else {
    newRole = UserRole.valueOf(currentRoleName + "_VIP");
    newExpirationDate = now.plusDays(30);
}
```

### B. Triển khai mã kiểm thử tự động Unit Test (JUnit 5 & Mockito)

Test nằm tại:

- `BE/src/test/java/app/notification/**`
- `BE/src/test/java/app/review/**`
- `BE/src/test/java/app/gamification/**`
- `BE/src/test/java/app/payment/**`

Ví dụ test biên daily limit:

```java
@Test
void handlePointEvent_shouldNotSaveOrUpdateScore_whenDailyLimitReached() {
    Long userId = 1L;
    UserPointAction action = UserPointAction.LOGIN_DAILY;
    PointEvent event = new PointEvent(this, userId, "CANDIDATE", action, null);

    when(logRepo.countActionsToday(eq(userId), eq(action.name()), any(OffsetDateTime.class)))
            .thenReturn((long) action.getDailyLimit());

    leaderboardService.handlePointEvent(event);

    verify(logRepo, never()).save(any(LeaderboardPointsLog.class));
    verify(scoreRepo, never())
            .upsertScore(anyLong(), anyString(), anyString(), anyString(), anyInt());
}
```

Ví dụ test notification không tồn tại:

```java
@Test
void markAsRead_shouldNotSave_whenNotificationDoesNotExist() {
    Long notificationId = 999L;
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

    notificationService.markAsRead(notificationId);

    verify(notificationRepository, never()).save(any(Notification.class));
}
```

Postman collection tự động:

- `docs/testing/feature-7/postman/feature7.postman_collection.json`
- Dùng Environment `KCPM - Khánh` với `api_base_url`, `candidate_email`, `candidate_password`, `candidate_token`.

---

# PHẦN B. BẢNG CHẤM ĐIỂM CHI TIẾT (LECTURER GRADING SCHEME)

---

## Câu 1. Xác định lớp tương đương (2.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt được |
|---|---:|---:|
| Xác định đúng lớp hợp lệ cho các biến/điều kiện | 0.8 | |
| Xác định đúng lớp không hợp lệ | 0.4 | |
| Phân biệt source behavior và yêu cầu đề xuất | 0.4 | |
| Gắn tag rõ ràng (`V`, `X`) | 0.4 | |
| **Tổng điểm Câu 1** | **2.0** | |

---

## Câu 2. Phân tích giá trị biên (2.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt được |
|---|---:|---:|
| Xác định đúng `L-1`, `L`, `L+1` của daily limit | 0.8 | |
| Xác định đúng các trạng thái expiration VIP | 0.8 | |
| Không khẳng định biên chưa có contract | 0.4 | |
| **Tổng điểm Câu 2** | **2.0** | |

---

## Câu 3. Thiết kế test case (3.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt được |
|---|---:|---:|
| Có case hợp lệ và không hợp lệ cho bốn module | 0.5 | |
| Có test tại biên daily limit/VIP expiration | 0.5 | |
| Input và precondition rõ ràng | 0.5 | |
| Expected result kèm HTTP status khi xác định được | 0.5 | |
| Tag bao phủ đầy đủ | 0.5 | |
| Phân biệt Designed/Executed/Passed/Characterization | 0.5 | |
| **Tổng điểm Câu 3** | **3.0** | |

---

## Câu 4. Triển khai kiểm thử tự động (3.0 điểm)

| Tiêu chí | Điểm tối đa | Điểm đạt được |
|---|---:|---:|
| Dùng đúng JUnit 5, Mockito và Spring Test | 0.5 | |
| Phủ các nhánh service/controller chính | 1.0 | |
| Có Postman automated test và assertion | 0.5 | |
| Test chạy có log làm bằng chứng | 0.5 | |
| Có báo cáo JaCoCo thực tế | 0.5 | |
| **Tổng điểm Câu 4** | **3.0** | |

---

# PHẦN C. BỔ SUNG: BẢNG KIỂM THỬ TÍCH HỢP & PHÂN TÍCH CHẤT LƯỢNG MÃ NGUỒN

---

## 1. Kiểm thử tích hợp hộp đen (Black-box Integration - Postman)

Collection gồm 16 request tự động. Kết quả chỉ được ghi Passed khi request có đúng token, fixture và biến ID. `401` ở test thiếu token là kết quả mong đợi; `401` ở test hợp lệ là lỗi cấu hình/xác thực của lần chạy.

| STT | API / Path | Method | Mục tiêu | Expected | Trạng thái xác minh |
|---:|---|:---:|---|---|---|
| 1 | `/api/auth/login` | POST | Đăng nhập và lưu Candidate token | `200` | Đã quan sát Passed trên Postman |
| 2 | `/api/notifications` | GET | Không token | `401/403` | Đã quan sát `401`, Passed |
| 3 | `/api/notifications` | GET | Token hợp lệ, lấy danh sách | `200`, array | Chỉ kết luận sau khi header Bearer được gửi đúng |
| 4 | `/api/notifications/{id}/read` | PUT | Mark-read ID tồn tại | `200` | Cần `notificationId` dạng Long; thiếu fixture thì skip |
| 5 | `/api/notifications/read-all` | PUT | Mark-read tất cả | `200` | Designed |
| 6 | `/api/reviews/company/{companyId}` | GET | Lấy review public | `200`, array | Designed |
| 7 | `/api/reviews` | POST | Tạo review | `200` | Cần user/company chưa duplicate |
| 8 | `/api/reviews` | POST | Duplicate review | Hiện `500` | Characterization |
| 9 | `/api/leaderboard` | GET | Lấy leaderboard | `200` | Designed |
| 10 | `/api/leaderboard/me` | GET | Lấy rank | `200` | Designed |
| 11 | `/api/leaderboard/missions` | GET | Lấy mission | `200` | Designed |
| 12 | `/api/leaderboard/logs` | GET | Candidate truy cập log | Hiện `200` theo SecurityConfig | Characterization |
| 13 | `/api/payment/vip-upgrade` | POST | Token hợp lệ, nâng VIP | `200` | Token phải còn hạn và đúng deployment |
| 14 | `/api/payment/vip-upgrade` | POST | Không token | `401/403` | Đã quan sát `401`, Passed |

Các lỗi `{{notificationId}}` chưa resolve, token thiếu/hết hạn hoặc review fixture bị trùng phải được sửa ở dữ liệu/Environment trước khi xem là bug sản phẩm.

---

## 2. Báo cáo đo lường độ bao phủ (JaCoCo) & Phân tích tĩnh (SonarCloud)

Kết quả được đọc trực tiếp từ `BE/target/surefire-reports/TEST-*.xml` và `BE/target/site/jacoco/jacoco.csv`:

| Module | Unit test Passed | Instruction | Branch | Line | Method |
|---|---:|---:|---:|---:|---:|
| Notification | 18/18 | 100,0% | 100,0% | 100,0% | 100,0% |
| Review | 13/13 | 96,6% | 100,0% | 97,6% | 100,0% |
| Gamification | 24/24 | 94,5% | 91,1% | 95,8% | 95,7% |
| Payment/VIP | 7/7 | 95,7% | 87,5% | 95,2% | 100,0% |
| **Tổng Feature 7** | **62/62** | **95,7%** | **92,1%** | **96,7%** | **97,2%** |

Integration test Java: 12 discovered, 11 passed, 1 skipped. Test bị skip là luồng point event ghi score cho bốn period vì H2 không thực thi native query PostgreSQL `ON CONFLICT`.

| Chỉ số chất lượng | Kết quả |
|---|---|
| Surefire Unit Test | 62/62 Passed |
| Surefire Integration Test | 11 Passed, 1 Skipped, 0 Failed/Error |
| JaCoCo Line Coverage | 96,7% |
| JaCoCo Branch Coverage | 92,1% |
| SonarCloud | Chưa có report Feature 7 đủ căn cứ trong workspace; không công bố số liệu |

---

## 3. Nhật ký theo dõi và sửa lỗi (Bug List)

| Bug ID tạm | Tóm tắt | Bằng chứng | Phân cấp đề xuất | Trạng thái |
|---|---|---|:---:|:---:|
| **OBS-F7-01** | User A mark-read notification của User B | Source và integration characterization test | Critical | Confirmed, chưa sửa |
| **OBS-F7-02** | Rating không có validation | `ReviewRequest`/service | Major | Confirmed by code review |
| **OBS-F7-03** | `limit` không có min/max validation | Leaderboard controller/service | Minor | Confirmed by code review |
| **OBS-F7-04** | `/me` và `/missions` nhận `userId` từ request | Controller source | Major | Cần đánh giá authorization thực tế |
| **OBS-F7-05** | VIP được cấp trực tiếp, chưa có payment gateway/callback | Payment controller | Critical nếu triển khai production | Confirmed by code review |
| **OBS-F7-06** | Duplicate review trả HTTP 500 | Integration characterization test | Major | Confirmed, contract status cần xác nhận |

Các ID trên là mã quan sát nội bộ, không phải Jira ID. Chỉ tạo Jira ticket và gán severity chính thức sau khi Product Owner xác nhận contract.

---

## 4. Bảng tổng kết kết quả kiểm thử (Summary)

| Hạng mục kiểm thử | Số lượng/Kết quả | Trạng thái thực tế |
|---|---:|---|
| Unit Test Feature 7 | 62 | 62 Passed |
| Integration Test Java | 12 | 11 Passed, 1 Skipped |
| Postman automated requests | 16 | Script đã tạo; cần chạy lại với token và fixture hợp lệ để chốt toàn bộ |
| JaCoCo Line Coverage | 96,7% | Đã xác minh từ CSV |
| JaCoCo Branch Coverage | 92,1% | Đã xác minh từ CSV |
| Quan sát/rủi ro | 6 | 4 confirmed bằng code/test, 2 cần contract/authorization review thêm |
| SonarCloud Feature 7 | Chưa xác minh | Không công bố số liệu |

**Kết luận:** Unit test và coverage đạt mức cao trên baseline đã xác minh. Chưa nên kết luận toàn bộ Feature 7 `GO` chỉ dựa vào coverage vì còn một integration test bị skip, Postman runner cần dữ liệu/token đúng, và ownership của `markAsRead` đã được xác nhận là rủi ro bảo mật.
