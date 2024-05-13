ALTER TABLE aktivitetsplikt_vurdering
    ADD CONSTRAINT unique_oppgave_id UNIQUE (oppgave_id);