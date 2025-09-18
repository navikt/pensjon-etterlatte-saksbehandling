alter table etteroppgjoer_beregnet_resultat
    add column har_ingen_inntekt bool;
update etteroppgjoer_beregnet_resultat
set har_ingen_inntekt = false
where har_ingen_inntekt = null;