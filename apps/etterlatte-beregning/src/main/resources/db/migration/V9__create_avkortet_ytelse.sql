CREATE TABLE avkortet_ytelse(
   id UUID PRIMARY KEY,
   behandling_id UUID NOT NULL,
   fom Date,
   tom Date,
   ytelse_etter_avkorting BIGINT,
   tidspunkt TIMESTAMP,
   regel_resultat TEXT
);