ALTER TABLE grunnlagsendringshendelse
    ADD COLUMN hendelse_gjelder_rolle TEXT,
    ADD COLUMN korrekt_i_pdl          TEXT DEFAULT 'IKKE_SJEKKET';