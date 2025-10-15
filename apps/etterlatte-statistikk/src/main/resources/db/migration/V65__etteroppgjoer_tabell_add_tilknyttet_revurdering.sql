ALTER TABLE etteroppgjoer_statistikk ADD COLUMN tilknyttet_revurdering bool;

-- Statistikk for etteroppgjør faktisk resultat, hva kom fra varsel og hva fra vedtak
CREATE VIEW etteroppgjoer_faktisk_resultat AS
SELECT eo.sak_id, eo.resultat_type, eo.tilknyttet_revurdering, eo.aar, eo.teknisk_tid
FROM etteroppgjoer_statistikk eo WHERE eo.hendelse = 'FERDIGSTILT'
