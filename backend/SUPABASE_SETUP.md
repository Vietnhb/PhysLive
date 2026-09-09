# Supabase setup

Backend kết nối Supabase bằng PostgreSQL/JPA.

## 1. Khai báo biến môi trường

PowerShell:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="postgres"
$env:SUPABASE_DB_PASSWORD="<database-password>"
$env:JWT_SECRET="<local-jwt-secret>"
```

Nếu dùng Supabase pooler port `6543`, thêm `&prepareThreshold=0` vào URL.

Không dùng `anon key` hoặc `service_role key` làm mật khẩu PostgreSQL.

## 2. Chạy backend

```powershell
.\mvnw.cmd spring-boot:run
```

Lần chạy đầu sẽ tạo/cập nhật bảng `roles`, `users` và thêm role `GUEST`.

## 3. Cấu hình production

Sau khi schema ổn định, đặt:

```powershell
$env:JPA_DDL_AUTO="validate"
```

Thông tin thật chỉ đặt trong environment/local config; không commit `application.properties`.
