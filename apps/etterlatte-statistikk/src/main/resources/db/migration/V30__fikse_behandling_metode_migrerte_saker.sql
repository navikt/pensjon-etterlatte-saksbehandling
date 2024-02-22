update sak
set behandling_metode = 'AUTOMATISK'
where kilde = 'PESYS'
  and saksbehandler = 'EY'
  and behandling_metode != 'AUTOMATISK';