CREATE SCHEMA vilkaarsvurdering;

ALTER TABLE vilkaarsvurdering
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE delvilkaar
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE migrert_yrkesskade
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE vilkaar
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE vilkaarsvurdering_kilde
    SET SCHEMA vilkaarsvurdering;
