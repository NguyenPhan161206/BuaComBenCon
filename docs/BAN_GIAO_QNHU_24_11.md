# Nghiệm thu Supabase và Android Studio trên qnhu-24/11

Người tiếp nhận lấy `main` mới nhất, chạy dự án trên máy của mình và đưa các bản sửa lên `qnhu-24/11`. Công việc hoàn tất khi cả database và ứng dụng chạy thành công theo các tiêu chí dưới đây.

## 1. Lấy mã nguồn

```bash
git fetch origin
git switch qnhu-24/11
git merge --ff-only origin/qnhu-24/11
git merge origin/main
```

Mốc đã gộp vào `main` là `94e2d12`. Tại mốc này, Gradle, SQL, Supabase Auth và Mailpit đã qua kiểm tra. Lần thử trước chưa cài được APK vì emulator chưa hoàn tất khởi động. [Kết quả QA](QA.md#nhánh-mvc-và-supabase)

## 2. Chạy database Supabase local

1. Khởi động Docker. Tại thư mục gốc của repo, chạy `npx --yes supabase@3.3.0 start` rồi `npx --yes supabase@3.3.0 status`.
2. Xác nhận ba migration và seed đã chạy. Kiểm tra Studio hiển thị dữ liệu mẫu và chạy bộ kiểm thử bằng lệnh sau.

```bash
psql -X -v ON_ERROR_STOP=1 postgresql://postgres:postgres@127.0.0.1:54322/postgres \
  -f supabase/tests/01_business_rules_test.sql
```

Database đạt khi stack khởi động, seed có dữ liệu và lệnh kiểm thử hoàn thành thành công. Nếu cần nạp lại từ đầu, chỉ dùng `db reset` trên stack local dành riêng cho dữ liệu thử. [Hướng dẫn Supabase local](HUONG_DAN_TRIEN_KHAI_SUPABASE.md#1-dựng-môi-trường-thử)

## 3. Chạy ứng dụng từ Android Studio

1. Mở thư mục gốc dự án trong Android Studio. Dùng Android SDK 36 và JDK 25 cho Gradle, chạy **Sync Project with Gradle Files**, rồi chọn cấu hình `app` và bấm **Run** trên emulator đã khởi động hoặc thiết bị Android.
2. Trong app, chọn **Kết nối Supabase**. Trên emulator chuẩn, nhập `http://10.0.2.2:54321` và publishable key từ `supabase status`. Trên thiết bị Android có ADB, dùng `adb reverse tcp:54321 tcp:54321` và nhập `http://127.0.0.1:54321`.
3. Đăng nhập bằng tài khoản mẫu, xác nhận app hiển thị vai trò do Supabase cấp và tải được các đợt. Dùng hai tài khoản Người chăm sóc và Điều phối viên để gửi, xét và cấp một suất theo lịch của đợt, rồi xác nhận Người chăm sóc xem được mã nhận trong app.

Android đạt khi Gradle Sync thành công, Android Studio cài và mở được app, và luồng trên chạy với dữ liệu từ Supabase local. [Tài khoản mẫu và kịch bản nghiệp vụ](HUONG_DAN_TRIEN_KHAI_SUPABASE.md#3-kiểm-tra-xếp-và-giao-suất)

## 4. Sửa lỗi và xác nhận lại

Nếu một bước thất bại, ghi lại lệnh hoặc thao tác, lỗi đầu tiên và môi trường chạy. Sửa nguyên nhân trên `qnhu-24/11`, chạy lại bước lỗi, rồi chạy lại cả bộ kiểm thử database và luồng Android Studio. Lặp lại cho đến khi cả hai đạt. Cập nhật `docs/QA.md` với commit, môi trường và kết quả thực tế. Commit phần sửa và kết quả rồi đẩy lên `origin/qnhu-24/11`.
