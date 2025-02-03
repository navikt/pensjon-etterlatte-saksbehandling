do $body$
DECLARE
antallfeil integer = 0;
antallSluttbehandling integer = 0;
antallSluttbehandlingerRiktig integer = 0;
BEGIN
    antallfeil := (select count(*) from revurdering_info where info->>'type' = 'SLUTTBEHANDLING_UTLAND');
    antallSluttbehandling := (select count(*) from revurdering_info where info->>'type' = 'SLUTTBEHANDLING');

    update revurdering_info set info = jsonb_set(info, '{type}', '"SLUTTBEHANDLING"'::JSONB) where info->>'type' = 'SLUTTBEHANDLING_UTLAND';

    antallSluttbehandlingerRiktig := (select count(*) from revurdering_info where info->>'type' = 'SLUTTBEHANDLING');
    if (antallfeil + antallSluttbehandling) = antallSluttbehandlingerRiktig then
        commit;
    else
        RAISE EXCEPTION 'Feil antall oppdatert, committer ikke migrering';
    end if;
END;
    $body$
LANGUAGE 'plpgsql';