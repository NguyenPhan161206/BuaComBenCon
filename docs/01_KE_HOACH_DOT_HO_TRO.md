# 01. Điều phối viên chuẩn bị đợt hỗ trợ

Đầu ra của bước này là một đợt có thể nhận hồ sơ, đủ thông tin công bố, có suất xác nhận và có tình nguyện viên được phân công. Các bước 02–05 dùng cùng mã đợt và cùng hồ sơ để đối chiếu xuyên suốt. [Đặc tả FR02](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr02-công-bố-và-tìm-đợt-hỗ-trợ)

## Thứ tự thực hiện bộ kế hoạch

| Thứ tự nghiệm thu | Công việc | Kết quả chuyển tiếp |
| --- | --- | --- |
| 06 | [Tài khoản và phiên](06_KE_HOACH_TAI_KHOAN_VA_PHIEN.md) | Đăng nhập, đổi tài khoản và cách ly dữ liệu đúng vai trò trên Android. |
| 01–05 | [Chuẩn bị đợt](01_KE_HOACH_DOT_HO_TRO.md), [gửi hồ sơ](02_KE_HOACH_GUI_HO_SO.md), [xét và phân bổ](03_KE_HOACH_XET_VA_PHAN_BO.md), [xem mã](04_KE_HOACH_XEM_MA_NHAN.md), [giao suất](05_KE_HOACH_GIAO_SUAT.md) | Một hồ sơ đi từ `PENDING` đến `RECEIVED` trên cùng đợt thử. |
| 07 | [Cam kết và xác nhận nguồn](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md) | Tách nguồn từ cam kết khỏi nguồn đơn vị tự xác nhận. Chạy lại phân bổ với tổng nguồn đã tính. |
| 08 | [Đối chiếu và báo cáo](08_KE_HOACH_DOI_CHIEU_VA_BAO_CAO.md) | Đóng đợt, đối chiếu số đã giao và công bố báo cáo theo phạm vi. |

Chuỗi 01–05 dùng nguồn có sẵn của `c2` để thử luồng hiện tại trước. Khi 07 hoàn tất, tổng nguồn từ bước 07 đi vào bước 01 và 03 của một lượt thử mới. Bước 08 lấy số nguồn của bước 07 và lượt giao của bước 05 để chốt. Dùng đợt thử riêng cho các tình huống 07–08 khi bộ số liệu khác với `c2`.

## Chuẩn bị phiên nghiệm thu

1. Hoàn tất nghiệm thu phiên và quyền theo bước 06 trước lượt nghiệm thu chính thức. Chạy bản debug bằng **Run** trong Android Studio trên emulator hoặc điện thoại và kết nối Supabase local. Khi thử nhanh phiên bản hiện tại trong lúc làm bước 06, mở **Kết nối Supabase** từ menu demo và ghi đó là lượt thử riêng.
2. Chuẩn bị ba tài khoản mẫu theo vai trò Điều phối viên, Người chăm sóc và Tình nguyện viên. Nếu dùng chung một thiết bị, đăng xuất và đăng nhập đúng tài khoản ở mỗi bước. Mở Supabase Studio để đối chiếu bằng dữ liệu giả.
3. Ghi commit của bản app, thiết bị, thời điểm thử, `campaign_id` và sau bước 02 ghi thêm `registration_id`. Giữ hai mã định danh này trong báo cáo QA để theo dõi cùng một hồ sơ qua các bước. [Hướng dẫn dựng localhost và tài khoản mẫu](HUONG_DAN_TRIEN_KHAI_SUPABASE.md)

## Luồng chính dùng đợt mẫu c2

1. Dùng `c2` (**Thêm một bữa cơm lành**) trên một stack local dành riêng cho thử nghiệm, vừa nạp seed. Đợt này đã có điều phối viên sở hữu và tình nguyện viên mẫu được phân công. `c1` đã phân bổ sẵn, phù hợp cho kiểm tra riêng khâu phát và giao suất.
2. Trước khi bấm đăng ký, đối chiếu lịch với đồng hồ PostgreSQL. Ghi các mốc thực tế từ truy vấn dưới đây. Nếu `c2` đã qua hạn nhận mới hoặc đã phân bổ, chuẩn bị lại dữ liệu trên stack thử riêng rồi bắt đầu từ bước này.

