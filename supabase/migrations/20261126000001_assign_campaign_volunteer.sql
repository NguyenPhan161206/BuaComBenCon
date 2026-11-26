-- Migration: Assign Volunteer to Campaign
-- Allows coordinators to assign volunteers to support food distribution for a campaign.

CREATE OR REPLACE FUNCTION public.assign_campaign_volunteer(
    p_campaign_id TEXT,
    p_volunteer_id UUID
) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF public.can_manage_campaign(p_campaign_id) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Bạn không có quyền phân công tình nguyện viên cho đợt này.' USING ERRCODE = '42501';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = p_volunteer_id AND role = 'volunteer') THEN
        RAISE EXCEPTION 'Tài khoản không phải tình nguyện viên hợp lệ.';
    END IF;
    INSERT INTO public.campaign_volunteers (campaign_id, volunteer_id)
    VALUES (p_campaign_id, p_volunteer_id)
    ON CONFLICT (campaign_id, volunteer_id) DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION public.create_campaign(
    p_title TEXT, p_meal TEXT, p_date DATE, p_pickup_start TIME, p_pickup_end TIME,
    p_location TEXT, p_rules TEXT, p_confirmed INTEGER,
    p_volunteer_id UUID DEFAULT NULL
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

    IF p_volunteer_id IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.profiles WHERE id = p_volunteer_id AND role = 'volunteer') THEN
            INSERT INTO public.campaign_volunteers (campaign_id, volunteer_id)
            VALUES (campaign_id, p_volunteer_id)
            ON CONFLICT (campaign_id, volunteer_id) DO NOTHING;
        END IF;
    END IF;

    RETURN jsonb_build_object('id', campaign_id);
END;
$$;

REVOKE ALL ON FUNCTION public.assign_campaign_volunteer(TEXT, UUID) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.assign_campaign_volunteer(TEXT, UUID) TO authenticated;

REVOKE ALL ON FUNCTION public.create_campaign(TEXT, TEXT, DATE, TIME, TIME, TEXT, TEXT, INTEGER, UUID) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.create_campaign(TEXT, TEXT, DATE, TIME, TIME, TEXT, TEXT, INTEGER, UUID) TO authenticated;
