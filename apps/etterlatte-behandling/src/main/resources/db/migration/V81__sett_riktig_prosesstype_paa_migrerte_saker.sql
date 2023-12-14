UPDATE behandling
SET prosesstype = 'AUTOMATISK'
WHERE kilde = 'PESYS'
  AND behandlingstype = 'FØRSTEGANGSBEHANDLING'
  AND virkningstidspunkt::JSONB -> 'kilde' ->> 'ident' = 'PESYS'
  AND virkningstidspunkt::JSONB ->> 'begrunnelse' = 'Automatisk importert fra Pesys'
  AND virkningstidspunkt::JSONB ->>'dato' = '2024-01'
  AND prosesstype = 'MANUELL';