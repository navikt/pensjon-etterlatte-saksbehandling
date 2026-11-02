-- oppdaterer til riktig status for annulert omgjort etteroppgjør
update etteroppgjoer
set status = 'FERDIGSTILT'
where sak_id = 23363
  and inntektsaar = 2025
  and status = 'VENTER_PAA_SVAR';

update etteroppgjoer
set status = 'FERDIGSTILT'
where sak_id = 16925
  and inntektsaar = 2024
  and status = 'VENTER_PAA_SVAR';