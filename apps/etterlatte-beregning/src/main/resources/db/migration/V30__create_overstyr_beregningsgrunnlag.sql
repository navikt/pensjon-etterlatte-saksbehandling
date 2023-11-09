CREATE TABLE overstyr_beregningsgrunnlag(
    id UUID PRIMARY KEY,
    behandlings_id UUID NOT NULL,
    dato_fra_og_med Date,
    dato_til_og_med Date,
    utbetalt_beloep BIGINT,
    trygdetid BIGINT,
    sak_id BIGINT
);
