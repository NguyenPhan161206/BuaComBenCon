-- Migration 02: Row Level Security (RLS) Policies
-- Implements role-based access control according to project specification.

-- Helper function to extract current authenticated user's role
CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS TEXT AS $$
    SELECT role FROM public.profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Enable RLS on all public tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.handovers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.commitments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policies
-- Users can read their own profile
CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id);

-- Coordinators can view all profiles
CREATE POLICY "Coordinators can view all profiles"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (public.current_user_role() = 'coordinator');

-- 2. Campaigns Policies
-- Authenticated users (and public) can read active campaigns
CREATE POLICY "Anyone can view non-archived campaigns"
    ON public.campaigns FOR SELECT
    TO authenticated
    USING (true);

-- Only coordinators can insert or update campaigns
CREATE POLICY "Coordinators can manage campaigns"
    ON public.campaigns FOR ALL
    TO authenticated
    USING (public.current_user_role() = 'coordinator')
    WITH CHECK (public.current_user_role() = 'coordinator');

-- 3. Registrations Policies
-- Caregivers can view their own registrations
CREATE POLICY "Caregivers view own registrations"
    ON public.registrations FOR SELECT
    TO authenticated
    USING (
        auth.uid() = user_id 
        OR public.current_user_role() = 'coordinator'
        OR (public.current_user_role() = 'volunteer' AND status IN ('ALLOCATED', 'RECEIVED'))
    );

-- Caregivers can insert their own registration
CREATE POLICY "Caregivers submit registration"
    ON public.registrations FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id OR public.current_user_role() = 'coordinator');

-- Caregivers can cancel their own pending/allocated registration
CREATE POLICY "Caregivers update own registration"
    ON public.registrations FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id OR public.current_user_role() = 'coordinator');

-- 4. Tickets Policies
-- Caregiver can view their own ticket; Volunteer & Coordinator can view tickets for validation
CREATE POLICY "View tickets"
    ON public.tickets FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.registrations r
            WHERE r.id = tickets.registration_id
            AND (r.user_id = auth.uid() OR public.current_user_role() IN ('coordinator', 'volunteer'))
        )
    );

-- 5. Handovers Policies
-- Volunteer and Coordinator can view handovers; Caregiver can view their meal handover
CREATE POLICY "View handovers"
    ON public.handovers FOR SELECT
    TO authenticated
    USING (
        public.current_user_role() IN ('coordinator', 'volunteer')
        OR EXISTS (
            SELECT 1 FROM public.registrations r
            WHERE r.id = handovers.registration_id AND r.user_id = auth.uid()
        )
    );

-- Volunteers and Coordinators can record handovers
CREATE POLICY "Record handovers"
    ON public.handovers FOR INSERT
    TO authenticated
    WITH CHECK (public.current_user_role() IN ('coordinator', 'volunteer'));

-- 6. Commitments Policies
-- Donors view their own commitments; Coordinators view all
CREATE POLICY "Donors view own commitments"
    ON public.commitments FOR SELECT
    TO authenticated
    USING (auth.uid() = donor_id OR public.current_user_role() = 'coordinator');

-- Donors can create commitments
CREATE POLICY "Donors create commitments"
    ON public.commitments FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = donor_id OR public.current_user_role() = 'coordinator');

-- 7. Audit Logs Policies
-- Only coordinators can view audit logs
CREATE POLICY "Coordinators view audit logs"
    ON public.audit_logs FOR SELECT
    TO authenticated
    USING (public.current_user_role() = 'coordinator');
