CREATE TABLE anvendt_trygdetid
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behandling_id     UUID NOT NULL,
    foer_kombinering  JSONB NOT NULL,
    etter_kombinering JSONB NOT NULL
);