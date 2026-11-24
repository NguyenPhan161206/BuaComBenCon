-- The campaign row is the first lock for every operation that can affect capacity.

CREATE FUNCTION public.lock_campaign(p_campaign_id TEXT)
RETURNS public.campaigns LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    campaign public.campaigns;
BEGIN
    SELECT * INTO campaign FROM public.campaigns WHERE id = p_campaign_id FOR UPDATE;
    IF NOT FOUND OR campaign.is_archived THEN
        RAISE EXCEPTION 'Đợt hỗ trợ không tồn tại hoặc đã kết thúc.';
    END IF;
    RETURN campaign;
END;
$$;

CREATE FUNCTION public.issue_ticket(p_registration_id TEXT, p_campaign public.campaigns)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    code TEXT := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));
BEGIN
    INSERT INTO public.tickets (registration_id, ticket_code, qr_payload, valid_from, valid_until)
    VALUES (p_registration_id, code, code, p_campaign.pickup_starts_at, p_campaign.pickup_ends_at)
    ON CONFLICT (registration_id) DO UPDATE SET ticket_code = EXCLUDED.ticket_code,
        qr_payload = EXCLUDED.qr_payload, is_revoked = false, is_used = false,
        valid_from = EXCLUDED.valid_from, valid_until = EXCLUDED.valid_until;
END;
$$;

CREATE FUNCTION public.submit_registration(
    p_campaign_id TEXT, p_recipient_name TEXT, p_phone TEXT,
    p_note TEXT DEFAULT '', p_is_assisted BOOLEAN DEFAULT false, p_consent BOOLEAN DEFAULT false,
    p_is_complete BOOLEAN DEFAULT true
) RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    campaign public.campaigns;
    recipient UUID;
    registration public.registrations;
    v_phone TEXT := regexp_replace(p_phone, '[[:space:].()-]', '', 'g');
    v_name TEXT := regexp_replace(btrim(p_recipient_name), '[[:space:]]+', ' ', 'g');
    instant TIMESTAMPTZ;
BEGIN
    IF (CASE WHEN p_is_assisted THEN public.can_manage_campaign(p_campaign_id)
        ELSE public.current_user_role() = 'caregiver' END) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền đăng ký theo kênh này.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(p_campaign_id);
    instant := clock_timestamp();
    IF NOT campaign.is_registration_open OR instant >= campaign.registration_closes_at THEN
        RAISE EXCEPTION 'Đợt này đã đóng nhận hồ sơ mới.';
    END IF;
    IF left(v_phone, 3) = '+84' THEN v_phone := '0' || substr(v_phone, 4); END IF;
    IF p_consent IS DISTINCT FROM true OR p_is_assisted IS NULL OR p_is_complete IS NULL
        OR v_name IS NULL OR char_length(v_name) NOT BETWEEN 2 AND 120
        OR v_phone IS NULL OR v_phone !~ '^0[0-9]{9,10}$' THEN
        RAISE EXCEPTION 'Cần tên, số điện thoại hợp lệ và sự đồng ý của người nhận.';
    END IF;

    INSERT INTO public.recipients (name_key, phone) VALUES (lower(v_name), v_phone)
    ON CONFLICT (name_key, phone) DO UPDATE SET name_key = EXCLUDED.name_key RETURNING id INTO recipient;

    INSERT INTO public.registrations (
        id, campaign_id, recipient_id, user_id, recipient_name, phone, note,
        is_assisted, assisted_by, consent_at, completed_info_at
    ) VALUES (
        gen_random_uuid()::text, p_campaign_id, recipient, CASE WHEN NOT p_is_assisted THEN auth.uid() END,
        v_name, v_phone, coalesce(p_note, ''), p_is_assisted, CASE WHEN p_is_assisted THEN auth.uid() END,
        instant, CASE WHEN p_is_complete THEN instant END
    )
    ON CONFLICT (campaign_id, recipient_id) DO UPDATE
    SET recipient_name = EXCLUDED.recipient_name, status = 'PENDING', is_approved = NULL,
        user_id = coalesce(registrations.user_id, EXCLUDED.user_id), note = EXCLUDED.note,
        is_assisted = EXCLUDED.is_assisted, assisted_by = EXCLUDED.assisted_by,
        consent_at = EXCLUDED.consent_at, completed_info_at = EXCLUDED.completed_info_at,
        queue_number = EXCLUDED.queue_number, reviewed_at = NULL, reviewed_by = NULL,
        updated_at = instant
    WHERE registrations.status = 'CANCELLED'
        AND (p_is_assisted OR registrations.user_id = auth.uid())
    RETURNING * INTO registration;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Người nhận đã có hồ sơ trong đợt. Vui lòng liên hệ điều phối viên.';
    END IF;
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id)
    VALUES ('registrations', registration.id, 'SUBMIT', auth.uid());
    RETURN jsonb_build_object('id', registration.id, 'status', registration.status);
