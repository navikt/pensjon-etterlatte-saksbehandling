ALTER TABLE grunnlagsendringshendelse
    DROP COLUMN korrekt_i_pdl;
ALTER TABLE grunnlagsendringshendelse
    ADD COLUMN samsvar_mellom_pdl_og_grunnlag JSONB;