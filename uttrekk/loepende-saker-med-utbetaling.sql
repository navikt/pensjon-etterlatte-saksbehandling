--Løpende saker med utbetaling større enn 0
--Database: behandling
WITH gjeldende_vedtak AS (
    SELECT
        v.*,
        ROW_NUMBER() OVER (
            PARTITION BY v.sakid
            ORDER BY v.datoattestert DESC
            ) AS rn
    FROM vedtak v
    WHERE v.saktype = 'OMSTILLINGSSTOENAD'
      AND v.vedtakstatus = 'IVERKSATT'
      AND v.type IN ('INNVILGELSE', 'OPPHOER', 'AVSLAG', 'ENDRING', 'INGEN_ENDRING')
      AND v.datovirkfom <= '2026-10-01'
)
SELECT COUNT(DISTINCT v.sakid) AS antall
FROM gjeldende_vedtak v
         JOIN utbetalingsperiode u ON u.vedtakid = v.id
WHERE v.rn = 1
  AND v.type IN ('INNVILGELSE', 'ENDRING')
  AND (v.opphoer_fom IS NULL OR v.opphoer_fom > '2026-10-01')
  AND u.type = 'UTBETALING'
  AND u.beloep > 0
  AND u.datofom <= '2026-10-01'
  AND (u.datotom IS NULL OR u.datotom >= '2026-10-01');

