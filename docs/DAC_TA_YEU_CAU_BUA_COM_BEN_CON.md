# Đặc tả yêu cầu ứng dụng Bữa cơm bên con

**Phiên bản đặc tả 1.0 · Phạm vi MVP · Chờ nhóm rà soát**

Bữa cơm bên con (Meals Beside You) hỗ trợ một nhóm thiện nguyện tổ chức phát suất ăn cho người chăm sóc bệnh nhi ung thư tại một điểm phát. Mỗi đợt có quy trình từ công bố, tiếp nhận hồ sơ, sắp xếp suất và cấp mã đến giao nhận và đối chiếu sau buổi phát.

Đề xuất sản phẩm phiên bản 3.0 xác định người dùng, phạm vi và luồng nghiệp vụ. Đặc tả này cụ thể hóa hành vi và tiêu chí nghiệm thu, đồng thời dùng kế hoạch 30 ngày để làm rõ phân quyền server, xử lý đồng thời và vận hành. Tài khoản và hộp thông báo trong app là phần bổ sung từ kế hoạch nhóm. Mục 9 ghi các quyết định đang chờ xác nhận. [Đề xuất gốc](source/2403700211_2403700101_2403700160_2403700235_2403700173.docx) · [Kế hoạch 30 ngày](KE_HOACH_PHAN_CONG_DU_AN.md)

## 1. Mục tiêu và phạm vi

| Người dùng | Kết quả sản phẩm cần đạt |
| --- | --- |
| Người chăm sóc | Biết đợt nào phù hợp, hồ sơ đang ở bước nào, có được cấp suất hay chưa và nhận ở đâu. Có thể nhận hỗ trợ qua điều phối viên dù thiếu thiết bị hoặc tài khoản. |
| Điều phối viên | Nắm nhu cầu và nguồn suất đã xác nhận, xử lý người đang chờ, quản lý ngoại lệ và giải thích được chênh lệch sau đợt phát. |
| Tình nguyện viên | Kiểm tra đúng mã trong đợt được giao, xác nhận sau khi trao suất và biết chuyển ai xử lý khi có vướng mắc. |
| Nhà hảo tâm | Gửi mong muốn hỗ trợ và biết số suất đã được xác nhận, đã chuẩn bị và đã trao qua báo cáo tổng hợp. |

**Trong MVP.** Một nhóm tổ chức, một điểm phát, nhiều đợt theo lịch. Phạm vi gồm tự đăng ký và đăng ký hộ, xét hồ sơ, cấp và thu hồi mã, giao nhận, cam kết cung cấp suất, thông báo trong app và báo cáo. Dùng dữ liệu giả để trình diễn và kiểm thử trước. Chỉ thử với gia đình thật khi đơn vị tổ chức đồng ý và có quy trình tiếp nhận, phát suất và bảo vệ dữ liệu đã thống nhất.

