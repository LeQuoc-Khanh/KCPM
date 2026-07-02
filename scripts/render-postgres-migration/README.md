# Hướng dẫn migrate PostgreSQL cũ sang Render PostgreSQL

Thư mục này dùng để export database PostgreSQL cũ và import sang Render PostgreSQL mới trên Windows bằng PowerShell.

Không commit file `.env` hoặc file backup `.sql` vì các file này có thể chứa mật khẩu và dữ liệu thật.

## Bước 1: Cài PostgreSQL client để có pg_dump và psql

Máy cần có PostgreSQL client tools:

```powershell
pg_dump --version
psql --version
```

Nếu PowerShell báo không tìm thấy lệnh, hãy cài PostgreSQL client tools. Trên Windows, các lệnh này thường nằm trong:

```text
C:\Program Files\PostgreSQL\<version>\bin
```

Sau khi cài hoặc thêm vào `PATH`, đóng PowerShell cũ và mở PowerShell mới.

## Bước 2: Tạo Render PostgreSQL trên Render

Vào Render Dashboard và tạo PostgreSQL database mới. Chờ database chuyển sang trạng thái sẵn sàng.

## Bước 3: Copy External Database URL hoặc lấy host/user/password/database từ Render PostgreSQL

Khi migrate từ máy local, hãy dùng thông tin external connection của Render PostgreSQL:

- Host
- Port
- Database
- Username
- Password

Bạn cũng có thể copy External Database URL rồi tách các thành phần này để điền vào `.env`.

## Bước 4: Copy .env.example thành .env

Trong thư mục này, chạy:

```powershell
Copy-Item .env.example .env
```

## Bước 5: Điền thông tin database cũ và Render PostgreSQL mới vào .env

Mở file `.env` và điền:

```text
OLD_DB_HOST=
OLD_DB_PORT=5432
OLD_DB_NAME=
OLD_DB_USER=
OLD_DB_PASSWORD=
OLD_DB_SSLMODE=require

RENDER_DB_HOST=
RENDER_DB_PORT=5432
RENDER_DB_NAME=
RENDER_DB_USER=
RENDER_DB_PASSWORD=
RENDER_DB_SSLMODE=require
```

Nếu database cũ hoặc Render không yêu cầu SSL, bạn có thể đổi `sslmode` theo cấu hình thật, ví dụ `disable`. Không đưa mật khẩu hoặc connection string thật vào source code.

## Bước 6: Chạy export_old_db.ps1

```powershell
.\export_old_db.ps1
```

Script sẽ tạo file:

```text
careermate_backup.sql
```

File này được tạo bằng `pg_dump` với `--no-owner --no-acl` để tránh lỗi owner/role khi import sang Render PostgreSQL.

## Bước 7: Chạy import_to_render_postgres.ps1

```powershell
.\import_to_render_postgres.ps1
```

Script dùng `psql` để import `careermate_backup.sql` vào Render PostgreSQL.

## Bước 8: Kết nối Render PostgreSQL bằng VS Code Database extension hoặc psql để kiểm tra bảng

Sau khi import xong, kiểm tra:

- Các bảng đã có đầy đủ.
- Dữ liệu quan trọng đã được import.
- Sequence/id tự tăng hoạt động đúng.
- Foreign key và constraint không báo lỗi.

## Bước 9: Cấu hình Render backend Web Service Environment

Vào Render backend Web Service → Environment và thêm:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://INTERNAL_RENDER_DB_HOST:5432/RENDER_DB_NAME
SPRING_DATASOURCE_USERNAME=RENDER_DB_USER
SPRING_DATASOURCE_PASSWORD=RENDER_DB_PASSWORD
```

Nên dùng internal host cho backend Render nếu backend và database cùng nằm trên Render.

## Bước 10: Redeploy backend

Redeploy backend Web Service. Sau đó test API register/login và các API quan trọng của hệ thống.
