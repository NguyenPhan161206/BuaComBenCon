-- Test Suite 01: Business Rules Verification
-- Tests the four critical queue/allocation test cases from Section 5 of the proposal:
-- 1. Bổ sung muộn (Late completion timestamp ordering)
-- 2. Trùng mốc (Tie-breaking by queue_number)
-- 3. Xét sau lượt đầu (Late review insertion into front of waitlist)
-- 4. Cấp lại (Cancelled slot reallocation to top waiting applicant)
-- 5. Anti-double claim (Atomic lock preventing dual handovers)

BEGIN;

-- Setup isolated test campaign with capacity 1
INSERT INTO public.campaigns (
    id, title, meal, event_date, pickup_time, location,
    confirmed_supply, prepared_supply, deadline, rules
) VALUES (
    'test_c1', 'Đợt kiểm thử xếp suất', 'Bữa trưa', CURRENT_DATE, '11:00 – 12:00',
    'Điểm kiểm thử', 1, 1, '10:00', 'Tiêu chuẩn kiểm thử'
);

-- Test Case 1: Bổ sung muộn
-- "Khi còn một suất, hồ sơ đăng ký hộ đủ thông tin lúc 08:02 được cấp trước hồ sơ gửi lúc 08:01 nhưng đến 08:15 mới đủ thông tin."
INSERT INTO public.registrations (
    id, campaign_id, recipient_name, phone, completed_info_at, queue_number, status
) VALUES
    ('TEST_REG_A', 'test_c1', 'Hồ sơ A gửi 08:01 đủ 08:15', '0911111111', '2026-11-24 08:15:00+07', 1, 'PENDING'),
    ('TEST_REG_B', 'test_c1', 'Hồ sơ B gửi hộ đủ 08:02', '0922222222', '2026-11-24 08:02:00+07', 2, 'PENDING');

-- Run allocation
SELECT public.allocate_campaign_meals('test_c1');

-- Verify TEST_REG_B is ALLOCATED and TEST_REG_A is WAITING
DO $$
DECLARE
    v_status_a TEXT;
    v_status_b TEXT;
BEGIN
    SELECT status INTO v_status_a FROM public.registrations WHERE id = 'TEST_REG_A';
    SELECT status INTO v_status_b FROM public.registrations WHERE id = 'TEST_REG_B';

    ASSERT v_status_b = 'ALLOCATED', 'Test Case 1 Thất bại: Hồ sơ B đủ thông tin lúc 08:02 phải được ALLOCATED';
    ASSERT v_status_a = 'WAITING', 'Test Case 1 Thất bại: Hồ sơ A đủ thông tin lúc 08:15 phải vào WAITING';
    RAISE NOTICE 'PASS: Test Case 1 (Bổ sung muộn) thành công.';
END $$;

-- Test Case 2: Trùng mốc
-- "Hai hồ sơ đủ điều kiện có cùng mốc đủ thông tin. Hồ sơ số tiếp nhận 12 đứng trước số 13."
DELETE FROM public.registrations WHERE campaign_id = 'test_c1';
UPDATE public.campaigns SET confirmed_supply = 1 WHERE id = 'test_c1';

INSERT INTO public.registrations (
    id, campaign_id, recipient_name, phone, completed_info_at, queue_number, status
) VALUES
    ('TEST_REG_13', 'test_c1', 'Hồ sơ 13', '0913131313', '2026-11-24 08:00:00+07', 13, 'PENDING'),
    ('TEST_REG_12', 'test_c1', 'Hồ sơ 12', '0912121212', '2026-11-24 08:00:00+07', 12, 'PENDING');

SELECT public.allocate_campaign_meals('test_c1');

DO $$
DECLARE
    v_status_12 TEXT;
    v_status_13 TEXT;
BEGIN
    SELECT status INTO v_status_12 FROM public.registrations WHERE id = 'TEST_REG_12';
    SELECT status INTO v_status_13 FROM public.registrations WHERE id = 'TEST_REG_13';

    ASSERT v_status_12 = 'ALLOCATED', 'Test Case 2 Thất bại: Hồ sơ 12 phải được ALLOCATED trước hồ sơ 13';
    ASSERT v_status_13 = 'WAITING', 'Test Case 2 Thất bại: Hồ sơ 13 phải ở WAITING';
    RAISE NOTICE 'PASS: Test Case 2 (Trùng mốc) thành công.';
