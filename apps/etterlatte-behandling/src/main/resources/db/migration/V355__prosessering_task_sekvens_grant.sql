-- clone-dev-db.sh kjører pg_dump, som leser sekvensverdier med `SELECT last_value ...`.
-- Da BIGSERIAL-sekvensen fulgte tabellen inn i public i V354, fikk den ingen grant:
-- V353 ga kun SELECT på selve tabellen til PUBLIC. Uten SELECT på sekvensen feiler
-- pg_dump med «permission denied for sequence» for lesende roller (personlige
-- IAM-brukere). Vi speiler tabell-granten på sekvensen.
--
-- Sekvensen arvet det generiske auto-navnet `task_id_seq` fra den opprinnelige
-- tabellen. Vi gir den samme prefiks som tabellen for å unngå forvirring/kollisjon
-- i public. Kolonnens default (nextval) følger med ved rename.
ALTER SEQUENCE public.task_id_seq RENAME TO prosessering_task_id_seq;

GRANT SELECT ON SEQUENCE public.prosessering_task_id_seq TO PUBLIC;
