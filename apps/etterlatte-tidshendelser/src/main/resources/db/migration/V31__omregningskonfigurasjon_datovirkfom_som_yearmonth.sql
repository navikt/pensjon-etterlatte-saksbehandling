ALTER TABLE omregningskonfigurasjon
ALTER COLUMN dato TYPE TEXT
USING to_char(dato, 'YYYY-MM');

ALTER TABLE omregningskonfigurasjon
    RENAME COLUMN dato TO datovirkfom;
