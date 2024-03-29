CREATE TABLE brev_til_generering
(
    id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    fnr VARCHAR NULL,
    behandling UUID NULL,
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    haandtert BOOLEAN DEFAULT FALSE,
    brevmal VARCHAR NOT NULL,
    saktype VARCHAR NOT NULL,
    PRIMARY KEY(id)
);