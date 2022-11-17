create TABLE beregning (
    beregningId UUID UNIQUE PRIMARY KEY,
    beregningstype text,
    beregning JSONB
);
