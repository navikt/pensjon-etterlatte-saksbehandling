-- Oppdaterer tilbakekrevingen til riktig status
update tilbakekreving set status = 'ATTESTERT' where id = 'c5a70a61-2c28-42e7-ab68-f05ae07f6e3a';

-- Setter status til ferdigstilt for attesteringsoppgave som er fullført
update oppgave set status = 'FERDIGSTILT' where id = '627b3bf3-f98e-4773-b4c2-e27fe896ee07';

-- Legger til hendelse for at dataene skal være konsistent
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('TILBAKEKREVING:ATTESTERT', now()::timestamp, 39226, 'c5a70a61-2c28-42e7-ab68-f05ae07f6e3a'::UUID, 10980, 'R125364', 'SAKSBEHANDLER', NULL, NULL);