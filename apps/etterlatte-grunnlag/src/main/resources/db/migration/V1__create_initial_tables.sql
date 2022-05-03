
CREATE TABLE opplysning
(
    id UUID PRIMARY KEY,
    sak_id BIGINT NOT NULL,
    data TEXT,
    kilde TEXT,
    type TEXT
);
