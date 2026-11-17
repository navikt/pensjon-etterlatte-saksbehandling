--Gjennomsnittlig utbetalt beløp per måned og annualisert, fra en gitt dato til dagen i dag
WITH RECURSIVE
    param AS (
        SELECT CURRENT_DATE AS dato
    ),
    linjer AS (
        SELECT
            l.id,
            l.sak_id,
            l.erstatter_id,
            l.type,
            l.periode_fra,
            l.periode_til,
            l.beloep
        FROM utbetalingslinje l
                 JOIN utbetaling u ON u.id = l.utbetaling_id
        WHERE u.saktype = 'OMSTILLINGSSTOENAD'
          AND EXISTS (
            SELECT 1
            FROM utbetalingshendelse h
            WHERE h.utbetaling_id = u.id
              AND h.status IN ('GODKJENT', 'GODKJENT_MED_FEIL')
        )
    ),
    erstattede (id) AS (
        SELECT l.erstatter_id
        FROM linjer l
                 CROSS JOIN param p
        WHERE l.erstatter_id IS NOT NULL
          AND l.periode_fra <= p.dato

        UNION

        SELECT l.erstatter_id
        FROM erstattede e
                 JOIN linjer l ON l.id = e.id
        WHERE l.erstatter_id IS NOT NULL
    ),
    beloep_per_sak AS (
        SELECT
            l.sak_id,
            SUM(l.beloep) AS maanedsbeloep
        FROM linjer l
                 CROSS JOIN param p
        WHERE l.type = 'UTBETALING'
          AND l.periode_fra >= DATE '2026-01-01'
          AND l.periode_fra <= p.dato
          AND (l.periode_til IS NULL OR l.periode_til >= p.dato)
          AND NOT EXISTS (
            SELECT 1
            FROM erstattede e
            WHERE e.id = l.id
        )
        GROUP BY l.sak_id
    )
SELECT
    COUNT(*) AS antall_saker_med_positiv_ytelse,
    ROUND(AVG(maanedsbeloep), 0) AS gjennomsnitt_per_maaned,
    ROUND(AVG(maanedsbeloep) * 12, 0) AS gjennomsnitt_annualisert
FROM beloep_per_sak
WHERE maanedsbeloep > 0;
