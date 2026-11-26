# Kiểm tra dự án giao diện

## Nghiệm thu Kế hoạch 01 · Chuẩn bị đợt hỗ trợ (Nhánh `hao-26/11`)

Kiểm tra và nghiệm thu ngày 26/11/2026 trên nhánh `hao-26/11`. Đợt hỗ trợ mẫu `c2` và luồng tạo đợt mới được đối chiếu đáp ứng đầy đủ tiêu chí nghiệm thu của [01_KE_HOACH_DOT_HO_TRO.md](01_KE_HOACH_DOT_HO_TRO.md).

| Hạng mục kiểm tra | Kết quả | Chi tiết đối chiếu |
| --- | --- | --- |
| **Đợt mẫu `c2` ("Thêm một bữa cơm lành")** | Đạt | `id: c2`, người sở hữu: Điều phối viên (`22222222-2222-2222-2222-222222222222`), tình nguyện viên phụ trách: `33333333-3333-3333-3333-333333333333` trong `campaign_volunteers`. |
| **Chỉ tiêu nguồn đợt `c2`** | Đạt | `confirmed_supply`: 30, `prepared_supply`: 30, `pledged_supply`: 10. Đã kiểm tra ràng buộc `prepared <= confirmed` và tối thiểu 1 suất. |
| **Quản lý tiếp nhận (Đóng/Mở)** | Đạt | Điều phối viên đóng tiếp nhận -> Người chăm sóc thấy "Đã đóng đăng ký" và không thể gửi hồ sơ. Mở lại khi còn hạn -> Nút Đăng ký hoạt động bình thường. |
| **Tạo đợt mới & Phân công tình nguyện viên** | Đạt | Bổ sung migration `20261126000001_assign_campaign_volunteer.sql` và hỗ trợ gán tình nguyện viên khi tạo đợt mới qua `create_campaign`. |
| **Kiểm thử logic nghiệp vụ (`DomainChecks`)** | 29/29 Đạt | 29/29 ca kiểm tra nghiệp vụ và ca biên đạt trên JVM. |

### Thông số bàn giao cho Kế hoạch 02 (`02_KE_HOACH_GUI_HO_SO.md`)
* `campaign_id`: `c2`
* `coordinator_id`: `22222222-2222-2222-2222-222222222222`
* `volunteer_id`: `33333333-3333-3333-3333-333333333333`
* `confirmed_supply`: 30
* `prepared_supply`: 30
* `is_registration_open`: `true`
* Điểm phát: Điểm An Tâm (mẫu)

## Nghiệm thu Kế hoạch 02 · Gửi hồ sơ đăng ký (Nhánh `hao-26/11`)

Kiểm tra và nghiệm thu ngày 26/11/2026 trên nhánh `hao-26/11`. Luồng gửi hồ sơ đăng ký của Người chăm sóc và Đăng ký hộ của Điều phối viên đã được nghiệm thu đầy đủ theo [02_KE_HOACH_GUI_HO_SO.md](02_KE_HOACH_GUI_HO_SO.md).

| Hạng mục kiểm tra | Kết quả | Chi tiết đối chiếu |
| --- | --- | --- |
| **Tự đăng ký của Người chăm sóc** | Đạt | Form kiểm tra hợp lệ: Tên >= 2 ký tự, SĐT chuẩn 10 số, checkbox đồng ý (`consent`). Sau khi gửi, hồ sơ tạo mới ở trạng thái `PENDING`, `is_approved` để trống, chưa cấp vé. |
| **Đăng ký hộ & Bổ sung thông tin** | Đạt | Điều phối viên đăng ký hộ (`is_assisted = true`), hỗ trợ cờ thiếu thông tin (`is_complete = false`). Thao tác bổ sung thông tin (`completeRegistration`) cập nhật cờ và ghi chú thành công. |
| **Đối chiếu trùng danh tính** | Đạt | Chặn đăng ký tự thân trùng lặp; chặn trùng lặp qua lại giữa kênh tự đăng ký và đăng ký hộ theo số điện thoại người nhận. |
| **Ca biên đóng đợt / hết hạn** | Đạt | Khi đợt đóng tiếp nhận, server và client chặn gửi hồ sơ mới với thông báo lỗi rõ ràng. Mở lại đợt cho phép đăng ký bình thường. |
| **Kiểm thử logic nghiệp vụ (`DomainChecks`)** | 33/33 Đạt | 33/33 ca kiểm tra bao gồm cả luồng tự đăng ký, đăng ký hộ, bổ sung và đối chiếu trùng đạt trên JVM. |

### Thông số bàn giao cho Kế hoạch 03 (`03_KE_HOACH_XET_VA_PHAN_BO.md`)
* `campaign_id`: `c2` ("Thêm một bữa cơm lành")
* `registration_id` mẫu hợp lệ: `R0036` (hoặc hồ sơ mới tạo) ở trạng thái `PENDING`
* `is_approved`: `null` (Chờ điều phối viên xét duyệt điều kiện)
* `tickets`: Chưa cấp vé (Sẽ được cấp sau khi xét đạt và chạy phân bổ)

