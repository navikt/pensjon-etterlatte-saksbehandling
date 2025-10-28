
delete from etteroppgjoer_pensjonsgivendeinntekt where skatteordning = 'SVALBARD';

update etteroppgjoer_pensjonsgivendeinntekt
set loensinntekt = 600000 where loensinntekt = 300000;
