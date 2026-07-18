# Báo cáo Phân tích Giá trị Biên (BVA) - AdminService

1. **Phần 1:** Thiết kế test cho API Admin có thật bằng Robust BVA `6n + 1` kết hợp phân hoạch tương đương.
2. **Phần 2:** Áp dụng BVA cho các logic thực tế của Admin Dashboard.

---

# PHẦN 1: API ADMIN TẠO NGƯỜI DÙNG

## 1.1. Mô tả API

- Endpoint: `POST /api/admin/users`.
- Chỉ tài khoản có quyền `ADMIN` được gọi.
- Dữ liệu được kiểm tra bởi `CreateAdminUserRequest` trước khi vào service.

| Biến đầu vào | Miền hợp lệ theo source |
|---|---|
| `fullName` | Không `null`, không blank, tối đa 100 ký tự |
| `email` | Không `null`, đúng định dạng, tối đa 100 ký tự và chưa tồn tại |
| `password` | `null` để tự sinh hoặc có độ dài từ 6 đến 72 ký tự |
| `userRole` | Một giá trị hợp lệ của enum `UserRole` |

## 1.2. Phân hoạch lớp tương đương

| Biến | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| `fullName` | Không blank, dài 1-100 | V1 | `null`, rỗng hoặc chỉ có dấu cách | X1 |
|  |  |  | Dài trên 100 | X2 |
| `email` | Đúng định dạng, dài tối đa 100, chưa tồn tại | V2 | `null` hoặc blank | X3 |
|  |  |  | Sai định dạng | X4 |
|  |  |  | Dài trên 100 | X5 |
|  |  |  | Email đã tồn tại | X6 |
| `password` | `null`, hệ thống tự sinh mật khẩu | V3 | Dài 0-5 | X7 |
|  | Dài 6-72 | V4 | Dài trên 72 | X8 |
| `userRole` | Giá trị có trong `UserRole` | V5 | `null` | X9 |
|  |  |  | Giá trị không tồn tại như `SUPER_ADMIN` | X10 |

## 1.3. Áp dụng công thức Robust BVA `6n + 1`

Chỉ có hai biến có đủ biên dưới và biên trên để áp dụng BVA:

1. `fullNameLength`: miền `[1, 100]`.
2. `passwordLength`: miền `[6, 72]` khi mật khẩu khác `null`.

`email` là dữ liệu định dạng và `userRole` là enum nên được kiểm tra bằng lớp tương đương, không tính vào `n`.

```text
n = 2
Số test BVA = 6n + 1 = 6 × 2 + 1 = 13 test case
```

| Biến | min- | min | min+ | nominal | max- | max | max+ |
|---|---:|---:|---:|---:|---:|---:|---:|
| `fullNameLength` | 0 | 1 | 2 | 50 | 99 | 100 | 101 |
| `passwordLength` | 5 | 6 | 7 | 39 | 71 | 72 | 73 |

Quy ước:

- `FN(n)`: họ tên không blank có đúng `n` ký tự.
- `PW(n)`: mật khẩu không blank có đúng `n` ký tự.
- Các trường không được thay đổi trong một test dùng giá trị nominal: `FN(50)`, email hợp lệ chưa tồn tại, `PW(39)`, vai trò `CANDIDATE`.

## 1.4. Bảng 13 test case Robust BVA theo `6n + 1`

