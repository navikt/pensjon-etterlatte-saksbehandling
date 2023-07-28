-- Vi nuker alt som er laget fra før, og gjenskaper det vakkert live. Dette betyr at alle åpne oppgaver mister saksbehandler i dev
truncate table oppgave;

-- Migrerer historiske oppgaver for alle fullførte trinn basert på behandlinghendelser
WITH source as (select *,
                       row_number() over (partition by behandlingid order by opprettet asc)  as rekkefolge, -- vi trenger å vite om dette er første / siste / nest siste hendelse
                       row_number() over (partition by behandlingid order by opprettet desc) as motsatt_rekkefolge
                from behandlinghendelse h
                         inner join behandling b on h.behandlingid = b.id
                         inner join sak s on b.sak_id = s.id
                where h.hendelse in ('VEDTAK:FATTET', 'VEDTAK:UNDERKJENT', 'VEDTAK:ATTESTERT', 'BEHANDLING:AVBRUTT')
                order by opprettet asc)
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
                                 when '' then ''
                                 else concat(source.valgtbegrunnelse, ': ', source.kommentar)
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
-- fra den siste behandlingshendelsen som skjedde på denne behandlingen.
-- Siden alle behandlinger får hendelsen "'BEHANDLING:OPPRETTET'" med en gang kan vi garantere at vi har
-- minst en hendelse per behandling
WITH hendelser_med_rekkefolge as (select *,
                                         row_number() over (partition by behandlingid order by opprettet desc) as motsatt_rekkefolge
                                  from behandlinghendelse hendelser
                                  order by opprettet asc),
     siste_hendelse_per_behandling
         as (select * from hendelser_med_rekkefolge where motsatt_rekkefolge = 1) -- motsatt_rekkefolge = 1 <=> siste hendelse
INSERT
INTO oppgave (SELECT gen_random_uuid()                                    as id,
                     'UNDER_ARBEID'                                       as status,
                     sak.enhet                                            as enhet,
                     behandling.sak_id                                    as sak_id,
                     null                                                 as saksbehandler,
                     behandling.id                                        as referanse,
                     null                                                 as merknad,
                     sisteHendelse.opprettet                              as opprettet,
                     case sisteHendelse.hendelse
                         when 'BEHANDLING:OPPRETTET'
                             then case behandling.behandlingstype
                                      when 'FØRSTEGANGSBEHANDLING' then 'FOERSTEGANGSBEHANDLING'
                                      else behandling.behandlingstype
                             end
                         when 'VEDTAK:FATTET' then 'ATTESTERING'
                         when 'VEDTAK:UNDERKJENT' then 'RETURNERT'

                         -- Disse skal ikke skje siden disse hendelsene impliserer at behandlingen ikke er åpen
                         when 'BEHANDLING:AVBRUTT' then 'SKAL_IKKE_SKJE'
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
                   -- vi vil kun ha åpne behandlinger
              where behandling.status not in ('IVERKSATT', 'ATTESTERT', 'AVBRUTT'));

-- Lager oppgaver basert på hendelser
WITH source as (SELECT g.id as hendelseid, *
                from grunnlagsendringshendelse g
                         inner join sak s on g.sak_id = s.id
                where status not in ('FORKASTET', 'VENTER_PAA_JOBB'))
INSERT
INTO oppgave (SELECT gen_random_uuid()                     as id,
                     case source.status
                         when 'SJEKKET_AV_JOBB' then 'UNDER_ARBEID'
                         when 'VURDERT_SOM_IKKE_RELEVANT' then 'FERDIGSTILT'
                         when 'HISTORISK' then 'FERDIGSTILT'
                         when 'TATT_MED_I_BEHANDLING' then 'FERDIGSTILT'
                         end                               as status,
                     source.enhet                          as enhet,
                     source.sak_id                         as sak_id,
                     null                                  as saksbehandler,
                     source.hendelseid                     as referanse,
                     concat(source.type, case coalesce(source.kommentar, '')
                                             when '' then ''
                                             else concat(': ', source.kommentar)
                         end)                              as merknad,
                     source.opprettet                      as opprettet,
                     'HENDELSE'                            as type,
                     source.saktype                        as saktype,
                     source.fnr                            as fnr,
                     source.opprettet + interval '1 month' as opprettet
              FROM source);

