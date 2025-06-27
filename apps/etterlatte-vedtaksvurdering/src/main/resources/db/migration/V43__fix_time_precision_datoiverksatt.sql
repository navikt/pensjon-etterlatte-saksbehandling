UPDATE vedtak
SET datoiverksatt = date_trunc('milliseconds', datoiverksatt)
WHERE datoiverksatt IS NOT NULL;