# Hoàn thiện đăng nhập và đăng xuất trên Android

Tài liệu này hướng dẫn chi tiết màn đăng nhập, phiên và đăng xuất trong bước 06. Đăng ký, khôi phục mật khẩu và sửa thông tin liên hệ được nghiệm thu cùng bước đó. [Bước 06 · Tài khoản và phiên làm việc](06_KE_HOACH_TAI_KHOAN_VA_PHIEN.md)

## Luồng cần đạt

Mặc định đề xuất là **mở màn đăng nhập khi chưa có phiên**, với nút **Trải nghiệm demo** riêng. Đăng nhập thành công sẽ vào đúng giao diện của vai trò do Supabase cấp. App **giữ phiên khi đóng rồi mở lại**. Khi đăng xuất, app quay về màn đăng nhập và yêu cầu Supabase thu hồi phiên của **thiết bị hiện tại**.

Mã hiện tại đã có form đăng nhập và đọc vai trò từ Supabase. Form nằm trong menu demo, phiên chỉ giữ trong bộ nhớ và nút đăng xuất mới xóa phiên phía app. [Luồng hiện tại](../app/src/main/kotlin/vn/edu/buacombencon/MainActivity.kt#L328)

## Các bước triển khai

1. **Đưa đăng nhập ra thành màn XML riêng.** Màn chính có email, mật khẩu, **Đăng nhập**, **Tạo tài khoản**, **Quên mật khẩu** và **Trải nghiệm demo**. Chuyển URL và publishable key vào mục cấu hình của bản debug để dễ thử với localhost. Chặn gửi lặp khi đang đăng nhập, hiển thị lỗi ngay trên màn và phân biệt sai thông tin, email chưa xác nhận, tài khoản thiếu vai trò và lỗi kết nối.

2. **Xác nhận danh tính rồi mới mở dữ liệu.** Sau khi Auth trả phiên hợp lệ, đọc hồ sơ của đúng `user.id` và lấy vai trò từ `profiles`. Nếu vai trò hợp lệ, mở màn chính của vai trò đó. Nếu tải dữ liệu đợt thất bại sau khi đã xác thực, giữ phiên và hiển thị **Thử lại**. Bộ chọn vai trò chỉ dùng trong chế độ demo. Tài khoản thật đổi vai trò sử dụng bằng cách đăng xuất rồi đăng nhập tài khoản tương ứng.

3. **Lưu và khôi phục phiên.** Dùng khóa Android Keystore để mã hóa bản ghi phiên bằng AES-GCM. Khi mở lại app, hiển thị trạng thái **Đang khôi phục phiên**, dùng refresh token lấy phiên mới rồi đọc lại hồ sơ và quyền từ server trước khi mở màn nghiệp vụ. Lưu cặp token mới mỗi lần refresh thành công. Nếu Supabase từ chối refresh vì phiên đã hết hiệu lực, xóa phiên và yêu cầu đăng nhập lại. Nếu chỉ mất mạng, giữ bản ghi đã mã hóa và cho thử lại. Mật khẩu và mã OTP chỉ tồn tại trong lúc nhập và gửi yêu cầu. [Android Keystore](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec) · [Vòng đời phiên Supabase](https://supabase.com/docs/guides/auth/sessions)

4. **Dùng một luồng đăng xuất chung.** Nút ở tab **Tài khoản** và menu đầu trang gọi cùng một thao tác. Khi bấm, khóa màn nghiệp vụ, xóa phiên đã lưu, dữ liệu màn hình, bản nháp và lịch sử điều hướng của tài khoản. Gọi `POST /auth/v1/logout?scope=local` cho phiên vừa thoát. Nếu gọi thất bại do mất mạng, app vẫn về màn đăng nhập và báo chưa xác nhận thu hồi trên server. Bỏ qua mọi phản hồi đến muộn từ tài khoản cũ. [Đăng xuất trong Supabase Auth](https://supabase.com/docs/guides/auth/signout)

## Tiêu chí nghiệm thu trên localhost

| Ca thử | Điều kiện đạt |
| --- | --- |
| Đăng nhập đúng và sai | Bốn tài khoản mẫu vào đúng vai trò. Sai mật khẩu, email chưa xác nhận và mất mạng đều có trạng thái rõ ràng. |
| Xoay màn hình, đóng và mở lại app | Phiên hợp lệ được khôi phục. Phiên bị thu hồi quay về đăng nhập. Mất mạng khi khôi phục hiện **Thử lại**. |
| Đăng xuất khi có mạng | App về màn đăng nhập. Nút Quay lại không mở lại dữ liệu cũ. Phiên vừa thoát không thể refresh. Phiên cùng tài khoản trên thiết bị khác vẫn dùng được. |
| Đăng xuất khi mất mạng và đổi tài khoản | App vẫn thoát cục bộ. Khi tài khoản B đăng nhập, màn hình, hồ sơ và bản nháp của A không xuất hiện. |
| Kiểm tra hồi quy | Chế độ demo vẫn vào được từ nút riêng. Tạo tài khoản, xác nhận email và quên mật khẩu vẫn chạy với Mailpit. |

Ghi commit, thiết bị và kết quả **Android Studio → Run** vào `docs/QA.md`. Phạm vi thu hồi đã chọn là refresh token của phiên hiện tại. Access token Supabase đã cấp hết hiệu lực theo hạn JWT. [Kết quả QA](QA.md#nhánh-mvc-và-supabase) · [Phạm vi thu hồi phiên](https://supabase.com/docs/guides/auth/signout)

## Các việc tiếp theo sau đăng nhập và đăng xuất

1. Hoàn tất sửa thông tin liên hệ và các ca nghiệm thu còn lại của [bước 06](06_KE_HOACH_TAI_KHOAN_VA_PHIEN.md).
2. Chạy luồng nhận suất [bước 01–05](01_KE_HOACH_DOT_HO_TRO.md#thứ-tự-thực-hiện-bộ-kế-hoạch) trên Android với Supabase local. Luồng phát suất dùng mã ngắn. Công việc quét QR bằng camera được ghi ở bước 05.
3. Tiếp tục [07. Cam kết và xác nhận nguồn](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md), rồi [08. Đối chiếu và báo cáo](08_KE_HOACH_DOI_CHIEU_VA_BAO_CAO.md). Chốt kết quả chạy Android và SQL trên localhost trước khi đẩy migration hosted.
