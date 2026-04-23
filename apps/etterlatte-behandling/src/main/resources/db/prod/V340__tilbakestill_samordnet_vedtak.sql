-- Tilbakestiller ett spesifikt vedtak fra SAMORDNET til RETURNERT for å rette saksbehandlerfeil.
UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id = '75d3a456-5728-445c-a836-e2c7be37010d' AND status = 'SAMORDNET';

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
       NOW(),
       68905,
       '75d3a456-5728-445c-a836-e2c7be37010d',
       20476,
       'EY',
       'MASKINELL',
       'Tilbakestilt maskinelt',
       NULL);

UPDATE oppgave SET status = 'UNDERKJENT' WHERE referanse = '75d3a456-5728-445c-a836-e2c7be37010d' AND status = 'FERDIGSTILT';
