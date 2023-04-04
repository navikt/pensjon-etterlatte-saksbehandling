CREATE TABLE opplysningsgrunnlag
(
    id           UUID PRIMARY KEY,
    trygdetid_id UUID references trygdetid (id) ON delete cascade,
    type         TEXT NOT NULL,
    opplysning   JSONB
);