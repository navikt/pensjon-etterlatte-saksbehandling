ALTER TABLE avkortet_ytelse ADD COLUMN restanse BIGINT;

CREATE TABLE avkorting_aarsoppgjoer(
   id UUID PRIMARY KEY,
   behandling_id UUID NOT NULL,
   maaned Date,
   beregning BIGINT,
   avkorting BIGINT,
   forventet_avkortet_ytelse BIGINT,
   restanse BIGINT,
   fordelt_restanse BIGINT,
   faktisk_avkortet_ytelse BIGINT
);
