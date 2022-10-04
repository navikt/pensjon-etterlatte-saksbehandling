ALTER TABLE grunnlagshendelse
    ADD COLUMN if not exists fom text,
    ADD COLUMN if not exists tom text