END;
$$;

CREATE FUNCTION public.complete_registration(p_registration_id TEXT, p_note TEXT DEFAULT '')
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    registration public.registrations;
    campaign public.campaigns;
    instant TIMESTAMPTZ;
BEGIN
    SELECT * INTO registration FROM public.registrations WHERE id = p_registration_id;
    IF NOT FOUND OR (registration.user_id = auth.uid()
        OR public.can_manage_campaign(registration.campaign_id)) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền bổ sung hồ sơ.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(registration.campaign_id);
    instant := clock_timestamp();
    IF instant >= campaign.completion_closes_at THEN
        RAISE EXCEPTION 'Đã hết hạn bổ sung hồ sơ.';
    END IF;
    UPDATE public.registrations SET completed_info_at = instant, note = coalesce(p_note, ''), updated_at = instant
    WHERE id = p_registration_id AND status = 'PENDING' AND completed_info_at IS NULL;
    IF NOT FOUND THEN RAISE EXCEPTION 'Hồ sơ không còn chờ bổ sung.'; END IF;
END;
$$;

CREATE FUNCTION public.review_registration(p_registration_id TEXT, p_is_approved BOOLEAN, p_reviewer_note TEXT DEFAULT '')
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    registration public.registrations;
    campaign public.campaigns;
    instant TIMESTAMPTZ;
BEGIN
    SELECT * INTO registration FROM public.registrations WHERE id = p_registration_id;
    IF NOT FOUND OR public.can_manage_campaign(registration.campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền xét hồ sơ.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(registration.campaign_id);
    instant := clock_timestamp();
    SELECT * INTO registration FROM public.registrations WHERE id = p_registration_id;
    IF registration.status <> 'PENDING' OR registration.is_approved IS NOT NULL OR p_is_approved IS NULL
        OR instant >= campaign.reallocation_closes_at
        OR (p_is_approved AND (registration.completed_info_at IS NULL OR registration.completed_info_at > campaign.completion_closes_at)) THEN
        RAISE EXCEPTION 'Hồ sơ không còn chờ xét hoặc chưa đủ thông tin đúng hạn.';
    END IF;

    UPDATE public.registrations
    SET is_approved = p_is_approved, reviewed_at = instant, reviewed_by = auth.uid(),
        updated_at = instant,
        status = CASE WHEN NOT p_is_approved THEN 'REJECTED'
            WHEN campaign.allocated_at IS NOT NULL THEN 'WAITING' ELSE 'PENDING' END
    WHERE id = p_registration_id RETURNING * INTO registration;
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id, details)
    VALUES ('registrations', p_registration_id, 'REVIEW', auth.uid(),
        jsonb_build_object('approved', p_is_approved, 'note', coalesce(p_reviewer_note, '')));
    RETURN jsonb_build_object('id', registration.id, 'status', registration.status);
END;
$$;

CREATE FUNCTION public.allocate_campaign_meals(p_campaign_id TEXT)
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    campaign public.campaigns;
    registration RECORD;
    assigned INTEGER := 0;
    waiting INTEGER := 0;
    instant TIMESTAMPTZ;
