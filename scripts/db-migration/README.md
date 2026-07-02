# Hướng dẫn migrate PostgreSQL sang Supabase

Thư mục này chứa script export/import database PostgreSQL cũ sang Supabase PostgreSQL mới. Không commit file `.env` hoặc file backup `.sql` vì các file này có thể chứa mật khẩu và dữ liệu thật.

## Bước 1: Cài PostgreSQL client

Cài PostgreSQL client để có hai lệnh:

- `pg_dump`
- `psql`

Sau khi cài, mở PowerShell mới và kiểm tra:

```powershell
pg_dump --version
psql --version
```

Nếu PowerShell báo `pg_dump is not recognized` hoặc `psql is not recognized`, nghĩa là PostgreSQL client chưa được cài hoặc thư mục `bin` chưa nằm trong `PATH`. Trên Windows, thư mục này thường có dạng:

```text
C:\Program Files\PostgreSQL\<version>\bin
```

Sau khi thêm vào `PATH`, hãy đóng PowerShell cũ, mở PowerShell mới rồi kiểm tra lại hai lệnh trên.

## Bước 2: Tạo file .env từ .env.example

Trong thư mục `scripts/db-migration`, copy file mẫu:

```powershell
Copy-Item .env.example .env
```

## Bước 3: Điền thông tin database cũ và Supabase mới

Mở file `.env` và điền:

- `OLD_DB_HOST`, `OLD_DB_PORT`, `OLD_DB_NAME`, `OLD_DB_USER`, `OLD_DB_PASSWORD`
- `SUPABASE_DB_HOST`, `SUPABASE_DB_PORT`, `SUPABASE_DB_NAME`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`

Với Supabase, lấy thông tin PostgreSQL connection trong Project Settings hoặc Database settings. Không đưa connection string, password, token vào source code.

## Bước 4: Chạy export_old_db.ps1

Từ thư mục `scripts/db-migration`, chạy:

```powershell
.\export_old_db.ps1
```

Script sẽ tạo file:

```text
careermate_backup.sql
```

File này được export bằng `pg_dump` với `--no-owner --no-acl` và `sslmode=require` để giảm lỗi role/owner khi import sang Supabase.

## Bước 5: Chạy import_to_supabase.ps1

Sau khi export thành công, import vào Supabase:

```powershell
.\import_to_supabase.ps1
```

Script dùng `psql`, bật `ON_ERROR_STOP=on`, và kết nối với `sslmode=require`.

## Bước 6: Kiểm tra bảng trong Supabase Table Editor

Vào Supabase Dashboard, mở Table Editor và kiểm tra:

- Các bảng đã được tạo đầy đủ.
- Dữ liệu quan trọng đã có.
- Sequence/id tự tăng hoạt động đúng nếu bảng có cột auto increment.
- Các constraint và foreign key không bị lỗi.

## Bước 7: Đổi Render Environment Variables sang Supabase mới

Trong Render service của backend, đổi các biến mới:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=<supabase-user>
SPRING_DATASOURCE_PASSWORD=<supabase-password>
```

Đồng thời đảm bảo các secret khác cũng nằm trong Environment Variables, ví dụ:

- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `GEMINI_API_KEYS`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

Sau đó deploy lại backend và kiểm tra log startup.
