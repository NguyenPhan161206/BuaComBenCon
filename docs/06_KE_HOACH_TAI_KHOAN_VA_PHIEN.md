# 06. Tài khoản và phiên làm việc

**Mục tiêu.** Người dùng có thể đăng ký, xác nhận email, đăng nhập, đăng xuất, khôi phục mật khẩu và cập nhật thông tin liên hệ. Android giữ phiên khi mở lại app. Supabase xác định vai trò và phạm vi dữ liệu. [Đặc tả FR01](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr01-tài-khoản-và-phiên-làm-việc)

**Hiện trạng.** Android đã có hộp thoại kết nối trong menu demo, đăng nhập email và mật khẩu, xác nhận và khôi phục qua OTP, đọc hồ sơ và vai trò từ Supabase. Phiên còn lưu trong bộ nhớ tiến trình. Thao tác đăng xuất hiện xóa phiên phía app. Màn Tài khoản chưa có chức năng sửa liên hệ. [Hướng dẫn Auth local](HUONG_DAN_TRIEN_KHAI_SUPABASE.md#2-kiểm-tra-auth-qua-mailpit) · [Mã Android hiện tại](../app/src/main/kotlin/vn/edu/buacombencon/MainActivity.kt)

## 1. Mở app và đăng nhập

1. Dựng màn XML dành cho tài khoản. Khi chưa có phiên, hiển thị email, mật khẩu, **Đăng nhập**, **Tạo tài khoản**, **Quên mật khẩu** và **Trải nghiệm demo**. Đặt URL và publishable key trong phần cấu hình của bản debug để người dùng thử localhost.
2. Kiểm tra đầu vào và chặn gửi lặp trong khi yêu cầu đang chạy. Phân biệt các tình huống email chưa xác nhận, thông tin đăng nhập sai, thiếu hồ sơ vai trò và lỗi kết nối.
3. Sau xác thực, đọc `profiles` theo `user.id` của phiên rồi đưa người dùng đến giao diện của vai trò tìm được. Nếu đã xác thực nhưng tải dữ liệu đợt thất bại, giữ phiên và hiện **Thử lại**. Chọn vai trò thủ công chỉ dùng trong demo.
4. Sau khi xoay màn hình, giữ phiên và đúng trạng thái điều hướng của tài khoản. Gắn các phản hồi bất đồng bộ với phiên đang hoạt động để phản hồi cũ không ghi đè màn hiện tại.

## 2. Đăng ký, xác nhận email và khôi phục mật khẩu

1. Gửi đăng ký email và mật khẩu qua Supabase Auth. Giữ mặc định **Người chăm sóc** do server cấp khi tạo hồ sơ. Vai trò Điều phối viên, Tình nguyện viên và Nhà hảo tâm sử dụng tài khoản đã được cấp quyền.
2. Nhập mã xác nhận lấy từ Mailpit trên localhost. Hiện lỗi nếu mã sai hoặc hết hạn, rồi cho nhập lại hoặc gửi lại theo giới hạn của Auth. Xác nhận thành công sẽ đưa người dùng về đăng nhập.
3. Trong **Quên mật khẩu**, nhập email, lấy mã khôi phục và đặt mật khẩu mới qua Auth. Sau khi đổi thành công, quay lại đăng nhập bằng mật khẩu mới. Giữ mật khẩu và OTP trong bộ nhớ trong lúc thực hiện thao tác.

## 3. Khôi phục phiên và đăng xuất

1. Mã hóa bản ghi phiên bằng AES-GCM với khóa lưu trong Android Keystore. Cập nhật bản ghi sau mỗi lần refresh. Khi mở lại app, xác thực lại với Supabase, đọc lại hồ sơ và vai trò trước khi mở màn nghiệp vụ.
2. Nếu phiên hết hiệu lực, xóa phiên đã lưu và yêu cầu đăng nhập lại. Nếu mạng gián đoạn, giữ bản ghi đã mã hóa và cho thử lại.
3. Gộp hai nút đăng xuất trong menu đầu trang và tab **Tài khoản** vào cùng một luồng. Xóa phiên đã lưu, trạng thái màn, bản nháp và lịch sử điều hướng của tài khoản rồi trở về đăng nhập. Gọi `POST /auth/v1/logout?scope=local` để thu hồi refresh token của phiên vừa thoát. Nếu mất mạng, app vẫn thoát cục bộ và hiển thị rằng server chưa xác nhận thu hồi. JWT đã cấp hết hiệu lực theo hạn của nó. [Phạm vi thu hồi Supabase Auth](https://supabase.com/docs/guides/auth/signout)

## 4. Sửa thông tin liên hệ

1. Thêm **Sửa thông tin liên hệ** trong tab **Tài khoản**. Cho sửa tên hiển thị và số điện thoại. Vai trò và email xác thực hiển thị để đối chiếu. Đổi email xác thực là một thao tác Auth riêng.
2. Thêm RPC `update_my_profile` qua migration mới. Lấy người cập nhật từ `auth.uid()`, chỉ nhận hai trường tên và số điện thoại, chuẩn hóa đầu vào và trả về hồ sơ vừa lưu. Giữ phần cập nhật hồ sơ và cấp vai trò thành các thao tác server riêng.
3. Cho phép số liên hệ trống ở hồ sơ Người chăm sóc. Nếu Nhà hảo tâm gửi cam kết mà hồ sơ thiếu số liên hệ hợp lệ, chuyển đến màn bổ sung trước khi gửi. Khi lưu thành công, làm mới trang Tài khoản và form dùng thông tin hồ sơ. Hồ sơ đăng ký và cam kết đã ghi trước đó giữ dữ liệu liên hệ đã ghi nhận lúc gửi.

## 5. Nghiệm thu trên localhost

| Ca thử | Điều kiện đạt |
| --- | --- |
| Đăng nhập và phân quyền | Bốn tài khoản mẫu vào đúng vai trò. Tài khoản tự đăng ký qua Mailpit chỉ nhận vai trò Người chăm sóc. |
| Đăng ký và khôi phục | Mã xác nhận và mã khôi phục đến Mailpit, mã sai bị từ chối, đăng nhập được bằng mật khẩu mới sau khi đặt lại. |
| Giữ phiên | Xoay màn hình, đóng rồi mở lại app với phiên hợp lệ. Phiên đã hết hiệu lực yêu cầu đăng nhập lại. Lỗi mạng cho thử lại. |
| Đăng xuất và đổi tài khoản | App về màn đăng nhập cả khi mất mạng. Khi server xác nhận đăng xuất, refresh token của phiên đó bị từ chối. Phiên ở thiết bị khác vẫn dùng được. Tài khoản mới chỉ thấy dữ liệu và bản nháp của mình. |
| Chỉnh liên hệ | Lưu và đọc lại tên, số của chính tài khoản sau khi mở lại app. Quyền và hồ sơ của tài khoản khác giữ nguyên. |

**Hoàn thành khi** các ca trên đạt qua Android Studio với Supabase local. Ghi commit, thiết bị và kết quả vào [QA](QA.md#nhánh-mvc-và-supabase). Sau đó dùng tài khoản đã nghiệm thu để chạy luồng đợt hỗ trợ và [07. Cam kết của nhà hảo tâm](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md).
