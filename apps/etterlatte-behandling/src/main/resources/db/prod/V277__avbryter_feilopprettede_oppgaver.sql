update oppgave
set status  = 'AVBRUTT',
    merknad = ('Oppgaven er avbrutt på grunn av feil opprettelse. ' || merknad)
where id in ('1a82a86c-b0c4-4df6-9c98-15b2e0589c21',
    '22f6e234-1781-4455-b832-a04e2204c58e');
