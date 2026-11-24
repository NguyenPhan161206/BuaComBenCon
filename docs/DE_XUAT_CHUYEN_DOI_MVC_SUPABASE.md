# Đề xuất chuyển đổi Bữa cơm bên con sang MVC và Supabase

## 1. Mục tiêu và phạm vi

Giữ **Android Kotlin, giao diện XML và một module `:app`**. Tách mã theo từng luồng để có thể chỉnh giao diện, điều hướng và nguồn dữ liệu ở những vị trí rõ ràng. Sử dụng Supabase Auth và PostgreSQL cho tài khoản, phân quyền và dữ liệu dùng chung. Đề xuất dựa trên đặc tả MVP 1.2 và ghi lại các quy tắc cấp suất cùng ca kiểm thử cần cho việc chuyển đổi. [Đặc tả MVP trong repo](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md)

Hiện `MainActivity.kt` điều hướng, gắn dữ liệu và trực tiếp xử lý một số thao tác như hủy đăng ký và xác nhận giao. `DemoStore.kt` lưu dữ liệu mẫu cục bộ bằng `SharedPreferences`. Đợt chuyển đổi đầu tiên tách mã Android trên nguồn dữ liệu này, sau đó mới tích hợp Supabase bằng dữ liệu giả. [MainActivity.kt](../app/src/main/kotlin/vn/edu/buacombencon/MainActivity.kt) · [DemoStore.kt](../app/src/main/kotlin/vn/edu/buacombencon/DemoStore.kt)

## 2. Kiến trúc Android và giao diện XML

| Phần | Trách nhiệm |
| --- | --- |
| **View** | Layout trong `res/layout/` định nghĩa bố cục. Lớp gắn dữ liệu ở `ui/` hiển thị nội dung, trạng thái đang tải, trống hoặc lỗi và chuyển thao tác người dùng cho Controller. |
| **Controller** | Xử lý thao tác, gọi repository, cập nhật trạng thái màn hình và yêu cầu điều hướng. `MainActivity` quản lý vòng đời và khung điều hướng chung. |
| **Model** | `domain/` chứa mô hình và trạng thái nghiệp vụ. Repository cung cấp dữ liệu theo từng luồng. Mã gọi Supabase và chuyển đổi dữ liệu trả về nằm trong `data/remote/`. |

Bắt đầu với danh sách và chi tiết đợt hỗ trợ. Controller gọi `CampaignRepository`, ban đầu đọc từ `DemoStore`. Khi tích hợp, thêm cách đọc từ Supabase qua cùng hợp đồng repository. Ví dụ, `screen_home.xml` vẫn định nghĩa bố cục trang chủ và lớp gắn dữ liệu điền danh sách đợt vào các View hiện có. Thêm `RegistrationRepository` khi chuyển tới luồng hồ sơ. Các màn mới như đăng nhập tiếp tục dùng XML.

Ở bước đầu, giữ `R.id`, tên package của launcher và các custom View như `MealArtView` và `QrView`. XML tham chiếu trực tiếp tên lớp của custom View, còn bộ dựng ảnh QA truy cập `render()` và trạng thái trong `MainActivity` bằng reflection. Nếu tách các phần này, cập nhật bộ QA trong cùng thay đổi. Kiểm tra lại nút Quay lại, bản nháp, trạng thái khi xoay màn hình và khả năng đọc dữ liệu demo đã lưu. [AndroidManifest.xml](../app/src/main/AndroidManifest.xml) · [RenderPreview.kt](../tools/qa/RenderPreview.kt)

### Cấu trúc đích

```text
BuaComBenCon/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/vn/edu/buacombencon/
│       │   ├── MainActivity.kt
│       │   ├── ui/
│       │   │   ├── shell/          # Điều hướng và khung ứng dụng
│       │   │   ├── caregiver/      # Controller và phần gắn dữ liệu vào XML
│       │   │   ├── coordinator/
│       │   │   ├── volunteer/
│       │   │   ├── donor/
│       │   │   └── account/
│       │   ├── domain/             # Mô hình và trạng thái nghiệp vụ
│       │   └── data/
│       │       ├── demo/           # Nguồn dữ liệu mẫu
│       │       └── remote/         # Supabase Auth, Data API, RPC và DTO
│       └── res/
│           ├── layout/             # Giao diện XML
│           ├── drawable/
│           └── values/
├── supabase/
│   ├── config.toml                 # Môi trường local và Auth
│   ├── migrations/                 # Schema, quyền, ràng buộc và RPC
│   ├── seed.sql                    # Dữ liệu giả
│   └── tests/                      # Quyền truy cập và giao dịch
├── docs/
└── tools/qa/
```

