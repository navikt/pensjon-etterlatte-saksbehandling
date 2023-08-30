ALTER TABLE avkortingsgrunnlag ADD COLUMN virkningstidspunkt Date;

ALTER TABLE avkortet_ytelse ADD COLUMN inntektsgrunnlag UUID;
ALTER TABLE avkortingsperioder ADD COLUMN inntektsgrunnlag UUID;

--UPDATE avkortet_ytelse SET type = 'FORRIGE_VEDTAK' where type = 'TIDLIGERE';
--UPDATE avkortet_ytelse SET type = 'AARSOPPGJOER' where type = 'NY';


