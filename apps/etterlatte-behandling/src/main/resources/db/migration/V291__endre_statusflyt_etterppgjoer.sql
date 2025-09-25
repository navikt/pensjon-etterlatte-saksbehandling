
update etteroppgjoer_behandling set status = 'MOTTATT_SKATTEOPPGJOER' where status = 'AVBRUTT_FORBEHANDLING';
update etteroppgjoer_behandling set status = 'VENTER_PAA_SVAR' where status = 'FERDIGSTILT_FORBEHANDLING';
update etteroppgjoer_behandling set status = 'FERDIGSTILT' where status = 'FERDIGSTILT_UTEN_VARSEL';




