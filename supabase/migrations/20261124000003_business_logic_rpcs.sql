-- Migration 03: Business Logic Database Functions (RPCs)
-- Implements atomic transactions, queue sorting, allocation rules, and double-claim prevention.

-- 1. Function: Submit Registration (Tiếp nhận và bổ sung)
CREATE OR REPLACE FUNCTION public.submit_registration(
    p_campaign_id TEXT,
    p_recipient_name TEXT,
    p_phone TEXT,
    p_note TEXT DEFAULT '',
    p_is_assisted BOOLEAN DEFAULT false
)
RETURNS JSONB AS $$
DECLARE
    v_campaign public.campaigns%ROWTYPE;
    v_reg_count INTEGER;
    v_new_id TEXT;
    v_user_id UUID := auth.uid();
    v_result public.registrations%ROWTYPE;
BEGIN
    -- Check campaign exists and is open
    SELECT * INTO v_campaign FROM public.campaigns WHERE id = p_campaign_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Đợt hỗ trợ không tồn tại.' USING ERRCODE = 'P0001';
    END IF;

    IF v_campaign.is_archived OR NOT v_campaign.is_registration_open THEN
        RAISE EXCEPTION 'Đợt này đã đóng đăng ký.' USING ERRCODE = 'P0002';
    END IF;

    -- Check duplicate active registration for recipient phone in this campaign
    IF EXISTS (
        SELECT 1 FROM public.registrations
        WHERE campaign_id = p_campaign_id AND phone = p_phone AND status != 'CANCELLED'
    ) THEN
        RAISE EXCEPTION 'Số điện thoại này đã có đăng ký còn hiệu lực trong đợt.' USING ERRCODE = 'P0003';
    END IF;

    -- Generate sequential registration ID (e.g. R0001, R0002...)
    SELECT count(*) + 1 INTO v_reg_count FROM public.registrations;
    v_new_id := 'R' || lpad(v_reg_count::text, 4, '0');

    -- Insert registration record
    INSERT INTO public.registrations (
        id, campaign_id, user_id, recipient_name, phone,
        is_assisted, assisted_by, status, completed_info_at, note
    ) VALUES (
        v_new_id, p_campaign_id, v_user_id, p_recipient_name, p_phone,
        p_is_assisted, CASE WHEN p_is_assisted THEN v_user_id ELSE NULL END,
        'PENDING', now(), p_note
    ) RETURNING * INTO v_result;

    -- Audit log
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('registrations', v_new_id, 'SUBMIT', v_user_id, jsonb_build_object(
        'campaign_id', p_campaign_id,
        'assisted', p_is_assisted
    ));

    RETURN jsonb_build_object(
        'id', v_result.id,
        'campaign_id', v_result.campaign_id,
        'status', v_result.status,
        'queue_number', v_result.queue_number,
        'completed_info_at', v_result.completed_info_at
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Function: Review Registration (Xét hồ sơ)
CREATE OR REPLACE FUNCTION public.review_registration(
    p_registration_id TEXT,
    p_is_approved BOOLEAN,
    p_reviewer_note TEXT DEFAULT ''
)
RETURNS JSONB AS $$
DECLARE
    v_reg public.registrations%ROWTYPE;
BEGIN
    SELECT * INTO v_reg FROM public.registrations WHERE id = p_registration_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Hồ sơ không tồn tại.' USING ERRCODE = 'P0001';
    END IF;

    IF v_reg.status != 'PENDING' THEN
        RAISE EXCEPTION 'Hồ sơ đã được xử lý (trạng thái: %).', v_reg.status USING ERRCODE = 'P0004';
    END IF;

    UPDATE public.registrations
    SET reviewed_at = now(),
        reviewed_by = auth.uid(),
        note = CASE WHEN p_reviewer_note != '' THEN p_reviewer_note ELSE note END
    WHERE id = p_registration_id
    RETURNING * INTO v_reg;

    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('registrations', p_registration_id, 'REVIEW', auth.uid(), jsonb_build_object(
        'approved', p_is_approved,
        'reviewed_at', v_reg.reviewed_at
    ));

    RETURN jsonb_build_object(
        'id', v_reg.id,
        'reviewed_at', v_reg.reviewed_at,
        'status', v_reg.status
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Function: Allocate Campaign Meals (Phân bổ lần đầu theo mốc đủ thông tin -> số tiếp nhận)
CREATE OR REPLACE FUNCTION public.allocate_campaign_meals(
    p_campaign_id TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_campaign public.campaigns%ROWTYPE;
    v_assigned_count INTEGER := 0;
    v_allocated_count INTEGER := 0;
    v_waiting_count INTEGER := 0;
    v_cur RECORD;
    v_ticket_code TEXT;
BEGIN
    -- Lock campaign for supply check
    SELECT * INTO v_campaign FROM public.campaigns WHERE id = p_campaign_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Đợt không tồn tại.' USING ERRCODE = 'P0001';
    END IF;

    -- Count existing assigned (ALLOCATED or RECEIVED)
    SELECT count(*) INTO v_assigned_count
    FROM public.registrations
    WHERE campaign_id = p_campaign_id AND status IN ('ALLOCATED', 'RECEIVED');

    -- Iterate eligible registrations ordered strictly by completed_info_at ASC, queue_number ASC
    FOR v_cur IN
        SELECT * FROM public.registrations
        WHERE campaign_id = p_campaign_id AND status IN ('PENDING', 'WAITING')
        ORDER BY completed_info_at ASC, queue_number ASC
        FOR UPDATE
    LOOP
        IF v_assigned_count < v_campaign.confirmed_supply THEN
            -- Grant meal allocation
            UPDATE public.registrations
            SET status = 'ALLOCATED', updated_at = now()
            WHERE id = v_cur.id;

            v_assigned_count := v_assigned_count + 1;
            v_allocated_count := v_allocated_count + 1;

            -- Create meal ticket if not existing
            v_ticket_code := 'BC' || substring(v_cur.id from 2);
            INSERT INTO public.tickets (
                registration_id, ticket_code, qr_payload, is_used, is_revoked,
                valid_from, valid_until
            ) VALUES (
                v_cur.id, v_ticket_code, v_ticket_code, false, false,
                now(), now() + interval '2 days'
            ) ON CONFLICT (registration_id) DO NOTHING;

        ELSE
            -- Capacity full: applicant placed in ordered waitlist
            UPDATE public.registrations
            SET status = 'WAITING', updated_at = now()
            WHERE id = v_cur.id;

            v_waiting_count := v_waiting_count + 1;
        END IF;
    END LOOP;

    RETURN jsonb_build_object(
        'campaign_id', p_campaign_id,
        'allocated', v_allocated_count,
        'waiting', v_waiting_count,
        'total_assigned', v_assigned_count,
        'confirmed_capacity', v_campaign.confirmed_supply
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Function: Cancel Registration (Hủy đăng ký & thu hồi vé)
CREATE OR REPLACE FUNCTION public.cancel_registration(
    p_registration_id TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_reg public.registrations%ROWTYPE;
BEGIN
    SELECT * INTO v_reg FROM public.registrations WHERE id = p_registration_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Hồ sơ không tồn tại.' USING ERRCODE = 'P0001';
    END IF;

    IF v_reg.status = 'RECEIVED' THEN
        RAISE EXCEPTION 'Suất ăn đã trao, không thể hủy.' USING ERRCODE = 'P0005';
    END IF;

    UPDATE public.registrations
    SET status = 'CANCELLED', updated_at = now()
    WHERE id = p_registration_id;

    -- Revoke ticket
    UPDATE public.tickets
    SET is_revoked = true
    WHERE registration_id = p_registration_id;

    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('registrations', p_registration_id, 'CANCEL', auth.uid(), jsonb_build_object(
        'prior_status', v_reg.status
    ));

    RETURN jsonb_build_object(
        'id', p_registration_id,
        'status', 'CANCELLED',
        'ticket_revoked', true
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Function: Reallocate Meal (Cấp lại suất cho hồ sơ đầu hàng chờ)
CREATE OR REPLACE FUNCTION public.reallocate_meal(
    p_campaign_id TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_campaign public.campaigns%ROWTYPE;
    v_assigned_count INTEGER;
    v_waiting_reg public.registrations%ROWTYPE;
    v_ticket_code TEXT;
BEGIN
    SELECT * INTO v_campaign FROM public.campaigns WHERE id = p_campaign_id FOR UPDATE;

    SELECT count(*) INTO v_assigned_count
    FROM public.registrations
    WHERE campaign_id = p_campaign_id AND status IN ('ALLOCATED', 'RECEIVED');

    IF v_assigned_count >= v_campaign.confirmed_supply THEN
        RAISE EXCEPTION 'Không có suất trống để cấp lại.' USING ERRCODE = 'P0006';
    END IF;

    -- Select top waiting applicant in queue
    SELECT * INTO v_waiting_reg
    FROM public.registrations
    WHERE campaign_id = p_campaign_id AND status = 'WAITING'
    ORDER BY completed_info_at ASC, queue_number ASC
    LIMIT 1
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'message', 'Không còn hồ sơ trong danh sách chờ.');
    END IF;

    -- Allocate
    UPDATE public.registrations
    SET status = 'ALLOCATED', updated_at = now()
    WHERE id = v_waiting_reg.id;

    v_ticket_code := 'BC' || substring(v_waiting_reg.id from 2);
    INSERT INTO public.tickets (
        registration_id, ticket_code, qr_payload, is_used, is_revoked,
        valid_from, valid_until
    ) VALUES (
        v_waiting_reg.id, v_ticket_code, v_ticket_code, false, false,
        now(), now() + interval '2 days'
    ) ON CONFLICT (registration_id) DO UPDATE SET is_revoked = false;

    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('registrations', v_waiting_reg.id, 'REALLOCATE', auth.uid(), jsonb_build_object(
        'campaign_id', p_campaign_id,
        'ticket_code', v_ticket_code
    ));

    RETURN jsonb_build_object(
        'success', true,
        'reallocated_id', v_waiting_reg.id,
        'recipient_name', v_waiting_reg.recipient_name,
        'ticket_code', v_ticket_code
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Function: Verify Ticket (Kiểm tra mã nhận)
CREATE OR REPLACE FUNCTION public.verify_ticket(
    p_ticket_code TEXT,
    p_campaign_id TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_ticket public.tickets%ROWTYPE;
    v_reg public.registrations%ROWTYPE;
    v_campaign public.campaigns%ROWTYPE;
BEGIN
    SELECT * INTO v_ticket FROM public.tickets WHERE ticket_code = p_ticket_code;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Không tìm thấy mã.');
    END IF;

    SELECT * INTO v_reg FROM public.registrations WHERE id = v_ticket.registration_id;
    IF v_reg.campaign_id != p_campaign_id THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Mã không thuộc đợt đang chọn.');
    END IF;

    IF v_ticket.is_used OR v_reg.status = 'RECEIVED' THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Mã đã được sử dụng. Không giao thêm suất; liên hệ điều phối viên nếu cần.');
    END IF;

    IF v_ticket.is_revoked OR v_reg.status != 'ALLOCATED' THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Đăng ký chưa được sắp xếp hoặc đã hủy. Liên hệ điều phối viên.');
    END IF;

    SELECT * INTO v_campaign FROM public.campaigns WHERE id = p_campaign_id;
    IF v_campaign.is_archived THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Đợt hỗ trợ đã kết thúc.');
    END IF;

    RETURN jsonb_build_object(
        'valid', true,
        'registration_id', v_reg.id,
        'recipient_name', v_reg.recipient_name,
        'campaign_title', v_campaign.title,
        'ticket_code', v_ticket.ticket_code
    );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- 7. Function: Confirm Handover (Xác nhận giao suất 1 lần với khóa dòng chống giao trùng)
CREATE OR REPLACE FUNCTION public.confirm_handover(
    p_ticket_code TEXT,
    p_campaign_id TEXT,
    p_verification_method TEXT DEFAULT 'QR_SCAN'
)
RETURNS JSONB AS $$
DECLARE
    v_ticket public.tickets%ROWTYPE;
    v_reg public.registrations%ROWTYPE;
    v_campaign public.campaigns%ROWTYPE;
    v_received_count INTEGER;
BEGIN
    -- Lock ticket row to prevent race conditions across devices
    SELECT * INTO v_ticket FROM public.tickets WHERE ticket_code = p_ticket_code FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Không tìm thấy mã nhận.' USING ERRCODE = 'P0007';
    END IF;

    IF v_ticket.is_used THEN
        RAISE EXCEPTION 'Mã này đã được xác nhận giao trước đó.' USING ERRCODE = 'P0008';
    END IF;

    IF v_ticket.is_revoked THEN
        RAISE EXCEPTION 'Mã đã bị thu hồi do đăng ký đã hủy.' USING ERRCODE = 'P0009';
    END IF;

    -- Lock registration row
    SELECT * INTO v_reg FROM public.registrations WHERE id = v_ticket.registration_id FOR UPDATE;
    IF v_reg.campaign_id != p_campaign_id THEN
        RAISE EXCEPTION 'Mã không thuộc đợt đang phát.' USING ERRCODE = 'P0010';
    END IF;

    IF v_reg.status != 'ALLOCATED' THEN
        RAISE EXCEPTION 'Hồ sơ không ở trạng thái được nhận suất (hiện tại: %).', v_reg.status USING ERRCODE = 'P0011';
    END IF;

    -- Check prepared quantity vs already received
    SELECT * INTO v_campaign FROM public.campaigns WHERE id = p_campaign_id;
    SELECT count(*) INTO v_received_count FROM public.registrations WHERE campaign_id = p_campaign_id AND status = 'RECEIVED';
    IF v_received_count >= v_campaign.prepared_supply THEN
        RAISE EXCEPTION 'Số suất đã trao đã bằng số thực chuẩn bị (%). Cần điều phối viên kiểm tra.', v_campaign.prepared_supply USING ERRCODE = 'P0012';
    END IF;

    -- Mark ticket used
    UPDATE public.tickets SET is_used = true WHERE id = v_ticket.id;

    -- Mark registration received
    UPDATE public.registrations SET status = 'RECEIVED', updated_at = now() WHERE id = v_reg.id;

    -- Insert handover record
    INSERT INTO public.handovers (
        registration_id, campaign_id, volunteer_id, handover_time, verification_method
    ) VALUES (
        v_reg.id, p_campaign_id, auth.uid(), now(), p_verification_method
    );

    -- Audit log
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('handovers', v_reg.id, 'HANDOVER', auth.uid(), jsonb_build_object(
        'ticket_code', p_ticket_code,
        'campaign_id', p_campaign_id,
        'method', p_verification_method
    ));

    RETURN jsonb_build_object(
        'success', true,
        'registration_id', v_reg.id,
        'recipient_name', v_reg.recipient_name,
        'ticket_code', p_ticket_code,
        'handover_time', now()
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
