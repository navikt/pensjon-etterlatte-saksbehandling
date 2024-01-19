-- Avslutter alle duplikate hendelser unntatt en per duplikat over (samsvar_mellom_pdl_og_grunnlag, sak_id, gjelder_person, hendelse_gjelder_rolle)

-- Finner duplikate hendelser ved å se på row_num over partisjonen og bare de med row_num > 1
create temporary view duplikate_grunnlagsendringshendelser AS
WITH duplikate_ider AS (
    SELECT id, ROW_NUMBER() over (PARTITION BY samsvar_mellom_pdl_og_grunnlag, sak_id, gjelder_person, hendelse_gjelder_rolle) as row_num
    FROM grunnlagsendringshendelse
    WHERE status = 'SJEKKET_AV_JOBB'
    order by opprettet desc
)
SELECT id from duplikate_ider
where row_num > 1;

-- setter de duplikate hendelsene til forkastet
update grunnlagsendringshendelse
set status = 'FORKASTET'
FROM duplikate_grunnlagsendringshendelser
where grunnlagsendringshendelse.id = duplikate_grunnlagsendringshendelser.id;

-- setter de duplikate oppgavene til avbrutt av EY med en merknad
update oppgave set status='AVBRUTT', saksbehandler='EY', merknad='Lukket på grunn av duplikat'
FROM duplikate_grunnlagsendringshendelser
where oppgave.referanse = duplikate_grunnlagsendringshendelser.id::text and oppgave.kilde='HENDELSE';