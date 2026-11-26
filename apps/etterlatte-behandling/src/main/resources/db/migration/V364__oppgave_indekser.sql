CREATE INDEX oppgave_referanse_idx ON oppgave (referanse);
CREATE INDEX oppgave_gruppe_id_type_idx ON oppgave (gruppe_id, type);
CREATE INDEX oppgave_fnr_idx ON oppgave (fnr);
CREATE INDEX oppgave_ventefrist_idx ON oppgave (type, kilde, frist) WHERE status = 'PAA_VENT';