Tạo package khi có luồng sử dụng. Đặt repository theo nghiệp vụ trong `data/`. Cây trên mô tả đích chuyển đổi, còn các custom View giữ vị trí hiện tại trong bước đầu.

## 3. Dữ liệu, tài khoản và quyền

- **Tài khoản và người nhận.** Supabase Auth quản lý đăng nhập email và mật khẩu, xác nhận email và khôi phục mật khẩu. Mô hình dữ liệu tách người nhận khỏi tài khoản đăng nhập. Hồ sơ đăng ký hộ ghi người nhận, điều phối viên tạo hồ sơ và sự đồng ý. Người nhận có thể nhận kết quả và mã ở bàn hỗ trợ. Số điện thoại trùng là tín hiệu đối chiếu. Cách xác nhận hai hồ sơ thuộc cùng một người cần chốt ở bước thiết kế dữ liệu.
- **Quyền theo vai trò và phạm vi.** Người chăm sóc xem hồ sơ của mình. Điều phối viên thao tác trong phạm vi được giao. Tình nguyện viên chỉ xem dữ liệu tối thiểu của đợt được phân công. Nhà hảo tâm xem cam kết của mình và báo cáo tổng hợp được công bố. Bộ chọn vai trò trong bản kết nối hiển thị các quyền tài khoản đã được cấp.
- **Đọc và ghi dữ liệu.** Dùng Data API với quyền truy cập tối thiểu và chính sách RLS cho các truy vấn đọc. Giới hạn thay đổi trạng thái, nguồn suất và lượt giao qua các RPC có kiểm tra vai trò, quyền trên đợt và quyền sở hữu khi áp dụng. Kiểm tra quyền thực thi của các hàm RPC và của view nếu dùng view cho báo cáo. Ứng dụng kết nối bằng publishable key và phiên người dùng. Khóa có quyền quản trị nằm ở môi trường server.

