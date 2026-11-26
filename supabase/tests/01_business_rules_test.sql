-- Run with psql -X -v ON_ERROR_STOP=1 on a seeded local instance.
BEGIN;
SET LOCAL plpgsql.check_asserts = on;

CREATE FUNCTION pg_temp.campaign(key text, capacity integer, phase text)
RETURNS void LANGUAGE sql AS $$
    INSERT INTO public.campaigns (id, coordinator_id, title, meal, event_date, pickup_time,
        location, confirmed_supply, prepared_supply, deadline, rules, registration_closes_at,
        completion_closes_at, allocation_at, cancellation_closes_at, reallocation_closes_at,
        pickup_starts_at, pickup_ends_at, allocated_at)
    VALUES (key, '22222222-2222-2222-2222-222222222222', key, 'Trưa', current_date, '11:00 – 12:00',
        'Mẫu', capacity, capacity, 'Mẫu', 'Mẫu',
        now() + (CASE phase WHEN 'intake' THEN 5 WHEN 'complete' THEN -10 ELSE -90 END) * interval '1 minute',
        now() + (CASE WHEN phase IN ('intake','complete') THEN 10 ELSE -75 END) * interval '1 minute',
        now() + (CASE WHEN phase IN ('intake','complete') THEN 15 ELSE -60 END) * interval '1 minute',
        now() + (CASE WHEN phase = 'pickup' THEN -40 ELSE 30 END) * interval '1 minute',
        now() + (CASE WHEN phase = 'pickup' THEN -30 ELSE 40 END) * interval '1 minute',
        now() + (CASE WHEN phase = 'pickup' THEN -15 ELSE 50 END) * interval '1 minute',
        now() + interval '1 day', CASE WHEN phase = 'pickup' THEN now() - interval '1 hour' END);
$$;

CREATE FUNCTION pg_temp.registration(
    key text, campaign text, minutes int, queue bigint, approved boolean, status text DEFAULT 'PENDING'
) RETURNS void LANGUAGE sql AS $$
    WITH recipient AS (
        INSERT INTO public.recipients (name_key,phone) VALUES (key,'0912222222') RETURNING id
    )
    INSERT INTO public.registrations (id, campaign_id, recipient_id, user_id,
        recipient_name, phone, consent_at, completed_info_at, queue_number, is_approved, status)
    SELECT key, campaign, id, '11111111-1111-1111-1111-111111111111', key, '0912222222', now() - interval '4 hours',
        now() - minutes * interval '1 minute', queue, approved, status FROM recipient;
$$;

SELECT pg_temp.campaign('qa-allocate', 1, 'allocate');
SELECT pg_temp.campaign('qa-tie', 1, 'allocate');
SELECT pg_temp.campaign('qa-capacity', 30, 'allocate');
SELECT pg_temp.campaign('qa-pickup', 1, 'pickup');
SELECT pg_temp.campaign('qa-complete', 1, 'complete');
SELECT pg_temp.campaign('qa-intake', 1, 'intake');

SELECT pg_temp.registration('late-completion', 'qa-allocate', 130, 10001, true);
SELECT pg_temp.registration('early-completion', 'qa-allocate', 143, 10002, true);
SELECT pg_temp.registration('late-review', 'qa-allocate', 135, 10003, NULL);
SELECT pg_temp.registration('declined', 'qa-allocate', 150, 10004, NULL);
SELECT pg_temp.registration('unreviewed', 'qa-allocate', 151, 10005, NULL);
SELECT pg_temp.registration('tie13', 'qa-tie', 150, 10013, true);
SELECT pg_temp.registration('tie12', 'qa-tie', 150, 10012, true);
SELECT pg_temp.registration('qa-case-' || n, 'qa-capacity', 200 - n, 10100 + n, true) FROM generate_series(1,35) g(n);
SELECT pg_temp.registration('pickup-ok', 'qa-pickup', 200, 11000, true, 'ALLOCATED');
SELECT pg_temp.registration('incomplete', 'qa-complete', NULL, 11001, NULL);

INSERT INTO public.campaign_volunteers (campaign_id, volunteer_id)
VALUES ('qa-pickup', '33333333-3333-3333-3333-333333333333');
INSERT INTO public.tickets (registration_id, ticket_code, qr_payload, valid_from, valid_until)
VALUES ('pickup-ok', 'QA_VALID', 'QA_VALID', now() - interval '15 minutes', now() + interval '1 hour');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', true);

