-- Vi faser ut felt aarsinntekt inntektUtland (årsinntekt utland) fordi vi skal begynne å fylle inn kun inntekt
-- frem til opphør (hvis det er opphøer). Vi beholder de gamle feltene som sikkerhet i tilfelle vi trenger å se på dem
-- senere.

ALTER TABLE avkortingsgrunnlag
    ADD COLUMN inntekt_tom BIGINT;

UPDATE avkortingsgrunnlag
SET inntekt_tom = aarsinntekt;


ALTER TABLE avkortingsgrunnlag
    ADD COLUMN inntekt_utland_tom BIGINT;

UPDATE avkortingsgrunnlag
SET inntekt_utland_tom = inntekt_utland;
