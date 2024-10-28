-- Setter oppgaver til alle feilede jobber til avbrytt
update oppgave
set status  = 'AVBRUTT',
    merknad = 'Avbrutt på grunn av bug under automatisk jobb'

where id IN ('2bcc10f5-bd0a-4629-89e9-31604a027055', 'ba003359-248c-4ea6-8f63-031e17b327d1',
             '1f844623-bfc7-4989-b5bc-912bee24f238', 'f2c0ca66-3027-4f63-8c7e-738d8a1e5dc3');
