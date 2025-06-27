UPDATE vedtak
SET datoiverksatt = to_char(
                            date_trunc('milliseconds', datoiverksatt::timestamp),
                            'YYYY-MM-DD"T"HH24:MI:SS.MS'
                    ) || 'Z'
WHERE vedtakstatus = 'IVERKSATT'
  AND datoiverksatt IS NOT NULL;