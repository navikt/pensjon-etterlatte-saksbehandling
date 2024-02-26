ALTER TABLE oppgave ADD COLUMN behandling_id uuid null;
ALTER TABLE oppgave ADD CONSTRAINT oppgave_behandling_key FOREIGN KEY (behandling_id) REFERENCES behandling (id);
CREATE INDEX ON oppgave(behandling_id);