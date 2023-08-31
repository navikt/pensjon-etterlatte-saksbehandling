ALTER TABLE avkortingsgrunnlag ADD COLUMN virkningstidspunkt Date;

ALTER TABLE avkortingsperioder ADD COLUMN inntektsgrunnlag UUID;

ALTER TABLE avkortet_ytelse ADD COLUMN inntektsgrunnlag UUID;
UPDATE avkortet_ytelse SET type = 'FORRIGE_VEDTAK' where type = 'TIDLIGERE';
UPDATE avkortet_ytelse SET type = 'AARSOPPGJOER' where type = 'NY';

ALTER TABLE avkortet_ytelse DROP COLUMN restanse;
ALTER TABLE avkortet_ytelse ADD COLUMN restanse UUID;


