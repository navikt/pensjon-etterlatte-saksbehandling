-- Persongalleri har blitt lagret med mellomrom i fnr som fører til feil etter opprettelse av manuell behandling
update grunnlagshendelse set opplysning=replace(opplysning, ' ', '')
where sak_id = 13008
  AND opplysning_id = '78459124-8e2d-4647-bebd-65b7090ddbcb'
  AND opplysning_type = 'PERSONGALLERI_V1';