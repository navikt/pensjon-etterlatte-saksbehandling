UPDATE behandling
SET status = 'IVERKSATT'
WHERE id = 'f82acb23-d7c5-476f-acf1-03e2472ec459'
  AND status = 'SAMORDNET';

INSERT INTO behandlinghendelse (id, hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype,
                                kommentar, valgtbegrunnelse)
VALUES (default, 'VEDTAK:IVERKSATT', now(), now(), 64987, 'f82acb23-d7c5-476f-acf1-03e2472ec459', 27140, null, null,
        null, null);
