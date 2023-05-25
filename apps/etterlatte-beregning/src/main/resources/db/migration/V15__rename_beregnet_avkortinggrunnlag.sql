ALTER TABLE IF EXISTS beregnet_avkortinggrunnlag RENAME TO avkortingsperioder;
ALTER TABLE avkortingsperioder DROP COLUMN avkortinggrunnlag;