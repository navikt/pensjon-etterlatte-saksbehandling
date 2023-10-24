ALTER TABLE trygdetid DROP CONSTRAINT trygdetid_behandling_id_key;
ALTER TABLE trygdetid ALTER COLUMN ident SET NOT NULL;
ALTER TABLE trygdetid ADD UNIQUE (behandling_id, ident);