END $$;

-- Test Case 3: Xét sau lượt đầu
-- "Lượt 09:45 đã cấp hết suất. Hồ sơ đủ thông tin lúc 08:15, được xét đạt lúc 09:50, vào hàng chờ trước hồ sơ đủ thông tin lúc 08:20."
INSERT INTO public.registrations (
    id, campaign_id, recipient_name, phone, completed_info_at, queue_number, status
) VALUES
    ('TEST_REG_LATE', 'test_c1', 'Hồ sơ xét muộn lúc 09:50', '0999999999', '2026-11-24 08:15:00+07', 99, 'PENDING'),
    ('TEST_REG_AFTER', 'test_c1', 'Hồ sơ nộp 08:20', '0988888888', '2026-11-24 08:20:00+07', 100, 'WAITING');

-- Review late record
SELECT public.review_registration('TEST_REG_LATE', true, 'Xét đạt sau giờ phân bổ');
SELECT public.allocate_campaign_meals('test_c1');

DO $$
DECLARE
    v_first_in_queue TEXT;
BEGIN
    -- Top of waiting list should be TEST_REG_13 (08:00), then TEST_REG_LATE (08:15), then TEST_REG_AFTER (08:20)
    SELECT id INTO v_first_in_queue FROM public.registrations
    WHERE campaign_id = 'test_c1' AND status = 'WAITING'
    ORDER BY completed_info_at ASC, queue_number ASC
    OFFSET 1 LIMIT 1;

    ASSERT v_first_in_queue = 'TEST_REG_LATE', 'Test Case 3 Thất bại: Hồ sơ xét muộn phải đứng trước hồ sơ 08:20 trong danh sách chờ';
    RAISE NOTICE 'PASS: Test Case 3 (Xét sau lượt đầu) thành công.';
END $$;

-- Test Case 4: Cấp lại
-- "Người được cấp trong ca bổ sung muộn hủy đúng hạn. Khi điều phối viên xác nhận cấp lại, hồ sơ đầu hàng chờ được cấp đúng suất vừa giải phóng."
SELECT public.cancel_registration('TEST_REG_12');
SELECT public.reallocate_meal('test_c1');

DO $$
DECLARE
    v_status_12 TEXT;
    v_status_13 TEXT;
BEGIN
    SELECT status INTO v_status_12 FROM public.registrations WHERE id = 'TEST_REG_12';
    SELECT status INTO v_status_13 FROM public.registrations WHERE id = 'TEST_REG_13';

    ASSERT v_status_12 = 'CANCELLED', 'Test Case 4 Thất bại: Người hủy phải có trạng thái CANCELLED';
    ASSERT v_status_13 = 'ALLOCATED', 'Test Case 4 Thất bại: Hồ sơ 13 đầu hàng chờ phải được cấp lại ALLOCATED';
    RAISE NOTICE 'PASS: Test Case 4 (Cấp lại suất) thành công.';
END $$;

-- Test Case 5: Giao nhận an toàn 1 lần (Chống giao trùng)
DO $$
DECLARE
    v_ticket_code TEXT := 'BCTEST_13';
    v_error_caught BOOLEAN := false;
BEGIN
    -- Update ticket code for consistency
    UPDATE public.tickets SET ticket_code = v_ticket_code WHERE registration_id = 'TEST_REG_13';

    -- First handover succeeds
    PERFORM public.confirm_handover(v_ticket_code, 'test_c1', 'QR_SCAN');

    -- Second handover attempt must fail with exception
    BEGIN
        PERFORM public.confirm_handover(v_ticket_code, 'test_c1', 'QR_SCAN');
    EXCEPTION WHEN OTHERS THEN
        v_error_caught := true;
    END;

    ASSERT v_error_caught = true, 'Test Case 5 Thất bại: Lần quét thứ hai phải bị chặn chống giao trùng';
    RAISE NOTICE 'PASS: Test Case 5 (Chống giao nhận trùng lặp) thành công.';
END $$;

ROLLBACK;
