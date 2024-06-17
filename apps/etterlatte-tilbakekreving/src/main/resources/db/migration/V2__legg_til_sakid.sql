ALTER TABLE tilbakekreving_hendelse ADD COLUMN sak_id BIGINT;
ALTER TABLE tilbakekreving_hendelse ALTER COLUMN kravgrunnlag_id DROP NOT NULL;