| TC | Trường được kiểm tra | Giá trị | Kết quả mong đợi | Tag |
|---:|---|---|---|---|
| 1 | Tất cả nominal | `FN(50)`, `PW(39)` | Hợp lệ | N |
| 2 | `fullNameLength` | 0 (`min-`) | Từ chối | B1, X1 |
| 3 | `fullNameLength` | 1 (`min`) | Hợp lệ | B2, V1 |
| 4 | `fullNameLength` | 2 (`min+`) | Hợp lệ | B3, V1 |
| 5 | `fullNameLength` | 99 (`max-`) | Hợp lệ | B4, V1 |
| 6 | `fullNameLength` | 100 (`max`) | Hợp lệ | B5, V1 |
| 7 | `fullNameLength` | 101 (`max+`) | Từ chối | B6, X2 |
| 8 | `passwordLength` | 5 (`min-`) | Từ chối | B7, X7 |
| 9 | `passwordLength` | 6 (`min`) | Hợp lệ | B8, V4 |
| 10 | `passwordLength` | 7 (`min+`) | Hợp lệ | B9, V4 |
| 11 | `passwordLength` | 71 (`max-`) | Hợp lệ | B10, V4 |
| 12 | `passwordLength` | 72 (`max`) | Hợp lệ | B11, V4 |
| 13 | `passwordLength` | 73 (`max+`) | Từ chối | B12, X8 |

Mỗi test chỉ thay đổi một biến; các biến còn lại giữ ở giá trị nominal. Vì vậy bảng trên đúng cấu trúc `6n + 1` và đồng thời bao phủ các lớp ngay ngoài biên.

## 1.5. Test case bổ sung từ lớp tương đương

| TC | Dữ liệu kiểm tra | Kết quả mong đợi | Tag |
|---:|---|---|---|
| 14 | `email = null` | Từ chối | X3 |
| 15 | `email = "abc"` | Từ chối vì sai định dạng | X4 |
| 16 | Email hợp lệ dài 101 ký tự | Từ chối | X5 |
| 17 | Email đã tồn tại nhưng viết hoa | Từ chối sau khi service chuyển về chữ thường | X6 |
| 18 | `password = null` | Hợp lệ, tự sinh mật khẩu dài 12 ký tự | V3 |
| 19 | `userRole = null` | Từ chối | X9 |
| 20 | `userRole = "SUPER_ADMIN"` | Từ chối | X10 |

---

# PHẦN 2: BVA CHO LOGIC THỰC TẾ TRONG ADMIN DASHBOARD

## 2.1. Logic số ngày của biểu đồ ứng tuyển

Trong `ApplicationByDay.getApplicationsChart(int days)`:

```java
if (days <= 0) days = 7;
```

| TC | `days` đầu vào | Giá trị thực tế | Số phần tử trả về | Kết quả |
|---:|---:|---:|---:|---|
| 1 | -1 | 7 | 7 | Dưới biên, dùng mặc định |
| 2 | 0 | 7 | 7 | Tại biên, dùng mặc định |
| 3 | 1 | 1 | 1 | Trên biên 1 đơn vị |

## 2.2. Logic giới hạn hoạt động gần đây

Trong `RecentActivityService.getRecentActivities(int limit)`:

```java
if (limit <= 0) limit = 5;
```

| TC | `limit` đầu vào | Giới hạn thực tế | Kết quả mong đợi |
|---:|---:|---:|---|
| 1 | -1 | 5 | Trả tối đa 5 hoạt động |
| 2 | 0 | 5 | Trả tối đa 5 hoạt động |
| 3 | 1 | 1 | Trả tối đa 1 hoạt động |

Với `limit = 5` và `N` là số bản ghi trong cơ sở dữ liệu:

| `N` | Số lượng trả về | Biên |
|---:|---:|---|
| 4 | 4 | `max-` |
| 5 | 5 | `max` |
| 6 | 5 | `max+` |

## 2.3. Logic hiển thị thời gian tương đối

Hàm `toTimeAgoVi(createdAt, now)` có các biên 1 phút, 60 phút và 24 giờ.

| TC | Khoảng thời gian | Kết quả mong đợi | Biên |
|---:|---|---|---|
| 1 | 59 giây | `Vừa xong` | Trước 1 phút |
| 2 | 60 giây | `1 phút trước` | Tại 1 phút |
| 3 | 59 phút 59 giây | `59 phút trước` | Trước 60 phút |
| 4 | 60 phút | `1 giờ trước` | Tại 60 phút |
| 5 | 23 giờ 59 phút 59 giây | `23 giờ trước` | Trước 24 giờ |
| 6 | 24 giờ | `1 ngày trước` | Tại 24 giờ |
