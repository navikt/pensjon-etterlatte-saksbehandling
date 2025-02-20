CREATE TABLE grunnlagshendelse_backup AS TABLE grunnlagshendelse;

update grunnlagshendelse
set opplysning = jsonb_set(opplysning::jsonb, '{innsender}', 'null')
where LENGTH(opplysning::JSON ->> 'innsender') < 11 AND opplysning_type = 'PERSONGALLERI_V1';
