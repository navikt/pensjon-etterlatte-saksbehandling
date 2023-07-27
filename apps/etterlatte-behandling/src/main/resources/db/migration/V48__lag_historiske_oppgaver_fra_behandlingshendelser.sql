-- Migrerer historiske oppgaver for alle fullførte trinn basert på behandlinghendelser
WITH s1 as (select *
            from behandlinghendelse h
                     inner join behandling b on h.behandlingid = b.id
                     inner join sak s on b.sak_id = s.id
            where h.hendelse in ('VEDTAK:FATTET', 'VEDTAK:UNDERKJENT', 'VEDTAK:ATTESTERT', 'BEHANDLING:AVBRUTT')
            order by opprettet asc),
     source as (select *,
                       row_number() over (partition by behandlingid order by opprettet asc)  as rekkefolge,
                       row_number() over (partition by behandlingid order by opprettet desc) as motsatt_rekkefolge
                from s1)

-- id, status, enhet, sak_id, saksbehandler, referanse, merknad, opprettet, type, saktype, fnr, frist
INSERT
INTO oppgave (select gen_random_uuid()                                as id,
                     'FERDIGSTILT'                                    as status,
                     source.enhet                                     as enhet,
                     source.sak_id                                    as sak_id,
                     source.ident                                     as saksbehandler,
                     source.behandlingid                              as referanse,
                     case source.hendelse
                         when 'BEHANDLING:AVBRUTT' then 'Behandlingen ble avbrutt'
                         else
                             case concat(source.valgtbegrunnelse, source.kommentar)
                                 when null then null
                                 when '' then ''
                                 else concat(coalesce(source.valgtbegrunnelse, ''), ': ', source.kommentar)
                                 end
                         end                                          as merknad,
                     source.opprettet                                 as opprettet,
                     case source.hendelse
                         when 'VEDTAK:FATTET' then
                             case source.rekkefolge
                                 when 1 then
                                     case source.behandlingstype
                                         when 'FØRSTEGANGSBEHANDLING' then 'FOERSTEGANGSBEHANDLING'
                                         else source.behandlingstype
                                         end
                                 else 'RETURNERT'
                                 end
                         when 'VEDTAK:UNDERKJENT' then 'ATTESTERING'
                         when 'VEDTAK:ATTESTERT' then 'ATTESTERING'
                         when 'BEHANDLING:AVBRUTT' then
                             case source.rekkefolge
                                 when 1 then
                                     case source.behandlingstype
                                         when 'FØRSTEGANGSBEHANDLING' then 'FOERSTEGANGSBEHANDLING'
                                         else source.behandlingstype
                                         end
                                 else case (SELECT hendelse
                                            from source s1
                                            where source.behandlingid = s1.behandlingid and s1.motsatt_rekkefolge = 2)
                                          when 'VEDTAK:UNDERKJENT' then 'RETURNERT'
                                          when 'VEDTAK:ATTESTERT' then 'HENDELSE'
                                          when 'VEDTAK:FATTET' then 'ATTESTERING'
                                     end
                                 end
                         end                                          as type,
                     source.saktype                                   as saktype,
                     source.fnr                                       as fnr,
                     source.behandling_opprettet + interval '1 month' as frist
              FROM source);