# 05. Tình nguyện viên kiểm tra mã và xác nhận giao suất

Đầu ra của bước này là một lượt giao được ghi đúng người, đúng đợt và đúng một lần. Dùng mã ngắn đã lấy ở bước 04 và thao tác trong khung phát của đợt. [Bước 04 · Xem mã nhận](04_KE_HOACH_XEM_MA_NHAN.md) · [Đặc tả FR06](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr06-kiểm-tra-mã-và-giao-nhận-tại-điểm-phát)

## Luồng chính trên app

1. Đăng nhập tài khoản Tình nguyện viên được gán cho đợt. Mở tab **Phát suất** và chọn đúng `campaign_id`. Xác nhận còn số suất thực chuẩn bị và đã đến `pickup_starts_at`. Với `c2`, khung phát mở từ phút 16 tính từ lúc nạp seed.
2. Ghi số **Đã trao** trước khi thao tác. Trong lượt thử chính với hồ sơ mới, kiểm tra bảng `handovers` chưa có bản ghi của `registration_id`.
3. Nhập mã ngắn và bấm kiểm tra. App báo mã hợp lệ và mở hộp thoại xác nhận. Chọn **Chưa giao**, rồi đối chiếu số đã trao giữ nguyên và hồ sơ còn `ALLOCATED`.
4. Kiểm tra lại cùng mã. Sau khi mô phỏng giao suất, bấm **Đã giao suất**. Mở tab **Đã trao** và xác nhận số đã trao tăng đúng một.
5. Tra lại cùng mã trong tab Phát suất. App phải báo mã đã dùng hoặc mất hiệu lực và số đã trao giữ nguyên. Làm mới dữ liệu ở tài khoản Người chăm sóc và kiểm tra hồ sơ hiện **Đã nhận**.
6. Đối chiếu các trạng thái trong cùng một truy vấn trên Studio.

```sql
SELECT r.id, r.campaign_id, r.status, t.is_used, t.is_revoked,
       h.id AS handover_id, h.volunteer_id, h.handover_time,
       h.verification_method
FROM public.registrations AS r
JOIN public.tickets AS t ON t.registration_id = r.id
LEFT JOIN public.handovers AS h ON h.registration_id = r.id
WHERE r.id = '<registration_id>';
```

Kết quả sau giao là `RECEIVED`, `is_used = true`, một `handover_id`, đúng tình nguyện viên và `verification_method = SHORT_CODE`.

## Ca kiểm tra bổ sung

| Ca | Cách thử | Điều kiện đạt |
| --- | --- | --- |
| Sai mã hoặc sai đợt | Nhập mã giả hoặc dùng mã của đợt khác | App báo cần kiểm tra lại. Số đã trao giữ nguyên. |
| Chưa đến giờ, hết hạn hoặc đã hủy | Dùng vé thử ở từng trạng thái | Không ghi nhận giao. Trạng thái vé và số liệu giữ nguyên sau thao tác bị từ chối. |
| Ngoài phạm vi | Dùng tình nguyện viên thử chưa được phân công cho đợt | Tài khoản không thấy đợt để phát suất và quyền phân công được xác minh ở server. |
| Hai thiết bị cùng mã | Hai phiên được phân công mở hộp thoại kiểm tra cho cùng mã, bấm xác nhận gần đồng thời | Chỉ một bản ghi giao. Phiên đến sau nhận kết quả lượt giao đã được ghi nhận. |
| Hai mã tranh suất cuối | Hai hồ sơ đã cấp cùng được xác nhận khi chỉ còn một suất thực chuẩn bị | Một lượt được ghi. Lượt còn lại nhận lỗi hết số thực chuẩn bị. |
| Mất mạng khi xác nhận | Trong môi trường thử, làm mất phản hồi và kiểm tra hộp thoại **Chưa xác định đã lưu giao nhận**. Khi có mạng chọn **Đối chiếu lại** | App cho biết lượt đã được ghi hoặc ghi thành công một lần nếu trước đó chưa lưu. Kiểm tra Studio để phân biệt hai tình huống. |

## Việc xử lý nếu ca thử chưa đạt

- Tách kiểm tra mã qua `verify_ticket` và lưu giao qua `confirm_handover`. Nếu lệch số liệu, đối chiếu ba trạng thái `handovers`, `tickets.is_used` và `registrations.status` trước khi thử lại.
- Quét QR bằng camera là công việc tiếp theo của FR06. Giữ nhập mã ngắn làm luồng đã có để nghiệm thu và làm phương án khi camera không dùng được.
- Sau khi cả năm bước đạt, ghi commit, thiết bị, mã đợt, mã hồ sơ và kết quả vào [docs/QA.md](QA.md#nhánh-mvc-và-supabase), rồi chạy lại bộ SQL trên stack local. Dùng lượt giao đã ghi nhận làm đầu vào của báo cáo trong đợt thử có bước 08. Biên bản nghiệm thu toàn bộ các hạng mục dự kiến đưa lên hosted được chốt ở bước 08.

**Hoàn thành khi** cùng hồ sơ từ bước 02 đã đi tới `RECEIVED`, số đã trao tăng đúng một và việc kiểm tra lại mã giữ nguyên số lượt giao. [Tiếp theo · 07. Cam kết nhà hảo tâm](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md) · [Đầu ra giao suất cho 08. Đối chiếu](08_KE_HOACH_DOI_CHIEU_VA_BAO_CAO.md)
