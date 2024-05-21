-- Hent opplysninger gjør oppslag på fnr i opplysning som fulltekst, med LIKE som ikke er spesielt performant.
-- Indeksen reduserer responstiden på et ikke-cachet oppslag fra ca 30ms til 2ms

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE EXTENSION IF NOT EXISTS btree_gin;

CREATE INDEX grunnlagshendelse_opplysning_gin_idx ON grunnlagshendelse USING GIN (opplysning gin_trgm_ops);