BEGIN
    IF public.can_manage_campaign(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền phân bổ đợt này.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(p_campaign_id);
    instant := clock_timestamp();
    IF campaign.allocated_at IS NOT NULL OR instant < campaign.allocation_at OR instant >= campaign.reallocation_closes_at THEN
        RAISE EXCEPTION 'Đợt này chưa đến giờ phân bổ, đã phân bổ hoặc đã hết hạn.';
    END IF;

    FOR registration IN SELECT id FROM public.registrations
        WHERE campaign_id = p_campaign_id AND status = 'PENDING' AND is_approved IS TRUE
          AND completed_info_at <= campaign.completion_closes_at
        ORDER BY completed_info_at, queue_number
    LOOP
        IF assigned < campaign.confirmed_supply THEN
            UPDATE public.registrations SET status = 'ALLOCATED', updated_at = instant WHERE id = registration.id;
            PERFORM public.issue_ticket(registration.id, campaign);
            assigned := assigned + 1;
        ELSE
            UPDATE public.registrations SET status = 'WAITING', updated_at = instant WHERE id = registration.id;
            waiting := waiting + 1;
        END IF;
    END LOOP;
    UPDATE public.campaigns SET allocated_at = instant, updated_at = instant WHERE id = p_campaign_id;
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id)
    VALUES ('campaigns', p_campaign_id, 'ALLOCATE', auth.uid());
    RETURN jsonb_build_object('allocated', assigned, 'waiting', waiting);
END;
$$;

CREATE FUNCTION public.cancel_registration(p_registration_id TEXT)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    registration public.registrations;
    campaign public.campaigns;
    instant TIMESTAMPTZ;
BEGIN
    SELECT * INTO registration FROM public.registrations WHERE id = p_registration_id;
    IF NOT FOUND OR (registration.user_id = auth.uid()
        OR public.can_manage_campaign(registration.campaign_id)) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền hủy hồ sơ.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(registration.campaign_id);
    instant := clock_timestamp();
    SELECT * INTO registration FROM public.registrations WHERE id = p_registration_id;
    IF registration.status = 'CANCELLED' THEN RETURN; END IF;
    IF registration.status NOT IN ('PENDING', 'WAITING', 'ALLOCATED') OR instant >= campaign.cancellation_closes_at THEN
        RAISE EXCEPTION 'Hồ sơ đã qua hạn hủy hoặc không thể hủy ở trạng thái hiện tại.';
    END IF;
    UPDATE public.registrations SET status = 'CANCELLED', updated_at = instant WHERE id = p_registration_id;
    UPDATE public.tickets SET is_revoked = true WHERE registration_id = p_registration_id;
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id)
    VALUES ('registrations', p_registration_id, 'CANCEL', auth.uid());
END;
$$;

CREATE FUNCTION public.reallocate_meal(p_campaign_id TEXT)
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    campaign public.campaigns;
    registration_id TEXT;
BEGIN
    IF public.can_manage_campaign(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền cấp lại ở đợt này.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(p_campaign_id);
    IF campaign.allocated_at IS NULL OR clock_timestamp() >= campaign.reallocation_closes_at THEN
        RAISE EXCEPTION 'Đợt chưa phân bổ lần đầu hoặc đã qua giờ chốt cấp lại.';
    END IF;
    IF (SELECT count(*) FROM public.registrations WHERE campaign_id = p_campaign_id
        AND status IN ('ALLOCATED', 'RECEIVED')) >= campaign.confirmed_supply THEN
        RAISE EXCEPTION 'Không có suất trống để cấp lại.';
    END IF;
    SELECT id INTO registration_id FROM public.registrations WHERE campaign_id = p_campaign_id
        AND status = 'WAITING' AND is_approved IS TRUE
    ORDER BY completed_info_at, queue_number LIMIT 1;
    IF NOT FOUND THEN RAISE EXCEPTION 'Không còn hồ sơ đủ điều kiện trong hàng chờ.'; END IF;

    UPDATE public.registrations SET status = 'ALLOCATED', updated_at = clock_timestamp() WHERE id = registration_id;
    PERFORM public.issue_ticket(registration_id, campaign);
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id)
    VALUES ('registrations', registration_id, 'REALLOCATE', auth.uid());
    RETURN jsonb_build_object('registration_id', registration_id);