DO $$
DECLARE result jsonb;
BEGIN
    PERFORM public.review_registration('declined', false);
    PERFORM public.allocate_campaign_meals('qa-allocate');
    ASSERT (SELECT status FROM public.registrations WHERE id = 'early-completion') = 'ALLOCATED', 'Earlier complete record gets the slot';
    ASSERT (SELECT status FROM public.registrations WHERE id = 'declined') = 'REJECTED', 'Rejected record must not get a slot';
    ASSERT (SELECT status FROM public.registrations WHERE id = 'unreviewed') = 'PENDING', 'Unreviewed record must stay pending';
    ASSERT (SELECT status FROM public.registrations WHERE id = 'late-completion') = 'WAITING', 'Other eligible record is waiting';
    RAISE NOTICE 'PASS: late completion, rejected and unreviewed eligibility';

    PERFORM public.allocate_campaign_meals('qa-tie');
    ASSERT (SELECT status FROM public.registrations WHERE id = 'tie12') = 'ALLOCATED', 'Sequence breaks timestamp tie';
    RAISE NOTICE 'PASS: timestamp tie';

    PERFORM public.review_registration('late-review', true);
    ASSERT (SELECT status FROM public.registrations WHERE id = 'late-review') = 'WAITING', 'Approved late record goes to waitlist';
    ASSERT (SELECT status FROM public.registrations WHERE id = 'early-completion') = 'ALLOCATED', 'Assigned slot is unchanged';
    PERFORM public.cancel_registration('early-completion');
    ASSERT (SELECT is_revoked FROM public.tickets WHERE registration_id = 'early-completion'), 'Cancelled ticket is revoked';
    result := public.reallocate_meal('qa-allocate');
    ASSERT result->>'registration_id' = 'late-review', 'Earlier complete late-reviewed record is first in waitlist';
    RAISE NOTICE 'PASS: late review, cancellation and reallocation';

    result := public.allocate_campaign_meals('qa-capacity');
    ASSERT (result->>'allocated')::int = 30 AND (result->>'waiting')::int = 5, '35 applicants -> 30 allocated and 5 waiting';
    RAISE NOTICE 'PASS: 35/30/5';

    -- Test registration toggle and volunteer assignment
    PERFORM public.set_registration_open('qa-intake', false);
    ASSERT NOT (SELECT is_registration_open FROM public.campaigns WHERE id = 'qa-intake'), 'Coordinator closes intake';
    PERFORM public.set_registration_open('qa-intake', true);
    ASSERT (SELECT is_registration_open FROM public.campaigns WHERE id = 'qa-intake'), 'Coordinator reopens intake';
    PERFORM public.assign_campaign_volunteer('qa-intake', '33333333-3333-3333-3333-333333333333');
    ASSERT EXISTS (SELECT 1 FROM public.campaign_volunteers WHERE campaign_id = 'qa-intake' AND volunteer_id = '33333333-3333-3333-3333-333333333333'), 'Volunteer assigned to campaign';
    RAISE NOTICE 'PASS: toggle registration and volunteer assignment';
END;
$$;

SELECT set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', true);
DO $$
DECLARE result jsonb;
BEGIN
    result := public.submit_registration('qa-intake', 'Người nhận thử', '+84 912345678',
        p_consent => true);
    ASSERT result->>'status' = 'PENDING', 'New application is pending';
    PERFORM public.complete_registration('incomplete', 'Đã bổ sung');
    ASSERT (SELECT completed_info_at IS NOT NULL FROM public.registrations WHERE id = 'incomplete'),
        'Supplementation must work after intake closes and before completion closes';
    ASSERT NOT has_table_privilege('authenticated', 'public.profiles', 'UPDATE'), 'Profiles cannot be edited directly by clients';
    ASSERT NOT has_function_privilege('anon','public.allocate_campaign_meals(text)','EXECUTE'), 'Only authenticated callers may allocate';
    RAISE NOTICE 'PASS: authenticated intake, supplementation, write and execute grants';
END;
$$;

SELECT set_config('request.jwt.claim.sub', '33333333-3333-3333-3333-333333333333', true);
DO $$
DECLARE result jsonb;
BEGIN
    ASSERT EXISTS (SELECT FROM public.campaigns WHERE id = 'qa-pickup'), 'Volunteer sees only assigned campaigns';
    ASSERT NOT EXISTS (SELECT FROM public.campaigns WHERE id = 'qa-tie'), 'Volunteer has no unassigned campaign rows';
    ASSERT NOT EXISTS (SELECT FROM public.registrations), 'Volunteer never reads registration records directly';

    result := public.verify_ticket('QA_VALID', 'qa-pickup');
    ASSERT (result->>'valid')::boolean, 'Code verifies in time';
    ASSERT (public.campaign_summary('qa-pickup')->>'received')::int = 0, 'Verification alone cannot record a handover';
    result := public.confirm_handover('QA_VALID', 'qa-pickup');
    ASSERT NOT (result->>'already_recorded')::boolean, 'First handover recorded';
    result := public.confirm_handover('QA_VALID', 'qa-pickup');
    ASSERT (result->>'already_recorded')::boolean, 'Reconciliation returns existing result';
    ASSERT (public.campaign_summary('qa-pickup')->>'received')::int = 1, 'Exactly one handover';
    RAISE NOTICE 'PASS: campaign scope, verify and idempotent handover';
END;
$$;

RESET ROLE;
ROLLBACK;
