-- PostgreSQL database dump

CREATE SCHEMA vilkaarsvurdering;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA vilkaarsvurdering;

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

SET default_tablespace = '';

CREATE TABLE vilkaarsvurdering.delvilkaar (
    id uuid DEFAULT vilkaarsvurdering.uuid_generate_v4() NOT NULL,
    vilkaar_id uuid,
    vilkaar_type character varying NOT NULL,
    hovedvilkaar boolean NOT NULL,
    tittel character varying NOT NULL,
    beskrivelse character varying,
    paragraf character varying,
    ledd integer,
    bokstav character varying,
    lenke character varying,
    resultat character varying,
    spoersmaal text
);

CREATE TABLE vilkaarsvurdering.migrert_yrkesskade (
    behandling_id uuid,
    sak_id bigint
);

CREATE TABLE vilkaarsvurdering.vilkaar (
    id uuid NOT NULL,
    vilkaarsvurdering_id uuid,
    resultat_kommentar character varying,
    resultat_tidspunkt timestamp without time zone,
    resultat_saksbehandler character varying
);

CREATE TABLE vilkaarsvurdering.vilkaarsvurdering (
    id uuid NOT NULL,
    behandling_id uuid NOT NULL,
    grunnlag_versjon integer NOT NULL,
    virkningstidspunkt date NOT NULL,
    resultat_utfall character varying,
    resultat_kommentar character varying,
    resultat_tidspunkt timestamp without time zone,
    resultat_saksbehandler character varying
);

CREATE TABLE vilkaarsvurdering.vilkaarsvurdering_kilde (
    vilkaarsvurdering_id uuid NOT NULL,
    kopiert_fra_vilkaarsvurdering_id uuid
);

ALTER TABLE ONLY vilkaarsvurdering.delvilkaar
    ADD CONSTRAINT delvilkaar_pkey PRIMARY KEY (id);

ALTER TABLE ONLY vilkaarsvurdering.vilkaar
    ADD CONSTRAINT vilkaar_pkey PRIMARY KEY (id);

ALTER TABLE ONLY vilkaarsvurdering.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_pkey PRIMARY KEY (vilkaarsvurdering_id);

ALTER TABLE ONLY vilkaarsvurdering.vilkaarsvurdering
    ADD CONSTRAINT vilkaarsvurdering_v2_pkey PRIMARY KEY (id);


CREATE INDEX delvilkaar_vilkaar_id_idx ON vilkaarsvurdering.delvilkaar USING btree (vilkaar_id);

CREATE INDEX delvilkaar_vilkaar_type_idx ON vilkaarsvurdering.delvilkaar USING btree (vilkaar_type);

CREATE INDEX vilkaar_vilkaarsvurdering_id_idx ON vilkaarsvurdering.vilkaar USING btree (vilkaarsvurdering_id);

CREATE INDEX vilkaarsvurdering_behandling_id_idx ON vilkaarsvurdering.vilkaarsvurdering USING btree (behandling_id);

ALTER TABLE ONLY vilkaarsvurdering.delvilkaar
    ADD CONSTRAINT delvilkaar_vilkaar_id_fkey FOREIGN KEY (vilkaar_id) REFERENCES vilkaarsvurdering.vilkaar(id) ON DELETE CASCADE;

ALTER TABLE ONLY vilkaarsvurdering.vilkaar
    ADD CONSTRAINT vilkaar_vilkaarsvurdering_id_fkey FOREIGN KEY (vilkaarsvurdering_id) REFERENCES vilkaarsvurdering.vilkaarsvurdering(id) ON DELETE CASCADE;

ALTER TABLE ONLY vilkaarsvurdering.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_kopiert_fra_vilkaarsvurdering_id_fkey FOREIGN KEY (kopiert_fra_vilkaarsvurdering_id) REFERENCES vilkaarsvurdering.vilkaarsvurdering(id);

ALTER TABLE ONLY vilkaarsvurdering.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_vilkaarsvurdering_id_fkey FOREIGN KEY (vilkaarsvurdering_id) REFERENCES vilkaarsvurdering.vilkaarsvurdering(id);
