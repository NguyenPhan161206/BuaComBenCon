-- Migration 01: Initial Schema for BuaComBenCon
-- Creates base tables, primary keys, foreign keys, constraints, and audit logging.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Profiles (Linked to Supabase Auth Users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('caregiver', 'coordinator', 'volunteer', 'donor')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for searching users by phone or role
CREATE INDEX IF NOT EXISTS idx_profiles_phone ON public.profiles(phone);
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);

-- 2. Campaigns (Support Events)
CREATE TABLE IF NOT EXISTS public.campaigns (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    meal TEXT NOT NULL,
    event_date DATE NOT NULL,
    pickup_time TEXT NOT NULL,
    location TEXT NOT NULL,
    confirmed_supply INTEGER NOT NULL DEFAULT 0 CHECK (confirmed_supply >= 0),
    pledged_supply INTEGER NOT NULL DEFAULT 0 CHECK (pledged_supply >= 0),
    prepared_supply INTEGER NOT NULL DEFAULT 0 CHECK (prepared_supply >= 0),
    deadline TEXT NOT NULL,
    rules TEXT NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    is_registration_open BOOLEAN NOT NULL DEFAULT true,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    archived_received INTEGER NOT NULL DEFAULT 0 CHECK (archived_received >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_campaigns_event_date ON public.campaigns(event_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_open ON public.campaigns(is_registration_open);

-- 3. Registrations (Applicant records)
CREATE TABLE IF NOT EXISTS public.registrations (
    id TEXT PRIMARY KEY,
    campaign_id TEXT NOT NULL REFERENCES public.campaigns(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    recipient_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    is_assisted BOOLEAN NOT NULL DEFAULT false,
    assisted_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ALLOCATED', 'WAITING', 'RECEIVED', 'CANCELLED')),
    queue_number SERIAL,
    completed_info_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    note TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Enforce rule: max 1 active registration per recipient phone per campaign
    CONSTRAINT uq_recipient_campaign_phone UNIQUE (campaign_id, phone)
);

CREATE INDEX IF NOT EXISTS idx_registrations_campaign_status ON public.registrations(campaign_id, status);
CREATE INDEX IF NOT EXISTS idx_registrations_queue_order ON public.registrations(completed_info_at ASC, queue_number ASC);
CREATE INDEX IF NOT EXISTS idx_registrations_user_id ON public.registrations(user_id);

-- 4. Tickets (Meal Allocation Tickets with QR Code & Short Code)
CREATE TABLE IF NOT EXISTS public.tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id TEXT NOT NULL UNIQUE REFERENCES public.registrations(id) ON DELETE CASCADE,
    ticket_code TEXT NOT NULL UNIQUE,
    qr_payload TEXT NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT false,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tickets_code ON public.tickets(ticket_code);
CREATE INDEX IF NOT EXISTS idx_tickets_registration ON public.tickets(registration_id);

-- 5. Handovers (One-time delivery confirmation records)
CREATE TABLE IF NOT EXISTS public.handovers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id TEXT NOT NULL UNIQUE REFERENCES public.registrations(id) ON DELETE RESTRICT,
    campaign_id TEXT NOT NULL REFERENCES public.campaigns(id) ON DELETE RESTRICT,
    volunteer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    handover_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    verification_method TEXT NOT NULL DEFAULT 'QR_SCAN' CHECK (verification_method IN ('QR_SCAN', 'SHORT_CODE', 'MANUAL_DISPATCH')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_handovers_campaign ON public.handovers(campaign_id);

-- 6. Commitments (Donor pledges)
CREATE TABLE IF NOT EXISTS public.commitments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id TEXT NOT NULL REFERENCES public.campaigns(id) ON DELETE CASCADE,
    donor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    donor_name TEXT NOT NULL,
    donor_phone TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    cadence TEXT NOT NULL DEFAULT 'Theo đợt' CHECK (cadence IN ('Theo đợt', 'Mong muốn theo tuần', 'Mong muốn theo tháng')),
    consent BOOLEAN NOT NULL DEFAULT true,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_commitments_donor ON public.commitments(donor_id);
CREATE INDEX IF NOT EXISTS idx_commitments_campaign ON public.commitments(campaign_id);

-- 7. Audit Logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    action TEXT NOT NULL,
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON public.audit_logs(entity_name, entity_id);
