-- Tilbakestiller ett spesifikt vedtak fra SAMORDNET til RETURNERT for å rette saksbehandlerfeil.
UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id = '75d3a456-5728-445c-a836-e2c7be37010d' AND status = 'SAMORDNET';

UPDATE vedtak
SET vedtakstatus         = 'RETURNERT',
    attestant            = NULL,
    datoattestert        = NULL,
    attestertvedtakenhet = NULL,
WHERE behandlingid = '75d3a456-5728-445c-a836-e2c7be37010d'
  AND vedtakstatus = 'SAMORDNET';

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
SELECT 'UNDERKJENT',
       NOW(),
       v.id,
       b.id,
       b.sak_id,
       'EY',
       'MASKINELL',
       'Tilbakestilt maskinelt',
       NULL
FROM behandling b
         JOIN vedtak v ON v.behandlingid = b.id
WHERE b.id = '75d3a456-5728-445c-a836-e2c7be37010d';

UPDATE oppgave SET status = 'UNDERKJENT' WHERE referanse = '75d3a456-5728-445c-a836-e2c7be37010d'  AND status = 'FERDIGSTILT';
