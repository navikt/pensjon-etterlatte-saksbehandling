CREATE TABLE avkortinggrunnlag(
   id UUID PRIMARY KEY,
   behandling_id UUID NOT NULL,
   fom Date,
   tom Date,
   aarsinntekt BIGINT,
   gjeldende_aar INTEGER,
   spesifikasjon TEXT
);