update grunnlagshendelse
set opplysning = jsonb_set(opplysning::jsonb, '{innsender}', 'null')
where LENGTH(opplysning::JSON ->> 'innsender') < 11 AND opplysning_type = 'PERSONGALLERI_V1';

-- todo hvordan gjøre denne med rollback?