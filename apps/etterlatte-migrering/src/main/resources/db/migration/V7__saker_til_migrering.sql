CREATE TABLE saker_til_migrering
(
    id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    sakId BIGINT NOT NULL, -- Pesys-id
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    haandtert BOOLEAN DEFAULT FALSE,
    PRIMARY KEY(id)
);