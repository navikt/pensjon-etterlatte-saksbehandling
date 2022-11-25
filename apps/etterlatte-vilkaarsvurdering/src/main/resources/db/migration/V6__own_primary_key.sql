CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE vilkaarsvurdering DROP CONSTRAINT vilkaarsvurdering_pkey;
ALTER TABLE vilkaarsvurdering ADD COLUMN ID UUID PRIMARY KEY DEFAULT (uuid_generate_v4());
CREATE UNIQUE INDEX ON vilkaarsvurdering (behandlingId);
