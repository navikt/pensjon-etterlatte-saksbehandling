-- Saksbehandlingstid for etteroppgjør
CREATE VIEW etteroppgjoer_saksbehandlingstid AS
SELECT forbehandling_id, hendelse, forbehandling_status, teknisk_tid, resultat_type, tilknyttet_revurdering
FROM etteroppgjoer_statistikk es
