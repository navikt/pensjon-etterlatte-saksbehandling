UPDATE doedshendelse
SET endret = opprettet,
    status = 'NY',
    utfall = null,
    kontrollpunkter = null
WHERE relasjon = 'EKTEFELLE'
  AND status = 'FERDIG'
  AND utfall = 'AVBRUTT'
  AND text(kontrollpunkter) like '%AVDOED_HAR_IKKE_VAERT_GIFT%'
