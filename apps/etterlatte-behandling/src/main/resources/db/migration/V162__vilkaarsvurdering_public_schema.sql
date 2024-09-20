ALTER TABLE vilkaarsvurdering.vilkaarsvurdering
    SET SCHEMA public;

ALTER TABLE vilkaarsvurdering.delvilkaar
    SET SCHEMA public;

ALTER TABLE vilkaarsvurdering.migrert_yrkesskade
    SET SCHEMA public;

ALTER TABLE vilkaarsvurdering.vilkaar
    SET SCHEMA public;

ALTER TABLE vilkaarsvurdering.vilkaarsvurdering_kilde
    SET SCHEMA public;

SET search_path TO vilkaarsvurdering, public;

DROP SCHEMA vilkaarsvurdering CASCADE;
DROP EXTENSION IF EXISTS "vilkaarsvurdering.uuid-ossp";

SET search_path TO public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
alter table delvilkaar alter id set default uuid_generate_v4();