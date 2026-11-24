# Bữa cơm bên con

Dự án Android native dùng **XML cho bố cục giao diện** và **Kotlin** cho điều hướng, gắn dữ liệu và xử lý thao tác. Ứng dụng dựa trên đề xuất Bữa cơm bên con trong file Word của nhóm. Giao diện tiếng Việt, nền kem và xanh lá, bố cục cuộn và thanh điều hướng theo từng vai trò.

**Đặc tả yêu cầu MVP.** [Phạm vi, quyền, quy tắc nghiệp vụ và tiêu chí nghiệm thu](docs/DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md)

**Kế hoạch hoàn thiện trong 1 tháng.** [Checklist triển khai G0–G7](docs/KE_HOACH_PHAN_CONG_DU_AN.md#44-checklist-triển-khai-theo-giai-đoạn) có người phụ trách và điều kiện nghiệm thu từng giai đoạn. [Bản phân công đầy đủ](docs/KE_HOACH_PHAN_CONG_DU_AN.md) có 60 nhiệm vụ cho Nguyên, Như, Tài, Duy và Hào. Mã nguồn hiện tại chạy với dữ liệu mẫu cục bộ. Backend và database nằm trong kế hoạch triển khai.

Ảnh tổng quan: [Xem giao diện](docs/ui-overview.png). APK cài thử: [BuaComBenCon-debug.apk](apk/BuaComBenCon-debug.apk).

## Mở và chạy trong Android Studio

1. Chọn **File → Open**, mở thư mục `BuaComBenCon` này (thư mục có `settings.gradle.kts`).
2. Trong **Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn Gradle JDK có sẵn của Android Studio. Máy hiện tại có `/home/binhnguyen/android-studio/jbr`.
3. Chờ **Gradle Sync**. Dự án dùng AGP `9.4.1` với Kotlin tích hợp, Gradle `9.6.0`, Android SDK Platform `36` và đích JVM `11`. Android Studio có thể cần tải thành phần build ở lần đầu.
4. Chọn cấu hình **Bữa cơm bên con**, module `app`, rồi chọn điện thoại hoặc emulator API 26 trở lên. Máy này đã có AVD `Pixel_10_API_36`.
5. Bấm **Run ▶**. Ở góc phải thanh trên cùng, bấm **Gia đình ⌄** để đổi vai trò trình diễn.

Nếu mở trên máy khác, chỉnh hoặc xóa `local.properties` để Android Studio nhận đường dẫn SDK của máy đó. Cấu hình Run có sẵn trong `.run/BuaComBenCon.run.xml`.

## Các màn hình và thao tác

| Vai trò | Giao diện |
| --- | --- |
| Người chăm sóc | Trang chủ, lọc đợt theo ngày/bữa ăn, chi tiết đợt, thông tin điều kiện và thời hạn, biểu mẫu đăng ký, kết quả tiếp nhận, danh sách đăng ký, QR/mã ngắn, hủy đăng ký, lịch sử, kết quả hỗ trợ |
| Điều phối viên | Tổng quan, danh sách đợt, tạo đợt, mở/đóng tiếp nhận, đăng ký hộ, kiểm tra/sắp xếp theo thứ tự, danh sách chờ, cung cấp mã cho gia đình, cập nhật số xác nhận và thực chuẩn bị, ghi chú đối chiếu |
| Tình nguyện viên | Chọn đợt, nhập mã ngắn, kiểm tra mã, xác nhận sau khi giao, danh sách đã trao, mô phỏng mất kết nối, hướng dẫn xử lý ngoại lệ |
| Nhà hảo tâm | Xem đợt, đăng ký số suất muốn đồng hành, mong muốn theo tuần/tháng, theo dõi cam kết, báo cáo số liệu tổng hợp |

Mọi vai trò đều có màn Tài khoản để đổi vai trò, xem hướng dẫn và khôi phục dữ liệu mẫu. Các màn hình có nút Quay lại; phím Back hoạt động trong ứng dụng. Nội dung biểu mẫu đang nhập được giữ khi xoay màn hình hoặc đổi sang màn khác rồi quay lại.

## Thử luồng hoàn chỉnh

### Gia đình đăng ký và điều phối sắp xếp

1. Vai trò **Gia đình** → Trang chủ → đợt **Thêm một bữa cơm lành** ngày mai → Xem chi tiết → Đăng ký 1 suất ăn.
2. Điền tên, số điện thoại đúng định dạng và đồng ý thông tin → Gửi đăng ký. Kết quả ban đầu là **Đã tiếp nhận**, chưa có mã.
3. Đổi vai trò **Điều phối viên** → Đợt hỗ trợ → cùng đợt ngày mai → Quản lý đợt → lọc **Cần sắp xếp** → Kiểm tra và sắp xếp → xác nhận đã kiểm tra điều kiện.
4. Đổi về **Gia đình** → Đăng ký → mở đăng ký vừa tạo. Mã QR và mã ngắn xuất hiện khi trạng thái là **Được nhận**.
5. Có thể hủy đăng ký; mã hết hiệu lực và suất được giải phóng. Điều phối viên xem xét người tiếp theo, không tự động chuyển suất khi gia đình chỉ mới báo hủy.

### Kiểm tra giao nhận và mã dùng lại

1. Đổi sang **Tình nguyện viên**, chọn **Bữa trưa ấm lòng** hôm nay.
2. Nhập **BC0013**, hoặc bấm nút điền mã mẫu.
3. Bấm **Kiểm tra mã** → **Đã giao suất**. Báo cáo tăng số đã trao lên 1.
4. Nhập lại cùng mã: ứng dụng báo mã đã dùng. Thử mã không tồn tại, mã sai đợt hoặc tắt công tắc mô phỏng có kết nối để xem trạng thái lỗi.
5. Nếu đã hủy mã mẫu khi thử luồng khác, vào Tài khoản → Khôi phục dữ liệu mẫu trước khi thử lại.

### Danh sách chờ

Đợt hôm nay có mẫu 35 hồ sơ: 30 hồ sơ đã được sắp xếp (gồm 12 đã nhận), 5 hồ sơ chờ. Gia đình mẫu có mã BC0013. Hủy đăng ký này, rồi Điều phối viên mở đợt hôm nay → lọc Danh sách chờ → kiểm tra hồ sơ đầu tiên để cấp lại suất còn trống. Số đã sắp xếp không vượt số xác nhận cung cấp.

### Nhà hảo tâm

Chọn một đợt, gửi số suất mong muốn hỗ trợ rồi xem **Cam kết**. Thao tác này chỉ đổi số cam kết, không tự đổi số xác nhận cung cấp. Xem **Kết quả** để đối chiếu cam kết, xác nhận, thực chuẩn bị, đã trao và chưa trao.

## Mã nguồn để chỉnh giao diện

| Phần giao diện | Layout trong `app/src/main/res/layout/` |
| --- | --- |
| Khung ứng dụng và điều hướng | `activity_main.xml`, `item_nav.xml`, `item_back.xml` |
| Người chăm sóc | `screen_home.xml`, `screen_detail.xml`, `screen_form.xml`, `screen_success.xml`, `screen_my_registrations.xml`, `screen_ticket.xml`, `screen_impact.xml` |
| Điều phối | `screen_dashboard.xml`, `screen_campaigns.xml`, `screen_manage.xml`, `screen_create.xml`, `screen_form.xml` cho đăng ký hộ, `dialog_supply.xml` |
| Phát suất | `screen_distribution.xml`, `screen_handed_over.xml`, `screen_help.xml` |
| Nhà hảo tâm | `screen_donor_home.xml`, `screen_pledge.xml`, `screen_commitments.xml` |
| Báo cáo và tài khoản | `screen_reports.xml`, `item_report.xml`, `screen_profile.xml` |
| Thẻ danh sách và trạng thái mã | `item_*.xml`, `ticket_*.xml` |

Mở các layout trong **Code / Split / Design** của Android Studio. Màu, chuỗi và style dùng chung ở `res/values/`, nền thẻ và nút ở `res/drawable/`. Trong `app/src/main/kotlin/`, `MainActivity.kt` nạp layout, điền dữ liệu và xử lý điều hướng. `DemoStore.kt` lưu dữ liệu mẫu bằng SharedPreferences. Các view khai báo trong XML như `MealArtView`, `UiIcon` và `QrView` dùng Canvas để vẽ minh họa, biểu tượng và QR.

## Build

Build chuẩn qua Gradle trên máy có quyền mạng/socket thông thường:

```bash
./gradlew :app:assembleDebug
```

Nếu cần dùng đúng bộ SDK/JDK đã cài, có thể build độc lập với Gradle:

```bash
./tools/build-sdk.sh
```

Script dùng SDK trong `ANDROID_HOME` hoặc `ANDROID_SDK_ROOT`, JDK trong `JAVA_HOME` và Kotlin đi kèm Android Studio. Có thể đặt `BCC_KOTLIN_DIR` để chọn thư mục trình biên dịch khác. Cần Build Tools `37.0.0`, Platform `android-36`, JDK và Python 3. APK tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Phạm vi và bằng chứng kiểm tra

Đây là **UI có tương tác và trạng thái cục bộ**, chưa phải hệ thống vận hành thật. Tên nhóm, điểm phát, người nhận, số điện thoại mẫu và số liệu đều là minh họa. Ngày của các đợt mẫu tính tương đối theo ngày mở ứng dụng.

Chưa tích hợp đăng nhập/phân quyền thật, máy chủ, đồng bộ nhiều thiết bị, xét điều kiện tự động, thông báo đẩy, SMS, camera quét QR, thanh toán hoặc xác nhận giao nhận ngoài đời. Thời hạn đăng ký nhập dạng văn bản; điều phối viên bật/tắt tiếp nhận trong bản UI, chưa có bộ lập lịch tự đóng theo giờ. Một cam kết nhà hảo tâm đang hoạt động được giữ để trình diễn; gửi lại sẽ thay thế cam kết mẫu trước. Chưa có màn duyệt xác nhận cam kết nhà tài trợ riêng.

APK Kotlin/XML đã qua Gradle build, lint và xác minh chữ ký. Có 32 cấu hình layoutlib được so sánh trước và sau khi chuyển sang Kotlin. Cả 64 cặp ảnh đầu và cuối vùng cuộn trùng từng pixel. Bản cập nhật này chưa được xác nhận bằng Android Studio Run, emulator hoặc thiết bị thật.

Xem [đối chiếu yêu cầu](docs/REQUIREMENTS.md) và [kiểm tra và ảnh giao diện](docs/QA.md).

Tài liệu Android chính thức: [Layouts in Views](https://developer.android.com/develop/ui/views/layout/declaring-layout).