[Vai trò và quyền](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#2-vai-trò-và-quyền) · [Quyền truy cập Supabase Data API](https://supabase.com/docs/guides/api/securing-your-api) · [RLS và database view](https://supabase.com/docs/guides/database/postgres/row-level-security#views-and-rls)

## 4. Các thao tác nghiệp vụ trên server

| Thao tác | Hành vi và điều kiện |
| --- | --- |
| Tiếp nhận và bổ sung | Kiểm tra hạn riêng cho nhận hồ sơ mới và bổ sung. Server ghi thời điểm hồ sơ đủ thông tin và số tiếp nhận. Tự đăng ký và đăng ký hộ dùng cùng bước đối chiếu để giữ tối đa một hồ sơ mỗi người nhận trong một đợt. |
| Xét hồ sơ | Điều phối viên xác nhận điều kiện và lưu kết quả, người xét, thời điểm xét. Thời điểm xét lưu riêng với mốc đủ thông tin dùng để xếp hàng. |
| Phân bổ lần đầu | Điều phối viên thực hiện vào giờ công bố. Xếp theo mốc đủ thông tin, rồi theo số tiếp nhận nếu trùng mốc. Cấp đến giới hạn suất đã xác nhận và đưa các hồ sơ đủ điều kiện còn lại vào một hàng chờ. Hồ sơ đủ thông tin đúng hạn nhưng được xét muộn có thể vào hàng chờ theo mốc ban đầu trước giờ chốt cấp lại, giữ nguyên các lượt đã cấp. |
| Hủy đăng ký | Hủy đúng hạn thu hồi mã và giải phóng suất nếu đã cấp. |
| Cấp lại suất | Điều phối viên thực hiện thao tác riêng cho hồ sơ đầu hàng chờ khi có suất trống và còn trong thời hạn cấp lại. |
| Kiểm tra mã | Kiểm tra quyền, đúng đợt, khung giờ và hiệu lực mã. Giữ nguyên trạng thái giao và số đã trao. |
| Xác nhận giao | Sau khi giao thực tế, server kiểm tra lại điều kiện, lưu lượt giao, đánh dấu mã đã dùng và cập nhật hồ sơ trong cùng giao dịch. Nếu mất phản hồi, app hiển thị kết quả chưa xác định và tra cứu lại hoặc gửi lại an toàn. |

Dùng đồng hồ server để kiểm tra các mốc của đợt. Thiết kế giao dịch và ràng buộc PostgreSQL để các thao tác cập nhật nguồn, cấp suất, hủy và xác nhận giao cho kết quả nhất quán khi chạy đồng thời. Báo cáo đối chiếu giữ riêng số cam kết, số xác nhận, số thực chuẩn bị, số đã trao và số chưa trao.

[FR02–FR06 và các chỉ số báo cáo](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#4-yêu-cầu-chức-năng)

## 5. Trình tự triển khai và tiêu chí kiểm tra

1. **Tách MVC trên dữ liệu mẫu.** Lấy mốc QA mới gắn với mã nguồn và SHA của APK, vì `verification.json` hiện ghi SHA của APK cũ. Tách danh sách và chi tiết đợt. Chạy build, lint, kiểm tra nghiệp vụ demo và ảnh layoutlib. Thử trên emulator để đối chiếu giao diện, nút Quay lại, bản nháp và xoay màn hình.
2. **Dựng Supabase local.** Chốt ERD, hợp đồng dữ liệu và phạm vi quyền. Thử Gradle với Kotlin tích hợp hiện có, plugin serialization, Supabase Kotlin SDK và Ktor. Dùng Mailpit để kiểm tra xác nhận email và đặt lại mật khẩu trên dữ liệu giả. Gradle là đường build chính. `tools/build-sdk.sh` cần cập nhật nếu tiếp tục dùng để build bản có các thư viện Supabase.
3. **Tích hợp từng luồng.** Thử Android → Supabase local với luồng xem đợt, đăng ký, xét và cấp suất. Chạy bốn ca thứ tự xếp suất bên dưới và kiểm tra kịch bản 35 hồ sơ hợp lệ cho kết quả 30 lượt cấp, 5 hồ sơ chờ. Kiểm tra hai yêu cầu tranh suất cuối, hai thiết bị xác nhận cùng lượt giao, hủy và giao đồng thời, gửi lại sau timeout và truy cập ngoài phạm vi. Dùng kết quả để xác nhận từng luồng trước khi mở rộng sang các màn còn lại.

| Ca kiểm thử | Kết quả mong đợi |
| --- | --- |
| Bổ sung muộn | Khi còn một suất, hồ sơ đăng ký hộ đủ thông tin lúc 08:02 được cấp trước hồ sơ gửi lúc 08:01 nhưng đến 08:15 mới đủ thông tin. Cả hai đã được xét đủ điều kiện trước giờ phân bổ. |
| Trùng mốc | Hai hồ sơ đủ điều kiện có cùng mốc đủ thông tin. Hồ sơ số tiếp nhận 12 đứng trước số 13. |
| Xét sau lượt đầu | Lượt 09:45 đã cấp hết suất. Hồ sơ đủ thông tin lúc 08:15, được xét đạt lúc 09:50, vào hàng chờ trước hồ sơ đủ thông tin lúc 08:20. Các suất đã cấp giữ nguyên. |
| Cấp lại | Người được cấp trong ca bổ sung muộn hủy đúng hạn. Mã cũ hết hiệu lực. Khi điều phối viên xác nhận cấp lại trước giờ chốt, hồ sơ đầu hàng chờ được cấp đúng suất vừa giải phóng. |

[QA bản Kotlin](QA.md#bản-kotlin-và-xml) · [Tệp xác minh APK](qa/verification.json) · [Kịch bản nghiệm thu MVP](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#7-kịch-bản-nghiệm-thu-xuyên-suốt) · [Kotlin tích hợp trong AGP](https://developer.android.com/build/migrate-to-built-in-kotlin) · [Mailpit và Supabase Auth](https://supabase.com/docs/guides/auth/testing-auth-flows)

Tài liệu này ghi lại thiết kế đề xuất. Mốc kiểm thử Android và kết quả tích hợp Supabase được ghi nhận theo từng giai đoạn thực thi:
- **Giai đoạn 1 (Đã hoàn thành):** Tách cấu trúc MVC trên Android với các tầng domain, demo repository, và các UI controller (`CaregiverController`, `CoordinatorController`, `VolunteerController`, `DonorController`, `AccountController`), đảm bảo tương thích 100% với layout XML và công cụ QA layoutlib.
- **Giai đoạn 2 (Đã hoàn thành):** Dựng hệ thống Supabase local gồm `config.toml`, 3 tệp migration (Schema, RLS Policies, Business RPCs), tệp `seed.sql` với kịch bản 35 hồ sơ cho 30 suất đợt c1, và tệp test `01_business_rules_test.sql` kiểm thử 5 ca nghiệp vụ.
- **Giai đoạn 3 (Đã hoàn thành):** Cấu hình build Gradle (Supabase SDK, Serialization, Ktor), xây dựng tầng Remote Data Layer (`dto/`, `SupabaseConfig`, các `SupabaseRepositories`, `RepositoryProvider`), và tài liệu hướng dẫn vận hành chi tiết trong `HUONG_DAN_TRIEN_KHAI_SUPABASE.md`.
