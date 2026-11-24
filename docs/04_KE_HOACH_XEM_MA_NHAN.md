# 04. Người chăm sóc xem trạng thái và mã nhận

Đầu ra của bước này là chủ hồ sơ thấy trạng thái hiện tại, mã nhận và hướng dẫn đến đúng điểm phát. Mã được dùng ở bước 05. [Bước 03 · Xét và phân bổ](03_KE_HOACH_XET_VA_PHAN_BO.md) · [Đặc tả FR05](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr05-theo-dõi-nhận-mã-và-cập-nhật-kết-quả)

## Luồng chính trên app

1. Đăng xuất Điều phối viên và đăng nhập đúng tài khoản Người chăm sóc đã gửi hồ sơ ở bước 02. Nếu tài khoản Người chăm sóc đang mở ở một thiết bị khác, chọn **Làm mới dữ liệu**.
2. Mở **Đăng ký**, tìm hồ sơ thuộc đợt đã chọn. Kiểm tra trạng thái **Được nhận**. Mở chi tiết hồ sơ hoặc nút xem mã nhận ở trang chi tiết đợt.
3. Xác nhận app hiển thị QR, mã ngắn, ngày, khung giờ và địa điểm. Dùng nút sao chép mã với dữ liệu giả để chuẩn bị kiểm tra ở bước 05. Mã có thể hiện trước giờ phát, còn hiệu lực giao thực tế do server kiểm tra theo `valid_from` và `valid_until`.
4. Đối chiếu mã trên app với vé của hồ sơ trong Studio. Nếu app báo mã chưa tải được, làm mới và đối chiếu lại quyền tài khoản cùng dòng vé. Ghi pass hoặc fail theo `registration_id`.

```sql
SELECT r.id, r.campaign_id, r.user_id, r.status,
       t.ticket_code, t.qr_payload, t.valid_from, t.valid_until,
       t.is_revoked, t.is_used
FROM public.registrations AS r
LEFT JOIN public.tickets AS t ON t.registration_id = r.id
WHERE r.id = '<registration_id>';
```

## Ca kiểm tra trạng thái và quyền xem

| Ca | Điều kiện đạt |
| --- | --- |
| Chờ xét hoặc đã xét đạt và chờ phân bổ | App hiển thị trạng thái tương ứng. Hồ sơ chưa có mã nhận dùng được. |
| Danh sách chờ | App hiển thị **Danh sách chờ**. Sau khi được cấp lại và làm mới, trạng thái chuyển sang **Được nhận** và có vé. |
| Từ chối, hủy hoặc đã giao | App hiển thị kết quả tương ứng và mã không còn dùng để nhận suất. Với hồ sơ hủy, đối chiếu `is_revoked`. Với hồ sơ đã giao, đối chiếu `is_used`. |
| Đổi tài khoản | Đăng xuất tài khoản A và vào Người chăm sóc B. Danh sách, chi tiết và mã của A không xuất hiện, kể cả khi bấm Quay lại. |
| Đăng ký hộ | Điều phối viên phụ trách xem mã từ **Quản lý đợt** để đưa cho người nhận tại bàn hỗ trợ. Hồ sơ hộ không có tài khoản sở hữu chưa xuất hiện trong danh sách của một tài khoản Người chăm sóc khác. |
| Sau khi triển khai đóng đợt ở bước 08 | Hồ sơ từng được cấp nhưng không đến hiển thị **Không đến nhận**. Hồ sơ chờ hoặc chờ xét bị kết thúc hiển thị **Hết hiệu lực**. Cả hai xuất hiện đúng trạng thái trong lịch sử và vé cũ đã hết hiệu lực. |

## Việc xử lý nếu ca thử chưa đạt

- Kiểm tra đường đọc từ `registrations` sang `tickets`, bộ lọc theo tài khoản và cách dựng UI trong `CaregiverController`. Kiểm tra thời hạn vé riêng khi diễn giải lý do mã mất hiệu lực.
- Khi bước 08 thêm `NOT_COLLECTED` và `EXPIRED`, cập nhật nhãn trạng thái, bộ lọc lịch sử và nội dung chi tiết hồ sơ cùng migration. [Bước 08 · Đóng đợt](08_KE_HOACH_DOI_CHIEU_VA_BAO_CAO.md#2-đóng-đợt-và-chốt-báo-cáo)
- Luồng hiện tại hiển thị kết quả khi tải hoặc làm mới dữ liệu. Ghi hộp thông báo trong app của FR05 thành việc riêng nếu phạm vi tiếp theo yêu cầu thông báo chủ động.

**Hoàn thành khi** mã ngắn trên app khớp với vé hợp lệ của chính hồ sơ, và đã thử đổi tài khoản để kiểm tra cách ly dữ liệu. [Tiếp theo · 05. Kiểm tra mã và giao suất](05_KE_HOACH_GIAO_SUAT.md)
