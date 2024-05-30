select u.sak_id, l.periode_fra, l.periode_til, l.opprettet, u.vedtak_id
from utbetaling u
         join utbetalingslinje l on l.utbetaling_id = u.id
where
    u.saktype = 'BARNEPENSJON'
  and (u.saksbehandler != 'EY' or u.opprettet < '2024-05-27')
