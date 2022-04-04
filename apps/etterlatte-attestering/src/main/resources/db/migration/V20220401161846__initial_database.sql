CREATE TYPE status as ENUM ('TIL_ATTESTERING', 'IKKE_ATTESTERT', 'ATTESTERT');

CREATE TABLE attestasjon (
    id BIGSERIAL PRIMARY KEY,
    vedtak_id VARCHAR NOT NULL,
    attestant_id VARCHAR,
    attestasjonstidspunkt TIMESTAMP,
    attestasjonsstatus status
);
