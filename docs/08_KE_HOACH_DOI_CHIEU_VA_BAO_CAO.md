# 08. Chuẩn bị, đối chiếu và báo cáo

**Mục tiêu.** Điều phối viên theo dõi số suất chuẩn bị, đối chiếu lượt giao và chốt đợt. Nhà hảo tâm thấy báo cáo tổng hợp theo phạm vi công bố. Báo cáo giải thích phần suất chưa trao và tách riêng số người không đến nhận. [Đặc tả FR08 và chỉ số báo cáo](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr08-chuẩn-bị-đối-chiếu-và-báo-cáo)

**Hiện trạng.** App đã có màn báo cáo, hiển thị số cam kết, xác nhận, thực chuẩn bị, đã trao và chưa trao. Điều phối viên cập nhật được nguồn và số thực chuẩn bị. RPC `campaign_summary` đọc số giao hiện tại và dùng `archived_received` cho đợt lưu trữ. Cần bổ sung thao tác đóng đợt, phân loại hồ sơ còn lại, chốt và công bố báo cáo. [Màn báo cáo](../app/src/main/kotlin/vn/edu/buacombencon/ui/donor/DonorController.kt) · [RPC hiện tại](../supabase/migrations/20261124000003_business_logic_rpcs.sql)

## 1. Theo dõi trước và trong buổi phát

1. Điều phối viên mở **Quản lý đợt**, lấy nguồn xác nhận từ bước 07 và nhập số thực chuẩn bị. Server kiểm tra số đã trao ≤ số thực chuẩn bị ≤ số xác nhận. Nếu giảm số xác nhận, giữ đủ cho các lượt đã cấp còn hiệu lực và đã giao. [Bước 07 · Xác nhận nguồn](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md)
2. Hiển thị riêng năm chỉ số. **Cam kết** và **Xác nhận** lấy từ nguồn đã đối chiếu. **Thực chuẩn bị** lấy số điều phối lưu. **Đã trao** đếm lượt giao hợp lệ trong `handovers`. **Chưa trao** bằng thực chuẩn bị trừ đã trao.
3. Trong khi phát, ghi nhận giao bằng luồng phát suất. Làm mới báo cáo sau khi giao và đối chiếu số trên app với dữ liệu server. Mục **Người không đến nhận** chỉ có kết quả sau đối chiếu cuối đợt.

## 2. Đóng đợt và chốt báo cáo

1. Sau `pickup_ends_at`, Điều phối viên mở **Đối chiếu cuối đợt**, kiểm tra số đã trao, hồ sơ còn vé chưa dùng, hồ sơ đang chờ và các ngoại lệ. Nhập ghi chú cách xử lý suất dư nếu còn suất chưa trao.
2. Thêm migration cho trạng thái `NOT_COLLECTED` và `EXPIRED`, cùng bản chốt báo cáo theo đợt. Bản chốt lưu năm chỉ số, số hồ sơ không đến, ghi chú, người chốt, thời điểm chốt và trạng thái công bố. Đợt lịch sử đã có báo cáo giữ số đã trao từ snapshot cũ và giữ thông tin về nguồn của số liệu đó.
3. Thêm RPC đóng đợt với quyền Điều phối viên phụ trách. Trong cùng giao dịch, khóa đợt, kiểm tra đã hết giờ phát, xác nhận số giao, chuyển hồ sơ đã cấp chưa nhận sang `NOT_COLLECTED`, chuyển hồ sơ đang chờ hoặc đang tiếp nhận còn treo sang `EXPIRED`, thu hồi vé chưa dùng và lưu bản chốt. Hồ sơ đã trao, đã hủy và bị từ chối giữ kết quả hiện tại.
4. Sau khi RPC thành công, app hiện trạng thái **Đã đối chiếu**. Gửi lại thao tác đóng phải trả về bản chốt đã có. Đợt đã đóng dừng nhận hồ sơ, phân bổ, cấp lại và ghi lượt giao mới.

## 3. Công bố và chỉnh sau chốt

1. Điều phối viên xem trước và chọn **Công bố báo cáo**. Server lưu người và thời điểm công bố. Trước khi công bố, báo cáo chỉ hiển thị cho Điều phối viên phụ trách.
2. Tab **Kết quả** của Nhà hảo tâm đọc bản tổng hợp đã công bố. Dữ liệu gồm thông tin đợt, năm chỉ số, số người không đến và ghi chú công khai. Đường đọc report và quyền đọc dữ liệu đợt phải cùng tuân theo trạng thái công bố để bản chốt chưa công bố không xuất hiện từ API trực tiếp. Thông tin người nhận và mã vé nằm ngoài dữ liệu báo cáo công khai.
3. Ghi nhận yêu cầu chỉnh sau chốt bằng người thực hiện, lý do và dữ liệu trước/sau. Cho Điều phối viên phụ trách sửa ghi chú qua thao tác có lưu dấu vết. Nếu lệch số do dữ liệu nguồn, xử lý lượt giao hoặc kết quả đối chiếu qua thủ tục được cấp quyền rồi tính lại từ dữ liệu nguồn. Giữ bản chốt cũ trong lịch sử điều chỉnh.

## 4. Nghiệm thu trên localhost

| Ca thử | Điều kiện đạt |
| --- | --- |
| Đối chiếu đủ năm chỉ số | Với 40 suất cam kết, 30 xác nhận, 30 chuẩn bị và 28 lượt giao, báo cáo ra đúng 40, 30, 30, 28 và 2 suất chưa trao. |
| Phân biệt suất dư và người không đến | Nếu chỉ có 29 hồ sơ được cấp, 28 đã nhận thì có 2 suất chưa trao và 1 hồ sơ `NOT_COLLECTED`. |
| Đóng đợt | Hồ sơ còn treo được phân loại đúng, vé chưa dùng bị thu hồi, ghi chú xử lý suất dư được lưu. Gửi lặp không tạo thêm bản chốt. |
| Công bố và giới hạn dữ liệu | Nhà hảo tâm chỉ thấy bản công bố tổng hợp. Điều phối viên ngoài phạm vi không chốt, công bố hoặc chỉnh được đợt. |
| Chỉnh sau chốt | Thay đổi có lý do, người thực hiện và dấu vết trước/sau. Các lượt giao nguồn và bản chốt trước có thể đối chiếu. |
| Hồi quy luồng phát và số nguồn | Cập nhật nguồn dưới giới hạn bị từ chối. Xác nhận giao mã cũ sau chốt giữ nguyên số đã trao. |

**Hoàn thành khi** Android hiển thị báo cáo đúng cho từng vai trò và bộ kiểm thử SQL local đạt trên đợt thử. Ghi commit, thiết bị, mã đợt và số đối chiếu vào [QA](QA.md#nhánh-mvc-và-supabase) trước khi dùng kết quả để xét triển khai Supabase hosted.
