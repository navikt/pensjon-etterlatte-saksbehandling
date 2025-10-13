-- Statistikk for etteroppgjør, i første omgang hvilke resultater som kom fra varsel og vedtak
CREATE VIEW etteroppgjoer_statistikk_view AS
SELECT eo.sak_id, eo.resultat_type, eo.tilknyttet_revurdering, eo.aar
FROM etteroppgjoer_statistikk eo
