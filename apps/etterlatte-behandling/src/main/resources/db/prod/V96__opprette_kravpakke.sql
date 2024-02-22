INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist, kilde)
VALUES(gen_random_uuid(), 'NY', '4862', 8906, 'KRAVPAKKE_UTLAND', NULL, '31a73af4-8375-4482-8055-921ba8e5c997', NULL, now()::timestamp, 'BARNEPENSJON', (select fnr from sak where id = '8906'), NULL, 'GENERELL_BEHANDLING');

INSERT INTO generellbehandling(id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status)
VALUES(gen_random_uuid()::UUID, NULL, 8906, now()::timestamp, 'KRAVPAKKE_UTLAND', '31a73af4-8375-4482-8055-921ba8e5c997', 'OPPRETTET');

INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('GENERELL_BEHANDLING:OPPRETTET', now()::timestamp, 11816, '31a73af4-8375-4482-8055-921ba8e5c997'::UUID, 8906, NULL, NULL, NULL, NULL);
