-- bare vilkårlig oppdaterer enumverdi for dev, siden vi hadde ingen støtte for
-- ingen endring uten utbetaling før denne PR'en
update etteroppgjoer_beregnet_resultat
set resultat_type = 'INGEN_ENDRING_MED_UTBETALING'
where resultat_type = 'INGEN_ENDRING';