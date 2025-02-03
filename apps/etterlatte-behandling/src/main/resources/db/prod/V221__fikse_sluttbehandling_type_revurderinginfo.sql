do $body$
DECLARE
antallSluttbehandlingerEndret integer;
BEGIN
    update revurdering_info set info = jsonb_set(info, '{type}', '"SLUTTBEHANDLING"'::JSONB) where info->>'type' = 'SLUTTBEHANDLING_UTLAND';

    antallSluttbehandlingerEndret := (select count(*) from revurdering_info where info->>'type' = 'SLUTTBEHANDLING');
    if antallSluttbehandlingerEndret == 8 then
        commit;
    else
        RAISE EXCEPTION 'Feil antall oppdatert, committer ikke migrering';
    end if;
END;
    $body$
LANGUAGE 'plpgsql';