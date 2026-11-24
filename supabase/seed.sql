-- Seed Data for BuaComBenCon MVP Scenario
-- Seeds campaigns, users for all 4 roles, registrations, tickets, and past reconciliation data.

-- 1. Demo Auth Users & Profiles
DO $$
DECLARE
    v_user_caregiver UUID := '11111111-1111-1111-1111-111111111111';
    v_user_coordinator UUID := '22222222-2222-2222-2222-222222222222';
    v_user_volunteer UUID := '33333333-3333-3333-3333-333333333333';
    v_user_donor UUID := '44444444-4444-4444-4444-444444444444';
    v_now TIMESTAMPTZ := now();
    v_idx INTEGER;
    v_reg_id TEXT;
    v_code TEXT;
    v_status TEXT;
    v_is_mine BOOLEAN;
    v_is_assisted BOOLEAN;
BEGIN
    -- Insert auth.users
    INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data)
    VALUES
        (v_user_caregiver, 'giadinh@buacombencon.vn', crypt('MatKhau@123', gen_salt('bf')), v_now, '{"full_name": "Người chăm sóc (mẫu)"}'),
        (v_user_coordinator, 'dieuphoi@buacombencon.vn', crypt('MatKhau@123', gen_salt('bf')), v_now, '{"full_name": "Điều phối viên (mẫu)"}'),
        (v_user_volunteer, 'tinhnguyen@buacombencon.vn', crypt('MatKhau@123', gen_salt('bf')), v_now, '{"full_name": "Tình nguyện viên (mẫu)"}'),
        (v_user_donor, 'nhahaotam@buacombencon.vn', crypt('MatKhau@123', gen_salt('bf')), v_now, '{"full_name": "Nhà hảo tâm (mẫu)"}')
    ON CONFLICT (id) DO NOTHING;

    -- Insert public.profiles
    INSERT INTO public.profiles (id, full_name, phone, role)
    VALUES
        (v_user_caregiver, 'Người chăm sóc (mẫu)', '0900000013', 'caregiver'),
        (v_user_coordinator, 'Điều phối viên (mẫu)', '0900000001', 'coordinator'),
        (v_user_volunteer, 'Tình nguyện viên (mẫu)', '0900000002', 'volunteer'),
        (v_user_donor, 'Nhà hảo tâm (mẫu)', '0900000003', 'donor')
    ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name, phone = EXCLUDED.phone, role = EXCLUDED.role;

    -- 2. Campaigns
    -- Today's campaign c1 (30 confirmed supply)
    INSERT INTO public.campaigns (
        id, title, meal, event_date, pickup_time, location,
        confirmed_supply, pledged_supply, prepared_supply, deadline, rules, note, is_registration_open, is_archived
    ) VALUES (
        'c1', 'Bữa trưa ấm lòng', 'Bữa trưa', CURRENT_DATE, '11:00 – 12:00',
        'Điểm phát An Tâm · khu sinh hoạt chung (mẫu)',
        30, 40, 30, '10:00 hôm nay',
        'Dành cho người chăm sóc bệnh nhi ung thư được nhóm hỗ trợ xác nhận. Mỗi người chăm sóc đăng ký 1 suất mỗi đợt. Sắp xếp theo thứ tự đăng ký hợp lệ; trường hợp ngoại lệ do điều phối viên xem xét.',
        'Đã sẵn sàng phát suất.', true, false
    ) ON CONFLICT (id) DO NOTHING;

    -- Tomorrow's campaign c2 (40 confirmed supply)
    INSERT INTO public.campaigns (
        id, title, meal, event_date, pickup_time, location,
        confirmed_supply, pledged_supply, prepared_supply, deadline, rules, note, is_registration_open, is_archived
    ) VALUES (
        'c2', 'Thêm một bữa cơm lành', 'Bữa trưa', CURRENT_DATE + interval '1 day', '11:00 – 12:00',
        'Điểm phát An Tâm · khu sinh hoạt chung (mẫu)',
        40, 50, 0, '18:00 ngày trước buổi phát',
        'Dành cho người chăm sóc bệnh nhi ung thư được nhóm hỗ trợ xác nhận. Mỗi người chăm sóc đăng ký 1 suất mỗi đợt.',
        'Đang tiếp nhận đăng ký.', true, false
    ) ON CONFLICT (id) DO NOTHING;

    -- Day after tomorrow campaign c3 (25 confirmed supply)
    INSERT INTO public.campaigns (
        id, title, meal, event_date, pickup_time, location,
        confirmed_supply, pledged_supply, prepared_supply, deadline, rules, note, is_registration_open, is_archived
    ) VALUES (
        'c3', 'Bữa chiều sẻ chia', 'Bữa chiều', CURRENT_DATE + interval '2 days', '16:30 – 17:30',
        'Điểm phát An Tâm · khu sinh hoạt chung (mẫu)',
        25, 35, 0, '18:00 ngày trước buổi phát',
        'Dành cho người chăm sóc bệnh nhi ung thư.',
        'Đang tiếp nhận đăng ký.', true, false
    ) ON CONFLICT (id) DO NOTHING;

    -- Historical campaign c0 (archived, 28 received, 2 unhanded meals)
    INSERT INTO public.campaigns (
        id, title, meal, event_date, pickup_time, location,
        confirmed_supply, pledged_supply, prepared_supply, deadline, rules, note, is_registration_open, is_archived, archived_received
    ) VALUES (
        'c0', 'Bữa cơm ngày hôm qua', 'Bữa trưa', CURRENT_DATE - interval '1 day', '11:00 – 12:00',
        'Điểm phát An Tâm · khu sinh hoạt chung (mẫu)',
        30, 40, 30, '10:00 hôm qua',
        'Dành cho người chăm sóc bệnh nhi ung thư.',
        '2 suất chưa trao được nhóm chuyển tới điểm hỗ trợ theo quy tắc của đợt (minh họa).',
        false, true, 28
    ) ON CONFLICT (id) DO NOTHING;

    -- 3. Registrations (Seeding 35 applicants for c1: 1..12 RECEIVED, 13..30 ALLOCATED, 31..35 WAITING)
    FOR v_idx IN 1..35 LOOP
        v_reg_id := 'R' || lpad(v_idx::text, 4, '0');
        v_code := 'BC' || lpad(v_idx::text, 4, '0');
        v_is_mine := (v_idx = 13);
        v_is_assisted := (v_idx % 4 = 0);

        IF v_idx <= 12 THEN
            v_status := 'RECEIVED';
        ELSIF v_idx <= 30 THEN
            v_status := 'ALLOCATED';
        ELSE
            v_status := 'WAITING';
        END IF;

        INSERT INTO public.registrations (
            id, campaign_id, user_id, recipient_name, phone,
            is_assisted, assisted_by, status, queue_number, completed_info_at, reviewed_at, note
        ) VALUES (
            v_reg_id, 'c1',
            CASE WHEN v_is_mine THEN v_user_caregiver ELSE NULL END,
            CASE WHEN v_is_mine THEN 'Người chăm sóc (mẫu)' ELSE 'Người chăm sóc ' || lpad(v_idx::text, 2, '0') END,
            '090' || lpad(v_idx::text, 7, '0'),
            v_is_assisted,
            CASE WHEN v_is_assisted THEN v_user_coordinator ELSE NULL END,
            v_status, v_idx,
            v_now - ((40 - v_idx) * interval '1 minute'),
            v_now - interval '30 minutes',
            ''
        ) ON CONFLICT (id) DO NOTHING;

        -- Create tickets for allocated & received
        IF v_status IN ('ALLOCATED', 'RECEIVED') THEN
            INSERT INTO public.tickets (
                registration_id, ticket_code, qr_payload, is_used, is_revoked, valid_from, valid_until
            ) VALUES (
                v_reg_id, v_code, v_code, (v_status = 'RECEIVED'), false, v_now - interval '1 hour', v_now + interval '1 day'
            ) ON CONFLICT (registration_id) DO NOTHING;
        END IF;

        -- Create handover record for RECEIVED
        IF v_status = 'RECEIVED' THEN
            INSERT INTO public.handovers (
                registration_id, campaign_id, volunteer_id, handover_time, verification_method
            ) VALUES (
                v_reg_id, 'c1', v_user_volunteer, v_now - ((15 - v_idx) * interval '2 minutes'), 'QR_SCAN'
            ) ON CONFLICT (registration_id) DO NOTHING;
        END IF;
    END LOOP;

    -- Add historical registration for user in c0
    INSERT INTO public.registrations (
        id, campaign_id, user_id, recipient_name, phone,
        is_assisted, status, completed_info_at
    ) VALUES (
        'R0036', 'c0', v_user_caregiver, 'Người chăm sóc (mẫu)', '0900000013',
        false, 'RECEIVED', v_now - interval '1 day'
    ) ON CONFLICT (id) DO NOTHING;

    -- 4. Commitments
    INSERT INTO public.commitments (
        campaign_id, donor_id, donor_name, donor_phone, quantity, cadence, consent, status
    ) VALUES (
        'c2', v_user_donor, 'Nhà hảo tâm (mẫu)', '0900000003', 10, 'Theo đợt', true, 'PENDING'
    );

END $$;
