-- Tilbakestiller to vedtak fra SAMORDNET til RETURNERT for å rette saksbehandlerfeil.
UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id = '24f2c40e-2add-450d-9e88-534d88a7853e' AND status = 'SAMORDNET';
UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id in( '2ac2e9af-729e-4fc3-8e06-6becd322b195', '849c71b6-22ac-4b3a-a397-f0a8a23a29f3') AND status = 'ATTESTERT';

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
       NOW(),
        2002962,
       '24f2c40e-2add-450d-9e88-534d88a7853e',
        993,
       'EY',
       'MASKINELL',
       'Tilbakestilt maskinelt',
       NULL);

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        468,
        '2ac2e9af-729e-4fc3-8e06-6becd322b195',
        169,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2002976,
        '849c71b6-22ac-4b3a-a397-f0a8a23a29f3',
        1002014,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);

-- Oppdaterer ikke oppgave sin status for dette er i dev, men hvis dette skal gjøres i prod må oppgave sannsynligvis også oppdateres.
