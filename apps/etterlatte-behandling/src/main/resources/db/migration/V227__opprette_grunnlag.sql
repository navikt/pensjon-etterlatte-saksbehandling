--
-- PostgreSQL database dump
--

-- Dumped from database version 14.15
-- Dumped by pg_dump version 14.15 (Homebrew)

CREATE EXTENSION IF NOT EXISTS btree_gin WITH SCHEMA public;
COMMENT ON EXTENSION btree_gin IS 'support for indexing common datatypes in GIN';

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';

SET default_tablespace = '';

CREATE TABLE public.behandling_versjon
(
    behandling_id  uuid    NOT NULL,
    sak_id         bigint  NOT NULL,
    hendelsenummer bigint  NOT NULL,
    laast          boolean NOT NULL
);

CREATE TABLE public.grunnlagshendelse
(
    sak_id          bigint NOT NULL,
    opplysning_id   uuid,
    opplysning      text,
    kilde           text,
    opplysning_type text,
    hendelsenummer  bigint NOT NULL,
    fnr             text,
    fom             text,
    tom             text
);

ALTER TABLE ONLY public.behandling_versjon
    ADD CONSTRAINT behandling_versjon_behandling_id_key UNIQUE (behandling_id);

ALTER TABLE ONLY public.grunnlagshendelse
    ADD CONSTRAINT grunnlagshendelse_pkey PRIMARY KEY (sak_id, hendelsenummer);

CREATE INDEX behandling_versjon_behandling_id_sak_id_idx
    ON public.behandling_versjon USING btree (behandling_id, sak_id);

CREATE INDEX grunnlagshendelse_fnr_idx
    ON public.grunnlagshendelse USING btree (fnr);

CREATE INDEX grunnlagshendelse_opplysning_gin_idx
    ON public.grunnlagshendelse USING gin (opplysning public.gin_trgm_ops);

CREATE INDEX grunnlagshendelse_opplysning_id_index
    ON public.grunnlagshendelse USING btree (opplysning_id);

CREATE INDEX grunnlagshendelse_opplysning_type_idx
    ON public.grunnlagshendelse USING btree (opplysning_type);

CREATE INDEX grunnlagshendelse_sak_id_idx
    ON public.grunnlagshendelse USING btree (sak_id);
