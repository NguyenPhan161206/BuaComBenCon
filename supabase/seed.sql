-- Local fixtures only. UTC timestamps use the server clock at reset.
DO $$
DECLARE
    caregiver CONSTANT UUID := '11111111-1111-1111-1111-111111111111';
    coordinator CONSTANT UUID := '22222222-2222-2222-2222-222222222222';
    volunteer CONSTANT UUID := '33333333-3333-3333-3333-333333333333';
    donor CONSTANT UUID := '44444444-4444-4444-4444-444444444444';
    instant CONSTANT TIMESTAMPTZ := now();
    recipient UUID;
    registration_id TEXT;
    code TEXT;
    name TEXT;
    phone TEXT;
BEGIN
    INSERT INTO auth.users (instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, confirmation_token, recovery_token, email_change_token_new,
        email_change, raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
    SELECT '00000000-0000-0000-0000-000000000000', id, 'authenticated', 'authenticated', email,
        extensions.crypt('MatKhau@123', extensions.gen_salt('bf')), instant, '', '', '', '',
        '{"provider":"email","providers":["email"]}', '{}', instant, instant
    FROM (VALUES (caregiver, 'giadinh@buacombencon.vn'), (coordinator, 'dieuphoi@buacombencon.vn'),
        (volunteer, 'tinhnguyen@buacombencon.vn'), (donor, 'nhahaotam@buacombencon.vn')) AS demo(id,email);

    INSERT INTO auth.identities (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
    SELECT id::text, id, jsonb_build_object('sub', id::text, 'email', email, 'email_verified', true),
        'email', instant, instant, instant
    FROM auth.users WHERE id IN (caregiver, coordinator, volunteer, donor);

    INSERT INTO public.profiles (id, full_name, phone, role) VALUES
        (caregiver, 'Người chăm sóc (mẫu)', '0900000013', 'caregiver'),
        (coordinator, 'Điều phối viên (mẫu)', '0900000001', 'coordinator'),
        (volunteer, 'Tình nguyện viên (mẫu)', '0900000002', 'volunteer'),
        (donor, 'Nhà hảo tâm (mẫu)', '0900000003', 'donor')
    ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name, phone = EXCLUDED.phone, role = EXCLUDED.role;

    INSERT INTO public.campaigns (
        id, coordinator_id, title, meal, event_date, pickup_time, location,
        confirmed_supply, pledged_supply, prepared_supply, deadline, rules,
        registration_closes_at, completion_closes_at, allocation_at, cancellation_closes_at,
        reallocation_closes_at, pickup_starts_at, pickup_ends_at,
        allocated_at, is_archived, archived_received, note
    ) VALUES
        ('c1', coordinator, 'Bữa trưa ấm lòng', 'Bữa trưa', CURRENT_DATE, 'Khung phát dữ liệu mẫu', 'Điểm An Tâm (mẫu)',
        30, 40, 30, 'Đợt đang phát', 'Một suất mỗi người nhận đã được xét đạt.',
        instant - interval '5 hours', instant - interval '4 hours', instant - interval '3 hours',
        instant - interval '2 hours', instant - interval '2 hours', instant - interval '1 hour', instant + interval '1 day',
        instant - interval '3 hours', false, 0, ''),
        ('c2', coordinator, 'Thêm một bữa cơm lành', 'Bữa trưa', CURRENT_DATE, 'Sau giờ phân bổ', 'Điểm An Tâm (mẫu)',
        30, 10, 30, '5 phút sau khi nạp dữ liệu mẫu', 'Một suất mỗi người nhận đã được xét đạt.',
        instant + interval '5 minutes', instant + interval '6 minutes', instant + interval '7 minutes',
        instant + interval '12 minutes', instant + interval '15 minutes', instant + interval '16 minutes', instant + interval '1 day',
        NULL, false, 0, ''),
        ('c0', coordinator, 'Bữa cơm ngày hôm qua', 'Bữa trưa', CURRENT_DATE - 1, '11:00 – 12:00', 'Điểm An Tâm (mẫu)',
        30, 40, 30, 'Đã kết thúc', 'Một suất mỗi người nhận đã được xét đạt.',
        instant - interval '26 hours', instant - interval '25 hours', instant - interval '24 hours',
        instant - interval '23 hours', instant - interval '23 hours', instant - interval '22 hours', instant - interval '21 hours',
        instant - interval '24 hours', true, 28, 'Hai suất chưa trao được chuyển đến điểm hỗ trợ theo quy trình mẫu.');

    INSERT INTO public.campaign_volunteers (campaign_id, volunteer_id) VALUES ('c1', volunteer), ('c2', volunteer);

    FOR idx IN 1..35 LOOP
        registration_id := 'R' || lpad(idx::text, 4, '0');
        code := 'BC' || lpad(idx::text, 4, '0');
        name := CASE WHEN idx = 13 THEN 'Người chăm sóc (mẫu)' ELSE 'Người chăm sóc ' || idx END;
        phone := '090' || lpad(idx::text, 7, '0');
        INSERT INTO public.recipients (name_key, phone) VALUES (lower(name), phone) RETURNING id INTO recipient;
        INSERT INTO public.registrations (id, campaign_id, recipient_id, user_id, recipient_name, phone, consent_at,
            is_assisted, assisted_by, status, completed_info_at, is_approved, reviewed_at, reviewed_by)
        VALUES (registration_id, 'c1', recipient, CASE WHEN idx = 13 THEN caregiver END, name, phone,
            instant - interval '5 hours', idx % 4 = 0, CASE WHEN idx % 4 = 0 THEN coordinator END,
            CASE WHEN idx <= 12 THEN 'RECEIVED' WHEN idx <= 30 THEN 'ALLOCATED' ELSE 'WAITING' END,
            instant - interval '5 hours' + idx * interval '1 second', true, instant - interval '4 hours', coordinator);

        IF idx <= 30 THEN
            INSERT INTO public.tickets (registration_id, ticket_code, qr_payload, is_used, valid_from, valid_until)
            VALUES (registration_id, code, code, idx <= 12, instant - interval '1 hour', instant + interval '1 day');
        END IF;
        IF idx <= 12 THEN
            INSERT INTO public.handovers (registration_id, campaign_id, volunteer_id, handover_time)
            VALUES (registration_id, 'c1', volunteer, instant - interval '30 minutes');
        END IF;
    END LOOP;

    INSERT INTO public.commitments (campaign_id, donor_id, donor_name, donor_phone, quantity)
    VALUES ('c2', donor, 'Nhà hảo tâm (mẫu)', '0900000003', 10);
END $$;
