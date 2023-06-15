ALTER TABLE avkortet_ytelse ADD COLUMN restanse BIGINT;

CREATE TABLE avkorting_aarsoppgjoer(
   id UUID PRIMARY KEY,
   behandling_id UUID NOT NULL,
   maaned Date,
   avkorting_forventet_inntekt BIGINT,
   tidligere_avkorting BIGINT,
   restanse BIGINT,
   ny_avkorting BIGINT
);
