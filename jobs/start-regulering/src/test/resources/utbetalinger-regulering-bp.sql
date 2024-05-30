select u.sak_id, l.beloep, l.periode_fra, l.periode_til, l.opprettet, u.vedtak_id
from utbetaling u
         join utbetalingslinje l on l.utbetaling_id = u.id
where
    u.saktype = 'BARNEPENSJON'
  and (u.saksbehandler = 'EY' and u.opprettet >= '2024-05-27')
