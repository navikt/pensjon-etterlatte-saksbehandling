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
