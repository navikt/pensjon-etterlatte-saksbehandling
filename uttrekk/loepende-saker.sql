
--Løpende saker på gitt dato, her for Omstillingsstønad
--Database: behandling
with gjeldende_vedtak AS (
    select DISTINCT ON (sakid) sakid,
    id,
    type,
    datovirkfom,
    opphoer_fom
FROM vedtak v
WHERE vedtakstatus = 'IVERKSATT'
  AND type IN ('INNVILGELSE', 'OPPHOER', 'AVSLAG', 'ENDRING', 'INGEN_ENDRING')
  AND saktype = 'OMSTILLINGSSTOENAD'
  and datovirkfom <= '2026-10-01'
ORDER BY sakid, datoattestert DESC, datovirkfom desc, id desc
    )
select count(DISTINCT gjeldende_vedtak.sakid)
from gjeldende_vedtak
where gjeldende_vedtak.type IN ('INNVILGELSE', 'ENDRING', 'INGEN_ENDRING')
  and (opphoer_fom is null or opphoer_fom > '2026-10-01')
;
