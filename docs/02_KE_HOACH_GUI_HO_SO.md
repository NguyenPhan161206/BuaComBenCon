# 02. Người chăm sóc xem đợt và gửi hồ sơ

Đầu ra của bước này là một hồ sơ mới ở trạng thái `PENDING`, thuộc đúng tài khoản và đúng đợt của bước 01. [Bước 01 · Chuẩn bị đợt](01_KE_HOACH_DOT_HO_TRO.md) · [Đặc tả FR03](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr03-tự-đăng-ký-và-đăng-ký-hộ)

## Luồng chính trên app

1. Đăng xuất tài khoản Điều phối viên. Đăng nhập Người chăm sóc và xác nhận app hiển thị vai trò Gia đình.
2. Mở **Trang chủ**, chọn đợt đã ghi ở bước 01 và đối chiếu ngày, điểm phát, tiêu chí và hạn nhận. Với `c2`, hoàn thành thao tác gửi trước `registration_closes_at`.
3. Mở **Đăng ký nhận suất**, nhập tên và số liên hệ giả dành riêng cho lượt thử. Đọc và đánh dấu đồng ý cung cấp thông tin. Ghi chú là tùy chọn.
4. Bấm **Gửi đăng ký** một lần. Xác nhận app mở màn kết quả tiếp nhận và hướng dẫn xem **Đăng ký của tôi**. Mở danh sách **Đăng ký** để thấy hồ sơ mới.
5. Đối chiếu bằng truy vấn dưới đây trong Studio. Thay `<campaign_id>` bằng mã đợt từ bước 01, ví dụ `c2`. Ghi lại `id` của hồ sơ vừa tạo và chuyển mã này cho bước 03.

```sql
SELECT id, campaign_id, user_id, recipient_id, status,
       is_assisted, consent_at, completed_info_at, queue_number, is_approved
FROM public.registrations
WHERE campaign_id = '<campaign_id>'
ORDER BY created_at DESC;
```

| Điểm đối chiếu | Kết quả mong đợi |
| --- | --- |
| Đợt và người gửi | `campaign_id` đúng và `user_id` là tài khoản Người chăm sóc vừa đăng nhập |
| Trạng thái | `PENDING`, `is_approved` còn trống |
| Dấu vết nhận hồ sơ | Có `consent_at`, `completed_info_at` và `queue_number` do server lưu |
| Loại đăng ký | `is_assisted = false` và hồ sơ xuất hiện trong danh sách của chính người gửi |
| Mã nhận | Chưa có vé cấp cho hồ sơ mới |

## Ca kiểm tra bổ sung

| Ca | Thao tác | Điều kiện đạt |
| --- | --- | --- |
| Thiếu thông tin hoặc sự đồng ý | Thử số liên hệ không hợp lệ hoặc bỏ chọn ô đồng ý | Form giữ thông tin đã nhập và hiển thị lỗi. Database giữ nguyên số hồ sơ. |
| Đợt vừa đóng | Mở form lúc đợt còn mở, đóng tiếp nhận bằng tài khoản Điều phối viên trên thiết bị khác rồi gửi | Server từ chối hồ sơ mới và app hiển thị lỗi. |
| Gửi lặp hoặc mất phản hồi | Bấm lại hoặc mở **Đăng ký** để đối chiếu trước khi thử lại với cùng tên và số liên hệ | Cùng người nhận trong cùng đợt có một hồ sơ hiệu lực. App có đường xem lại hồ sơ đã nhận. |
| Đăng ký hộ | Điều phối viên mở **Đăng ký hộ**, nhập thông tin giả và xác nhận sự đồng ý | Hồ sơ ở `PENDING`, có `is_assisted = true`, `assisted_by` và `consent_at`. Thử trùng với hồ sơ tự đăng ký của cùng người để xác nhận hai kênh cùng đối chiếu danh tính. |
| Bổ sung sau hạn nhận mới | Dùng hồ sơ đăng ký hộ đánh dấu còn cần bổ sung. Sau hạn nhận mới, dùng nút **Ghi nhận đủ thông tin** trước hạn bổ sung | Hồ sơ cũ nhận `completed_info_at` mới. Hồ sơ mới gửi cùng thời điểm bị từ chối. |

## Việc xử lý nếu ca thử chưa đạt

- Theo dõi từ form qua RPC `submit_registration` và `complete_registration`. Ưu tiên hiện lý do từ server và cho người dùng làm mới danh sách sau lỗi mạng.
- Ghi riêng các yêu cầu FR03 vượt quá form hiện tại. Bản hiện tại dùng tên và số liên hệ đã chuẩn hóa để đối chiếu, và RPC yêu cầu số điện thoại hợp lệ. Ca đăng ký hộ người không có điện thoại cần triển khai thêm phương thức liên hệ tại bàn hỗ trợ trước khi đánh dấu đạt FR03 đầy đủ.
- Số liên hệ trên hồ sơ đăng ký được ghi tại thời điểm gửi. Việc cập nhật thông tin tài khoản ở bước 06 giữ nguyên dữ liệu đã ghi trong hồ sơ cũ. [Bước 06 · Sửa liên hệ](06_KE_HOACH_TAI_KHOAN_VA_PHIEN.md#4-sửa-thông-tin-liên-hệ)

**Hoàn thành:** Đã nghiệm thu trên nhánh `hao-26/11`. Hồ sơ hợp lệ đã được tạo ở trạng thái `PENDING`, chưa cấp mã, có đầy đủ `consent_at`, `completed_info_at` và cơ chế chống trùng lặp. [Chi tiết QA](QA.md#nghiệm-thu-kế-hoạch-02--gửi-hồ-sơ-đăng-ký-nhánh-hao-2611) · [Tiếp theo · 03. Xét và phân bổ](03_KE_HOACH_XET_VA_PHAN_BO.md)