## Nhánh MVC và Supabase

| Hạng mục | Kết quả |
| --- | --- |
| Gradle `:app:assembleDebug :app:lintDebug` | Build debug thành công. Lint có 0 lỗi và 339 cảnh báo. |
| Demo và XML | 18/18 ca nghiệp vụ, OpenCV đọc đúng `BC0013`, layoutlib dựng 32/32 cấu hình. |
| Supabase local | CLI 3.3.0 và PostgreSQL 17 áp dụng migration và seed. Bộ SQL chạy qua cả các ca quyền, hạn, thứ tự, cấp lại và 35 hồ sơ cho 30 suất. |
| Auth và Mailpit | Bốn tài khoản mẫu đăng nhập đúng vai trò. Tài khoản thử mới nhận mã xác nhận và mã khôi phục trong Mailpit, đổi mật khẩu rồi đăng nhập lại qua API. |
| Các luồng qua Data API | Gửi, xét, cấp suất và đọc vé theo tài khoản. Cam kết của nhà hảo tâm giữ nguyên chỉ tiêu xác nhận. |
| Repository Kotlin kết nối thật | Chạy từ JVM kiểm thử, nạp dữ liệu đúng cho bốn vai trò trên stack Supabase local. |
| Giao dịch đồng thời | Hai yêu cầu cùng mã chỉ tạo một lượt giao. Hai mã tranh suất thực chuẩn bị cuối, hai yêu cầu cấp lại tranh suất cuối, và hủy khi đang phát đều giữ đúng số suất. |
| Android emulator | Chưa hoàn tất lượt thử APK. AVD API 36 chạy chế độ phần mềm kết nối ADB nhưng chưa báo hoàn tất khởi động. |

APK Gradle của lần kiểm tra có SHA-256
`28691cb03f91ff39041481ef01472056fb77553017fa1d9ba869c15ff3579bd6`.
Ảnh và APK được lưu trong Git từ bản demo trước là mốc lịch sử.

## Bản Kotlin và XML

| Hạng mục | Kết quả |
| --- | --- |
| Gradle `:app:assembleDebug :app:lintDebug` | Thành công. Kotlin biên dịch, tác vụ Java báo `NO-SOURCE`. Lint có 0 lỗi và 330 cảnh báo. |
| Build bằng Android SDK | Thành công. APK trong `apk/` có chữ ký v2/v3 và cùng chứng chỉ debug với APK trước. |
| Nghiệp vụ và QR | 18/18 ca kiểm tra cục bộ đạt. OpenCV đọc đúng mã mẫu `BC0013`. |
| Giao diện qua layoutlib | 32 cấu hình đạt, bao gồm 5 trạng thái vé nhận. Cả 64 cặp ảnh trước và sau khi chuyển Kotlin trùng từng pixel. |
| Chạy trên Android | Chưa kiểm tra bản Kotlin qua Android Studio Run, emulator hoặc điện thoại thật. |

## Bản chuyển bố cục sang XML

| Hạng mục | Kết quả |
| --- | --- |
| Build Android SDK và chữ ký APK | AAPT2, Java, DEX và chữ ký v2/v3 đạt. APK trong `apk/` dùng cùng chứng chỉ debug với APK đã có. |
| Kiểm tra nghiệp vụ cục bộ | 18/18 kiểm tra `DemoStore` đạt. OpenCV giải mã đúng `BC0013` từ QR mẫu. |
| So sánh với trạng thái trước chuyển đổi | 32 cấu hình layoutlib, gồm cả bốn vai trò, các trạng thái nhận suất, màn nhỏ, chữ 130% và chế độ ngang. Cả 64 cặp ảnh đầu và cuối vùng cuộn trùng từng pixel. |
| Kiểm tra trên Android | Chưa xác nhận chạy bản cập nhật qua Android Studio, emulator hoặc thiết bị thật. |

## Kiểm tra giao diện gốc

Kiểm tra ngày 19/11/2026. Phân biệt rõ kết quả biên dịch, dựng giao diện trên máy chủ và chạy trên thiết bị.

| Hạng mục | Kết quả |
| --- | --- |
| AAPT2 compile/link toàn bộ XML và manifest | Đạt |
| Biên dịch Java 11 trên Android API 36 | Đạt |
| D8 và đóng gói APK debug | Đạt |
| Xác minh chữ ký APK bằng apksigner | Đạt, v2/v3 |
| Package và launcher | `vn.edu.buacombencon`, `MainActivity` |
| Android tối thiểu / target | API 26 / 36 |
| Kiểm tra hành vi của DemoStore | 18 kiểm tra đạt; xem `qa/domain-checks.log` |
| Mã QR của bộ mã hóa native | Bộ giải mã OpenCV đọc lại đúng `BC0013` |
| Ảnh giao diện native | 19 cấu hình từ cùng Java/XML qua Android Studio layoutlib; mỗi cấu hình có ảnh đầu và cuối vùng cuộn |
| Màn hình nhỏ / chữ lớn / ngang | 320×740dp, 393×852dp, cỡ chữ 130%, 852×393dp |
| Gradle assembleDebug trong phiên thực thi | Chưa chạy được vì môi trường không cho tạo socket khóa của Gradle; APK được build bằng công cụ SDK trực tiếp |
| Android Studio Gradle Sync và Run | Chưa xác nhận qua IDE |
| Cài APK và tương tác trên emulator/điện thoại | Chưa thực hiện vì ADB bị chặn tạo socket |

