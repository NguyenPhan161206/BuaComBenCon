# 07. Cam kết của nhà hảo tâm và xác nhận nguồn

**Mục tiêu.** Nhà hảo tâm gửi cam kết và theo dõi kết quả. Điều phối viên xác nhận toàn bộ, xác nhận một phần hoặc từ chối theo từng cam kết. Số có thể phân bổ được tính từ nguồn đã xác nhận và truy được về từng nguồn. [Đặc tả FR07](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr07-cam-kết-của-nhà-hảo-tâm-và-xác-nhận-nguồn)

**Hiện trạng.** App đã gửi được cam kết qua `save_commitment` và tăng số cam kết của đợt. Tab **Cam kết** đọc bản ghi mới nhất. Điều phối viên hiện nhập tổng nguồn xác nhận của đợt. Phần cần làm là danh sách cam kết, xử lý từng cam kết và ghi nguồn tự chuẩn bị riêng. [Màn Nhà hảo tâm](../app/src/main/kotlin/vn/edu/buacombencon/ui/donor/DonorController.kt) · [RPC hiện tại](../supabase/migrations/20261124000003_business_logic_rpcs.sql)

## 1. Chuẩn bị hợp đồng dữ liệu và cách tính

1. Bổ sung `confirmed_quantity` mặc định 0, kết quả xử lý, người xử lý, thời điểm và ghi chú lý do vào cam kết qua migration mới. Giữ `quantity` là số đề nghị ban đầu. Thể hiện xác nhận một phần khi `0 < confirmed_quantity < quantity`. Bổ sung trạng thái từ chối và giữ lịch sử bị từ chối hoặc đã hủy.
2. Tách nguồn tự chuẩn bị đã xác nhận khỏi số xác nhận từ từng cam kết. Tính `confirmed_supply` bằng **nguồn tự chuẩn bị + tổng số đã xác nhận từ cam kết**. Tính `pledged_supply` từ lượng đề nghị của các cam kết còn hiệu lực, gồm phần đề nghị ban đầu của cam kết được xác nhận một phần.
3. Kiểm tra dữ liệu cũ trước khi chuyển cách tính. Phân loại phần tổng đã xác nhận cũ theo nguồn có bằng chứng. Tách phần chưa xác định để điều phối đối chiếu trước khi đưa vào tổng phân bổ mới. Giữ số đã công bố của đợt lưu trữ làm mốc lịch sử.
4. Giữ các số tổng hợp mà luồng phân bổ và phát suất đang dùng. Mọi cập nhật nguồn vẫn chạy qua RPC, dùng cùng khóa đợt và giữ số xác nhận tối thiểu bằng số đã cấp hoặc đã giao, đồng thời giữ số thực chuẩn bị trong giới hạn hiện có.

## 2. Nhà hảo tâm gửi và theo dõi cam kết

1. Đăng nhập vai trò Nhà hảo tâm, mở **Đồng hành → đợt hỗ trợ → Gửi cam kết**. Kiểm tra thông tin liên hệ, nhập số suất và chọn mong muốn theo đợt, tuần hoặc tháng. Tuần và tháng là thông tin để liên hệ, mỗi lần gửi tạo cam kết cho đợt đã chọn.
2. Gửi qua RPC nhận mã yêu cầu do app tạo cho mỗi lần gửi. Server xác định người gửi từ phiên, xác thực sự đồng ý và đợt còn nhận cam kết. Lưu cam kết mới ở `PENDING` và trả mã bản ghi. Gửi lại cùng mã yêu cầu sau lỗi mạng phải trả về cam kết đã có.
3. Mở tab **Cam kết** để thấy danh sách các cam kết của mình theo đợt, số đề nghị, số được xác nhận và trạng thái. Đổi sang tài khoản Nhà hảo tâm khác để kiểm tra phạm vi hiển thị. Form báo lỗi liên hệ và dẫn tới mục cập nhật tài khoản khi cần. [Bước 06 · Tài khoản](06_KE_HOACH_TAI_KHOAN_VA_PHIEN.md)

## 3. Điều phối viên xác nhận nguồn

1. Mở danh sách cam kết trong **Quản lý đợt**. Hiển thị số đề nghị, trạng thái hiện tại và thông tin liên hệ trong phạm vi đợt phụ trách.
2. Sau khi đối chiếu, chọn **Xác nhận** và nhập số chấp nhận trong khoảng từ 1 đến số đề nghị, hoặc chọn **Từ chối** kèm lý do. Ghi người và thời điểm xử lý.
3. RPC ghi **giá trị xác nhận của cam kết**, tính lại tổng nguồn và lưu dấu vết trong cùng giao dịch. Gửi lại cùng kết quả giữ nguyên tổng. Nếu phiên khác đã chỉnh bản ghi, yêu cầu làm mới trước khi ghi quyết định mới. Giảm tổng nguồn chỉ được chấp nhận khi vẫn đáp ứng các lượt đã cấp, thực chuẩn bị và đã giao.
4. Dùng mục **Nguồn tự chuẩn bị** để ghi số đơn vị trực tiếp xác nhận ngoài cam kết. Màn quản lý hiển thị riêng cam kết, xác nhận từ cam kết, nguồn tự chuẩn bị và tổng có thể phân bổ.
5. Người hảo tâm làm mới tab Cam kết để kiểm tra kết quả. Chuyển các số đã đối chiếu sang bước đối chiếu và báo cáo. [Bước 08 · Đối chiếu và báo cáo](08_KE_HOACH_DOI_CHIEU_VA_BAO_CAO.md)

## 4. Nghiệm thu trên localhost

Dùng một đợt thử có số nguồn ban đầu bằng 0. Đối chiếu dòng trong `commitments`, tổng trong `campaigns` và trạng thái hiển thị trên Android.

| Ca thử | Điều kiện đạt |
| --- | --- |
| Cam kết 10, xác nhận 3 | Sau gửi, cam kết là 10 và xác nhận là 0. Sau xử lý, cam kết vẫn là 10, số được xác nhận là 3, tổng có thể phân bổ là 3. |
| Gửi và xác nhận lại | Gửi lại cùng mã yêu cầu giữ một bản ghi. Xác nhận lại 3 suất giữ tổng ở 3. |
| Thêm nguồn tự chuẩn bị 2 | Màn quản lý chỉ ra 3 suất từ cam kết và 2 suất tự chuẩn bị. Tổng có thể cấp là 5. |
| Từ chối và giảm nguồn | Cam kết bị từ chối không còn trong tổng cam kết hiệu lực. Yêu cầu giảm nguồn làm tổng thấp hơn số đã cấp bị từ chối. |
| Phân quyền và thao tác đồng thời | Nhà hảo tâm chỉ thấy cam kết của mình và không có thao tác xác nhận. Điều phối viên thao tác trong đợt được giao. Hai yêu cầu không tính lặp một nguồn. |

**Hoàn thành khi** nhóm thử được cam kết từ Android, điều phối xác nhận một phần, nhà hảo tâm đọc lại kết quả và số nguồn khớp trong database. Ghi commit, thiết bị và kết quả vào [QA](QA.md#nhánh-mvc-và-supabase).
