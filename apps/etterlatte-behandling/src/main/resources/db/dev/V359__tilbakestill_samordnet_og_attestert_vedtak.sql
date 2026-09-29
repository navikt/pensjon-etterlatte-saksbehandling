UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id = in(
                                                                              'e639e274-3f62-4dee-9719-0599b453d6a7',
                                                                              'e7b26469-a32c-40e5-a919-a4704b15071e',
                                                                              '69aef3cd-47e6-46b3-8597-803ff3cb4e94',
                                                                              '85f2eb96-8a73-4bab-90a9-cb468cb9d19a',
                                                                              '1c90560e-2a63-4587-a624-729e2bf4e711'
                                                                             ) AND status = 'SAMORDNET';

UPDATE behandling SET status = 'RETURNERT', sist_endret = NOW() WHERE id in(
                                                                            'eeeb6bea-c266-4b08-8103-6f82e230abbe',
                                                                            '6ec4b9e4-5e98-445b-9046-0a65b61bde3d'
                                                                           ) AND status = 'ATTESTERT';

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
       NOW(),
        2003011,
       'e639e274-3f62-4dee-9719-0599b453d6a7',
        1009498,
       'EY',
       'MASKINELL',
       'Tilbakestilt maskinelt',
       NULL);

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003017,
        'e7b26469-a32c-40e5-a919-a4704b15071e',
        1012260,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003036,
        '69aef3cd-47e6-46b3-8597-803ff3cb4e94',
        1011759,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);


INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003068,
        '85f2eb96-8a73-4bab-90a9-cb468cb9d19a',
        1012694,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);


INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003102,
        '1c90560e-2a63-4587-a624-729e2bf4e711',
        1012861,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);


INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003008,
        'eeeb6bea-c266-4b08-8103-6f82e230abbe',
        1012473,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);


INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('UNDERKJENT',
        NOW(),
        2003023,
        '6ec4b9e4-5e98-445b-9046-0a65b61bde3d',
        1012741,
        'EY',
        'MASKINELL',
        'Tilbakestilt maskinelt',
        NULL);

-- Oppdaterer ikke oppgave sin status for dette er i dev, men hvis dette skal gjøres i prod må oppgave sannsynligvis også oppdateres.
