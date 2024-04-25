-- Flytter mottatt dato en dag frem for klager som er "redigerbare" og opprettet før feilen ble patchet
-- på grunn av feil parsing av innsendt mottatt dato
update klage
set innkommende_klage = jsonb_set(innkommende_klage, '{mottattDato}',
                                  to_jsonb(to_char(date(innkommende_klage ->> 'mottattDato') + interval '1 day',
                                                   'YYYY-MM-DD')))
where status in ('OPPRETTET', 'FORMKRAV_OPPFYLT', 'FORMKRAV_IKKE_OPPFYLT', 'UTFALL_VURDERT', 'RETURNERT') and opprettet < to_date('2024-04-25', 'YYYY-MM-DD');
