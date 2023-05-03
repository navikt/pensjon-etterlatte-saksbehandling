CREATE TABLE bp_beregningsgrunnlag
(
    behandlings_id  UUID,
    soesken_med_i_beregning      TEXT,
    kilde           TEXT,
    PRIMARY KEY (behandlings_id)
);