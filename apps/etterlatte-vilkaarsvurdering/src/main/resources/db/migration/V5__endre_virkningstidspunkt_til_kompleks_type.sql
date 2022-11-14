ALTER TABLE vilkaarsvurdering ALTER COLUMN virkningstidspunkt TYPE JSONB
    USING virkningstidspunkt::jsonb