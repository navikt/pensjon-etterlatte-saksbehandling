CREATE TABLE beregnet_avkortinggrunnlag(
   id UUID PRIMARY KEY,
   avkortinggrunnlag UUID NOT NULL,
   behandling_id UUID NOT NULL,
   fom Date,
   tom Date,
   avkorting BIGINT,
   tidspunkt TIMESTAMP,
   regel_resultat TEXT
);