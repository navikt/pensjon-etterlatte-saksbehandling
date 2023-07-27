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
                                            where source.behandlingid = s1.behandlingid
                                              and s1.motsatt_rekkefolge = 2)
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

-- få med en åpen oppgave av riktig type for alle behandlinger som ikke er avsluttet. typen oppgave kan utledes
-- fra den siste behandlingshendelsen som skjedde på denne behandlingen
WITH hendelser_med_rekkefolge as (select *,
                                         row_number() over (partition by behandlingid order by opprettet desc) as motsatt_rekkefolge
                                  from behandlinghendelse hendelser
                                  order by opprettet asc),
     siste_hendelse_per_behandling as (select * from hendelser_med_rekkefolge where motsatt_rekkefolge = 1) -- motsatt_rekkefolge = 1 <=> siste hendelse
INSERT
INTO oppgave (SELECT gen_random_uuid()                                    as id,
                     'UNDER_ARBEID'                                       as status,
                     sak.enhet                                            as enhet,
                     behandling.sak_id                                    as sak_id,
                     null                                                 as saksbehandler,
                     behandling.id                                        as referanse,
                     null                                                 as merknad,
                     sisteHendelse.opprettet,
                     case sisteHendelse.hendelse
                         when 'BEHANDLING:OPPRETTET'
                             then case behandling.behandlingstype
                                      when 'FØRSTEGANGSBEHANDLING' then 'FOERSTEGANGSBEHANDLING'
                                      else behandling.behandlingstype
                             end
                         when 'BEHANDLING:AVBRUTT' then 'SKAL_IKKE_SKJE'
                         when 'VEDTAK:FATTET' then 'ATTESTERING'
                         when 'VEDTAK:UNDERKJENT' then 'RETURNERT'
                         when 'VEDTAK:ATTESTERT' then 'SKAL_IKKE_SKJE'
                         when 'VEDTAK:IVERKSATT' then 'SKAL_IKKE_SKJE'
                         end                                              as type,
                     sak.saktype                                          as saktype,
                     sak.fnr                                              as fnr,
                     behandling.behandling_opprettet + interval '1 month' as frist
              FROM behandling behandling
                       inner join sak sak on behandling.sak_id = sak.id
                       inner join siste_hendelse_per_behandling sisteHendelse
                                  on behandling.id = sisteHendelse.behandlingid
              where behandling.status not in ('IVERKSATT', 'ATTESTERT', 'AVBRUTT'));


