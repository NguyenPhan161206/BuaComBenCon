# Kế hoạch phân công dự án Bữa cơm bên con trong một tháng

**Bắt đầu ở [checklist triển khai theo giai đoạn](#44-checklist-triển-khai-theo-giai-đoạn).** Mỗi giai đoạn có công việc nhỏ, người phụ trách và điều kiện nghiệm thu. Các bảng công việc ở mục 9–13 dùng để giao việc chi tiết.

**Thời gian:** 01 tháng, lập lịch tương đối từ **Ngày 1 đến Ngày 30**. Khi chốt ngày khởi động, Nguyên điền ngày thực tế vào bảng tiến độ; không tự mặc định một hạn nộp chưa được cung cấp.

**Nhóm:** Nguyên — trưởng nhóm; Như; Tài; Duy; Hào, theo đúng thứ tự phân công của nhóm.

**Sản phẩm cần bàn giao:** ứng dụng Android kết nối backend và PostgreSQL thật, có đăng nhập, phân quyền, đăng ký và sắp xếp suất ăn, mã nhận dùng một lần, ghi nhận giao nhận, đồng hành của nhà hảo tâm, báo cáo và hướng dẫn vận hành.

**Trạng thái của tài liệu:** kế hoạch triển khai. Các ô công việc bên dưới chưa được đánh dấu hoàn thành. Backend, database hoặc triển khai được mô tả trong kế hoạch không có nghĩa là đã được xây dựng.

## 1. Điểm bắt đầu và mục tiêu cuối tháng

### 1.1. Những gì đang có

Dự án Android hiện tại dùng **XML cho bố cục** và Kotlin cho điều hướng, gắn dữ liệu và xử lý thao tác. Các màn cho người chăm sóc, điều phối viên, tình nguyện viên và nhà hảo tâm đã có thể trình diễn với dữ liệu mẫu cục bộ trong `DemoStore.kt`. Có APK debug và ảnh giao diện dựng qua Android Studio layoutlib.

Các phần cần làm tiếp là tài khoản thật, kiểm tra quyền tại server, database dùng chung, API, giao nhận đồng thời trên nhiều thiết bị, quét QR bằng camera, thông báo trong ứng dụng, triển khai và kiểm thử trên thiết bị. Build APK hoặc có giao diện chưa đủ để kết luận những phần này hoạt động.

Giữ giao diện hiện có làm nền; chuyển từng màn sang API theo kế hoạch. Không xóa toàn bộ để làm lại hoặc đổi ngôn ngữ Android trong tháng này nếu chưa giải quyết được một trở ngại cụ thể.

### 1.2. Định nghĩa một dự án hoàn thiện trong phạm vi tháng này

Cuối tháng, một người kiểm thử phải có thể cài ứng dụng, sử dụng tài khoản đúng quyền và thực hiện toàn bộ quy trình sau trên hệ thống được triển khai:

1. Điều phối viên tạo đợt và xác nhận khả năng cung cấp suất.
2. Người chăm sóc tự đăng ký; điều phối viên đăng ký hộ được cho người không có tài khoản.
3. Server ghi nhận hồ sơ chờ kiểm tra; người điều phối xác nhận điều kiện và sắp xếp theo quy tắc công bố.
4. Hồ sơ đủ điều kiện được cấp suất hoặc đưa vào danh sách chờ; hủy suất giải phóng chỗ để người điều phối cấp cho người tiếp theo.
5. Người được cấp có QR và mã ngắn đúng đợt. Tình nguyện viên kiểm tra và xác nhận sau khi giao thực tế.
6. Hai thiết bị không thể cùng ghi nhận giao hai lần cho một lượt đăng ký.
7. Nhà hảo tâm gửi cam kết; đơn vị xác nhận số có thể cung cấp; báo cáo phân biệt cam kết, xác nhận, chuẩn bị, đã trao và chưa trao.
8. Dữ liệu còn nguyên sau khi đóng app, đổi thiết bị và khởi động lại backend.
9. Nhóm có thể cài lại hệ thống từ mã nguồn, chạy migration và phục hồi database từ bản sao lưu đã thử khôi phục.

Mục tiêu tháng là **một MVP hoàn chỉnh trong phạm vi đã chốt**. Việc đưa vào phục vụ gia đình thật cần đơn vị tổ chức tiếp nhận, quy trình hỗ trợ thực tế và kết quả chạy thử; không mặc định đã đủ điều kiện vận hành chỉ vì hết tháng hoặc CI đạt.

## 2. Phạm vi phải làm và phần để sau

### 2.1. Phạm vi bắt buộc

| Nhóm chức năng | Kết quả bắt buộc |
| --- | --- |
| Tài khoản | Đăng ký, xác minh email, đăng nhập, đăng xuất, lấy lại mật khẩu, sửa thông tin liên hệ |
| Phân quyền | Bốn vai trò do server xác định; tình nguyện viên chỉ thao tác đợt được giao; người chăm sóc chỉ xem đăng ký của mình |
| Đợt hỗ trợ | Ngày cụ thể, bữa ăn, điểm phát, hạn đăng ký, giờ nhận, tiêu chí, mở/đóng tiếp nhận, đóng đối chiếu |
| Đăng ký | Tự đăng ký và đăng ký hộ; kiểm tra trùng; ghi nhận đồng ý cung cấp thông tin; kết quả tiếp nhận rõ ràng |
| Sắp xếp suất | Xác nhận điều kiện, phân bổ, danh sách chờ có thứ tự, hủy, cấp lại, lý do khi xử lý ngoại lệ |
| Nhận suất | QR và mã ngắn ngẫu nhiên, thời hạn, thu hồi khi hủy, kiểm tra đúng đợt, xác nhận dùng một lần |
| Tình nguyện viên | Quét camera và nhập mã; không tự báo thành công khi mất mạng; tra cứu tình huống cần điều phối hỗ trợ |
| Nhà hảo tâm | Nhiều cam kết được lưu riêng, số suất mong muốn, đề nghị theo tuần/tháng, đơn vị xác nhận hoặc từ chối |
| Báo cáo | Cam kết, xác nhận cung cấp, thực chuẩn bị, đã trao, chưa trao và cách xử lý phần còn lại |
| Thông báo | Hộp thông báo trong app cho kết quả sắp xếp, hủy, thay đổi lịch; cập nhật khi tải lại dữ liệu |
| Hạ tầng | API HTTPS, PostgreSQL bền vững, cấu hình riêng dev/test/staging, backup, kiểm tra restore, log đã che thông tin riêng |
| Chất lượng | Test nghiệp vụ, HTTP/database thật, kiểm tra đồng thời, E2E app–API–DB và APK release trên thiết bị |

Giả định kỹ thuật để lập kế hoạch: tài khoản tự đăng ký dùng **email và mật khẩu**; số điện thoại là thông tin liên hệ của người chăm sóc. Gia đình không có email/tài khoản vẫn được đăng ký hộ. Tài và Nguyên phải xác nhận lựa chọn này ở Ngày 2; nếu đổi sang đăng nhập bằng số điện thoại thì phải bổ sung chi phí và công việc xác minh/khôi phục qua kênh tương ứng, không chỉ đổi nhãn ô nhập.

### 2.2. Chưa đưa vào tháng đầu

- Nhiều bệnh viện, nhiều đơn vị tổ chức hoặc nhiều điểm phát hoạt động độc lập.
- Thanh toán, giữ tiền, tự xác nhận chuyển khoản, viện phí hoặc tài trợ theo hồ sơ công khai của trẻ.
- Đăng ảnh trẻ, công khai bệnh án hoặc chia sẻ câu chuyện hoàn cảnh để được cấp suất.
- Ứng dụng iOS, website quản trị riêng, microservices hoặc Kubernetes.
- Tự xác nhận giao suất khi offline và đồng bộ giao nhận không cần đối chiếu sau đó.
- Push notification, SMS nhắc lịch, chat, bản đồ dẫn đường và lập đợt lặp tự động.

Mong muốn đồng hành theo tuần/tháng được ghi nhận; từng đợt vẫn phải có số xác nhận cung cấp riêng. Hộp thông báo trong app là phần bắt buộc, còn push/SMS là phần mở rộng.

## 3. Phân công chính cho năm người

| Thứ tự | Thành viên | Vai trò phát triển chính | Chịu trách nhiệm cuối cùng về |
| --- | --- | --- | --- |
| 1 | **Nguyên — trưởng nhóm** | Quản lý phạm vi, kiến trúc, backend nghiệp vụ lõi, tích hợp | Quy tắc nghiệp vụ, đăng ký/sắp xếp/mã nhận/giao nhận, giải quyết phụ thuộc, nghiệm thu và bàn giao |
| 2 | **Như** | Android cho người chăm sóc, tài khoản và lớp gọi API dùng chung | Luồng gia đình, phiên đăng nhập trên app, hiển thị trạng thái đúng và trải nghiệm người dùng |
| 3 | **Tài** | Backend nền tảng và các module hỗ trợ | Auth/phân quyền, đợt hỗ trợ, nguồn suất, nhà hảo tâm, báo cáo, thông báo và triển khai API |
| 4 | **Duy** | Android cho điều phối, tình nguyện viên, nhà hảo tâm | Công cụ tổ chức, đăng ký hộ, quét/nhập mã, xác nhận giao và màn đồng hành/báo cáo |
| 5 | **Hào** | Database, dữ liệu kiểm thử, kiểm thử tích hợp và vận hành dữ liệu | ERD/migration/ràng buộc/index, DB test, tổng hợp QA, backup và phục hồi |

Mỗi thành viên viết test cho phần mình làm và sửa lỗi thuộc phần đó. Hào tổ chức kiểm thử hệ thống, không phải người duy nhất chịu trách nhiệm chất lượng. Như và Duy cùng làm Android; Nguyên và Tài cùng làm backend; Hào phối hợp cả hai phía để tránh database bị tách rời khỏi API.

### 3.1. Người quyết định và người kiểm tra chéo

| Hạng mục | Chủ trì | Phối hợp | Người review/nghiệm thu |
| --- | --- | --- | --- |
| Phạm vi, quy tắc sắp xếp và thay đổi yêu cầu | Nguyên | Cả nhóm | Như, Tài và Hào xác nhận ảnh hưởng |
| UI, điều hướng và trạng thái gia đình | Như | Duy | Nguyên |
| UI cho các vai trò tổ chức và nhà hảo tâm | Duy | Như | Nguyên |
| Hợp đồng API | Nguyên | Tài, Như, Duy, Hào | Phải có người Android và database review |
| Auth, quyền, đợt, nguồn suất, cam kết, báo cáo | Tài | Nguyên, Hào | Nguyên; Hào kiểm tra tác động dữ liệu |
| Đăng ký, phân bổ, mã nhận, giao nhận | Nguyên | Tài, Hào | Tài review code; Hào kiểm tra đồng thời |
| ERD, migration và thay đổi cấu trúc DB | Hào | Tài, Nguyên | Nguyên hoặc Tài review trước khi áp dụng |
| Tích hợp Android/API | Như và Duy theo màn | Tài, Nguyên | Hào chạy kịch bản xuyên hệ thống |
| Triển khai API và cấu hình mạng | Tài | Hào | Nguyên |
| Backup/restore, báo cáo QA | Hào | Tài | Nguyên kiểm tra bằng chứng |
| APK release và hướng dẫn sử dụng | Như | Duy | Hào thử cài, Nguyên ký biên bản bàn giao |

## 4. Lịch một tháng và các mốc chốt

### 4.1. Cách tính thời gian

- Lịch dùng 30 ngày liên tiếp, chia bốn tuần chính và hai ngày dự phòng/bàn giao. Ngày nghỉ cụ thể do nhóm xếp lại trong từng tuần.
- Ước lượng ban đầu: **100 giờ công việc/người**, tổng 500 giờ; dự phòng tối đa 10 giờ/người cho lỗi hoặc chậm tích hợp, tổng ngân sách khoảng 550 giờ.
- Mức này tương đương khoảng 25–28 giờ/người/tuần. Đây là ước lượng lập kế hoạch, không phải năng suất hay lịch rảnh đã được xác nhận của thành viên.
- Các việc thiết kế, review và sửa lỗi đã có trong bảng nhiệm vụ. Nguyên phải rà lại lịch rảnh ở Ngày 1; nếu không đáp ứng được, giảm phần mở rộng trước, không bỏ kiểm tra phân quyền, chống giao trùng hoặc backup để giữ hình thức “xong”.

### 4.2. Các giai đoạn

| Mốc | Ngày | Mục tiêu | Điều kiện ra mốc |
| --- | --- | --- | --- |
| **G0** | 1–2 | Chốt yêu cầu, phạm vi và cách làm việc | Có vai trò, quy tắc nghiệp vụ, danh sách màn/API và tiêu chí nghiệm thu được cả nhóm đọc |
| **G1** | 3–5 | Chốt thiết kế, ERD, API và cấu trúc mã nguồn | Android có thể làm với mock theo hợp đồng; backend/database thống nhất tên trường và trạng thái |
| **G2** | 6–9 | Nền tảng chạy thật | DB migration/seed chạy; API kết nối DB; đăng nhập thật từ Android; quyền bị kiểm tra tại server |
| **G3** | 10–15 | Luồng lõi đăng ký đến mã nhận | Đăng ký thật, xét điều kiện, phân bổ/chờ/hủy/cấp lại; QR lấy từ server, không lấy từ DemoStore |
| **G4** | 16–20 | Giao nhận, nhà hảo tâm và báo cáo | Camera/nhập mã hoạt động; server chống giao trùng; cam kết/xác nhận/chuẩn bị/đã trao được tách đúng |
| **G5** | 21–24 | Hoàn thiện tích hợp và xử lý lỗi | Đủ bốn vai trò chạy trên staging; thông báo trong app; test lỗi, quyền và đồng thời đạt |
| **G6** | 25–28 | Triển khai bản bàn giao và chạy thử | APK release + HTTPS + DB bền vững; E2E thiết bị; backup restore đã thử; hết lỗi chặn nghiệm thu |
| **G7** | 29–30 | Dự phòng, nghiệm thu, nộp và bàn giao | Có gói bàn giao, hướng dẫn dựng lại, kết quả kiểm thử, phân công người tiếp nhận vận hành |

Không đánh dấu qua mốc chỉ vì đến ngày dự kiến. Nếu một mốc chưa đạt, ghi rõ lỗi, người sửa và ảnh hưởng tới lịch; các việc độc lập vẫn được tiếp tục.

### 4.3. Mỗi tuần từng người phải có gì

| Thời gian | Nguyên | Như | Tài | Duy | Hào |
| --- | --- | --- | --- | --- | --- |
| Tuần 1 — Ngày 1–7 | Chốt nghiệp vụ, cấu trúc repo, OpenAPI; bắt đầu đăng ký | Rà UI, tách màn, tạo API client và repository; mock đúng hợp đồng | Dựng API, validation, lỗi, auth nền tảng | Thiết kế luồng tổ chức, tách màn và thành phần dùng chung | ERD, migration nền tảng, DB dev/test và dữ liệu giả |
| Tuần 2 — Ngày 8–14 | API đăng ký, xét điều kiện, phân bổ/chờ/hủy; test transaction | Login thật, danh sách đợt/chi tiết/đăng ký; bỏ dữ liệu mẫu ở các màn đã nối | Hoàn tất auth/quyền; API đợt và nguồn suất; bắt đầu cam kết | Màn điều phối, tạo đợt, đăng ký hộ, danh sách và phân bổ | Migration nghiệp vụ, constraint/index, test DB và hỗ trợ API |
| Tuần 3 — Ngày 15–21 | Mã nhận, giao nhận một lần, audit; sửa lỗi tích hợp | Mã nhận/hủy/lịch sử, phiên hết hạn, thông báo; thử với thiết bị khác | Cam kết/xác nhận, báo cáo, thông báo, endpoint vận hành | Quét/nhập QR, giao nhận, màn nhà hảo tâm và báo cáo | Bộ dữ liệu E2E, test đồng thời, CI và kế hoạch backup |
| Tuần 4 — Ngày 22–28 | Đóng luồng còn thiếu, rà bảo mật/nghiệp vụ, điều phối release | Sửa lỗi app, kiểm tra thiết bị, cấu hình release và tài liệu gia đình | Sửa lỗi API, triển khai HTTPS, cấu hình SMTP/DB, log và tài liệu API | Sửa lỗi các vai trò hỗ trợ, camera, lỗi mạng; tài liệu thao tác | Chạy E2E, test tải nhỏ, thử khôi phục, báo cáo QA và đối chiếu số liệu |
| Ngày 29–30 | Chốt nghiệm thu, tổng hợp báo cáo và bàn giao | APK cuối, hướng dẫn cài và trình diễn gia đình | Cấu hình triển khai và hướng dẫn chạy API | Trình diễn điều phối/phát suất/nhà hảo tâm | Bàn giao migration, backup, restore và kết quả kiểm thử |

### 4.4. Checklist triển khai theo giai đoạn

Phạm vi từ đề xuất Word là **một nhóm thiện nguyện, một điểm phát, bốn vai trò và một quy trình từ đăng ký đến đối chiếu suất ăn**. Lịch 30 ngày, phân công, backend và PostgreSQL theo kế hoạch kỹ thuật của nhóm. Mỗi ô chỉ được đánh dấu khi có kết quả có thể kiểm tra, chẳng hạn PR, kết quả kiểm thử hoặc biên bản nghiệm thu. [Đề xuất sản phẩm phiên bản 3.0](source/2403700211_2403700101_2403700160_2403700235_2403700173.docx)

#### G0 · Ngày 1–2 · Chốt quy trình và phạm vi

- [ ] **Nguyên** trao đổi với đại diện nhóm tổ chức và người chăm sóc khi có thể. Ghi cách đăng ký, phát suất và tổng hợp hiện tại, cùng các giả định còn cần kiểm chứng.
- [ ] **Nguyên, Hào** chốt tiêu chí đủ điều kiện, thời hạn, quy tắc xếp theo lượt hợp lệ, xử lý hồ sơ ngoại lệ có lý do, hủy và cấp lại suất.
- [ ] **Như, Duy** lập danh sách màn XML theo bốn vai trò. Đánh dấu màn đã có dữ liệu mẫu, màn phải nối API và màn cần bổ sung như đăng nhập, thông báo và quét QR.
- [ ] **Nguyên, Tài, Hào** chốt dữ liệu tối thiểu, người được xem và trường hợp đăng ký hộ có sự đồng ý. Báo cáo nhà hảo tâm chỉ dùng số liệu tổng hợp.
- [ ] **Cả nhóm** chốt ngày bắt đầu, trách nhiệm, tiêu chí nghiệm thu và kịch bản giả lập 35 hồ sơ hợp lệ cho 30 suất.

**Qua mốc khi** nhóm có tài liệu phạm vi, quy tắc nghiệp vụ và danh sách màn cần làm. Các thông tin chưa xác minh được ghi rõ.

#### G1 · Ngày 3–5 · Thiết kế dữ liệu và giao diện kết nối

- [ ] **Hào** vẽ ERD cho tài khoản, đợt, đăng ký, mã nhận, lượt giao, cam kết, thông báo và nhật ký. Xác định khóa, ràng buộc và dữ liệu kiểm thử.
- [ ] **Nguyên, Tài** viết OpenAPI cho bốn vai trò. Chốt trường dữ liệu, trạng thái, lỗi, quyền theo từng đợt và thứ tự thực hiện các thao tác.
- [ ] **Như, Duy** xây lớp gọi API và lớp truy xuất dữ liệu. Nối thử XML với phản hồi giả lập đúng hợp đồng, gồm trạng thái tải, trống và lỗi.
- [ ] **Tài, Hào** thống nhất cấu hình dev, test và staging, cách chạy migration trên DB rỗng và cách nạp dữ liệu mẫu.

**Qua mốc khi** Android hiển thị được dữ liệu giả lập đúng hợp đồng, còn backend và database thống nhất tên trường, trạng thái và quyền.

#### G2 · Ngày 6–9 · Dựng API, PostgreSQL và đăng nhập

- [ ] **Hào** chạy migration và seed trên PostgreSQL dev/test. Tạo tài khoản mẫu cho bốn vai trò và dữ liệu một điểm phát.
- [ ] **Tài** kết nối API với DB. Làm xác thực, phiên đăng nhập, lỗi có mã rõ ràng và kiểm tra quyền trên server.
- [ ] **Như** kết nối đăng nhập thật từ Android và xử lý hết phiên, đăng xuất. Hoàn thiện đăng ký, xác minh email và khôi phục mật khẩu theo lịch NH04.
- [ ] **Tài, Hào** kiểm tra tài khoản gia đình chỉ đọc hồ sơ của mình, tình nguyện viên chỉ thao tác đợt được giao và nhà hảo tâm chỉ xem dữ liệu được phép.

**Qua mốc khi** có thể dựng DB mới, đăng nhập từ Android và chứng minh server chặn cả quyền sai vai trò lẫn quyền ngoài phạm vi dữ liệu.

#### G3 · Ngày 10–15 · Mở đợt, tiếp nhận, sắp xếp và cấp mã

- [ ] **Tài, Duy** làm luồng điều phối tạo đợt, công bố tiêu chí, lịch, hạn đăng ký, điểm nhận và số suất xác nhận. Áp dụng đóng tiếp nhận theo trạng thái và hạn ở server.
- [ ] **Nguyên, Như** nối danh sách đợt, chi tiết và tự đăng ký. Lưu hồ sơ vào DB, kiểm tra trùng và hiển thị “Đã tiếp nhận” ngay sau khi gửi hợp lệ.
- [ ] **Nguyên, Duy** làm đăng ký hộ có sự đồng ý liên hệ. Điều phối viên xem được hồ sơ, xét điều kiện và ghi lý do khi xử lý ngoại lệ.
- [ ] **Nguyên, Hào** thực hiện phân bổ và danh sách chờ có thứ tự. Kiểm tra gửi đồng thời khi chỉ còn một suất, hủy và cấp lại cho hồ sơ kế tiếp.
- [ ] **Nguyên, Như, Duy** cấp QR và mã ngắn từ server theo mỗi lượt được sắp xếp. Gia đình xem được mã và hướng dẫn nhận, điều phối viên cung cấp mã trực tiếp cho người được đăng ký hộ. Hoàn thiện màn theo dõi và lấy mã theo lịch NH06.

**Qua mốc khi** API chuyển đúng trạng thái trên PostgreSQL, cấp 30 suất cho 35 hồ sơ hợp lệ theo quy tắc đã chốt và thu hồi mã khi hủy. Còn 5 hồ sơ chờ trước khi thử ca hủy và cấp lại.

#### G4 · Ngày 16–20 · Phát suất, đồng hành và đối chiếu

- [ ] **Duy, Nguyên** nối màn tình nguyện viên với API. Cho phép chọn đợt được giao, quét QR hoặc nhập mã ngắn, kiểm tra rồi xác nhận sau khi trao suất.
- [ ] **Nguyên, Hào** kiểm tra mã sai đợt, đã dùng, hết hạn và bị thu hồi. Hai thiết bị xác nhận cùng một mã chỉ tạo một lượt giao.
- [ ] **Duy, Nguyên** xử lý thiếu mã, mất mạng và kết quả gửi chưa rõ. Chuyển điều phối viên hỗ trợ với danh sách dự phòng, rồi đối chiếu trước khi ghi đã giao.
- [ ] **Tài, Duy** cho nhà hảo tâm gửi nhiều cam kết theo đợt và ghi nhận mong muốn theo tuần hoặc tháng. Điều phối viên xác nhận số có thể cung cấp cho từng đợt.
- [ ] **Tài, Duy, Hào** làm màn báo cáo cam kết, xác nhận, thực chuẩn bị, đã trao, chưa trao và ghi chú xử lý phần còn lại. Đối chiếu số đã trao với lượt giao đã lưu.

**Qua mốc khi** quét và nhập mã đều dẫn đến cùng kết quả giao nhận. Báo cáo giả lập với 30 suất chuẩn bị, 28 suất đã trao và 2 suất chưa trao khớp dữ liệu lưu.

#### G5 · Ngày 21–24 · Tích hợp đủ bốn vai trò

- [ ] **Tài, Như** đưa kết quả sắp xếp, hủy và đổi lịch vào hộp thông báo trong app. Kiểm tra quyền xem và cập nhật sau khi tải lại.
- [ ] **Như, Duy** hoàn tất nối API cho các màn bàn giao. Kiểm tra gửi lặp, xoay màn hình, màn hình nhỏ, chữ lớn, bàn phím, quay lại và dữ liệu sau đăng xuất.
- [ ] **Hào** chạy từ tạo đợt đến báo cáo trên staging bằng tài khoản và dữ liệu giả. Thử tự đăng ký, đăng ký hộ, danh sách chờ, hủy, cấp lại, phát suất và cam kết của nhà hảo tâm.
- [ ] **Cả nhóm** sửa lỗi chặn nghiệm thu, chạy lại các ca liên quan và lưu bằng chứng theo môi trường, phiên bản và kết quả.

**Qua mốc khi** cả bốn vai trò chạy trên cùng API và database staging. Các luồng đúng quyền, sai quyền và mất mạng có kết quả kiểm tra.

#### G6 · Ngày 25–28 · Chạy thử bản bàn giao

- [ ] **Tài, Hào** cấu hình HTTPS, PostgreSQL có lưu trữ bền vững, migration, sao lưu và nhật ký đã che dữ liệu riêng.
- [ ] **Như, Duy** build APK release, thử cài và chạy luồng trọn vẹn trên thiết bị. Dùng camera thật để thử quét mã và một thiết bị thứ hai để kiểm tra ghi nhận trùng.
- [ ] **Hào** thử sao lưu và khôi phục vào DB riêng. Kiểm tra lại số hồ sơ, lượt giao và báo cáo sau khôi phục.
- [ ] **Nguyên, cả nhóm** chạy thử với dữ liệu giả. Ghi mức độ dễ đăng ký và hiểu kết quả của gia đình, thời gian kiểm tra tại điểm phát, thời gian đối chiếu của điều phối viên và các điểm còn vướng.

**Qua mốc khi** APK release cài được, chạy trọn luồng với API HTTPS và có biên bản kiểm thử thiết bị cùng kết quả khôi phục DB.

#### G7 · Ngày 29–30 · Nghiệm thu và bàn giao

- [ ] **Nguyên** chạy kịch bản demo cuối và đối chiếu từng mục nghiệm thu ở phần 16.
- [ ] **Như, Duy** bàn giao APK, tài khoản demo, hướng dẫn cài và hướng dẫn theo bốn vai trò.
- [ ] **Tài, Hào** bàn giao mã nguồn, tài liệu API, migration, seed, hướng dẫn dựng lại hệ thống, thao tác sao lưu và phục hồi.
- [ ] **Cả nhóm** chốt lỗi còn mở, người tiếp nhận, phạm vi chạy thử và hồ sơ nộp. Chỉ tiến hành thử với gia đình thật khi đơn vị tổ chức đồng ý và xác nhận quy trình tại điểm phát.

**Qua mốc khi** người tiếp nhận có thể dựng lại hệ thống, cài app và trình diễn trọn luồng từ tạo đợt đến đối chiếu bằng dữ liệu giả.

## 5. Kiến trúc và cấu trúc dự án dự kiến

### 5.1. Công nghệ

| Phần | Chọn cho kế hoạch tháng này | Người phụ trách |
| --- | --- | --- |
| Android | Kotlin + XML, giữ thiết kế hiện có và tách màn khỏi dữ liệu | Như, Duy |
| Gọi API | Một API client dùng chung, HTTP client và bộ chuyển JSON; chọn và khóa phiên bản ở G1 | Như; Tài hỗ trợ hợp đồng |
| Backend | Node.js + Express, REST API; một backend tổ chức theo module nghiệp vụ | Tài, Nguyên |
| Database | PostgreSQL; migration và seed có phiên bản, công cụ migration thống nhất tại G1 | Hào |
| Hợp đồng | OpenAPI và mẫu request/response/error; cập nhật cùng code | Nguyên chủ trì |
| Môi trường | Docker Compose cho DB và môi trường chạy cục bộ; staging riêng | Hào, Tài |
| Kiểm thử | Unit, HTTP + PostgreSQL test riêng, kiểm thử đồng thời, E2E trên Android | Cả nhóm, Hào tổng hợp |
| CI | Kiểm tra API/database và Gradle build; kết quả thiết bị ghi riêng | Hào; Như/Tài phối hợp |
| Triển khai | Một API HTTPS, một PostgreSQL có lưu trữ bền vững; chọn nơi chạy sau khi kiểm tra điều kiện ở G1 | Tài, Hào, Nguyên |

Chốt phiên bản runtime, thư viện và image cụ thể ở G1, ghi trong lockfile/cấu hình build. Không dùng nhãn `latest` cho môi trường bàn giao. Kế hoạch chưa mặc định một nhà cung cấp hosting hoặc mức phí chưa được nhóm kiểm tra.

### 5.2. Cấu trúc repo đích

```text
BuaComBenCon/
├── app/                         # Android hiện có
│   └── src/main/
│       ├── kotlin/vn/edu/buacombencon/
│       │   ├── core/            # API client, session, kết quả API, điều hướng chung
│       │   ├── data/            # DTO, repository, ánh xạ dữ liệu
│       │   ├── ui/auth/         # Như
│       │   ├── ui/caregiver/    # Như
│       │   ├── ui/coordinator/  # Duy
│       │   ├── ui/volunteer/    # Duy
│       │   ├── ui/donor/        # Duy
│       │   └── demo/            # Chỉ ở bản demo/debug nếu còn cần
│       └── res/                 # XML, chuỗi, màu và biểu tượng
├── backend/
│   ├── src/
│   │   ├── app.js               # Khởi tạo ứng dụng, không tự listen khi import
│   │   ├── server.js            # Listen, kiểm tra cấu hình, shutdown
│   │   ├── config/
│   │   ├── middleware/         # Auth, quyền, validation, lỗi và request ID
│   │   ├── modules/            # auth, campaigns, registrations, claims, pledges...
│   │   └── db/
│   ├── database/migrations/
│   ├── database/seeds/          # Chỉ dữ liệu giả và tài khoản thử theo môi trường
│   ├── tests/unit/
│   ├── tests/integration/
│   ├── tests/concurrency/
│   ├── .env.example
│   └── package.json
├── infra/                       # Compose, proxy HTTPS, mẫu triển khai
├── tools/                       # Công cụ dựng/check và vận hành
├── docs/
│   ├── KE_HOACH_PHAN_CONG_DU_AN.md
│   ├── requirements.md
│   ├── architecture.md
│   ├── erd.md
│   ├── data-dictionary.md
│   ├── openapi.yaml
│   ├── test-plan.md
│   ├── test-report.md
│   ├── deployment.md
│   ├── backup-restore.md
│   └── user-guide.md
└── README.md
```

Đây là cấu trúc đích; một số thư mục chưa tồn tại. Với mỗi module backend, tách route → middleware → controller → service → model/repository → DB. Controller không tự chứa toàn bộ quy tắc cấp suất. Phần Android dùng repository để màn hình không gọi trực tiếp SQL hay chứa dữ liệu nghiệp vụ làm nguồn quyết định.

Như phụ trách `core/`, `data/` dùng chung; Duy góp module/DTO qua PR. Hai người thống nhất giao diện lớp trước khi sửa. Nguyên phụ trách các module đăng ký/phân bổ/mã nhận; Tài phụ trách auth/đợt/nguồn suất/cam kết/báo cáo/thông báo. Hào phụ trách migration và seed; người viết API phải gửi yêu cầu schema kèm ví dụ truy vấn và test.

## 6. Quy tắc nghiệp vụ phải thống nhất trước khi code

| Mã | Quy tắc | Người triển khai chính | Cách kiểm chứng |
| --- | --- | --- | --- |
| BR01 | Gửi đăng ký chỉ tạo hồ sơ tiếp nhận; chưa chắc có suất | Nguyên, Như | Hồ sơ mới không có mã nhận hoạt động |
| BR02 | Hồ sơ người nhận độc lập tài khoản; đăng ký hộ không cần tạo email giả | Hào, Nguyên, Duy | Người không có `user_id` vẫn được cấp và nhận suất |
| BR03 | Một người nhận có một hồ sơ đăng ký cho một đợt; gửi lại request không tạo bản ghi trùng | Hào, Nguyên | Ràng buộc và HTTP test; tự đăng ký và đăng ký hộ dùng chung đối chiếu |
| BR04 | Không cấp quá tổng số suất đã xác nhận cung cấp | Nguyên, Hào | Test nhiều request đồng thời tranh suất cuối |
| BR05 | Chỉ hồ sơ được xác nhận đủ điều kiện mới được xét cấp suất | Nguyên | PENDING chưa kiểm tra không được ALLOCATED |
| BR06 | Xếp theo thời điểm hồ sơ đăng ký hợp lệ; trường hợp ưu tiên ngoại lệ phải có quyền và lý do | Nguyên, Duy | Server chặn bỏ qua người trước nếu không có lý do hợp lệ; audit có bằng chứng |
| BR07 | Hủy trước giới hạn của đợt thu hồi mã và giải phóng suất; người điều phối xác nhận cấp lại | Nguyên | Không còn dùng được mã cũ; danh sách chờ không bị nhảy lượt |
| BR08 | Mỗi lượt được cấp có QR/mã ngắn ngẫu nhiên, đúng đợt và thời hạn | Nguyên, Như | Mã sai đợt, hết hạn, bị thu hồi đều bị từ chối |
| BR09 | Chỉ xác nhận sau giao thực tế; một đăng ký tối đa một handover | Nguyên, Duy, Hào | Hai thiết bị cùng bấm chỉ có một bản ghi giao nhận |
| BR10 | Kiểm tra mã thành công chưa có nghĩa là đã giao | Nguyên, Duy | Có bước xác nhận riêng, đọc mã không làm tăng báo cáo |
| BR11 | Cam kết, xác nhận cung cấp và thực chuẩn bị là ba đại lượng riêng | Tài, Hào | Gửi cam kết không tự tăng khả năng cấp suất |
| BR12 | Số đã trao không vượt thực chuẩn bị; không giảm nguồn xác nhận xuống dưới số đã cấp mà bỏ qua đối chiếu | Nguyên, Tài, Hào | Test đồng thời cập nhật nguồn, cấp và giao |
| BR13 | Server quyết định trạng thái, thời hạn và quyền; giờ/role trên điện thoại không được dùng làm căn cứ duy nhất | Tài, Nguyên | Sửa request/đổi đồng hồ máy không vượt quyền hoặc vượt hạn |
| BR14 | Khi mạng lỗi, app không tự đổi sang Đã nhận; timeout phải tra lại trạng thái trước khi thử lại | Như, Duy | Ngắt mạng/mất response, thử lại không tạo giao trùng |
| BR15 | Nhà hảo tâm chỉ xem số tổng hợp; không truy cập danh tính người nhận bằng cách đổi URL/ID | Tài | Test API trả 403/404 theo quy ước, không trả dữ liệu riêng |
| BR16 | Thay đổi quan trọng có người thao tác, thời gian, lý do và vết audit | Nguyên, Tài, Hào | Tra được đăng ký hộ, cấp/hủy/cấp lại, đổi nguồn và giao nhận |
| BR17 | Khi kết thúc đợt, số chưa trao có giải thích; hồ sơ đã được cấp nhưng không đến được ghi nhận riêng | Tài, Nguyên, Duy | Báo cáo và danh sách trạng thái khớp database |

Không coi hai người có cùng số điện thoại là cùng một người một cách tự động. Hào và Nguyên thiết kế bước tìm/đối chiếu hồ sơ người nhận và hợp nhất có kiểm tra; tránh vừa đăng ký hộ vừa tự đăng ký làm phát sinh hai hồ sơ không liên quan.

## 7. Database từ thiết kế đến vận hành

**Chủ trì: Hào.** Nguyên và Tài review quy tắc/ràng buộc; Như và Duy kiểm tra các trường cần hiển thị.

### 7.1. Các bảng dự kiến

| Bảng | Dữ liệu chính và mục đích |
| --- | --- |
| `organizations` | Một nhóm tổ chức; lưu tên, đầu mối đã xác nhận và cấu hình nghiệp vụ |
| `distribution_points` | Điểm phát, địa chỉ/hướng dẫn, người phụ trách; tháng đầu chỉ vận hành một điểm |
| `users` | Tài khoản, email chuẩn hóa, password hash, trạng thái xác minh/khóa; không lưu mật khẩu rõ |
| `user_roles` | Vai trò đã được cấp, người cấp và thời gian; không nhận quyền staff từ request tự đăng ký |
| `sessions` | Hash refresh token/phiên, hạn dùng, thu hồi, xoay token và thông tin phục vụ kiểm tra phiên |
| `account_tokens` | Hash token xác minh email/khôi phục mật khẩu, thời hạn, đánh dấu đã dùng |
| `beneficiaries` | Người chăm sóc nhận hỗ trợ; `user_id` có thể null; thông tin liên hệ tối thiểu, đồng ý liên hệ, người tạo hồ sơ |
| `campaigns` | Ngày/bữa ăn, điểm phát, hạn đăng ký, khung phát, tiêu chí, trạng thái, số thực chuẩn bị và phiên bản đối chiếu |
| `volunteer_assignments` | Người phát được phân công vào đợt nào; kiểm soát quyền tại điểm phát |
| `registrations` | Người nhận + đợt, nguồn tự đăng ký/đăng ký hộ, xác minh điều kiện, thời điểm xếp hàng và trạng thái |
| `claim_codes` | Hash tra cứu QR/mã ngắn, bản mã để cấp lại nội dung cho người có quyền, thời hạn, đã dùng/thu hồi; không chứa danh tính trẻ |
| `handovers` | Một lượt giao cho một đăng ký, mã đã dùng, người giao, thời điểm và request truy vết |
| `pledges` | Từng cam kết nhà hảo tâm, số muốn hỗ trợ, mong muốn theo đợt/tuần/tháng, trạng thái và ghi chú liên hệ |
| `supply_commitments` | Số đã được đơn vị xác nhận cung cấp, nguồn từ cam kết hoặc đơn vị tự chuẩn bị; liên kết cam kết nếu có |
| `notifications` | Thông báo trong app theo tài khoản, loại sự kiện, dữ liệu tối thiểu, đã đọc hay chưa |
| `outbox_events` | Công việc gửi email/thông báo cần xử lý lại sau transaction; trạng thái retry, không tạo trùng |
| `audit_logs` | Sự kiện nghiệp vụ, actor, đối tượng, trạng thái trước/sau và lý do; metadata theo danh sách cho phép |
| `idempotency_keys` | Khóa request, actor, endpoint, hash nội dung và kết quả đã xử lý để thử lại an toàn |

Không lưu tên trẻ, bệnh án hoặc ảnh trẻ trong phạm vi này. Hào viết data dictionary mô tả kiểu, null/default, khóa, index, dữ liệu riêng và thời gian giữ dự kiến cho từng trường. Thời hạn giữ dữ liệu phải được nhóm và đơn vị tiếp nhận thống nhất; không tự gắn một thời hạn pháp lý chưa được xác minh.

### 7.2. Ràng buộc bắt buộc

- Khóa chính ổn định và khóa ngoại đúng đối tượng, kèm phạm vi tổ chức khi cần.
- Email chuẩn hóa có ràng buộc duy nhất theo mô hình tài khoản được chốt.
- `registrations` duy nhất theo `(campaign_id, beneficiary_id)`. Nếu được đăng ký lại sau hủy, cập nhật cùng hồ sơ, ghi audit và cấp thứ tự xếp hàng mới; không giữ ưu tiên cũ một cách ngầm định.
- `handovers.registration_id` duy nhất: kể cả cấp lại mã cũng không tạo được lượt giao thứ hai.
- Hash token/mã có index; một đăng ký không có hai mã nhận chưa dùng/chưa thu hồi cùng lúc theo quy tắc đã chốt.
- Số lượng không âm; số cam kết có giới hạn; thời điểm đóng/nhận có thứ tự hợp lệ.
- Trạng thái có tập giá trị rõ; hủy hoặc sửa sau khi đã giao phải đi theo quy trình đối soát, không xóa handover để “sửa số”.
- Index phục vụ danh sách theo đợt/trạng thái/thứ tự, lịch sử theo người nhận và tra mã.

Giới hạn tổng số suất là quy tắc liên quan nhiều bản ghi, không thể chỉ viết một `CHECK` đơn giản rồi coi đã chống được tranh chấp. Nguyên/Tài phải dùng transaction và khóa theo đợt nhất quán cho mọi thao tác làm đổi nguồn, phân bổ, hủy và giao; Hào viết test thật trên PostgreSQL. Tham khảo cơ chế khóa trong [tài liệu PostgreSQL](https://www.postgresql.org/docs/current/explicit-locking.html).

### 7.3. Nguồn số liệu báo cáo

| Chỉ số | Nguồn tính thống nhất |
| --- | --- |
| Cam kết | Tổng số mong muốn từ các cam kết còn hiệu lực; cam kết hủy/từ chối phải thể hiện riêng theo quy ước báo cáo |
| Xác nhận cung cấp | Tổng số xác nhận hiệu lực trong `supply_commitments`, gồm nguồn đơn vị và nguồn nhà hảo tâm; không cộng hai lần cùng một nguồn |
| Thực chuẩn bị | Số được điều phối ghi nhận cho buổi phát, có audit khi sửa |
| Đã trao | Số lượt giao hợp lệ trong `handovers`; không lấy số lần quét mã hoặc trạng thái cache trên điện thoại |
| Chưa trao | Thực chuẩn bị trừ đã trao; trong buổi phát chưa gọi đây là số người vắng |
| Người không đến nhận | Hồ sơ đã được cấp nhưng chưa nhận khi đợt được đóng/đối chiếu theo quy tắc |

Không giữ nhiều tổng đếm có thể sửa riêng mà không có nguồn đối chiếu. Nếu tạo trường đếm để tối ưu, phải cập nhật trong cùng transaction và có test so với nguồn gốc.

### 7.4. Migration, seed và an toàn dữ liệu

1. Tạo database dev, test và staging riêng; test không được tự rơi về `DATABASE_URL` dev/staging.
2. Mọi schema thay đổi qua migration có thứ tự; migration đã áp dụng dùng chung thì viết migration tiếp theo, không âm thầm sửa file cũ.
3. Seed chỉ dùng người/địa điểm giả, đánh dấu rõ; không đưa dữ liệu gia đình thật hoặc mật khẩu dùng chung lên Git.
4. Kiểm tra migration từ database trống và từ phiên bản trước. Sao lưu trước thay đổi trên môi trường có dữ liệu cần giữ.
5. Tài khoản ứng dụng không dùng quyền quản trị database để phục vụ request thường.
6. Thiết lập backup theo lịch đã chốt, mã hóa/giới hạn người đọc, lưu tách khỏi nơi chạy DB và thử phục hồi vào database khác.
7. Bàn giao kết quả restore gồm thời gian thực hiện, phiên bản DB, số bản ghi/đối chiếu và thao tác đã kiểm tra sau khôi phục. Có file backup chưa chứng minh restore được.

## 8. Backend và hợp đồng API

### 8.1. Nhóm API và chủ sở hữu

Tất cả endpoint bên dưới dự kiến nằm dưới `/api/v1`. Đây là hợp đồng cần triển khai, không phải mô tả API đã tồn tại.

| Nhóm | Endpoint dự kiến | Chủ trì | Android tích hợp |
| --- | --- | --- | --- |
| Auth | `POST /auth/register`, `/verify-email`, `/login`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` | Tài | Như |
| Hồ sơ | `GET /me`, `PATCH /me` | Tài | Như |
| Đợt hỗ trợ | `GET /campaigns`, `GET /campaigns/:id`, `POST /campaigns`, `PATCH /campaigns/:id`, `POST /campaigns/:id/open`, `/close-registration` | Tài | Như, Duy |
| Phân công người phát | `GET/POST/DELETE /campaigns/:id/volunteers...` theo schema chi tiết | Tài | Duy |
| Hồ sơ đăng ký hộ | `GET /beneficiaries`, `POST /beneficiaries`, thao tác đối chiếu/liên kết hồ sơ theo quyền | Nguyên | Duy |
| Đăng ký | `POST /campaigns/:id/registrations`, `GET /me/registrations`, `GET /campaigns/:id/registrations` | Nguyên | Như, Duy |
| Xét điều kiện | `POST /registrations/:id/review` | Nguyên | Duy |
| Phân bổ/chờ | `POST /registrations/:id/allocate`, `POST /campaigns/:id/promote-next` | Nguyên | Duy |
| Hủy | `POST /registrations/:id/cancel` | Nguyên | Như, Duy |
| Mã nhận | `GET /registrations/:id/claim-code`, thao tác thu hồi/cấp lại có kiểm soát | Nguyên | Như, Duy |
| Giao nhận | `POST /campaigns/:id/claims/check`, `POST /campaigns/:id/handovers` | Nguyên | Duy |
| Cam kết | `POST /campaigns/:id/pledges`, `GET /me/pledges`, `PATCH /pledges/:id` | Tài | Duy |
| Xác nhận nguồn | `POST /pledges/:id/confirm`, `/decline`; `GET/POST/PATCH /campaigns/:id/supply-commitments...` | Tài | Duy |
| Thực chuẩn bị/đóng đợt | `PUT /campaigns/:id/preparation`, `POST /campaigns/:id/reconcile` | Tài phối hợp Nguyên | Duy |
| Báo cáo | `GET /campaigns/:id/report`, `GET /reports/summary` | Tài | Như, Duy |
| Thông báo | `GET /me/notifications`, `POST /me/notifications/:id/read` | Tài | Như, Duy |
| Vận hành | `GET /health/live`, `GET /health/ready` | Tài | Hào kiểm tra |

Đến Ngày 5, `docs/openapi.yaml` phải ghi đủ method, path, auth, quyền, schema, ví dụ, phân trang và lỗi cho từng API trong bảng. Các dấu `...` phải được thay bằng endpoint cụ thể trước khi bắt đầu module tương ứng.

### 8.2. Quy ước dùng chung

- Thời gian lưu theo timestamp có múi giờ, truyền ISO 8601; hiển thị theo `Asia/Ho_Chi_Minh`. Không dùng số ngày tương đối trong bản ghi nghiệp vụ thật.
- Dùng ID ổn định do server/database sinh; client không tự đoán ID hoặc quyền bằng vị trí trong danh sách.
- Lỗi có `code`, thông báo dễ hiểu và `requestId`. Không đưa stack trace, SQL, mật khẩu, mã nhận hoặc token ra response/log.
- Danh sách có phân trang, bộ lọc và thứ tự ổn định; hồ sơ chờ dùng thứ tự server xác nhận.
- Các request ghi quan trọng có khóa idempotency; cùng khóa + cùng nội dung trả kết quả cũ, cùng khóa + nội dung khác bị từ chối.
- HTTP 401 yêu cầu xử lý lại phiên; 403/404 xử lý theo chính sách che dữ liệu; 409 cho xung đột nghiệp vụ; 429 cho quá số lần; 5xx/timeout không được đổi thành “thành công” trên app.
- Không tự tăng role từ dữ liệu client. Tài khoản công khai chỉ xin vai trò gia đình/nhà hảo tâm; staff do người có quyền nội bộ cấp qua quy trình được audit.
- Có kiểm tra đầu vào, giới hạn kích thước request, giới hạn thử đăng nhập/tra mã, truy vấn có tham số và HTTPS trên staging/bản bàn giao.

Tài chịu trách nhiệm cấu hình và kiểm tra bảo vệ API; Nguyên review các nhánh quyền của nghiệp vụ lõi. Các lựa chọn TLS, validation và xử lý lỗi dựa trên hướng dẫn [Express production security](https://expressjs.com/en/advanced/best-practice-security/); nhóm vẫn phải kiểm thử cấu hình triển khai thực tế.

### 8.3. Mã nhận và giao nhận không bị lặp

QR/mã ngắn tuần tự của bản UI chỉ dùng minh họa. Bản server dùng chuỗi ngẫu nhiên đủ dài, không chứa thông tin người nhận. Lưu hash phục vụ tra cứu; phần cần trả lại QR/mã ngắn cho người được quyền phải được lưu mã hóa, với khóa cấu hình ngoài Git. Nguyên và Tài chốt định dạng, độ dài, thời hạn và giới hạn thử ở G1.

`claims/check` chỉ kiểm tra. `handovers` mới thực hiện transaction: kiểm tra quyền theo đợt, khóa theo thứ tự thống nhất, kiểm tra mã/trạng thái/thời hạn/số thực chuẩn bị, tạo handover, đánh dấu mã đã dùng, ghi audit và kết quả idempotency rồi commit. Nếu lỗi ở giữa, rollback toàn bộ.

Nếu response bị mất sau commit, app tra lại hoặc gửi cùng khóa idempotency. Database phải còn đúng một handover. Khóa idempotency không thay thế ràng buộc duy nhất theo đăng ký; cần cả hai.

## 9. Công việc chi tiết của Nguyên — trưởng nhóm

**Đầu ra cuối tháng:** nghiệp vụ lõi chạy đúng, các phần ghép thành một hệ thống, hồ sơ nghiệm thu và gói bàn giao. Tổng ước lượng các nhiệm vụ: **100 giờ**, ngoài phần dự phòng chung.

| Mã | Giai đoạn/ngày đích | Công việc cụ thể | Phụ thuộc | Đầu ra và điều kiện hoàn thành | Giờ |
| --- | --- | --- | --- | --- | --- |
| N01 | G0, ngày 1–2 | Đọc đề xuất, chốt phạm vi một tháng, viết BR01–BR17, tiêu chí hỗ trợ và kịch bản nghiệm thu; chia việc và hỏi lịch rảnh cả nhóm | Tài liệu yêu cầu và UI hiện có | `requirements.md`, backlog có chủ sở hữu, danh sách giả định cần xác nhận; cả nhóm đọc được | 8 |
| N02 | G1, ngày 3–5 | Chốt kiến trúc, trạng thái, ma trận quyền và chủ trì OpenAPI; review ERD/API với Hào/Tài, hợp đồng với Như/Duy | N01; bản nháp H02, T02, NH02, D02 | `architecture.md`, hợp đồng đủ để hai phía làm độc lập, quy tắc cấp/hủy/giao không mâu thuẫn | 8 |
| N03 | G1–G2, xong ngày 9 | Tổ chức repo, branch/PR, cấu hình chạy chung; ghép smoke test app–API–DB, review nền tảng và xử lý phụ thuộc đầu tiên | T01, H03, NH03 | Thành viên mới làm theo README để chạy được môi trường; có đăng nhập và đọc dữ liệu thật | 8 |
| N04 | G2–G3, xong ngày 11 | Viết API hồ sơ người nhận, tự đăng ký, đăng ký hộ, đối chiếu trùng, xem đăng ký và kiểm tra điều kiện | N02, H03–H04, T03–T04 | Không cấp mã khi mới tiếp nhận; quyền sở hữu và trường hợp không có tài khoản được test | 8 |
| N05 | G3, xong ngày 15 | Viết phân bổ, xếp chờ, hủy, cấp tiếp theo, đăng ký lại đúng thứ tự; ngoại lệ có lý do; khóa transaction theo đợt | N04, T05, H05 | Kiểm thử tranh suất cuối đạt; không vượt số xác nhận; hủy thu hồi mã và giữ lịch sử | 14 |
| N06 | G3–G4, xong ngày 18 | Sinh/cấp lại/thu hồi mã; kiểm tra mã; giao nhận dùng một lần và idempotency | N05, H05; D06 và NH06 tích hợp theo hợp đồng | Sai đợt/hết hạn/đã dùng đều bị từ chối; hai request đồng thời chỉ ghi một lượt | 12 |
| N07 | G3–G4, xong ngày 20 | Audit thay đổi quan trọng, chuyển trạng thái và kết nối sự kiện thông báo; review đóng đợt với Tài | N04–N06, T07–T08 | Tra được người thao tác và lý do; không có trạng thái đổi nửa chừng hoặc thông báo trước commit | 8 |
| N08 | G4–G5, xong ngày 23 | Ghép luồng hai phía, xử lý khác biệt schema/lỗi; bảo đảm bản release không còn dùng DemoStore cho nghiệp vụ | NH05–NH08, D04–D08, API liên quan | Cùng một dữ liệu hiển thị nhất quán trên ít nhất hai thiết bị/tài khoản | 8 |
| N09 | G5, xong ngày 24 | Cùng Hào/Tài chạy race tests, rà quyền, giới hạn nguồn/chuẩn bị/giao, hủy đua với giao nhận | H06, T09, N05–N06 | Có log/test cho trường hợp đồng thời và bản sửa; không chỉ test mock | 8 |
| N10 | G6, ngày 25–27 | Điều phối sửa lỗi chặn nghiệm thu, chạy kịch bản tổng thể, review release và hướng dẫn sử dụng | H08–H10, NH09–NH11, D09–D11 | Không còn lỗi P0/P1; danh sách hạn chế còn lại được ghi rõ và chấp nhận đúng phạm vi | 8 |
| N11 | G6–G7, ngày 28–29 | Kiểm tra môi trường bàn giao, quyền truy cập, backup/restore, tag phiên bản và quyết định đạt/chưa đạt nghiệm thu | T10, H09–H11 | Checklist phát hành có bằng chứng, phiên bản và người chịu trách nhiệm vận hành | 6 |
| N12 | G7, ngày 30 | Tổng hợp báo cáo, thuyết trình quy trình, bàn giao source/APK/tài liệu; lập việc sau tháng đầu | T12, NH12, D12, H12 | Gói bàn giao mở được, liên kết đúng, không lẫn dữ liệu hoặc mật khẩu thật | 4 |

Nguyên cập nhật tiến độ mỗi ngày, review chéo và gỡ điểm nghẽn. Khi bản thân làm module lõi, Nguyên gửi Tài review; không tự bỏ review vì là trưởng nhóm.

## 10. Công việc chi tiết của Như — Android cho gia đình và tài khoản

**Đầu ra cuối tháng:** người chăm sóc đăng nhập, đăng ký, theo dõi và xuất trình mã bằng dữ liệu server thật; app có lớp gọi API dùng chung. Tổng: **100 giờ**.

| Mã | Giai đoạn/ngày đích | Công việc cụ thể | Phụ thuộc | Đầu ra và điều kiện hoàn thành | Giờ |
| --- | --- | --- | --- | --- | --- |
| NH01 | G0, ngày 1–2 | Rà tất cả màn gia đình hiện có, ghi hành trình, thông tin cần hỏi và lỗi cần giải thích | UI hiện có, N01 | Danh sách màn và trạng thái trống/đang tải/lỗi/chờ/được nhận/hủy; không hỏi thông tin bệnh án trẻ | 8 |
| NH02 | G1, ngày 3–5 | Thiết kế tách màn, repository, DTO, session và thành phần dùng chung với Duy; review OpenAPI | NH01, N02 bản nháp | Cấu trúc Android thống nhất, mock khớp tên trường/trạng thái, phân chia file rõ | 10 |
| NH03 | G1–G2, xong ngày 9 | Làm API client, parse lỗi, timeout, base URL theo môi trường, session/refresh và lưu token bằng cơ chế bảo vệ dựa trên Keystore | N02; mock trước, T03 để nghiệm thu thật | Gọi API ngoài main thread; không đóng băng UI; hết phiên/refresh lỗi không lặp vô hạn hoặc lộ token | 10 |
| NH04 | G2–G3, xong ngày 11 | Màn đăng ký tài khoản, xác minh email, login/logout, lấy lại mật khẩu, hồ sơ; ẩn bộ đổi role demo khỏi release | T03, NH03 | Đăng nhập thật, lỗi dễ hiểu; logout xóa dữ liệu phiên/cache riêng; không tự chọn quyền staff | 8 |
| NH05 | G3, xong ngày 14 | Nối trang chủ/lọc đợt/chi tiết/thời hạn/đăng ký vào API; kiểm tra biểu mẫu và đồng ý liên hệ | T04, N04, NH03 | Đăng ký ghi DB, chống bấm lặp; màn tiếp nhận không hứa chắc có suất; lỗi 409 hiển thị đúng | 12 |
| NH06 | G3–G4, xong ngày 17 | Màn đăng ký của tôi, kết quả sắp xếp, QR/mã ngắn lấy từ server và hướng dẫn nhận | N05–N06 | Không tự sinh BCxxxx theo thứ tự; chỉ chủ hồ sơ được lấy mã; mã thu hồi/hết hạn hiển thị đúng | 10 |
| NH07 | G4, xong ngày 20 | Hủy có xác nhận, lịch sử, thay đổi lịch và trạng thái cập nhật; xử lý khi quay lại app | N05, T04, T07 | Hủy thật được lưu; thiết bị khác đọc được; không khôi phục trạng thái cũ từ SharedPreferences | 10 |
| NH08 | G4–G5, xong ngày 22 | Hộp thông báo trong app, đánh dấu đọc; trạng thái offline/chưa xác nhận; giữ bản nháp biểu mẫu khi xoay | T08, NH03 | Thông báo đúng chủ tài khoản; tải lại thấy kết quả mới; lỗi mạng không hiển thị thành công giả | 8 |
| NH09 | G5–G6, xong ngày 25 | Kiểm tra máy nhỏ/chữ lớn/Back/bàn phím/rotation; lập và sửa lỗi thuộc luồng gia đình | H08 | Bằng chứng thiết bị/emulator; nút không bị bàn phím che; dữ liệu riêng không lẫn khi đổi tài khoản | 8 |
| NH10 | G6, ngày 25–27 | Cùng Hào chạy E2E; sửa lỗi parse/API/session, cấu hình build release và bản cài | N08, T10, H08 | APK release cài và kết nối staging HTTPS; không chứa debug endpoint hoặc thao tác khôi phục demo | 8 |
| NH11 | G6, ngày 28 | Viết hướng dẫn cài/chạy Android, luồng gia đình, ảnh màn hình và hạn chế | NH10 | Tài liệu đủ để người khác cài và làm theo; ảnh là từ phiên bản bàn giao | 4 |
| NH12 | G7, ngày 29–30 | Bàn giao APK, cấu hình build, phần Android dùng chung; trình diễn gia đình | NH11 | Người khác dựng lại được app; khóa ký được quản lý riêng, không đưa lên Git | 4 |

## 11. Công việc chi tiết của Tài — backend nền tảng và hỗ trợ

**Đầu ra cuối tháng:** backend có auth/quyền thật, các API hỗ trợ đầy đủ và môi trường HTTPS chạy được. Tổng: **100 giờ**.

| Mã | Giai đoạn/ngày đích | Công việc cụ thể | Phụ thuộc | Đầu ra và điều kiện hoàn thành | Giờ |
| --- | --- | --- | --- | --- | --- |
| T01 | G0–G1, xong ngày 5 | Dựng Express, cấu hình môi trường, cấu trúc module, validation, lỗi/request ID, health và script chạy | N01 sơ bộ, H01 | API khởi động được; thiếu cấu hình phải báo lỗi rõ; import app để test không tự mở cổng | 8 |
| T02 | G1, ngày 3–5 | Thiết kế tài khoản/phiên, quyền theo vai trò và theo đợt, email xác minh/reset; review schema identity | N02, H02 | Hợp đồng auth, ma trận quyền và quy trình tạo staff; không có API tự nâng role | 10 |
| T03 | G2, xong ngày 9 | Auth thật: hash mật khẩu, xác minh email, đăng nhập, xoay/thu hồi refresh, logout, reset một lần; middleware quyền | T02, H03–H04 | HTTP/DB test đúng-sai-hết hạn đạt; SMTP thử gửi được; Android đăng nhập thật với NH03–NH04 | 14 |
| T04 | G2–G3, xong ngày 11 | API đợt/điểm phát, danh sách/chi tiết, mở đóng/hạn đăng ký, phân công người phát | N02, H03, T03 | Server kiểm tra thời gian/quyền; người phát chưa được phân công không thao tác được | 10 |
| T05 | G3, xong ngày 14 | API nguồn xác nhận và thực chuẩn bị; kiểm tra giảm nguồn, tranh chấp với phân bổ; ghi audit | T04, H05, N05 phối hợp | Cấp suất dựa đúng nguồn xác nhận; không sửa số làm vi phạm lượt đã cấp/đã giao | 10 |
| T06 | G3–G4, xong ngày 19 | Cam kết nhà hảo tâm, sửa/hủy, mong muốn tuần/tháng, liên hệ, xác nhận một phần hoặc từ chối có lý do | T03–T05, H05 | Lưu được nhiều cam kết độc lập; xác nhận không bị cộng lặp khi thử lại; nhà hảo tâm không xem người nhận | 10 |
| T07 | G4, xong ngày 20 | Báo cáo theo đợt/tổng hợp, đóng và đối chiếu đợt, ghi cách xử lý suất còn lại | T05–T06, N06–N07 | Năm chỉ số khớp DB; chỉ báo cáo tổng hợp được chia sẻ; chặn sửa âm thầm sau đối chiếu | 8 |
| T08 | G4–G5, xong ngày 22 | Thông báo trong app, sự kiện sau commit, outbox email và retry có giới hạn; đánh dấu đọc | N07, T03, H05 | Sự kiện không mất hoặc lặp thông báo khi retry; không lộ token/mã trong log | 8 |
| T09 | G5, xong ngày 24 | Test quyền/IDOR, giới hạn đăng nhập/tra mã, validation, xử lý lỗi/timeout; sửa lỗi bảo mật và nghiệp vụ | Các module backend, H06 | Có test cả request bị từ chối; API không chỉ dựa vào việc ẩn nút trên Android | 8 |
| T10 | G5–G6, xong ngày 26 | Triển khai backend/proxy HTTPS, env/secrets, SMTP, readiness, restart và log; phối hợp DB/backup với Hào | H07, T09, N08 | Bản staging/bàn giao chạy được từ bên ngoài; DB không mở công khai; log đã che dữ liệu riêng | 6 |
| T11 | G6, ngày 27–28 | Hoàn chỉnh OpenAPI, lệnh vận hành, biến môi trường mẫu, cách cập nhật/quay lại phiên bản tương thích | T10 | Thành viên khác chạy được API theo tài liệu; `.env.example` không chứa giá trị bí mật thật | 4 |
| T12 | G7, ngày 29–30 | Bàn giao dịch vụ/API, quyền quản lý theo người tiếp nhận; trình diễn và trả lời phần backend | T11, H11 | Có phiên bản, nơi chạy, cách kiểm tra sức khỏe và đầu mối xử lý khi lỗi | 4 |

## 12. Công việc chi tiết của Duy — Android cho các vai trò hỗ trợ

**Đầu ra cuối tháng:** điều phối viên, tình nguyện viên và nhà hảo tâm làm việc qua API thật trên Android. Tổng: **100 giờ**.

| Mã | Giai đoạn/ngày đích | Công việc cụ thể | Phụ thuộc | Đầu ra và điều kiện hoàn thành | Giờ |
| --- | --- | --- | --- | --- | --- |
| D01 | G0, ngày 1–2 | Rà hành trình điều phối/phát suất/đồng hành, tình huống đông người, không có tài khoản hoặc không tìm thấy mã | N01, UI hiện có | Danh sách thao tác và trạng thái phải có; thông tin tại điểm phát dễ đọc | 8 |
| D02 | G1, ngày 3–5 | Thiết kế/tách màn, dùng thành phần chung của Như; chốt DTO và lỗi với Nguyên/Tài | D01, NH02, N02 | File/module không giẫm nhau; mock theo OpenAPI, không tự đặt schema khác | 8 |
| D03 | G1–G2, xong ngày 9 | Tạo nền màn hỗ trợ, điều hướng theo quyền server, nối danh sách đợt và API client | NH03, T03–T04 theo hợp đồng | Không hiển thị chức năng staff cho người không có quyền; request vẫn được server kiểm tra | 10 |
| D04 | G3, xong ngày 14 | Màn tạo/sửa/mở/đóng đợt, chọn người phát, ghi nguồn xác nhận và thực chuẩn bị | T04–T05, D03 | Dữ liệu ghi DB, xử lý giới hạn và xung đột; thời gian là ngày giờ cụ thể | 12 |
| D05 | G3–G4, xong ngày 17 | Đăng ký hộ, tra cứu người nhận, xét điều kiện, danh sách chờ, phân bổ/cấp tiếp, ngoại lệ có lý do | N04–N05 | Người không có tài khoản vẫn được hỗ trợ; cấp/hủy/cấp lại phản ánh trên máy gia đình | 12 |
| D06 | G4, xong ngày 18 | Tích hợp camera quét QR, nhập mã ngắn dự phòng, kiểm tra đúng đợt và xác nhận sau khi giao | N06, quyền camera và thiết bị thử | Người từ chối quyền camera vẫn nhập mã; quét liên tiếp không gửi nhiều xác nhận; không tự báo giao thành công | 10 |
| D07 | G4, xong ngày 20 | Màn nhà hảo tâm: tạo/sửa/hủy và xem từng cam kết, đề nghị tuần/tháng, trạng thái liên hệ/xác nhận | T06 | Không chỉ giữ một cam kết demo; số xác nhận do đơn vị phản hồi riêng | 10 |
| D08 | G4–G5, xong ngày 22 | Màn báo cáo, đối chiếu cuối đợt, ghi chú phần chưa trao, thông báo của vai trò hỗ trợ | T07–T08 | Số liệu khớp API; không đưa danh sách cá nhân vào màn công khai/nhà hảo tâm | 8 |
| D09 | G5, xong ngày 24 | Test sai mã/đã dùng/sai đợt, mạng mất/timeout, thu hồi quyền, thao tác trên hai thiết bị; sửa lỗi | N06, T09, H08 phối hợp | Có bằng chứng cảnh báo đúng và không giao trùng; app không giữ quyền staff cũ sau logout | 8 |
| D10 | G6, xong ngày 27 | Tích hợp APK release với Như; sửa lỗi camera/layout/parse; kiểm thử tại điểm phát giả lập | NH10, T10, H08 | Cài release trên thiết bị có camera, chạy đủ các vai trò hỗ trợ | 6 |
| D11 | G6, ngày 28 | Viết hướng dẫn thao tác cho điều phối, tình nguyện viên, nhà hảo tâm và tình huống cần trợ giúp | D10 | Người khác đọc hướng dẫn và thực hiện được, có ảnh đúng phiên bản | 4 |
| D12 | G7, ngày 29–30 | Trình diễn ba vai trò, bàn giao mã màn hỗ trợ và danh sách giới hạn còn lại | D11 | Quy trình thực hiện trọn vẹn; ghi rõ trường hợp phải gọi người điều phối | 4 |

Duy phối hợp với Như khi sửa lớp gọi API và thành phần dùng chung; camera hoặc trạng thái mạng phải được kiểm tra trên thiết bị, không chỉ trên bản xem trước bố cục.

## 13. Công việc chi tiết của Hào — database, QA và phục hồi

**Đầu ra cuối tháng:** cấu trúc dữ liệu có thể dựng lại, các ràng buộc được kiểm chứng và bộ bằng chứng nghiệm thu hệ thống. Tổng: **100 giờ**.

| Mã | Giai đoạn/ngày đích | Công việc cụ thể | Phụ thuộc | Đầu ra và điều kiện hoàn thành | Giờ |
| --- | --- | --- | --- | --- | --- |
| H01 | G0, ngày 1–2 | Kiểm kê môi trường/thiết bị/cổng DB, lập test strategy, dựng cách tách dev/test | N01 sơ bộ | Danh sách môi trường và cách không chạy test vào DB dùng chung; test-plan bản đầu | 8 |
| H02 | G1, ngày 3–5 | ERD, data dictionary, trạng thái, nguồn số liệu và quy tắc trùng người nhận; review cùng Nguyên/Tài | N01, T02, N02 bản nháp | ERD khớp API; hồ sơ không có tài khoản được mô hình hóa đúng; người Android review trường hiển thị | 12 |
| H03 | G1–G2, xong ngày 8 | Migration identity/campaign, Compose PostgreSQL, cấu hình DB app/test, lệnh migrate/check và index đầu tiên | H02 | Dựng được từ DB trống, API readiness kiểm tra DB thật; role ứng dụng có quyền phù hợp | 12 |
| H04 | G2, xong ngày 9 | Seed giả cho bốn vai trò, hồ sơ không tài khoản và đợt; fixture rõ phạm vi; kiểm tra migration nâng cấp | H03, T03 phối hợp | Không có tài khoản/mật khẩu thật trên Git; lặp seed theo quy ước không phá dữ liệu cần giữ | 8 |
| H05 | G2–G4, theo API liên quan, xong ngày 18 | Migration đăng ký/mã/giao/cam kết/nguồn/notification/audit; unique/index và truy vấn phục vụ nghiệp vụ | H02; schema chốt theo N04–N06 và T05–T08 | Unique theo người–đợt và handover; test constraint, transaction và các đường cập nhật cùng đợt | 10 |
| H06 | G3–G5, xong ngày 24 | Viết/chạy test HTTP + PostgreSQL và test đồng thời, kiểm tra rollback/retry, kết quả báo cáo | API từng phần, H05 | Test chạy trên DB riêng; có trường hợp lỗi/đua đồng thời, không chỉ tuyến thành công | 10 |
| H07 | G3–G5, xong ngày 22 | CI, mẫu môi trường, lịch backup, nơi lưu độc lập, script backup/restore và quy trình cập nhật migration | H03, T01, NH03 | CI có job API/DB/Android; backup không nằm trong repo; có cách kiểm tra thất bại | 10 |
| H08 | G4–G6, xong ngày 26 | Chạy E2E đủ vai trò trên app–API–DB và ít nhất hai phiên/thiết bị; quản lý danh sách lỗi và retest | N08, NH05–NH10, D04–D10 | Kịch bản nghiệm thu có phiên bản, expected/actual, bằng chứng và người sửa | 8 |
| H09 | G6, ngày 25–27 | Thử tải nhỏ, tranh suất/giao nhận, khởi động lại dịch vụ và phục hồi backup vào DB khác | H06–H07, T10 | Số liệu không sai sau test; restore thử thành công và ứng dụng đọc được dữ liệu phục hồi | 8 |
| H10 | G6, ngày 28 | Tổng hợp test-report, phân loại lỗi P0/P1/P2, đối chiếu số liệu và kiểm tra gói release | H08–H09 | Không còn lỗi P0/P1; mọi kết luận có môi trường và bằng chứng phù hợp | 6 |
| H11 | G6–G7, ngày 28–29 | Chốt migration/version DB, backup trước bàn giao, phân quyền truy cập và kiểm tra restore cuối | H09–H10, T11 | Có bản sao lưu và hướng dẫn tương ứng với phiên bản bàn giao; bí mật chuyển ngoài Git | 4 |
| H12 | G7, ngày 30 | Bàn giao ERD/data dictionary/seed/test/backup-runbook; trình diễn khôi phục và đối chiếu | H11 | Người khác hiểu và thực hiện lại được, có người tiếp nhận vận hành dữ liệu | 4 |

## 14. Trình tự phối hợp và đường phụ thuộc

### 14.1. Trình tự bắt buộc

1. **N01 + NH01 + D01 + H01** tạo bộ yêu cầu và tình huống sử dụng; Tài dựng skeleton độc lập trong T01.
2. **N02 + H02 + T02 + NH02 + D02** chốt hợp đồng. Có bản nháp để review qua lại; đến Ngày 5 mới khóa phiên bản v1. Không đợi một tài liệu hoàn hảo mới cho người khác đọc.
3. **H03/H04 + T03 + NH03/NH04** tạo luồng đăng nhập thật; **T04** cung cấp đợt cho hai phía Android.
4. **N04 + T05 + H05** cung cấp dữ liệu và giới hạn cho **N05**; Như/Duy làm màn với mock đã chốt rồi thay bằng endpoint thật từng phần.
5. **N05/N06** cung cấp mã và giao nhận cho **NH06/D06**; **T06/T07** cấp API nhà hảo tâm/báo cáo cho **D07/D08**.
6. **T08 + NH08/D08** hoàn tất thông báo; **N08/H06/H08** kiểm tra tích hợp xuyên suốt.
7. **T09/H09/N09** giải quyết quyền, tranh chấp và khôi phục; **T10/NH10/D10** tạo bản triển khai và APK release.
8. **H10/H11 + N10/N11** chốt nghiệm thu; cả năm người bàn giao theo N12/NH12/T12/D12/H12.

### 14.2. Mốc bàn giao giữa các cặp

| Cặp phối hợp | Bàn giao chậm nhất | Nội dung phải giao |
| --- | --- | --- |
| Nguyên/Tài/Hào → Như/Duy | Ngày 5 | OpenAPI v1, trạng thái, quyền, mẫu dữ liệu và lỗi |
| Hào → Tài/Nguyên | Ngày 8 | Migration nền tảng, kết nối dev/test, fixture, hướng dẫn chạy |
| Tài → Như | Ngày 9 | Auth dùng DB thật, tài khoản thử, xác minh/reset và response lỗi |
| Tài → Như/Duy | Ngày 11 | Danh sách/chi tiết/tạo đợt và phân công người phát |
| Nguyên → Như/Duy | Ngày 15 | Đăng ký, xét điều kiện, phân bổ/chờ/hủy và mã nhận cơ bản |
| Nguyên → Duy/Hào | Ngày 18 | Check mã và giao nhận có transaction/idempotency; test sai mã/đã dùng |
| Tài → Duy/Hào | Ngày 20 | Cam kết, xác nhận nguồn và báo cáo/đối chiếu |
| Như/Duy → Hào | Ngày 24 | APK tích hợp đủ vai trò, danh sách máy thử và lỗi đã biết |
| Tài/Hào → cả nhóm | Ngày 26 | Môi trường HTTPS, DB bền vững, tài khoản thử và log/backup vận hành |
| Cả nhóm → Nguyên | Ngày 28 | Code, tài liệu, bằng chứng kiểm thử và phần chưa đạt |

Nếu API thay đổi, người thay đổi cập nhật OpenAPI, ví dụ và thông báo cho người tích hợp ngay trong PR. Không để Như/Duy tự đoán từ response mới xuất hiện trên server.

## 15. Trạng thái và ma trận quyền để tránh làm lệch nhau

### 15.1. Trạng thái đăng ký

| Trạng thái | Ý nghĩa | Chuyển tiếp chính |
| --- | --- | --- |
| `PENDING` | Đã tiếp nhận, chưa xác nhận đủ điều kiện | Sau review đủ điều kiện: xét sang ALLOCATED/WAITING; không đạt: REJECTED; người dùng hủy: CANCELLED |
| `WAITING` | Đủ điều kiện, chưa có suất | ALLOCATED khi người điều phối cấp theo thứ tự; CANCELLED khi hủy; EXPIRED khi đóng đợt |
| `ALLOCATED` | Đã được cấp và có mã hoạt động | RECEIVED sau giao; CANCELLED theo quy tắc trước giờ; NOT_COLLECTED khi kết thúc mà không đến |
| `RECEIVED` | Đã xác nhận giao | Không quay về trạng thái chờ bằng API thông thường; sửa sai đi theo quy trình đối soát có audit |
| `CANCELLED` | Đã hủy, mã bị thu hồi | Có thể trở về PENDING trên cùng hồ sơ nếu đợt còn cho phép đăng ký lại; xếp thứ tự mới |
| `REJECTED` | Đã kiểm tra nhưng không đạt tiêu chí của đợt | Kết thúc cho đợt; việc xem xét lại phải có quyền/lý do và quy tắc riêng |
| `NOT_COLLECTED` | Được cấp nhưng không đến khi đợt đóng | Mã không còn dùng được; giữ lại để đối chiếu |
| `EXPIRED` | Hồ sơ chưa được cấp đã hết hiệu lực khi đóng đợt | Giữ lịch sử, không cấp/giao cho đợt đã đóng |

Tình trạng mã nhận và handover có dữ liệu riêng; không chỉ thay nhãn UI. Review đủ điều kiện lưu người/thời điểm/kết quả; quyết định cấp luôn kiểm tra lại nguồn và thứ tự trong transaction.

### 15.2. Phạm vi quyền tối thiểu

| Thao tác | Người chăm sóc | Điều phối viên | Tình nguyện viên | Nhà hảo tâm |
| --- | --- | --- | --- | --- |
| Xem đợt đang công bố | Có | Có | Có | Có |
| Tự đăng ký, xem/hủy đăng ký và lấy mã của mình | Có, đúng hồ sơ và hạn | Hỗ trợ theo quyền | Không tự truy cập hồ sơ riêng | Không |
| Đăng ký hộ, xem danh sách người nhận | Không | Có theo phạm vi tổ chức | Chỉ dữ liệu tối thiểu trong đợt được phân công | Không |
| Tạo/sửa đợt, xét điều kiện, cấp/chuyển suất | Không | Có | Không | Không |
| Kiểm tra mã, xác nhận giao | Không | Chỉ khi có quyền phát và được phân công | Có, đúng đợt được phân công | Không |
| Gửi/sửa cam kết của mình | Khi có thêm vai trò nhà hảo tâm | Theo tài khoản/vai trò phù hợp | Khi có thêm vai trò nhà hảo tâm | Có |
| Xác nhận nguồn/thực chuẩn bị/đóng đối chiếu | Không | Có theo quyền | Không | Không |
| Xem báo cáo tổng hợp | Có theo phạm vi công bố | Có | Theo đợt được giao | Có theo phạm vi công bố |
| Cấp quyền staff | Không | Chỉ người được cấp quyền quản trị nội bộ cụ thể | Không | Không |

Một tài khoản có nhiều vai trò phải được server cấp từng quyền; việc chọn không gian làm việc trên app chỉ là điều hướng trong quyền đã được cấp. Staff ban đầu tạo qua công cụ bootstrap nội bộ; không đưa mã/mật khẩu bootstrap lên Git.

## 16. Kế hoạch kiểm thử và tiêu chí nghiệm thu

### 16.1. Bộ tình huống bắt buộc

| Mã test | Tình huống | Người chuẩn bị/sửa chính | Kết quả mong đợi |
| --- | --- | --- | --- |
| AT01 | Tạo tài khoản/xác minh email/login đúng-sai | Tài, Như | Không dùng được tài khoản chưa đủ điều kiện; lỗi đăng nhập không tiết lộ thông tin riêng |
| AT02 | Logout, refresh hết hạn/thu hồi và reset token đã dùng | Tài, Như | Phiên cũ không tiếp tục gọi API; token khôi phục chỉ dùng một lần |
| AT03 | Tự gán role trong request hoặc mở màn staff bằng link | Tài, Duy | Server từ chối, không trả dữ liệu staff |
| AT04 | Gia đình A đổi ID để xem đăng ký/mã của B | Nguyên, Như | Không đọc được hồ sơ hoặc mã của người khác |
| AT05 | Tình nguyện viên thao tác đợt chưa được phân công | Tài, Nguyên, Duy | Bị từ chối dù mã nhận có tồn tại |
| AT06 | Nhà hảo tâm gọi endpoint danh sách người nhận | Tài | Không trả thông tin riêng |
| AT07 | Đợt hết hạn/đóng đăng ký và điện thoại bị đổi giờ | Tài, Nguyên | Quyết định theo thời gian/trạng thái server |
| AT08 | Đăng ký mới, bấm gửi hai lần, retry sau timeout | Nguyên, Như, Hào | Một hồ sơ; chưa tự cấp suất/mã |
| AT09 | Đăng ký hộ người không có tài khoản | Nguyên, Duy, Hào | Đủ luồng từ hồ sơ đến nhận, không cần email giả |
| AT10 | Tự đăng ký và đăng ký hộ cùng người | Nguyên, Hào | Đối chiếu người nhận; không cấp hai lượt vì tạo trùng hồ sơ |
| AT11 | Hồ sơ chưa xác nhận đủ điều kiện bị yêu cầu cấp | Nguyên | Không cấp vượt bước xét điều kiện |
| AT12 | Bốn hồ sơ hợp lệ tranh ba suất | Nguyên, Hào | Ba được cấp, một chờ theo thứ tự đã chốt |
| AT13 | Nhiều request đồng thời tranh suất cuối | Nguyên, Hào | Không vượt nguồn; người đứng trước/ngoại lệ được xử lý đúng quy tắc |
| AT14 | Hủy hồ sơ đã cấp, cấp cho người tiếp theo | Nguyên, Như, Duy | Mã cũ bị thu hồi, không cộng thêm tổng số đã cấp |
| AT15 | Đăng ký lại sau hủy | Nguyên, Hào | Không nhân bản đăng ký; thứ tự hàng chờ mới, không lấy ưu tiên cũ |
| AT16 | Mã không tồn tại/sai đợt/hết hạn/bị thu hồi | Nguyên, Duy | Báo cần kiểm tra; không tạo handover |
| AT17 | Check mã nhưng chưa bấm đã giao | Nguyên, Duy | Số đã trao giữ nguyên |
| AT18 | Hai điện thoại cùng xác nhận một mã | Nguyên, Hào, Duy | Chỉ một handover và một lần tăng báo cáo |
| AT19 | Commit thành công nhưng response giao nhận bị mất | Nguyên, Duy, Hào | Retry cùng khóa trả kết quả cũ; không giao hai lần |
| AT20 | Hủy diễn ra đồng thời với giao nhận | Nguyên, Hào | Chỉ một kết quả hợp lệ; không vừa hủy vừa có mã còn sử dụng được |
| AT21 | Giao quá thực chuẩn bị hoặc giảm nguồn dưới số đã cấp | Nguyên, Tài, Hào | Bị chặn hoặc đi qua quy trình đối chiếu được phép, không để số âm/sai |
| AT22 | Gửi cam kết, xác nhận một phần và thử lại xác nhận | Tài, Duy, Hào | Cam kết và xác nhận riêng; nguồn không bị cộng lặp |
| AT23 | Đóng đợt còn người chưa nhận | Tài, Nguyên, Duy | Mã hết hiệu lực; chưa trao và người vắng phân biệt được; có ghi chú xử lý |
| AT24 | Thông báo sắp xếp/hủy/thay đổi lịch | Tài, Như, Duy | Đúng chủ tài khoản, chỉ sau commit; không lộ thông tin của người khác |
| AT25 | API trả lỗi, mất mạng hoặc không cấp quyền camera | Như, Duy | Có trạng thái và cách thử lại/nhập mã; không hiển thị giao thành công giả |
| AT26 | Xoay máy, chữ lớn, bàn phím, Back và đổi tài khoản | Như, Duy | Không mất dữ liệu nhập cần giữ, không lẫn phiên, nút quan trọng sử dụng được |
| AT27 | Khởi động lại app/API/DB | Tài, Hào, Như | Dữ liệu bền vững, readiness phản ánh đúng; không trở lại seed mặc định |
| AT28 | Dựng DB trống và nâng cấp từ migration trước | Hào | Migration chạy đúng; giữ dữ liệu cần giữ |
| AT29 | Backup và restore sang DB độc lập | Hào, Tài | Dữ liệu/số liệu khớp; app thực hiện đọc/ghi kiểm tra trên môi trường phục hồi |
| AT30 | Clone mã nguồn, cấu hình mẫu, build API/app và cài APK release | Cả nhóm, Hào ghi nhận | Người không viết module dựng lại được theo README |

Mỗi test ghi môi trường, commit/build, dữ liệu đầu vào, expected/actual, thời gian và bằng chứng. Test tự động dùng database riêng. Test thiết bị và thử phục hồi có biên bản riêng; không thay thế bằng ảnh mock, ảnh layoutlib hoặc một dòng “unit test pass”.

### 16.2. Kịch bản nghiệm thu từ đầu đến cuối

**Dữ liệu giả do Hào chuẩn bị:** một đơn vị, một điểm phát, một đợt đang nhận ba suất; thêm hai đợt khác để thử mã sai đợt/chưa tới thời gian. Bốn người nhận A, B, C, D; D không có tài khoản và được đăng ký hộ. Có tài khoản điều phối, tình nguyện viên được phân công và nhà hảo tâm.

1. Nhà hảo tâm cam kết **10 suất** cho đợt; trước xác nhận, khả năng cấp chưa tự tăng.
2. Điều phối viên thống nhất/xác nhận thực cung cấp **3 suất** từ cam kết này, ghi rõ xác nhận một phần. Không cộng thêm ba suất nguồn đơn vị để tránh đếm hai lần.
3. A, B, C tự đăng ký; D được đăng ký hộ. Server tiếp nhận và lưu database.
4. Điều phối viên xét điều kiện theo thứ tự: A/B/C được cấp ba suất, D chờ.
5. B báo hủy trước giới hạn; mã của B bị thu hồi.
6. Điều phối viên cấp suất trống cho D; D được cung cấp mã qua điều phối, không cần đăng nhập.
7. Điều phối ghi **thực chuẩn bị 3 suất**. Tình nguyện viên kiểm tra/giao cho A và D trong thời gian hợp lệ.
8. Thử lại mã A và thử dùng mã B: không giao thêm. Thử hai thiết bị cùng xác nhận một mã trên bộ dữ liệu kiểm thử riêng: chỉ có một handover.
9. C không đến. Sau khi đóng đợt, báo cáo: **cam kết 10; xác nhận 3; chuẩn bị 3; đã trao 2; chưa trao 1**. C được ghi nhận không đến; điều phối bổ sung cách xử lý suất còn lại.
10. Nhà hảo tâm xem đúng số tổng hợp mà không thấy thông tin cá nhân của A/B/C/D.
11. Đóng/mở lại app và khởi động lại backend: dữ liệu vẫn còn. Hào backup rồi restore sang DB khác, đối chiếu lại các số trên.

Không thực hiện test phá dữ liệu, tranh chấp hoặc phục hồi thử trực tiếp lên database đang phục vụ người thật.

### 16.3. Điều kiện chấp nhận bản cuối

- Toàn bộ chức năng bắt buộc ở mục 2.1 có thể thực hiện với dữ liệu server/database thật.
- AT01–AT30 có kết quả; các test chặn phát hành phải đạt, không bỏ qua bằng việc đổi nhãn hoặc dùng dữ liệu cứng.
- Không còn lỗi P0/P1. P0 là lỗi mất/lộ dữ liệu, vượt quyền hoặc phá giao nhận; P1 là lỗi chặn luồng chính hoặc làm báo cáo/cấp/giao sai. P2 là lỗi nhỏ có cách xử lý, được ghi rõ.
- Có APK release đã cài và chạy trên ít nhất một điện thoại Android thật; có kiểm tra thêm emulator hoặc thiết bị khác để thử hai phiên/giao trùng. Nếu chưa có thiết bị thật, ghi chưa đạt phần này.
- Backend có HTTPS, DB bền vững, phiên bản xác định, backup và bản restore thử đã kiểm tra.
- Source không chứa mật khẩu/token/mã nhận thật, khóa ký app, khóa mã hóa, dữ liệu gia đình thật hoặc `.env` thật.
- Người khác dựng lại được hệ thống theo tài liệu và có người nhận trách nhiệm vận hành.

## 17. Triển khai và bàn giao từng bước

### 17.1. Lệnh dự kiến phải có

Đây là giao diện lệnh cần nhóm tạo và kiểm tra, **chưa phải xác nhận chúng đang có trong repo**.

| Lệnh/mục cấu hình | Mục đích | Người làm |
| --- | --- | --- |
| `docker compose -f infra/compose.yaml up -d db` | Khởi động PostgreSQL dev | Hào |
| `npm ci --prefix backend` | Cài đúng dependency từ lockfile | Tài |
| `npm --prefix backend run db:migrate` | Áp dụng migration đúng môi trường | Hào |
| `npm --prefix backend run db:seed:dev` | Dữ liệu giả cho dev, có kiểm tra môi trường | Hào |
| `npm --prefix backend run dev` | Chạy API cục bộ | Tài |
| `npm --prefix backend run check` | Kiểm tra tĩnh/lint/schema | Tài |
| `npm --prefix backend test` | Unit và các kiểm tra không phá dữ liệu dùng chung | Nguyên, Tài |
| `npm --prefix backend run test:integration` | HTTP + DB test riêng, cấu hình bắt buộc riêng | Hào |
| `npm --prefix backend run test:concurrency` | Tranh suất/giao nhận/nguồn trên DB test | Nguyên, Hào |
| `./gradlew :app:assembleDebug` | Build Android phát triển | Như |
| `./gradlew :app:assembleRelease` | Build Android bàn giao bằng khóa quản lý riêng | Như, Duy |
| Script backup/restore có môi trường đích rõ ràng | Sao lưu và khôi phục có kiểm tra | Hào |

Ngày bàn giao, README phải thể hiện đúng lệnh đã tồn tại và đã chạy. Nếu một lệnh đổi tên, sửa tài liệu/test tương ứng, không để người nhận làm theo lệnh dự kiến bị bỏ quên.

### 17.2. Quy trình phát hành

1. **Nguyên:** chốt commit ứng viên, danh sách tính năng và lỗi còn lại; dừng thay đổi chức năng mới sau Ngày 24 nếu chưa thật sự cần cho nghiệm thu.
2. **Hào:** đối chiếu schema, kiểm tra migration từ bản trước, tạo backup môi trường cần giữ dữ liệu; không dùng reset database để sửa nhanh.
3. **Tài:** cấu hình API/DB/SMTP/HTTPS và secrets qua nơi quản lý riêng; chỉ mở cổng cần thiết; health/live và health/ready phản ánh đúng trạng thái.
4. **Hào + Tài:** chạy migration và thử restart trên môi trường đích; kiểm tra dữ liệu và log. Nếu phát hành không tương thích, dừng theo runbook; không tự rollback migration có thể làm mất dữ liệu.
5. **Như + Duy:** build APK release trỏ đúng API HTTPS; gỡ chức năng đổi role/demo/reset dữ liệu mẫu khỏi bản bàn giao; kiểm tra cài mới và nâng cấp nếu có.
6. **Hào:** thực hiện smoke/E2E với dữ liệu thử được phân biệt rõ; kiểm tra báo cáo và không có dữ liệu nhạy cảm trong log.
7. **Nguyên:** kiểm tra bằng chứng, ghi đạt/chưa đạt, gắn tag phiên bản sau khi điều kiện phát hành đạt và công bố gói bàn giao cho người nhận đã xác định.
8. **Cả nhóm:** bàn giao quyền truy cập qua kênh phù hợp, hướng dẫn sử dụng, backup/restore, người hỗ trợ khi có lỗi; theo dõi phiên chạy thử và ghi việc cần cải thiện.

Không xem việc đưa bản staging lên mạng là đã có đơn vị sử dụng thật. Nếu chưa xác nhận bên tiếp nhận, bàn giao ở mức môi trường nghiệm thu có tài khoản thử và ghi rõ trạng thái đó.

### 17.3. Cấu hình cần bàn giao nhưng không commit giá trị thật

`DATABASE_URL`, cấu hình DB test riêng, khóa ký/xác thực phiên, khóa mã hóa mã nhận, cấu hình SMTP, URL API/public URL, giới hạn thử đăng nhập/tra mã, múi giờ, cấu hình backup, khóa ký APK và thông tin tiếp nhận vận hành.

Tài và Hào cung cấp `.env.example` với mô tả từng biến và giá trị minh họa an toàn. Như cung cấp cấu hình build debug/release và cách truyền khóa ký ngoài repo. Không gửi toàn bộ `.env` thật trong báo cáo học phần.

## 18. Cách làm việc và theo dõi tiến độ

### 18.1. Git và review

- Repository đích: `git@github.com:NguyenPhan161206/BuaComBenCon.git`.
- Tại thời điểm lập kế hoạch, nhánh mặc định được kiểm tra là `docs/project-description`. Nhóm dùng nhánh mặc định hiện hành để tích hợp; đổi tên sang `main` là việc quản trị riêng, không tự mặc định đã đổi.
- Mỗi nhiệm vụ có branch, ví dụ `feature/N05-allocation`, `feature/NH05-registration-ui`, `feature/T03-auth`, `feature/D06-qr-scan`, `feature/H03-database`.
- Commit theo phần đã hoàn thành và kiểm tra; PR ghi mã việc, hành vi thay đổi, cách chạy và bằng chứng. Migration/schema phải có người backend review; hợp đồng API phải có người Android đọc.
- Không để một PR ôm toàn bộ backend hoặc cả Android. Nguyên tích hợp đều từ tuần đầu, không đợi ngày cuối để ghép.
- Không commit file build tạm, cache IDE/Gradle, `.env`, keystore, credential, backup database hoặc dữ liệu thật. APK/ảnh mẫu bàn giao nếu lưu trong repo phải có mục đích và phiên bản rõ.

### 18.2. Nhịp làm việc

| Nhịp | Nội dung | Chủ trì |
| --- | --- | --- |
| Mỗi ngày, khoảng 10–15 phút | Mỗi người báo đã xong gì, tiếp theo làm gì, đang vướng ai; cập nhật mã việc | Nguyên |
| Khi đổi schema/API | Review nhanh người viết + người dùng API + Hào nếu ảnh hưởng DB | Chủ module |
| Cuối mỗi giai đoạn | Demo trên hệ thống đúng mức bằng chứng, chạy checklist qua mốc | Nguyên, Hào |
| Ngày 7, 14, 21, 28 | Rà kế hoạch tuần tiếp theo, giờ còn lại, phụ thuộc và lỗi chặn | Cả nhóm |
| Ngày 24 trở đi | Ưu tiên sửa lỗi, E2E, triển khai và tài liệu; chức năng mới phải xem tác động lịch | Nguyên |

Nguyên xử lý điểm nghẽn được báo trong ngày: giao người hỗ trợ, cắt việc mở rộng hoặc điều chỉnh thứ tự. Khi một việc chậm ảnh hưởng người khác, báo ngay thay vì chờ họ hỏi.

### 18.3. Mẫu cập nhật công việc

```markdown
### N05 — Phân bổ và danh sách chờ
- Người thực hiện: Nguyên
- Người review: Tài; Hào kiểm tra database/đồng thời
- Trạng thái: Chưa bắt đầu / Đang làm / Chờ review / Bị vướng / Hoàn thành
- Đầu vào: OpenAPI, nguồn xác nhận, schema registrations
- Đầu ra: API + transaction + test + tài liệu cập nhật
- Hạn mục tiêu: Ngày 15
- Đã kiểm tra: ghi lệnh, môi trường, commit và kết quả thực tế
- Vướng mắc: mô tả cụ thể, cần ai hỗ trợ, ảnh hưởng nhiệm vụ nào
- Liên kết: PR/commit/bằng chứng
```

“Hoàn thành” yêu cầu code, test, review và tài liệu tương ứng. Với phần tích hợp, còn phải có bằng chứng hoạt động cùng thành phần thật; mock đúng chỉ là một bước trung gian.

## 19. Rủi ro có thể làm trễ lịch một tháng

| Rủi ro | Dấu hiệu nhận biết | Người xử lý | Hành động trong lịch tháng |
| --- | --- | --- | --- |
| Không đủ giờ rảnh | Ngày 2 chưa thống nhất được lịch; liên tục không có đầu ra | Nguyên | Rà lại phạm vi; giữ lõi bắt buộc; không nhận thêm chức năng mở rộng |
| Backend và Android chờ nhau | Màn xong nhưng chưa có tên trường/lỗi thống nhất | Nguyên, Tài, Như, Duy | Chốt OpenAPI Ngày 5, có mock theo hợp đồng, ghép mỗi endpoint khi sẵn |
| Hai người sửa cùng MainActivity | Conflict liên tục hoặc mọi màn nằm chung một file | Như, Duy | Tách màn và thành phần chung sớm; giao chủ file và review phần dùng chung |
| Database đổi muộn | Sửa bảng trực tiếp hoặc API liên tục thiếu trường | Hào, Nguyên, Tài | Review ERD trước G2; migration tiếp theo có test nâng cấp, không sửa âm thầm |
| Phân quyền chỉ làm trên UI | Dùng request thủ công vẫn đọc được dữ liệu người khác | Tài, Nguyên | HTTP test quyền theo actor và đối tượng ngay từ G2, không chờ tuần cuối |
| Cấp/giao trùng khi nhiều thiết bị | Unit đạt nhưng race test làm số vượt giới hạn | Nguyên, Hào | Khóa/transaction/unique/idempotency, test trên PostgreSQL thật |
| Camera/SMTP/hosting chưa chạy được | Đến tuần 3 chưa có thiết bị hoặc môi trường | Duy, Tài, Hào | Thử sớm từ G2; giữ nhập mã dự phòng, chọn môi trường khả thi; ghi phần còn chưa đạt |
| Backup có mà không restore được | Chưa thử khôi phục đến Ngày 25 | Hào, Tài | Ưu tiên restore test trước phát hành; không gọi bàn giao hoàn tất nếu dữ liệu chưa khôi phục được |
| Phát sinh yêu cầu mới cuối tháng | Muốn thêm thanh toán, chat hoặc nhiều bệnh viện | Nguyên | Ghi backlog sau tháng đầu; chỉ nhận việc cần để sửa luồng nghiệm thu đã chốt |

Nếu camera chưa đạt nhưng nhập mã đã đạt, nhóm có thể trình diễn phần nhập mã và ghi camera chưa hoàn thành; không đánh dấu toàn bộ phạm vi bắt buộc đạt. Tương tự, không có thiết bị hoặc đơn vị tiếp nhận phải ghi đúng thiếu bằng chứng tương ứng.

## 20. Checklist bàn giao cuối tháng

### 20.1. Chức năng

- [ ] Có tài khoản, xác minh/khôi phục, đăng nhập và đăng xuất thật.
- [ ] Có quyền server theo vai trò, người sở hữu và đợt được phân công.
- [ ] Có đợt hỗ trợ với ngày giờ/thời hạn/tiêu chí đúng.
- [ ] Có tự đăng ký, đăng ký hộ và đối chiếu trùng hồ sơ.
- [ ] Có kiểm tra điều kiện, phân bổ, danh sách chờ, hủy và cấp lại.
- [ ] Có QR/mã ngắn do server cấp, thu hồi và kiểm tra hết hạn.
- [ ] Có quét camera và nhập mã dự phòng.
- [ ] Có giao nhận một lần, xử lý đồng thời/retry không giao trùng.
- [ ] Có nhiều cam kết nhà hảo tâm và đơn vị xác nhận riêng.
- [ ] Có ghi thực chuẩn bị, báo cáo và đối chiếu phần chưa trao.
- [ ] Có thông báo trong app và xử lý lỗi mạng rõ ràng.

### 20.2. Kỹ thuật và bằng chứng

- [ ] Repo đúng dự án, hướng dẫn mở/chạy khớp mã nguồn.
- [ ] ERD, data dictionary, migration, seed và OpenAPI được cập nhật.
- [ ] Database thật bền vững, tách dev/test/staging.
- [ ] Test unit/HTTP/DB/đồng thời và E2E có kết quả.
- [ ] APK release đã thử cài trên thiết bị và trỏ đúng API HTTPS.
- [ ] API/DB restart không mất dữ liệu hoặc tự nạp lại seed.
- [ ] Backup có lịch, quyền truy cập và kết quả restore đã kiểm tra.
- [ ] Không có bí mật hoặc dữ liệu gia đình thật trong Git/artifact công khai.
- [ ] Không còn lỗi P0/P1; phần chưa đạt và hạn chế được ghi rõ.

### 20.3. Hồ sơ và người tiếp nhận

- [ ] README từ clone đến chạy local, test và build.
- [ ] Hướng dẫn triển khai/cập nhật/kiểm tra sức khỏe/xử lý sự cố.
- [ ] Hướng dẫn gia đình, điều phối, người phát, nhà hảo tâm.
- [ ] Báo cáo kiểm thử, ảnh/video đúng phiên bản, danh sách thiết bị/môi trường.
- [ ] APK, tag/commit bàn giao, cấu hình mẫu và tài liệu nghiệp vụ.
- [ ] Khóa ký, secrets và quyền vận hành được bàn giao ngoài Git cho người nhận xác định.
- [ ] Có người phụ trách hỗ trợ, backup và xử lý khi dịch vụ gặp lỗi sau bàn giao.
- [ ] Nguyên tổng hợp và cả năm thành viên xác nhận phần việc thực tế đã hoàn thành.

## 21. Việc cần làm ngay trong ngày đầu

1. **Nguyên:** tạo backlog N01–N12/NH01–NH12/T01–T12/D01–D12/H01–H12; xác nhận lịch rảnh, phạm vi và ngày bắt đầu thực tế.
2. **Như:** chạy dự án Android, lập danh sách màn gia đình/tài khoản và xác định chỗ đang đọc DemoStore.
3. **Tài:** dựng skeleton backend, liệt kê API auth/đợt/nguồn/cam kết/báo cáo, lập `.env.example` chưa chứa secrets thật.
4. **Duy:** chạy các vai trò điều phối/phát suất/đồng hành, vẽ hành trình và liệt kê phần phải nối API/camera.
5. **Hào:** lập ERD bản đầu, kiểm tra môi trường PostgreSQL riêng cho dự án và soạn bộ dữ liệu giả 3 suất/4 người nhận.
6. **Cả nhóm:** chốt giờ trao đổi hằng ngày, nơi ghi lỗi/PR và điều kiện qua G0. Chưa cần triển khai tất cả chức năng ngay trong ngày đầu.

## 22. Nguồn và tài liệu tham khảo

- Yêu cầu sản phẩm: `docs/source/2403700211_2403700101_2403700160_2403700235_2403700173.docx`, “Đề xuất ứng dụng Bữa cơm bên con”, phiên bản 3.0.
- Hiện trạng Android: `README.md`, `docs/REQUIREMENTS.md`, `docs/QA.md`, `MainActivity.kt` và `DemoStore.kt`.
- [Express production security](https://expressjs.com/en/advanced/best-practice-security/): tham khảo TLS, kiểm tra đầu vào và bảo vệ dịch vụ.
- [PostgreSQL explicit locking](https://www.postgresql.org/docs/current/explicit-locking.html): tham khảo khóa/transaction cho thao tác đồng thời.
- [PostgreSQL backup and restore](https://www.postgresql.org/docs/current/backup.html): tham khảo các phương án sao lưu và khôi phục.

Các mốc 30 ngày, công nghệ đích, ước lượng giờ và cách phân công là phương án triển khai của nhóm trong tài liệu này; không phải yêu cầu đã ghi nguyên văn trong đề xuất gốc hoặc kết quả hệ thống đã chạy.