END;
$$;

CREATE FUNCTION public.verify_ticket(p_ticket_code TEXT, p_campaign_id TEXT)
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    registration public.registrations;
    ticket public.tickets;
    campaign public.campaigns;
    instant TIMESTAMPTZ := clock_timestamp();
BEGIN
    IF public.can_distribute(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn chưa được phân công phát ở đợt này.' USING ERRCODE = '42501';
    END IF;
    SELECT * INTO ticket FROM public.tickets WHERE ticket_code = upper(btrim(p_ticket_code));
    SELECT * INTO registration FROM public.registrations WHERE id = ticket.registration_id;
    IF NOT FOUND OR registration.campaign_id <> p_campaign_id THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Mã không hợp lệ cho đợt đang chọn.');
    END IF;
    IF ticket.is_used OR ticket.is_revoked OR registration.status <> 'ALLOCATED' THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Mã đã dùng hoặc đã mất hiệu lực.');
    END IF;
    SELECT * INTO campaign FROM public.campaigns WHERE id = p_campaign_id;
    IF campaign.is_archived OR instant < greatest(ticket.valid_from, campaign.pickup_starts_at)
        OR instant >= least(ticket.valid_until, campaign.pickup_ends_at) THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Mã nằm ngoài khung giờ nhận.');
    END IF;
    IF (SELECT count(*) FROM public.handovers WHERE campaign_id = p_campaign_id) >= campaign.prepared_supply THEN
        RETURN jsonb_build_object('valid', false, 'error', 'Đã trao hết số suất thực chuẩn bị.');
    END IF;
    RETURN jsonb_build_object('valid', true, 'recipient_name', registration.recipient_name);
END;
$$;

CREATE FUNCTION public.confirm_handover(
    p_ticket_code TEXT, p_campaign_id TEXT, p_verification_method TEXT DEFAULT 'SHORT_CODE'
) RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    v_registration_id TEXT;
    handover public.handovers;
    checked JSONB;
    instant TIMESTAMPTZ;
BEGIN
    IF public.can_distribute(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn chưa được phân công phát ở đợt này.' USING ERRCODE = '42501';
    END IF;
    PERFORM 1 FROM public.campaigns WHERE id = p_campaign_id FOR UPDATE;
    SELECT r.id INTO v_registration_id FROM public.tickets t JOIN public.registrations r ON r.id = t.registration_id
    WHERE t.ticket_code = upper(btrim(p_ticket_code)) AND r.campaign_id = p_campaign_id;
    SELECT * INTO handover FROM public.handovers h WHERE h.registration_id = v_registration_id;
    IF FOUND THEN
        RETURN jsonb_build_object('already_recorded', true, 'handover_time', handover.handover_time);
    END IF;
    checked := public.verify_ticket(p_ticket_code, p_campaign_id);
    IF (checked->>'valid')::boolean IS DISTINCT FROM true THEN RAISE EXCEPTION '%', checked->>'error'; END IF;

    instant := clock_timestamp();
    INSERT INTO public.handovers (registration_id, campaign_id, volunteer_id, handover_time, verification_method)
    VALUES (v_registration_id, p_campaign_id, auth.uid(), instant, p_verification_method);
    UPDATE public.tickets t SET is_used = true WHERE t.registration_id = v_registration_id;
    UPDATE public.registrations SET status = 'RECEIVED', updated_at = instant WHERE id = v_registration_id;
    INSERT INTO public.audit_logs (entity_name, entity_id, action, actor_id)
    VALUES ('handovers', v_registration_id, 'HANDOVER', auth.uid());
    RETURN jsonb_build_object('already_recorded', false, 'handover_time', instant);
END;
$$;

CREATE FUNCTION public.create_campaign(
    p_title TEXT, p_meal TEXT, p_date DATE, p_pickup_start TIME, p_pickup_end TIME,
    p_location TEXT, p_rules TEXT, p_confirmed INTEGER
) RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    campaign_id TEXT := gen_random_uuid()::text;
    pickup_start TIMESTAMPTZ := (p_date + p_pickup_start) AT TIME ZONE 'Asia/Ho_Chi_Minh';
    pickup_end TIMESTAMPTZ := (p_date + p_pickup_end) AT TIME ZONE 'Asia/Ho_Chi_Minh';
BEGIN
    IF public.current_user_role() IS DISTINCT FROM 'coordinator' THEN
        RAISE EXCEPTION 'Bạn không có quyền tạo đợt.' USING ERRCODE = '42501';
    END IF;
    IF nullif(btrim(p_title), '') IS NULL OR nullif(btrim(p_meal), '') IS NULL
        OR nullif(btrim(p_location), '') IS NULL OR nullif(btrim(p_rules), '') IS NULL
        OR p_confirmed IS NULL OR p_confirmed NOT BETWEEN 1 AND 1000
        OR pickup_start IS NULL OR pickup_end IS NULL OR pickup_end <= pickup_start
        OR pickup_start - interval '2 hours' <= clock_timestamp() THEN
        RAISE EXCEPTION 'Cần thông tin đầy đủ và giờ nhận bắt đầu sau ít nhất 2 giờ.';
    END IF;
    INSERT INTO public.campaigns (id, coordinator_id, title, meal, event_date, pickup_time,
        location, deadline, rules, confirmed_supply, registration_closes_at, completion_closes_at, allocation_at,
        cancellation_closes_at, reallocation_closes_at, pickup_starts_at, pickup_ends_at)
    VALUES (campaign_id, auth.uid(), btrim(p_title), btrim(p_meal), p_date,
        to_char(pickup_start AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI') || ' – ' ||
        to_char(pickup_end AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'),
        btrim(p_location), to_char((pickup_start - interval '2 hours') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI DD/MM/YYYY'),
        btrim(p_rules), p_confirmed, pickup_start - interval '2 hours', pickup_start - interval '90 minutes',
        pickup_start - interval '75 minutes', pickup_start - interval '45 minutes',
        pickup_start - interval '30 minutes', pickup_start, pickup_end);
    RETURN jsonb_build_object('id', campaign_id);
END;
$$;

CREATE FUNCTION public.update_campaign_supply(p_campaign_id TEXT, p_confirmed INTEGER, p_prepared INTEGER, p_note TEXT)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF public.can_manage_campaign(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền cập nhật nguồn của đợt.' USING ERRCODE = '42501';
    END IF;
    PERFORM public.lock_campaign(p_campaign_id);
    IF p_confirmed IS NULL OR p_prepared IS NULL OR p_confirmed > 1000 OR p_prepared > p_confirmed
        OR p_confirmed < (SELECT count(*) FROM public.registrations WHERE campaign_id = p_campaign_id AND status IN ('ALLOCATED', 'RECEIVED'))
        OR p_prepared < (SELECT count(*) FROM public.handovers WHERE campaign_id = p_campaign_id) THEN
        RAISE EXCEPTION 'Nguồn suất phải đủ cho các lượt đã cấp và đã trao.';
    END IF;
    UPDATE public.campaigns SET confirmed_supply = p_confirmed, prepared_supply = p_prepared,
        note = coalesce(p_note, ''), updated_at = clock_timestamp() WHERE id = p_campaign_id;
END;
$$;

CREATE FUNCTION public.set_registration_open(p_campaign_id TEXT, p_open BOOLEAN)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE campaign public.campaigns;
BEGIN
    IF public.can_manage_campaign(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền mở hoặc đóng đợt.' USING ERRCODE = '42501';
    END IF;
    campaign := public.lock_campaign(p_campaign_id);
    IF p_open AND clock_timestamp() >= campaign.registration_closes_at THEN RAISE EXCEPTION 'Đợt đã hết hạn nhận mới.'; END IF;
    UPDATE public.campaigns SET is_registration_open = p_open, updated_at = clock_timestamp() WHERE id = p_campaign_id;
END;
$$;

CREATE FUNCTION public.campaign_summary(p_campaign_id TEXT)
RETURNS JSONB LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = '' AS $$
DECLARE campaign public.campaigns;
BEGIN
    SELECT * INTO campaign FROM public.campaigns WHERE id = p_campaign_id;
    IF NOT FOUND OR (public.can_distribute(p_campaign_id) OR
        (public.current_user_role() IN ('caregiver', 'donor') AND campaign.is_archived)) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Báo cáo chưa được công bố hoặc nằm ngoài phạm vi.' USING ERRCODE = '42501';
    END IF;
    RETURN jsonb_build_object('pledged', campaign.pledged_supply, 'confirmed', campaign.confirmed_supply,
        'prepared', campaign.prepared_supply, 'received',
        CASE WHEN campaign.is_archived THEN campaign.archived_received
             ELSE (SELECT count(*) FROM public.handovers WHERE campaign_id = p_campaign_id) END,
        'recent_handovers', CASE WHEN public.can_distribute(p_campaign_id) THEN (
            SELECT coalesce(jsonb_agg(handover_time ORDER BY handover_time DESC), '[]'::jsonb)
            FROM (SELECT handover_time FROM public.handovers WHERE campaign_id = p_campaign_id
                ORDER BY handover_time DESC LIMIT 50) recent
        ) ELSE '[]'::jsonb END);
END;
$$;

CREATE FUNCTION public.save_commitment(p_campaign_id TEXT, p_quantity INTEGER, p_cadence TEXT, p_consent BOOLEAN)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE donor public.profiles;
BEGIN
    SELECT * INTO donor FROM public.profiles WHERE id = auth.uid() AND role = 'donor';
    IF NOT FOUND THEN RAISE EXCEPTION 'Tài khoản chưa được cấp quyền nhà hảo tâm.' USING ERRCODE = '42501'; END IF;
    IF p_consent IS DISTINCT FROM true OR p_quantity IS NULL OR p_quantity NOT BETWEEN 1 AND 1000 THEN
        RAISE EXCEPTION 'Cần số suất cam kết hợp lệ và sự đồng ý.';
    END IF;
    PERFORM public.lock_campaign(p_campaign_id);
    INSERT INTO public.commitments (campaign_id, donor_id, donor_name, donor_phone, quantity, cadence)
    VALUES (p_campaign_id, donor.id, donor.full_name, donor.phone, p_quantity, p_cadence);
    UPDATE public.campaigns SET pledged_supply = pledged_supply + p_quantity WHERE id = p_campaign_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.lock_campaign(TEXT), public.issue_ticket(TEXT, public.campaigns)
    FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.submit_registration(TEXT, TEXT, TEXT, TEXT, BOOLEAN, BOOLEAN, BOOLEAN),
    public.complete_registration(TEXT, TEXT), public.review_registration(TEXT, BOOLEAN, TEXT),
    public.allocate_campaign_meals(TEXT), public.cancel_registration(TEXT), public.reallocate_meal(TEXT),
    public.verify_ticket(TEXT, TEXT), public.confirm_handover(TEXT, TEXT, TEXT),
    public.create_campaign(TEXT, TEXT, DATE, TIME, TIME, TEXT, TEXT, INTEGER),
    public.update_campaign_supply(TEXT, INTEGER, INTEGER, TEXT), public.set_registration_open(TEXT, BOOLEAN),
    public.campaign_summary(TEXT), public.save_commitment(TEXT, INTEGER, TEXT, BOOLEAN) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_registration(TEXT, TEXT, TEXT, TEXT, BOOLEAN, BOOLEAN, BOOLEAN),
    public.complete_registration(TEXT, TEXT), public.review_registration(TEXT, BOOLEAN, TEXT),
    public.allocate_campaign_meals(TEXT), public.cancel_registration(TEXT), public.reallocate_meal(TEXT),
    public.verify_ticket(TEXT, TEXT), public.confirm_handover(TEXT, TEXT, TEXT),
    public.create_campaign(TEXT, TEXT, DATE, TIME, TIME, TEXT, TEXT, INTEGER),
    public.update_campaign_supply(TEXT, INTEGER, INTEGER, TEXT), public.set_registration_open(TEXT, BOOLEAN),
    public.campaign_summary(TEXT), public.save_commitment(TEXT, INTEGER, TEXT, BOOLEAN) TO authenticated;