**Ngoài phạm vi.** Mở rộng nhiều đơn vị hoặc điểm phát độc lập, giữ tiền hoặc tự xác nhận chuyển khoản, hỗ trợ viện phí, đi lại, chỗ ở hoặc tư vấn dinh dưỡng điều trị. Push, SMS và tự đồng bộ lượt giao ngoại tuyến nằm ngoài kế hoạch tháng đầu. [Đề xuất gốc, mục 5.2](source/2403700211_2403700101_2403700160_2403700235_2403700173.docx) · [Phạm vi kế hoạch](KE_HOACH_PHAN_CONG_DU_AN.md#2-phạm-vi-phải-làm-và-phần-để-sau)

## 2. Vai trò và quyền

| Vai trò | Được thực hiện | Phạm vi dữ liệu |
| --- | --- | --- |
| Người chăm sóc | Xem đợt đã công bố, tự đăng ký, theo dõi, lấy mã, hủy theo hạn, xem lịch sử | Hồ sơ và mã của mình, hướng dẫn nhận và báo cáo đã công bố |
| Điều phối viên | Tạo đợt, xét hồ sơ, đăng ký hộ, cấp và thu hồi mã theo nghiệp vụ, xác nhận nguồn, ghi thực chuẩn bị và đối chiếu | Đợt và hồ sơ trong phạm vi được giao. Chỉ xác nhận giao nếu được cấp quyền phát và phân công vào đợt. |
| Tình nguyện viên | Chọn đợt được phân công, kiểm tra mã, xác nhận giao, chuyển ngoại lệ cho điều phối viên | Thông tin tối thiểu để đối chiếu và phát suất trong đợt được giao |
| Nhà hảo tâm | Xem nhu cầu đã công bố, gửi và theo dõi cam kết của mình, xem báo cáo | Cam kết của mình và số liệu tổng hợp theo phạm vi công bố |

Server kiểm tra vai trò, quyền trên đợt và quyền sở hữu hồ sơ ở từng thao tác. Bộ chọn không gian làm việc chỉ hiển thị quyền tài khoản đã được cấp. Quyền điều phối, phát suất và quản trị nội bộ được cấp qua quy trình có ghi nhận.

## 3. Vòng đời đợt hỗ trợ và hồ sơ

### 3.1. Đợt hỗ trợ

| Mốc nghiệp vụ | Điều kiện và tác vụ |
| --- | --- |
| Chuẩn bị công bố | Điều phối viên nhập ngày, bữa ăn, điểm phát, khung giờ nhận, hạn đăng ký, tiêu chí, quy tắc sắp xếp và đầu mối hỗ trợ. |
| Tiếp nhận | Đợt đang mở và còn hạn theo giờ server. Có thể tự đăng ký hoặc đăng ký hộ. Mọi hồ sơ đi vào cùng quy trình xét. |
| Ngừng tiếp nhận | Kết thúc nhận hồ sơ theo trạng thái hoặc hạn. Điều phối viên tiếp tục xử lý hồ sơ đã nhận và cấp lại suất trong giới hạn đã công bố. |
| Phát suất | Người phát được phân công kiểm tra mã và xác nhận giao trong khung giờ hợp lệ. Điều phối viên xử lý trường hợp cần trợ giúp tại điểm phát. |
| Đóng và đối chiếu | Rà soát lượt giao và ngoại lệ, phân loại hồ sơ còn lại, kết thúc hiệu lực mã và chốt báo cáo. Điều chỉnh sau chốt cần quyền và lý do. |

Các mốc mô tả nghiệp vụ. Hạn đăng ký, khung giờ nhận và thời điểm đóng đối chiếu là ba mốc thời gian riêng. Tên trạng thái lưu và cách xử lý việc mở lại đợt cần chốt ở G1.

### 3.2. Trạng thái đăng ký

| Trạng thái | Người dùng hiểu là | Điều kiện chuyển tiếp |
| --- | --- | --- |
| `PENDING` | Đã tiếp nhận, chờ xét | Điều phối xác nhận đủ điều kiện để xét `ALLOCATED` hoặc `WAITING`. Kết quả không đạt chuyển `REJECTED`. Có thể hủy theo quy tắc đợt. |
| `WAITING` | Đủ điều kiện, đang chờ suất | Khi có suất trống, điều phối xét theo thứ tự và chuyển sang `ALLOCATED`. Có thể hủy hoặc hết hiệu lực khi đóng đợt. |
| `ALLOCATED` | Được cấp suất | Chuyển `RECEIVED` sau xác nhận giao hợp lệ, `CANCELLED` khi hủy đúng hạn, hoặc `NOT_COLLECTED` sau đối chiếu nếu xác nhận không đến nhận. |
| `RECEIVED` | Đã giao suất | Lưu lượt giao trong lịch sử. Sửa sai phải qua đối chiếu có ghi nhận. |
| `CANCELLED` | Đã hủy | Mã đã cấp bị thu hồi. Nếu được đăng ký lại, dùng cùng hồ sơ, chuyển `PENDING` và nhận thứ tự mới. |
| `REJECTED` | Đã xét, không đạt tiêu chí | Lưu kết quả xét. Việc xem lại cần quyền, lý do và quy trình được nhóm chốt. |
| `NOT_COLLECTED` | Được cấp nhưng không đến nhận | Chốt khi kết thúc đợt sau khi đối chiếu. Mã hết hiệu lực. |
| `EXPIRED` | Hồ sơ chưa được cấp đã hết hiệu lực | Giữ trong lịch sử theo phạm vi được xem. Luồng phát suất dừng khi đợt đã đóng. |

Kết quả xét đủ điều kiện, hiệu lực mã và lượt giao có dữ liệu riêng. Một hồ sơ được cấp có thể có mã đã hết hạn hoặc cần cấp lại, vì vậy app hiển thị tình trạng mã cùng trạng thái hồ sơ. Kết quả `RECEIVED` phải dựa trên lượt giao được server lưu. [Trạng thái trong kế hoạch nhóm](KE_HOACH_PHAN_CONG_DU_AN.md#151-trạng-thái-đăng-ký)

## 4. Yêu cầu chức năng

### FR01 Tài khoản và phiên làm việc

**Kế hoạch nhóm.** Người dùng được đăng ký, xác minh email, đăng nhập, đăng xuất, lấy lại mật khẩu và sửa thông tin liên hệ. Khi hết phiên, app yêu cầu đăng nhập lại và giữ riêng dữ liệu từng tài khoản. Phương án lập kế hoạch dùng email và mật khẩu, cần xác nhận ở G0.

**Nghiệm thu.** Đăng nhập đúng quyền, đăng xuất xóa phiên trên app, phiên đã thu hồi bị server từ chối. Tài khoản tự đăng ký không thể tự cấp quyền điều phối hoặc phát suất.

### FR02 Công bố và tìm đợt hỗ trợ

Người chăm sóc xem lịch, bữa ăn, vị trí, hạn đăng ký và trạng thái tiếp nhận. Chi tiết đợt trình bày đối tượng hỗ trợ, quy tắc xếp suất, giờ nhận và đầu mối liên hệ. Điều phối viên tạo đợt, cập nhật thay đổi và quản lý mở hoặc đóng tiếp nhận. Server quyết định thao tác còn hợp lệ dựa trên thời gian của đợt.

**Nghiệm thu.** Đợt đã đóng tiếp nhận vẫn hiển thị hướng dẫn cần thiết. Yêu cầu đăng ký mới bị từ chối khi đã hết hạn, kể cả khi người dùng mở app từ trước hoặc đổi giờ điện thoại.

### FR03 Tự đăng ký và đăng ký hộ

1. Người chăm sóc chọn đợt và gửi thông tin xác nhận/liên hệ cần thiết. Điều phối viên có thể tìm hồ sơ sẵn có và đăng ký hộ khi người chăm sóc đồng ý.
2. Hệ thống kiểm tra đợt còn tiếp nhận và đối chiếu người nhận. Một người nhận có tối đa một hồ sơ trong một đợt. Số điện thoại dùng chung chỉ là dấu hiệu để đối chiếu.
3. Hồ sơ đạt yêu cầu tiếp nhận được lưu ở `PENDING`. App cho biết bước tiếp theo và nơi xem kết quả. Điều phối viên xét điều kiện nhận suất ở FR04.

**Nghiệm thu.** Gửi lại sau timeout chỉ có một hồ sơ. Tự đăng ký và đăng ký hộ cùng người đi qua cùng bước đối chiếu. Người được đăng ký hộ có thể nhận suất qua điều phối viên mà không cần tạo tài khoản.

### FR04 Xét điều kiện, phân bổ, danh sách chờ và hủy

- Điều phối viên xét theo tiêu chí đã công bố. Lưu kết quả xét, người xử lý và thời điểm. Quyền nhận suất không phụ thuộc vào tương tác công khai hay mức độ nổi tiếng của hồ sơ.
- Xếp hồ sơ đủ điều kiện theo thứ tự đăng ký hợp lệ của đợt. Khi hết suất xác nhận, đưa hồ sơ đủ điều kiện vào danh sách chờ. Bỏ qua thứ tự cần quyền, lý do và dấu vết để rà soát.
- Hủy đúng hạn thu hồi mã và giải phóng suất đã cấp nếu có. Điều phối viên xét cấp suất cho hồ sơ tiếp theo và cập nhật kết quả cho các bên liên quan.
- Cập nhật nguồn, cấp suất, hủy và ghi nhận giao phải kiểm tra nhất quán khi nhiều người thao tác cùng lúc. Giới hạn nguồn gồm các lượt đã cấp còn chiếm suất và lượt đã giao.

**Nghiệm thu.** 35 hồ sơ hợp lệ cho 30 suất tạo 30 lượt cấp và 5 hồ sơ chờ. Hai yêu cầu đồng thời tranh suất cuối chỉ có một lượt cấp hợp lệ. Sau một lượt hủy, mã cũ hết hiệu lực và người đứng đầu danh sách chờ được xét lại.

### FR05 Theo dõi, nhận mã và cập nhật kết quả

Người chăm sóc có danh sách đăng ký và lịch sử nhận. Khi được cấp suất, người chăm sóc xem QR, mã ngắn, thời gian, địa điểm và cách nhờ hỗ trợ. Điều phối viên chuyển mã trực tiếp cho người được đăng ký hộ theo kênh đã thống nhất. Mỗi mã gắn với một hồ sơ, một đợt và thời hạn sử dụng, không chứa thông tin nhận dạng hay bệnh án.

**Kế hoạch nhóm.** Hộp thông báo trong app cập nhật kết quả sắp xếp, hủy hoặc thay đổi lịch sau khi lưu thành công. Người đăng ký hộ nhận cập nhật qua điều phối viên nếu họ không dùng app.

**Nghiệm thu.** Hồ sơ chờ chưa có mã dùng được. Hủy hoặc cấp lại mã làm mất hiệu lực mã cũ. Đổi tài khoản hoặc truy cập hồ sơ người khác không trả nội dung mã.

### FR06 Kiểm tra mã và giao nhận tại điểm phát

1. Tình nguyện viên chọn đợt được giao, quét QR hoặc nhập mã ngắn. App cho phép nhập mã khi camera không dùng được.
2. Server kiểm tra quyền, đúng đợt, khung giờ, hiệu lực mã và trạng thái hồ sơ. Kết quả kiểm tra chỉ dùng để đối chiếu.
3. Sau khi giao suất thực tế, người phát bấm xác nhận. Server kiểm tra lại điều kiện, lưu lượt giao, đánh dấu mã đã dùng và cập nhật trạng thái hồ sơ trong cùng thao tác.
4. Với mã sai, mã đã dùng, thiếu mã hoặc mất mạng, app hướng dẫn chuyển bàn điều phối. Danh sách dự phòng theo quy trình đơn vị. Khi mất phản hồi, app hiển thị kết quả chưa xác định. Sau khi có mạng, app tra cứu lại hoặc thử lại an toàn để xác định lượt giao đã được lưu hay chưa.

**Nghiệm thu.** Chỉ kiểm tra mã giữ nguyên số đã trao. Hai thiết bị xác nhận cùng một hồ sơ chỉ tạo một lượt giao. Nếu mất phản hồi sau khi server lưu, thử lại cho thấy kết quả đã có. Khi hủy và giao đồng thời, nếu hủy được lưu trước thì xác nhận giao bị từ chối. Nếu giao được lưu trước, yêu cầu hủy thông thường bị từ chối.

### FR07 Cam kết của nhà hảo tâm và xác nhận nguồn

Nhà hảo tâm chọn đợt, gửi số suất mong muốn và có thể ghi nhận mong muốn theo tuần/tháng. Mỗi cam kết có trạng thái riêng để theo dõi liên hệ, xác nhận một phần hoặc từ chối. Điều phối viên xác nhận số có thể cung cấp cho từng đợt. Nguồn từ cam kết và nguồn tự chuẩn bị phải truy vết được để tránh đếm lặp.

**Nghiệm thu.** Nhà hảo tâm cam kết 10 suất, đơn vị xác nhận 3 suất. Khi chưa có nguồn khác, khả năng phân bổ tăng đúng 3 suất sau xác nhận. Thử lại cùng thao tác xác nhận giữ nguyên tổng nguồn.

### FR08 Chuẩn bị, đối chiếu và báo cáo

Điều phối viên ghi số thực chuẩn bị, theo dõi số đã trao từ lượt giao đã lưu và ghi cách xử lý suất còn lại sau buổi phát. Khi đóng đợt, điều phối viên xử lý kết quả còn treo của danh sách, chốt số tổng hợp và lưu dấu vết nếu điều chỉnh.

**Nghiệm thu.** Báo cáo đúng năm chỉ số ở mục 5, có ghi chú xử lý phần chưa trao khi chốt. Nhà hảo tâm chỉ thấy báo cáo tổng hợp theo phạm vi công bố. Kiểm tra giao nhận hoặc chỉnh số liệu trái quyền bị từ chối.

## 5. Chỉ số báo cáo và quy tắc đối chiếu

| Chỉ số | Nguồn tính | Dùng để biết |
| --- | --- | --- |
| Cam kết | Tổng số suất từ các cam kết còn hiệu lực theo quy ước báo cáo | Mức nhà hảo tâm muốn hỗ trợ |
| Xác nhận cung cấp | Tổng nguồn được điều phối xác nhận cho đợt, mỗi nguồn tính một lần | Giới hạn suất có thể cấp |
| Thực chuẩn bị | Số lượng điều phối ghi cho buổi phát | Số suất có thể bàn giao thực tế |
| Đã trao | Số lượt giao hợp lệ đã được lưu | Số suất giao đến người nhận |
| Chưa trao | Thực chuẩn bị trừ đã trao, tính tại thời điểm báo cáo | Phần suất còn lại cần theo dõi hoặc xử lý |

Số đã trao không vượt số thực chuẩn bị. Thao tác giảm nguồn xuống dưới số đã cấp phải bị chặn hoặc đi qua quy trình đối chiếu được cấp quyền. **Số suất chưa trao** đo phần suất còn lại. **Số người không đến nhận** đếm hồ sơ đã cấp được xác nhận không đến sau khi đóng đợt. Hai chỉ số có thể khác nhau, ví dụ khi thực chuẩn bị 30 suất, phát 28 suất và chỉ có 29 hồ sơ được cấp.

## 6. Dữ liệu và yêu cầu chất lượng

### 6.1. Dữ liệu tối thiểu

| Dữ liệu | Nội dung nghiệp vụ cần lưu |
| --- | --- |
| Người chăm sóc | Định danh nội bộ, thông tin liên hệ tối thiểu, ghi nhận đồng ý liên hệ. Liên kết tài khoản nếu có. |
| Đợt hỗ trợ | Điểm phát, lịch, hạn, tiêu chí, quy tắc xếp suất, trạng thái và thông tin nguồn. |
| Hồ sơ đăng ký | Người nhận, đợt, tự đăng ký hoặc đăng ký hộ, thời điểm hồ sơ hợp lệ, kết quả xét và trạng thái. |
| Mã nhận và lượt giao | Liên kết hồ sơ, thời hạn và hiệu lực mã, người xác nhận giao và thời điểm giao. |
| Cam kết và nguồn suất | Đợt, nhà hảo tâm nếu có, số cam kết, trạng thái, số được xác nhận và nguồn gốc xác nhận. |
| Đối chiếu và nhật ký | Kết quả sau đợt, ghi chú xử lý, người thực hiện, thời điểm, thay đổi và lý do khi cần. |

### 6.2. Yêu cầu chất lượng và cách kiểm tra

| Yêu cầu | Cách kiểm tra |
| --- | --- |
| Quyền riêng tư | Biểu mẫu không yêu cầu tên, ảnh hay bệnh án của trẻ. Kiểm tra báo cáo, thông báo và nhật ký để loại dữ liệu riêng, mã nhận và token khỏi nội dung hiển thị ngoài quyền. |
| Tiếp cận | Người thiếu thiết bị đi được toàn bộ luồng qua đăng ký hộ. Người từ chối quyền camera vẫn nhập được mã. Thử thao tác trên màn nhỏ, chữ lớn và khi xoay màn hình. |
| Tính nhất quán | Thử đăng ký lặp, tranh suất cuối, hủy đồng thời với giao, và hai thiết bị xác nhận cùng hồ sơ trên database kiểm thử. |
| Khả năng vận hành | Ghi nhận người thao tác và lý do với các thay đổi quan trọng. Làm rõ trạng thái chờ, lỗi mạng, kết quả chưa xác định và đầu mối xử lý ngay trên màn hình. |
| Bền vững dữ liệu | Theo kế hoạch nhóm, staging dùng HTTPS và PostgreSQL bền vững. Thử khởi động lại, sao lưu, khôi phục vào database riêng và đối chiếu hồ sơ/lượt giao/báo cáo. |

## 7. Kịch bản nghiệm thu xuyên suốt

| Bước | Thao tác trên dữ liệu giả | Kết quả kiểm tra |
| --- | --- | --- |
| 1 | Công bố một đợt có 30 suất xác nhận, tiếp nhận 35 hồ sơ hợp lệ. Dùng hồ sơ đăng ký hộ ở vị trí 31 theo thứ tự hợp lệ. | Hồ sơ chờ xét hiển thị đã tiếp nhận. Sau khi xét và sắp xếp theo thứ tự, 30 hồ sơ có suất và 5 hồ sơ chờ. |
| 2 | Một người được cấp hủy đúng hạn, điều phối cấp suất cho hồ sơ chờ đầu tiên | Mã đã hủy mất hiệu lực. Sau khi cấp lại, có 30 hồ sơ được cấp và 4 hồ sơ chờ. Người được đăng ký hộ nhận mã qua điều phối. |
| 3 | Điều phối ghi chuẩn bị 30 suất. Tình nguyện viên giao và xác nhận 28 suất, trong đó thử cả QR và mã ngắn | Có 28 lượt giao. Chỉ quét mã hoặc dùng lại mã đã giao không tăng số lượt. |
| 4 | Hai người đã được cấp không đến. Điều phối đối chiếu lượt giao, ghi cách xử lý và đóng đợt | Báo cáo 30 suất chuẩn bị, 28 đã trao, 2 chưa trao. Hai hồ sơ chuyển `NOT_COLLECTED`, bốn hồ sơ chờ chuyển `EXPIRED`. Nhà hảo tâm thấy số tổng hợp. |

Chạy thêm ca cam kết 10 suất và xác nhận 3 suất để kiểm tra hai đại lượng này độc lập. Kiểm tra sai quyền, sai đợt, mã hết hạn, mất kết nối và tranh chấp trên dữ liệu thử riêng. Khi nghiệm thu bản bàn giao, ghi phiên bản, môi trường, đầu vào, kết quả thực tế và bằng chứng. [Bộ kiểm thử AT01–AT30](KE_HOACH_PHAN_CONG_DU_AN.md#161-bộ-tình-huống-bắt-buộc)

## 8. Đánh giá tính hữu ích

Trao đổi với người chăm sóc và đơn vị tổ chức về quy trình hiện tại, sau đó quan sát cùng một tình huống khi dùng bản thử. Ghi điều kiện quan sát, cỡ mẫu và cách đo trước khi so sánh.

| Câu hỏi | Quan sát cần thu thập |
| --- | --- |
| Người chăm sóc có tự hoàn tất được thao tác cần thiết? | Tỷ lệ hoàn tất đăng ký trong nhóm thử, tỷ lệ đọc đúng trạng thái/điểm nhận, số lần cần nhờ hỗ trợ. Thử cả luồng đăng ký hộ. |
| Người phát có đối chiếu được tại điểm nhận? | Thời gian kiểm tra và xác nhận một lượt, số ca nhập mã dự phòng, số ca cần chuyển điều phối. |
| Điều phối viên có dễ tổng hợp và giải thích phần thiếu hoặc còn lại? | Thời gian từ kết thúc phát đến báo cáo, số chênh lệch phải rà, khả năng truy ra lý do của từng ngoại lệ. |

Các số 35/30 và 30/28/2 là dữ liệu minh họa. Kết quả kiểm thử xác nhận hành vi theo đặc tả. Kết luận về mức độ hữu ích cần bằng chứng từ khảo sát và sử dụng thử.

## 9. Quyết định cần chốt

| Khi nào | Nội dung | Nhóm cần thống nhất |
| --- | --- | --- |
| G0 | Tiêu chí tham gia và trường hợp ngoại lệ | Ai xét, thông tin tối thiểu để xét, trường hợp ưu tiên và cách ghi lý do. |
| G0 | Thứ tự xếp suất | Mốc tính hồ sơ hợp lệ, cách xử lý khi trùng thời điểm, hồ sơ thiếu thông tin và mốc hủy/cấp lại. |
| G0 | Tài khoản và đăng ký hộ | Phương án xác minh, thông tin liên hệ tối thiểu, cách nhận diện/đối chiếu người đăng ký và kênh cung cấp mã cho người không dùng app. |
| G1 | Hiệu lực mã và thời điểm đóng đợt | Khung tra mã, giới hạn thử, quy trình cấp mã thay thế, mở lại đợt hoặc xử lý hồ sơ còn chờ xét khi đóng. |
| G1 | Phần nguồn thay đổi và lượt giao ngoại lệ | Cách xử lý khi nguồn giảm, mất mạng tại bàn phát, người đã đến nhưng chưa giao, cũng như cách sửa lượt giao hoặc báo cáo sau chốt. |
| Trước chạy thử thật | Dữ liệu riêng và đo tính hữu ích | Ai được xem dữ liệu, giữ trong bao lâu, ai xử lý yêu cầu chỉnh/xóa và phương án thử do đơn vị tổ chức chấp thuận. |

## 10. Truy vết nguồn và hiện trạng

| Nguồn và nội dung | Yêu cầu trong đặc tả | Ca kiểm thử liên quan trong kế hoạch |
| --- | --- | --- |
| Word mục 1 và 5.2 · Vai trò, người hưởng lợi và phạm vi | Mục 1–2 | AT03–AT06, kịch bản xuyên suốt |
| Word mục 2.1–2.3 · Tìm đợt, đăng ký, theo dõi và đăng ký hộ | FR02, FR03, FR05 | AT04, AT07–AT10, AT14 |
| Word mục 3.1, 4.1 và 4.4 · Điều phối, xếp suất, danh sách chờ và ngoại lệ | FR02–FR04 | AT11–AT15, AT20–AT21 |
| Word mục 3.2 và 4.2 · Kiểm tra mã, giao nhận và mất kết nối | FR05–FR06 | AT05, AT16–AT21, AT25 |
| Word mục 3.3 và 4.3 · Đồng hành, số suất và đối chiếu | FR07–FR08, mục 5 | AT06, AT22–AT23 |
| Word mục 5.3 · Đánh giá tính hữu ích | Mục 8 | Quan sát sử dụng thử |
| Kế hoạch nhóm · Tài khoản, thông báo, phân quyền server và vận hành | FR01, FR05, mục 2 và 6.2 | AT01–AT06, AT24, AT26–AT30 |

Bản Android hiện tại trình diễn các luồng với dữ liệu mẫu cục bộ. [Đối chiếu yêu cầu với bản UI](REQUIREMENTS.md) ghi các chức năng đã có và khoảng trống. [Checklist G0–G7](KE_HOACH_PHAN_CONG_DU_AN.md#44-checklist-triển-khai-theo-giai-đoạn) dùng để lập tiến độ xây dựng API, cơ sở dữ liệu, tích hợp và nghiệm thu.
