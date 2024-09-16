CREATE SCHEMA vilkaarsvurdering;

ALTER TABLE public.vilkaarsvurdering
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE public.delvilkaar
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE public.migrert_yrkesskade
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE public.vilkaar
    SET SCHEMA vilkaarsvurdering;

ALTER TABLE public.vilkaarsvurdering_kilde
    SET SCHEMA vilkaarsvurdering;
