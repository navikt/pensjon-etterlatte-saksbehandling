CREATE TABLE opplysningsgrunnlag
(
    id           UUID PRIMARY KEY,
    trygdetid_id UUID references trygdetid (id) ON delete cascade,
    type         TEXT NOT NULL,
    opplysning   JSONB,
    kilde        JSONB
);

ALTER TABLE trygdetid ADD COLUMN sak_id BIGINT;
ALTER TABLE trygdetid ADD UNIQUE (behandling_id)