Các kiểm tra hành vi bao gồm đăng ký đóng/mở, không tự cấp suất khi mới gửi, chặn đăng ký trùng, cấp dựa trên số xác nhận, bảo toàn danh sách chờ khi đủ suất, cấp lại sau hủy, mã sai đợt, mã không tồn tại, mã đã dùng, chưa đến ngày phát, giới hạn số thực chuẩn bị, lưu trạng thái sau tái tạo kho dữ liệu, cam kết không tự đổi số xác nhận và giữ số suất chưa trao trong báo cáo.

Ảnh được dựng từ chính View/XML của ứng dụng bằng Android Studio layoutlib; không phải bản HTML và không phải ảnh chụp emulator. Bộ kiểm tra cung cấp Context/SharedPreferences và bộ điều phối Back giả lập trên máy chủ để dựng View. Nó không xác minh vòng đời Android thật, bàn phím, quyền, camera hoặc cử chỉ hệ thống. Những phần đó cần kiểm tra trên thiết bị.

## Ảnh giao diện

| Màn | Ảnh đầu | Ảnh sau cuộn |
| --- | --- | --- |
| Trang chủ gia đình | [Xem](qa/01-home.png) | [Xem](qa/01-home-bottom.png) |
| Chi tiết đợt | [Xem](qa/02-detail.png) | [Xem](qa/02-detail-bottom.png) |
| Đăng ký nhận suất | [Xem](qa/03-form.png) | [Xem](qa/03-form-bottom.png) |
| Đăng ký của tôi | [Xem](qa/04-registrations.png) | [Xem](qa/04-registrations-bottom.png) |
| Mã nhận suất | [Xem](qa/05-ticket.png) | [Xem](qa/05-ticket-bottom.png) |
| Tổng quan điều phối | [Xem](qa/06-coordinator.png) | [Xem](qa/06-coordinator-bottom.png) |
| Quản lý đợt | [Xem](qa/07-manage.png) | [Xem](qa/07-manage-bottom.png) |
| Tạo đợt | [Xem](qa/08-create.png) | [Xem](qa/08-create-bottom.png) |
| Đăng ký hộ | [Xem](qa/09-assisted.png) | [Xem](qa/09-assisted-bottom.png) |
| Phát suất | [Xem](qa/10-volunteer.png) | [Xem](qa/10-volunteer-bottom.png) |
| Danh sách đã trao | [Xem](qa/11-handed-over.png) | [Xem](qa/11-handed-over-bottom.png) |
| Nhà hảo tâm | [Xem](qa/12-donor.png) | [Xem](qa/12-donor-bottom.png) |
| Biểu mẫu đồng hành | [Xem](qa/13-pledge.png) | [Xem](qa/13-pledge-bottom.png) |
| Báo cáo tổng hợp | [Xem](qa/14-report.png) | [Xem](qa/14-report-bottom.png) |
| Tài khoản | [Xem](qa/15-profile.png) | [Xem](qa/15-profile-bottom.png) |
| Trang chủ 320dp | [Xem](qa/16-home-small.png) | [Xem](qa/16-home-small-bottom.png) |
| Trang chủ chữ 130% | [Xem](qa/17-home-large-text.png) | [Xem](qa/17-home-large-text-bottom.png) |
| Biểu mẫu 320dp chữ 130% | [Xem](qa/18-form-small.png) | [Xem](qa/18-form-small-bottom.png) |
| Chế độ ngang | [Xem](qa/19-landscape.png) | [Xem](qa/19-landscape-bottom.png) |

## Chạy lại kiểm tra

Trên máy hiện tại:

```bash
./tools/qa/check-local.sh
# Thêm toàn bộ ảnh native:
./tools/qa/check-local.sh --render
```

Kiểm tra native dùng layoutlib và trình biên dịch Kotlin có sẵn trong Android Studio. Có thể đặt `STUDIO_DIR`, `ANDROID_HOME`, `JAVA_HOME` và `BCC_KOTLIN_DIR` khi chạy trên máy khác. Đặt `BCC_QA_OUTPUT` để ghi kết quả sang thư mục khác. Giải mã độc lập cần Python có OpenCV (`cv2`).

Android 16 thay đổi cách xử lý Back; mã ứng dụng có đăng ký `OnBackInvokedCallback` từ API 33 và xử lý insets cho system bars/bàn phím. Việc kiểm tra cử chỉ, thanh hệ thống và bàn phím thực tế vẫn cần chạy trên Android. Tham chiếu: [thay đổi Android 16](https://developer.android.com/about/versions/16/behavior-changes-16#predictive-back).
