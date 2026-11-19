# Đối chiếu với tài liệu yêu cầu

Nguồn: `2403700211_2403700101_2403700160_2403700235_2403700173.docx`, tiêu đề “Đề xuất ứng dụng Bữa cơm bên con”, phiên bản 3.0. File nguồn được giữ nguyên trong `docs/source/`.

| Phần tài liệu | Thể hiện trong UI |
| --- | --- |
| 1.1–1.2: người chăm sóc, điều phối, tình nguyện viên, nhà hảo tâm | Bốn vai trò trong bộ chọn góc phải, mỗi vai trò có thanh điều hướng riêng |
| 2.1: ngày, bữa ăn, điểm phát, hạn đăng ký, tiêu chí và đầu mối | Thẻ đợt trên trang chủ, màn chi tiết, trạng thái mở/đóng tiếp nhận, hướng dẫn liên hệ |
| 2.1: gửi đăng ký không mặc định được nhận | Màn thành công ghi Đã tiếp nhận; không cấp QR cho hồ sơ chờ xét |
| 2.2: danh sách đăng ký, được nhận/chờ/hủy, mã nhận, lịch sử | Tab Đăng ký; màn trạng thái/QR; hủy có xác nhận; tab Lịch sử |
| 2.2: mã không chứa danh tính trẻ; có mã ngắn thay QR | QR chứa chuỗi BCxxxx; màn mã ngắn và nút sao chép |
| 2.3: không bắt buộc gia đình cài app | Luồng điều phối Đăng ký hộ; có thể mở mã để cung cấp trực tiếp |
| 2.3: không yêu cầu ảnh/bệnh án/câu chuyện công khai | Biểu mẫu chỉ hỏi tên người chăm sóc, số liên hệ và ghi chú; có giải thích không nhập bệnh án |
| 3.1: tạo đợt, thời gian, điểm, số suất | Tạo đợt tại một điểm mẫu; cập nhật số xác nhận và số chuẩn bị |
| 3.1, 4.1: danh sách chờ và sắp xếp lại khi hủy | Danh sách có bộ lọc; cấp theo thứ tự, kiểm tra điều kiện do người điều phối xác nhận; hủy giải phóng suất |
| 3.2: chọn đúng đợt, nhập/quét mã, xác nhận khi giao | Có chọn đợt và nhập mã ngắn; chưa tích hợp camera quét |
| 3.2, 4.2: mã đã dùng, sai đợt, cần kiểm tra | Cảnh báo trước giao; kiểm tra lại trước khi đổi sang Đã nhận |
| 3.2: khi mất kết nối, không tự xem là thành công | Công tắc mô phỏng mất kết nối; chặn xác nhận và hướng dẫn đến bàn điều phối |
| 3.3: đồng hành theo đợt, mong muốn tuần/tháng | Biểu mẫu nhà hảo tâm và màn cam kết chờ liên hệ |
| 3.3, 4.3: cam kết/xác nhận/chuẩn bị/đã trao/chưa trao | Năm chỉ số tách riêng trong báo cáo; phần chưa trao đang diễn ra không gọi là người vắng |
| 3.3: báo cáo không lộ thông tin riêng | Màn nhà hảo tâm và kết quả gia đình chỉ có tổng hợp |
| 4.1: ví dụ 35 hồ sơ/30 suất/5 chờ | Bộ dữ liệu mẫu hôm nay có đúng phân bổ này |
| 4.3: ví dụ 30 chuẩn bị/28 trao/2 còn lại | Đợt hôm qua đã kết thúc, có ghi chú xử lý 2 suất mẫu |
| 5.2: một nhóm, một điểm phát, chưa giữ tiền | Một điểm phát An Tâm giả lập, không có thanh toán hoặc viện phí |
| 5.3: cần kiểm chứng hữu ích thực tế | README phân biệt kiểm tra source/APK/layoutlib với chạy thật và sử dụng thực tế |

Giao diện chưa triển khai các ngoại lệ ưu tiên có ghi lý do, nhật ký kiểm toán, phát danh sách dự phòng, luồng từ chối hồ sơ không đủ điều kiện hoặc xác nhận cam kết tài trợ chi tiết. Các phần này cần thiết kế và triển khai chức năng ở giai đoạn tiếp theo nếu làm sản phẩm thực tế.
