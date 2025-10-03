
update etteroppgjoer set status = 'MOTTATT_SKATTEOPPGJOER' where status = 'AVBRUTT_FORBEHANDLING';
update etteroppgjoer set status = 'VENTER_PAA_SVAR' where status = 'FERDIGSTILT_FORBEHANDLING';
update etteroppgjoer set status = 'FERDIGSTILT' where status = 'FERDIGSTILT_UTEN_VARSEL';
update etteroppgjoer set status = 'FERDIGSTILT' where status = 'FERDIGSTILT_REVURDERING';
