# Chạy và nghiệm thu Bữa cơm bên con với Supabase local

Dùng Docker, Supabase CLI 3.3, Android SDK 36 và JDK 25 cho Gradle wrapper. Thực hiện tại thư mục gốc của dự án với dữ liệu mẫu.

## 1. Dựng môi trường thử

```bash
npx --yes supabase@3.3.0 start
npx --yes supabase@3.3.0 status
./gradlew :app:assembleDebug :app:lintDebug
```

Lần khởi động đầu áp dụng ba migration và nạp seed. Nhánh này cập nhật trực tiếp các migration thử nghiệm trước khi gộp. Với stack local cũ dùng riêng cho dữ liệu mẫu, chạy `npx --yes supabase@3.3.0 db reset` để nạp lại. Lệnh này xóa dữ liệu của database local đích.

| Thành phần | Địa chỉ mặc định |
| --- | --- |
| API từ máy chạy Docker | `http://127.0.0.1:54321` |
| API từ emulator chuẩn | `http://10.0.2.2:54321` |
| PostgreSQL local | `postgresql://postgres:postgres@127.0.0.1:54322/postgres` |
| Studio | `http://127.0.0.1:54323` |
| Mailpit | `http://127.0.0.1:54324` |

Trong APK debug, mở bộ chọn vai trò ở đầu trang, chọn **Kết nối Supabase** rồi nhập URL và **Publishable key** từ `supabase status`. Nếu dùng cổng chuyển tiếp ADB, chạy `adb reverse tcp:54321 tcp:54321` rồi dùng `http://127.0.0.1:54321`. HTTP loopback được bật riêng cho bản debug. Phiên đăng nhập lưu trong bộ nhớ ứng dụng, giữ qua xoay màn hình và yêu cầu đăng nhập lại sau khi tiến trình kết thúc.

| Vai trò mẫu | Email |
| --- | --- |
| Người chăm sóc | `giadinh@buacombencon.vn` |
| Điều phối viên | `dieuphoi@buacombencon.vn` |
| Tình nguyện viên | `tinhnguyen@buacombencon.vn` |
| Nhà hảo tâm | `nhahaotam@buacombencon.vn` |

Mật khẩu seed dùng chung là `MatKhau@123`. Trong chế độ Supabase, vai trò tới từ tài khoản. Để thử vai trò khác, đăng xuất rồi đăng nhập bằng tài khoản tương ứng.

## 2. Kiểm tra Auth qua Mailpit

1. Với địa chỉ email giả mới, chọn **Tạo tài khoản người chăm sóc**. Mở Mailpit, lấy mã email xác nhận và nhập vào app. Đăng nhập lại rồi kiểm tra vai trò Người chăm sóc.
2. Chọn **Quên mật khẩu** cho tài khoản vừa tạo. Lấy mã từ email khôi phục, nhập mã và mật khẩu mới. Kiểm tra đăng nhập bằng mật khẩu mới.
3. Đăng nhập lần lượt bốn tài khoản seed để kiểm tra vai trò và đợt được phân công. Tài khoản seed đã xác nhận email trước.

Mẫu email local trong `supabase/templates/` chứa mã một lần. Mailpit giữ email trong môi trường thử. [Tài liệu Supabase Auth local](https://supabase.com/docs/guides/auth/testing-auth-flows)

## 3. Kiểm tra xếp và giao suất

```bash
psql -X -v ON_ERROR_STOP=1 postgresql://postgres:postgres@127.0.0.1:54322/postgres \
  -f supabase/tests/01_business_rules_test.sql
```

Bộ test tạo dữ liệu riêng trong giao dịch rồi rollback. Các ca kiểm tra thứ tự theo mốc đủ thông tin và số tiếp nhận, xét muộn, hủy và cấp lại, 35 hồ sơ cho 30 suất, phạm vi đọc của tình nguyện viên và đối chiếu khi gửi lại xác nhận giao.

1. Vừa nạp seed, đăng nhập Người chăm sóc và gửi hồ sơ vào **Thêm một bữa cơm lành**. Đợt nhận hồ sơ mới trong 5 phút đầu. Đăng xuất, đăng nhập Điều phối viên, tìm cùng đợt và xét hồ sơ.
2. Sau phút thứ 7 và trước phút thứ 15, chọn **Phân bổ lần đầu**. Kiểm tra hồ sơ đã xét đạt nhận mã. Khi chuyển vai trò, đăng nhập đúng tài khoản rồi chọn **Làm mới dữ liệu**.
3. Đăng nhập Tình nguyện viên, chọn **Bữa trưa ấm lòng**, nhập mã seed `BC0013` và bấm kiểm tra. Kiểm tra mã giữ nguyên số đã trao. Bấm **Đã giao suất** sau bước giao giả lập, rồi tra lại mã. Số đã trao phải tăng đúng một lượt.
4. Với hai phiên thử trên cùng một đợt gần hết nguồn, thử xác nhận cùng một mã, giao hai mã khi chỉ còn một suất thực chuẩn bị và cấp lại khi chỉ còn một suất trống. Đối chiếu số lượt đã giao và vé còn hiệu lực trong database.
5. Nhập một cam kết từ tài khoản Nhà hảo tâm. Kiểm tra cam kết xuất hiện và số suất đã xác nhận giữ nguyên. Mở báo cáo của đợt đã công bố.

Điều phối viên quản lý đợt mình tạo. Tình nguyện viên được gán theo đợt trong `campaign_volunteers`. Quy tắc nhận diện người nhận trong MVP dùng cặp tên và số liên hệ đã chuẩn hóa. Hai người có cùng số liên hệ và tên khác nhau có thể có hồ sơ riêng.

## 4. Kiểm tra chế độ demo

```bash
BCC_QA_OUTPUT=/tmp/bcc-qa ./tools/qa/check-local.sh --render
```

Trên emulator, kiểm tra nút Quay lại, bản nháp sau xoay màn hình và dữ liệu demo đã lưu. Layoutlib dựng XML và bộ QA kiểm tra mã QR bằng OpenCV. APK trong `apk/` thuộc bản demo trước đợt chuyển đổi. Dùng APK Gradle tại `app/build/outputs/apk/debug/app-debug.apk` để nghiệm thu kết nối.
