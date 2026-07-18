# Postman automated test - Feature 7

## Chuẩn bị

1. Chạy backend tại `http://localhost:8080`.
2. Database phải có một Candidate và một Company hợp lệ.
3. Import `feature7.postman_collection.json` và chọn Environment **KCPM - Khánh**.
4. Collection dùng trực tiếp `api_base_url`, `candidate_email`, `candidate_password`, `candidate_token` và `candidate_refresh_token` từ Environment này.
5. Sửa collection variable `companyId` nếu company dùng để test không có ID `1`.

## Chạy trên Postman

Chọn collection **KCPM - Feature 7 Automated Tests**, bấm **Run collection**, chọn environment **KCPM - Khánh** và giữ nguyên thứ tự request.

Folder VIP phải chạy cuối vì endpoint nâng cấp làm thay đổi role, expiration và token của user. `REV-03` tạo dữ liệu thật; chạy collection lần hai với cùng Candidate/Company sẽ khiến request này gặp duplicate. Muốn chạy lặp lại độc lập, dùng Candidate khác, Company khác hoặc xóa review test khỏi database trước lần chạy mới.

Nếu Candidate không có notification, `NOT-02` được đánh dấu skipped; cần tạo notification trước để chạy case này.

## Chạy bằng Newman

```powershell
newman run feature7.postman_collection.json -e feature7.local.postman_environment.json
```

Xuất báo cáo JSON:

```powershell
newman run feature7.postman_collection.json -e feature7.local.postman_environment.json -r cli,json --reporter-json-export feature7-newman-report.json
```

## Lưu ý về characterization test

- `NOT-04` xác nhận hành vi hiện tại: notification ID không tồn tại vẫn trả `200`.
- `REV-04` xác nhận hành vi hiện tại: duplicate review trả `500`.
- `GAM-04` xác nhận SecurityConfig hiện cho mọi user đã đăng nhập truy cập system log.

Các case trên pass nghĩa là hành vi hiện tại được tái hiện đúng, không có nghĩa hành vi đó đáp ứng yêu cầu nghiệp vụ mong muốn.
