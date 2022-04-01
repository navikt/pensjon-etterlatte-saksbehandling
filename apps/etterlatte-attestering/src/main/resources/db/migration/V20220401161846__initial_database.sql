CREATE TYPE status as ENUM ('TIL_ATTESTERING', 'IKKE_ATTESTERT', 'ATTESTERT');

CREATE TABLE attestasjon (
    vedtak_id VARCHAR NOT NULL,
    attestant_id VARCHAR,
    attestasjonstidspunkt TIMESTAMP,
    attestasjonsstatus status,
    PRIMARY KEY(vedtak_id)
);
