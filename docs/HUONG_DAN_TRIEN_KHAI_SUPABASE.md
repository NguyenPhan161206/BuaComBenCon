# Hướng dẫn Triển khai và Kiểm thử Supabase cho Bữa Cơm Bên Con

Tài liệu này hướng dẫn cách thiết lập, chạy migration, nạp dữ liệu mẫu (seed), kiểm thử nghiệp vụ và kết nối ứng dụng Android với hệ thống Supabase local.

---

## 1. Yêu cầu môi trường

- **Docker Desktop** (đã khởi chạy).
- **Supabase CLI** (cài đặt qua npm hoặc Scoop/Homebrew):
  ```bash
  npm install -g supabase
  # hoặc trên Windows Scoop: scoop bucket add supabase https://github.com/supabase/scoop-bucket.git && scoop install supabase
  ```
- **Android Studio & Android SDK** (compileSdk 36, Java 11/17).

---

## 2. Cấu trúc thư mục Supabase

```text
supabase/
├── config.toml                 # Cấu hình môi trường local, Auth, Inbucket (Mailpit)
├── migrations/
│   ├── 20261124000001_initial_schema.sql       # Bảng, khóa ngoại, ràng buộc, index
│   ├── 20261124000002_rls_policies.sql         # Chính sách bảo mật Row-Level Security theo 4 vai trò
│   └── 20261124000003_business_logic_rpcs.sql  # Giao dịch RPC xếp suất, hủy, cấp lại, chống giao trùng
├── seed.sql                    # Dữ liệu mẫu kịch bản MVP: 35 hồ sơ cho 30 suất đợt c1, các tài khoản demo
└── tests/
    └── 01_business_rules_test.sql              # Bộ test kiểm tra 5 ca nghiệp vụ khắt khe
```

---

## 3. Khởi chạy Supabase Local

1. Mở terminal tại thư mục gốc của dự án:
   ```bash
   cd C:\Users\QuynhNhu\Downloads\BuaComBenCon-main\BuaComBenCon-main
   ```

2. Khởi động các container Supabase (PostgreSQL, GoTrue Auth, PostgREST, Studio, Mailpit):
   ```bash
   supabase start
   ```

   Khi khởi động thành công, terminal sẽ hiển thị các URL:
   - **Studio URL:** `http://127.0.0.1:54323`
   - **API URL:** `http://127.0.0.1:54321`
   - **DB URL:** `postgresql://postgres:postgres@127.0.0.1:54322/postgres`
   - **Inbucket (Mailpit Email testing):** `http://127.0.0.1:54324`
   - **anon key & service_role key:** Hiển thị trên màn hình console.

3. Áp dụng migrations và seed dữ liệu mẫu:
   ```bash
   supabase db reset
   ```
   Lệnh này tự động thực thi toàn bộ migration theo thứ tự thời gian và chạy `seed.sql`.

---

## 4. Tài khoản Demo đã cấu hình sẵn trong Seed Data

Mật khẩu mặc định cho tất cả tài khoản demo là: `MatKhau@123`

| Vai trò | Email đăng nhập | Số điện thoại | Quyền hạn chính |
| --- | --- | --- | --- |
| **Người chăm sóc** | `giadinh@buacombencon.vn` | `0900000013` | Tự đăng ký, xem đăng ký của mình, lấy mã nhận, hủy suất |
| **Điều phối viên** | `dieuphoi@buacombencon.vn` | `0900000001` | Tạo đợt, cập nhật chỉ tiêu, đăng ký hộ, xét duyệt, phân bổ, cấp lại suất |
| **Tình nguyện viên**| `tinhnguyen@buacombencon.vn`| `0900000002` | Quét/nhập mã nhận, kiểm tra tính hợp lệ, xác nhận giao nhận 1 lần |
| **Nhà hảo tâm** | `nhahaotam@buacombencon.vn` | `0900000003` | Gửi cam kết suất ăn theo đợt/tuần/tháng, xem báo cáo minh bạch |

---

## 5. Chạy kiểm thử nghiệp vụ (Business Rules Tests)

Thực thi test script SQL trong thư mục `tests/`:

```bash
# Sử dụng psql kết nối tới database local
psql -h 127.0.0.1 -p 54322 -U postgres -d postgres -f supabase/tests/01_business_rules_test.sql
```

Kết quả mong đợi:
```text
NOTICE:  PASS: Test Case 1 (Bổ sung muộn) thành công.
NOTICE:  PASS: Test Case 2 (Trùng mốc) thành công.
NOTICE:  PASS: Test Case 3 (Xét sau lượt đầu) thành công.
NOTICE:  PASS: Test Case 4 (Cấp lại suất) thành công.
NOTICE:  PASS: Test Case 5 (Chống giao nhận trùng lặp) thành công.
ROLLBACK
```

---

## 6. Kết nối từ Android Emulator vào Supabase Local

1. Khi chạy ứng dụng trên **Android Emulator**, địa chỉ máy host cục bộ được ánh xạ qua:
   - `http://10.0.2.2:54321` (đã được cấu hình trong `SupabaseConfig.DEFAULT_URL`).
2. Nếu chạy trên **thiết bị thật qua Wi-Fi**, đổi IP trong `SupabaseConfig.kt` thành IP LAN của máy tính (ví dụ: `http://192.168.1.x:54321`).
3. Chuyển đổi giữa chế độ Demo và Supabase:
   ```kotlin
   // Trong code khởi tạo hoặc cài đặt
   repositoryProvider.mode = DataSourceMode.SUPABASE
   ```

---

## 7. Sao lưu và Khôi phục Database (Backup & Restore)

- **Sao lưu:**
  ```bash
  supabase db dump -f backup_buacombencon_$(date +%Y%m%d).sql
  ```
- **Khôi phục vào database mới:**
  ```bash
  psql -h 127.0.0.1 -p 54322 -U postgres -d postgres -f backup_buacombencon_YYYYMMDD.sql
  ```
