create table etterbetaling
(
    behandlings_id UUID PRIMARY KEY
        CONSTRAINT etterbetaling_behandling_fk_id
            REFERENCES behandling (id),
    opprettet   TIMESTAMP,
    fra_dato DATE,
    til_dato DATE
)