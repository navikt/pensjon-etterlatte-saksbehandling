-- Flytter prosessering-jobbkøen fra sitt eget skjema inn i public. Da slipper vi å
-- håndtere et ekstra skjema når dev-databasen klones lokalt (clone-dev-db.sh), og
-- tabellen ligger sammen med behandlings øvrige public-tabeller. Prefikset navn
-- unngår kollisjon og gjør opphavet tydelig.
--
-- V352/V353 opprettet `prosessering.task` og ble deployet til dev. Denne migreringen
-- konsoliderer sluttilstanden likt på tvers av miljøer: på dev flyttes den eksisterende
-- tabellen, på ferske databaser flyttes tabellen som nettopp ble opprettet av V352.
-- Eid sekvens og indeks følger med tabellen ved SET SCHEMA.
ALTER TABLE prosessering.task SET SCHEMA public;
ALTER TABLE public.task RENAME TO prosessering_task;
ALTER INDEX public.idx_task_plukk RENAME TO idx_prosessering_task_plukk;

DROP SCHEMA IF EXISTS prosessering;

-- Bevarer lesetilgangen V353 ga personlige IAM-roller, nå på public-tabellen.
GRANT SELECT ON public.prosessering_task TO PUBLIC;
