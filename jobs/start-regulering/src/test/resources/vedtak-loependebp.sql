select distinct vedtak.sakid from vedtak
where type = 'INNVILGELSE'
  and vedtakstatus = 'IVERKSATT'
  and saktype = 'BARNEPENSJON'
  and sakid not in (
    select distinct vedtak.sakid from vedtak
    where type = 'OPPHOER'
      and vedtakstatus = 'IVERKSATT'
      and datovirkfom < '2024-06-01'
);
