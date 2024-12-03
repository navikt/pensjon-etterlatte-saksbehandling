-- Sletter kolonner brukt ifm. migrering av oppgaver til ny statusflyt
ALTER TABLE oppgave DROP COLUMN type_backup;
ALTER TABLE oppgave DROP COLUMN status_backup;
