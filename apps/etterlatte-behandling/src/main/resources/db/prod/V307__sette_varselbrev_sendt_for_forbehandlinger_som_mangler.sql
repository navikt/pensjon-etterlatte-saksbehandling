UPDATE etteroppgjoer_behandling
SET varselbrev_sendt = opprettet::date
WHERE varselbrev_sendt IS NULL
  AND status = 'FERDIGSTILT'
  AND etteroppgjoer_resultat_type != 'INGEN_ENDRING_UTEN_UTBETALING';
