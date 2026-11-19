# Bữa cơm bên con

Dự án giao diện Android native bằng **Java + XML**, dựa trên đề xuất ứng dụng Bữa cơm bên con trong file Word của nhóm. Mở được như một dự án Android Studio; có APK debug để cài thử. Giao diện tiếng Việt, nền kem và xanh lá, bố cục cuộn và thanh điều hướng theo từng vai trò.

**Kế hoạch hoàn thiện trong 1 tháng:** [Phân công Nguyên, Như, Tài, Duy, Hào](docs/KE_HOACH_PHAN_CONG_DU_AN.md), gồm 60 nhiệm vụ, lịch ngày 1–30, Android, backend, database, kiểm thử, triển khai và bàn giao. Backend và database trong tài liệu là phần cần triển khai tiếp; mã nguồn hiện tại là bản UI có dữ liệu mẫu.

Ảnh tổng quan: [Xem giao diện](docs/ui-overview.png). APK cài thử: [BuaComBenCon-debug.apk](apk/BuaComBenCon-debug.apk).

## Mở và chạy trong Android Studio

1. Chọn **File → Open**, mở thư mục `BuaComBenCon` này (thư mục có `settings.gradle.kts`).
2. Trong **Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn Gradle JDK có sẵn của Android Studio. Máy hiện tại có `/home/binhnguyen/android-studio/jbr`.
3. Chờ **Gradle Sync**. Dự án dùng AGP `9.4.1`, Gradle `9.6.0`, Android SDK Platform `36` và Java source level `11`. Android Studio có thể cần tải thành phần Gradle/plugin nếu máy chưa có cache.
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

- `app/src/main/res/layout/activity_main.xml`: thanh trên cùng, vùng nội dung cuộn và thanh điều hướng.
- `app/src/main/res/layout/screen_home.xml`: bố cục trang chủ, khối minh họa và danh sách đợt.
- `app/src/main/res/layout/screen_form.xml`: biểu mẫu đăng ký và đăng ký hộ.
- `app/src/main/res/values/`: màu, chuỗi giao diện và style.
- `app/src/main/java/vn/edu/buacombencon/MainActivity.java`: dựng nội dung từng màn, điều hướng, kiểm tra biểu mẫu và xử lý thao tác.
- `DemoStore.java`: dữ liệu mẫu và trạng thái lưu bằng SharedPreferences; không gọi máy chủ.
- `MealArtView.java`, `UiIcon.java`: minh họa và biểu tượng vector vẽ bằng Android Canvas.
- `QrCode.java`, `QrView.java`: mã QR thật chứa đúng mã ngắn, không chứa thông tin người nhận.

Mở XML trong **Code / Split / Design** của Android Studio. Phần nội dung dựng bằng Java được chỉnh trong các phương thức cùng tên như `home()`, `ticket()`, `manage()`, `distribution()`, `reports()`.

## Build

Build chuẩn qua Gradle trên máy có quyền mạng/socket thông thường:

```bash
./gradlew :app:assembleDebug
```

Nếu cần dùng đúng bộ SDK/JDK đã cài, có thể build độc lập với Gradle:

```bash
./tools/build-sdk.sh
```

Script dùng SDK trong `ANDROID_HOME` hoặc `ANDROID_SDK_ROOT`, JDK trong `JAVA_HOME`; nếu chưa đặt sẽ dùng đường dẫn đã kiểm tra trên máy hiện tại. Cần Build Tools `36.0.0`, Platform `android-36`, JDK và Python 3. APK tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Phạm vi và bằng chứng kiểm tra

Đây là **UI có tương tác và trạng thái cục bộ**, chưa phải hệ thống vận hành thật. Tên nhóm, điểm phát, người nhận, số điện thoại mẫu và số liệu đều là minh họa. Ngày của các đợt mẫu tính tương đối theo ngày mở ứng dụng.

Chưa tích hợp đăng nhập/phân quyền thật, máy chủ, đồng bộ nhiều thiết bị, xét điều kiện tự động, thông báo đẩy, SMS, camera quét QR, thanh toán hoặc xác nhận giao nhận ngoài đời. Thời hạn đăng ký nhập dạng văn bản; điều phối viên bật/tắt tiếp nhận trong bản UI, chưa có bộ lập lịch tự đóng theo giờ. Một cam kết nhà hảo tâm đang hoạt động được giữ để trình diễn; gửi lại sẽ thay thế cam kết mẫu trước. Chưa có màn duyệt xác nhận cam kết nhà tài trợ riêng.

APK đã biên dịch tài nguyên và Java, chuyển DEX, đóng gói và xác minh chữ ký bằng Android SDK. Các kiểm tra hành vi mẫu và kiểm tra giải mã QR độc lập có trong `docs/qa/`. Ảnh xem trước được dựng từ chính View/XML bằng **layoutlib của Android Studio**, không phải ảnh chụp emulator. Chạy Gradle và ADB trong môi trường thực thi hiện tại bị chặn tạo socket, nên **chưa xác nhận Run trên emulator/điện thoại hoặc Gradle Sync qua IDE**.

Xem [đối chiếu yêu cầu](docs/REQUIREMENTS.md) và [kiểm tra và ảnh giao diện](docs/QA.md).

Tài liệu Android chính thức: [Layouts in Views](https://developer.android.com/develop/ui/views/layout/declaring-layout).
