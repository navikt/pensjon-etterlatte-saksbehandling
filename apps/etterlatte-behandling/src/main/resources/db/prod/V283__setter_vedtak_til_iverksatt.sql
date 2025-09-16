update behandling
set status = 'IVERKSATT'
where sak_id = 20602
  and id = 'db663e4b-3b03-483e-8343-0f7bdf488da6'
  and status = 'SAMORDNET';

-- Legger også inn en hendelse
insert
into behandlinghendelse (hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype,
                         kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', (select opprettet + interval '1 minute'
                             from behandlinghendelse
                             where hendelse = 'VEDTAK:SAMORDNET'
                               and sakid = 20602
                               and behandlingid = 'db663e4b-3b03-483e-8343-0f7bdf488da6'),
        (select opprettet + interval '1 minute'
         from behandlinghendelse
         where hendelse = 'VEDTAK:SAMORDNET'
           and sakid = 20602
           and behandlingid = 'db663e4b-3b03-483e-8343-0f7bdf488da6'), 60094,
        'db663e4b-3b03-483e-8343-0f7bdf488da6', 20602, null, null, 'Manuelt registrert hendelse', null);