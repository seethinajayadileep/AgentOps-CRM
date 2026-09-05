-- Hibernate ddl-auto created leads_status_check from an older LeadStatus list.
-- Qualification writes HOT / COLD / FOLLOWED_UP, which that check rejected.

ALTER TABLE public.leads DROP CONSTRAINT IF EXISTS leads_status_check;
ALTER TABLE public.leads ADD CONSTRAINT leads_status_check
    CHECK (status IN (
        'NEW',
        'QUALIFIED',
        'HOT',
        'COLD',
        'FOLLOWED_UP',
        'CLOSED',
        'CONTACTED',
        'PROPOSAL_SENT',
        'NEGOTIATION',
        'WON',
        'LOST',
        'ARCHIVED'
    ));
