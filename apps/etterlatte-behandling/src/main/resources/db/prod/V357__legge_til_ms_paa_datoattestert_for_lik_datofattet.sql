UPDATE vedtak
SET datoattestert = (datoattestert::timestamptz + interval '1 millisecond')::text
WHERE datofattet IS NOT NULL
  AND datoattestert IS NOT NULL
  AND datofattet = datoattestert;