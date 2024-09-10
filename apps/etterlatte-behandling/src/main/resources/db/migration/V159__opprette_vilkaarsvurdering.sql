--
-- PostgreSQL database dump
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

SET default_tablespace = '';

CREATE TABLE public.delvilkaar (
                                   id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
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


CREATE TABLE public.migrert_yrkesskade (
                                           behandling_id uuid,
                                           sak_id bigint
);

CREATE TABLE public.vilkaar (
                                id uuid NOT NULL,
                                vilkaarsvurdering_id uuid,
                                resultat_kommentar character varying,
                                resultat_tidspunkt timestamp without time zone,
                                resultat_saksbehandler character varying
);



CREATE TABLE public.vilkaarsvurdering (
                                          id uuid NOT NULL,
                                          behandling_id uuid NOT NULL,
                                          grunnlag_versjon integer NOT NULL,
                                          virkningstidspunkt date NOT NULL,
                                          resultat_utfall character varying,
                                          resultat_kommentar character varying,
                                          resultat_tidspunkt timestamp without time zone,
                                          resultat_saksbehandler character varying
);


CREATE TABLE public.vilkaarsvurdering_kilde (
                                                vilkaarsvurdering_id uuid NOT NULL,
                                                kopiert_fra_vilkaarsvurdering_id uuid
);


ALTER TABLE ONLY public.delvilkaar
    ADD CONSTRAINT delvilkaar_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.vilkaar
    ADD CONSTRAINT vilkaar_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_pkey PRIMARY KEY (vilkaarsvurdering_id);

ALTER TABLE ONLY public.vilkaarsvurdering
    ADD CONSTRAINT vilkaarsvurdering_v2_pkey PRIMARY KEY (id);


CREATE INDEX delvilkaar_vilkaar_id_idx ON public.delvilkaar USING btree (vilkaar_id);

CREATE INDEX delvilkaar_vilkaar_type_idx ON public.delvilkaar USING btree (vilkaar_type);

CREATE INDEX vilkaar_vilkaarsvurdering_id_idx ON public.vilkaar USING btree (vilkaarsvurdering_id);

CREATE INDEX vilkaarsvurdering_behandling_id_idx ON public.vilkaarsvurdering USING btree (behandling_id);

ALTER TABLE ONLY public.delvilkaar
    ADD CONSTRAINT delvilkaar_vilkaar_id_fkey FOREIGN KEY (vilkaar_id) REFERENCES public.vilkaar(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.vilkaar
    ADD CONSTRAINT vilkaar_vilkaarsvurdering_id_fkey FOREIGN KEY (vilkaarsvurdering_id) REFERENCES public.vilkaarsvurdering(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_kopiert_fra_vilkaarsvurdering_id_fkey FOREIGN KEY (kopiert_fra_vilkaarsvurdering_id) REFERENCES public.vilkaarsvurdering(id);

ALTER TABLE ONLY public.vilkaarsvurdering_kilde
    ADD CONSTRAINT vilkaarsvurdering_kilde_vilkaarsvurdering_id_fkey FOREIGN KEY (vilkaarsvurdering_id) REFERENCES public.vilkaarsvurdering(id);