```sql
SELECT id, coordinator_id, is_registration_open, is_archived,
       confirmed_supply, prepared_supply, allocated_at,
       clock_timestamp() AS server_now,
       registration_closes_at, completion_closes_at, allocation_at,
       cancellation_closes_at, reallocation_closes_at,
       pickup_starts_at, pickup_ends_at
FROM public.campaigns
WHERE id = 'c2';
```

| Việc cần làm với c2 | Mốc seed so với lúc nạp dữ liệu |
| --- | --- |
| Gửi hồ sơ mới | Trước phút 5 |
| Ghi nhận bổ sung, nếu thử hồ sơ thiếu | Trước phút 6 |
| Phân bổ lần đầu | Từ phút 7 đến trước phút 15 |
| Hủy hồ sơ đã cấp, nếu thử cấp lại | Trước phút 12 |
| Cấp lại từ hàng chờ | Trước phút 15 |
| Kiểm tra mã và giao suất | Từ phút 16, trước `pickup_ends_at` |

3. Đăng nhập Điều phối viên → **Đợt hỗ trợ** → **Quản lý đợt**. So sánh tên đợt, lịch, điều kiện nhận và chỉ tiêu với Studio. Kiểm tra `allocated_at` còn trống và số hồ sơ đã cấp của lượt thử ban đầu bằng 0.
4. Xác nhận có ít nhất một suất trong `confirmed_supply` và có đủ `prepared_supply` cho lượt thử. Số xác nhận là trần cấp suất. Số thực chuẩn bị là trần giao suất. Với bản hiện tại, dùng chức năng cập nhật nguồn của đợt nếu cần. Sau bước 07, điều phối cập nhật từng nguồn và server tính tổng xác nhận. Làm mới giao diện để kiểm tra giá trị đã lưu.
5. Kiểm tra bảng `campaign_volunteers` có dòng gán tình nguyện viên mẫu cho `c2`. Ghi tên đợt và hạn nhận hồ sơ để chuyển ngay sang bước 02.

## Kiểm tra riêng luồng tạo và quản lý đợt

1. Với Điều phối viên, mở **Đợt hỗ trợ → Tạo đợt**, nhập tên, ngày, loại bữa, khung giờ phát cùng ngày, số suất và tiêu chí. Chọn giờ bắt đầu phát còn cách giờ server hơn hai tiếng để hạn nhận hồ sơ nằm trong tương lai.
2. Lưu và đối chiếu dòng mới trong `campaigns`. Kiểm tra `coordinator_id`, phần nguồn và dãy thời gian. Bản hiện tại tính các hạn từ giờ bắt đầu phát trong RPC `create_campaign`. Việc cho nhập riêng từng mốc và đầu mối liên hệ thuộc FR02, cần ghi thành việc riêng nếu nghiệm thu theo toàn bộ đặc tả.
3. Cập nhật **Đã xác nhận** và **Thực chuẩn bị**, rồi đọc lại đợt. Thử số thực chuẩn bị lớn hơn số xác nhận và kiểm tra dữ liệu vẫn giữ giá trị hợp lệ trước đó.
4. Đóng tiếp nhận trước hạn. Kiểm tra Người chăm sóc thấy đợt đã đóng. Mở lại khi còn hạn và kiểm tra nút đăng ký xuất hiện. Với đợt hết hạn, server phải từ chối mở lại kênh nhận mới.
5. Nếu dùng đợt mới cho cả bước 05, chuẩn bị phân công tình nguyện viên trong dữ liệu thử. Bản hiện tại lấy quyền phát từ `campaign_volunteers` và chưa có thao tác phân công trong luồng tạo đợt. Nếu thiếu phân công, tiếp tục chuỗi chính bằng `c2`.

**Hoàn thành:** Đã nghiệm thu trên nhánh `hao-26/11` với mã đợt `c2`, quyền điều phối (`coordinator`), quyền phát suất (`volunteer` trong `campaign_volunteers`), các mốc server, số xác nhận (30) và thực chuẩn bị (30). [Chi tiết QA](QA.md#nghiệm-thu-kế-hoạch-01--chuẩn-bị-đợt-hỗ-trợ-nhánh-hao-2611) · [Tiếp theo · 02. Gửi hồ sơ](02_KE_HOACH_GUI_HO_SO.md)
