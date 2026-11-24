# 03. Điều phối viên xét hồ sơ và phân bổ

Đầu ra của bước này là hồ sơ đã xét đủ điều kiện được cấp suất theo thứ tự server, có vé nhận và có lịch sử xét. [Bước 02 · Gửi hồ sơ](02_KE_HOACH_GUI_HO_SO.md) · [Đặc tả FR04](DAC_TA_YEU_CAU_BUA_COM_BEN_CON.md#fr04-xét-điều-kiện-phân-bổ-danh-sách-chờ-và-hủy)

## Luồng chính trên app

1. Đăng nhập Điều phối viên sở hữu đợt. Mở **Đợt hỗ trợ → Quản lý đợt** và chọn đúng `campaign_id`. Dùng **Làm mới dữ liệu** nếu hồ sơ vừa được gửi từ thiết bị khác.
2. Tìm hồ sơ theo `registration_id` ghi ở bước 02. Mở **Xét điều kiện** và kiểm tra hồ sơ đã đủ thông tin. Với hồ sơ còn thiếu, ghi nhận bổ sung trước hạn rồi mới xét.
3. Chọn **Đạt** cho hồ sơ chính. Đối chiếu `is_approved = true`, `reviewed_at` và `reviewed_by`. Trước lần phân bổ đầu tiên, hồ sơ đã xét đạt vẫn có trạng thái `PENDING`. Ghi lại `completed_info_at` để kiểm tra mốc này giữ nguyên sau khi xét.
4. Đến `allocation_at` và trước `reallocation_closes_at`, bấm **Phân bổ lần đầu** và xác nhận. Với seed `c2`, dùng khoảng từ phút 7 đến trước phút 15 tính từ lúc nạp seed.
5. Kiểm tra `campaigns.allocated_at` đã có giá trị. Hồ sơ chính chuyển `ALLOCATED` nếu còn suất theo thứ tự và có một vé gắn đúng `registration_id`. Đối chiếu theo mã đợt từ bước 01, ví dụ `c2`, rồi ghi kết quả cho người chăm sóc ở bước 04.

```sql
SELECT r.id, r.status, r.is_approved, r.completed_info_at,
       r.queue_number, r.reviewed_at, r.reviewed_by,
       t.id IS NOT NULL AS has_ticket
FROM public.registrations AS r
LEFT JOIN public.tickets AS t ON t.registration_id = r.id
WHERE r.campaign_id = '<campaign_id>'
ORDER BY r.completed_info_at NULLS LAST, r.queue_number;
```

## Ca kiểm tra bổ sung

Chạy trên một đợt thử riêng hoặc trong bộ SQL hồi quy. Giữ vé của hồ sơ chính để dùng xuyên suốt bước 04 và 05.

| Ca | Cách thử | Điều kiện đạt |
| --- | --- | --- |
| Điều kiện xét | Cho một hồ sơ không đạt và để một hồ sơ chưa xét, sau đó phân bổ | Hồ sơ không đạt là `REJECTED`. Hồ sơ chưa xét giữ `PENDING`. Cả hai chưa có vé. |
| Thứ tự | Hai hồ sơ đủ điều kiện có mốc đủ thông tin khác nhau, rồi thử một cặp trùng mốc | Hồ sơ đủ thông tin sớm hơn đi trước. Khi trùng mốc, `queue_number` nhỏ hơn đi trước. |
| Giới hạn nguồn | Cho 35 hồ sơ hợp lệ tranh 30 suất xác nhận | 30 hồ sơ `ALLOCATED`, 5 hồ sơ `WAITING`. |
| Xét muộn | Xét đạt hồ sơ đủ thông tin đúng hạn sau lần phân bổ đầu tiên và trước giờ chốt cấp lại | Hồ sơ vào hàng chờ theo mốc đủ thông tin ban đầu. Các suất đã cấp giữ nguyên. |
| Hủy và cấp lại | Người chăm sóc hủy trước hạn. Điều phối bấm **Cấp lại một suất** trước giờ chốt | Hồ sơ cũ `CANCELLED`, vé cũ bị thu hồi, hồ sơ đủ điều kiện đầu hàng chờ được cấp. |
| Thời gian và thao tác lặp | Thử phân bổ trước giờ, bấm lại sau khi đã phân bổ, hoặc hai phiên tranh suất cấp lại cuối | Server từ chối thao tác sai mốc. Hai phiên tranh một chỗ chỉ cấp thêm một suất. |

## Việc xử lý nếu ca thử chưa đạt

- Kiểm tra riêng ba thao tác qua RPC `review_registration`, `allocate_campaign_meals` và `reallocate_meal`. Khi sai thứ tự hoặc số lượng, đối chiếu mốc đủ thông tin, số tiếp nhận, số xác nhận và `allocated_at` trước khi sửa.
- Trong lượt thử sau bước 07, đối chiếu số đã cấp với tổng xác nhận từ cam kết và nguồn tự chuẩn bị. Số cam kết đang chờ xét chưa làm tăng khả năng cấp. [Bước 07 · Cách tính nguồn](07_KE_HOACH_CAM_KET_NHA_HAO_TAM.md#1-chuẩn-bị-hợp-đồng-dữ-liệu-và-cách-tính)
- Chạy lại `supabase/tests/01_business_rules_test.sql` với `ON_ERROR_STOP=1` sau khi sửa quy tắc server. Ghi riêng các ca đã chạy qua Android và qua SQL. [Lệnh kiểm thử SQL](HUONG_DAN_TRIEN_KHAI_SUPABASE.md#3-kiểm-tra-xếp-và-giao-suất)

**Hoàn thành khi** hồ sơ chính đã được cấp, có đúng một vé và số suất đang giữ chỗ cộng số đã trao không vượt chỉ tiêu đã xác nhận trong Supabase. [Tiếp theo · 04. Xem trạng thái và mã nhận](04_KE_HOACH_XEM_MA_NHAN.md)
