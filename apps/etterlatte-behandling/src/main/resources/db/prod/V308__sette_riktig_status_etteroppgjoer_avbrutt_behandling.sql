-- oppdaterer til riktig status for etteroppgjøret for to saker som fikk feil status
update etteroppgjoer
set status = 'VENTER_PAA_SVAR'
where sak_id in (19066, 18558)
  and status = 'MOTTATT_SKATTEOPPGJOER';