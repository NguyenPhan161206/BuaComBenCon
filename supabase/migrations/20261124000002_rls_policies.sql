-- Policies govern reads. All writes go through the business functions.

CREATE FUNCTION public.current_user_role()
RETURNS TEXT LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT role FROM public.profiles WHERE id = auth.uid();
$$;

CREATE FUNCTION public.can_manage_campaign(p_campaign_id TEXT)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT public.current_user_role() = 'coordinator' AND EXISTS (
        SELECT 1 FROM public.campaigns WHERE id = p_campaign_id AND coordinator_id = auth.uid()
    );
$$;

CREATE FUNCTION public.can_distribute(p_campaign_id TEXT)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT coalesce(public.can_manage_campaign(p_campaign_id) OR (
        public.current_user_role() = 'volunteer' AND EXISTS (
            SELECT 1 FROM public.campaign_volunteers
            WHERE campaign_id = p_campaign_id AND volunteer_id = auth.uid()
        )
    ), false);
$$;

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campaign_volunteers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recipients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.handovers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.commitments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON public.profiles, public.campaigns, public.campaign_volunteers, public.recipients,
    public.registrations, public.tickets, public.handovers, public.commitments, public.audit_logs
    FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.profiles, public.campaigns, public.registrations, public.tickets, public.commitments
    TO authenticated;

REVOKE EXECUTE ON FUNCTION public.current_user_role(), public.can_manage_campaign(TEXT),
    public.can_distribute(TEXT) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.current_user_role(), public.can_manage_campaign(TEXT),
    public.can_distribute(TEXT) TO authenticated;

CREATE POLICY profiles_own ON public.profiles FOR SELECT TO authenticated USING (id = auth.uid());

CREATE POLICY campaigns_by_role ON public.campaigns FOR SELECT TO authenticated USING (
    public.current_user_role() IN ('caregiver', 'donor') OR public.can_distribute(id)
);

CREATE POLICY registrations_own_or_managed ON public.registrations FOR SELECT TO authenticated USING (
    user_id = auth.uid() OR public.can_manage_campaign(campaign_id)
);

CREATE POLICY tickets_own_or_managed ON public.tickets FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.registrations r WHERE r.id = registration_id)
);

CREATE POLICY commitments_own_or_managed ON public.commitments FOR SELECT TO authenticated USING (
    donor_id = auth.uid() OR public.can_manage_campaign(campaign_id)
);
