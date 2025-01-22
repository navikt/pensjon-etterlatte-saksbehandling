# Oppdater behandling

Oppdater behandling er en Rapid-app, som brukes til å lese meldinger relevant for behandlings-appen og sende HTTP-kall 
til behandlingsappen for å gjøre forskjellige handlinger. Denne appen er egentlig bindeleddet som gir relevante
meldinger fra rapid til behandlingsappen, som ikke er en Rapid-app i seg selv.

## Meldinger som leses
### Grunnlagsendring
Når et grunnlag er endret / oppdatert på en sak sender grunnlags-appen ut en melding om dette. For at behandling skal
reagere leser oppdater behandling disse meldingene, og poster til behandling at grunnlaget er endret 
i en sak.

### Hendelser om vedtak
Når vedtak sender ut melding om at noe har skjedd på et vedtak leses dette og sendes med et HTTP-kall til behandling.

### PdlHendelser
Når hendelser fra PDL sender ut en melding om en personhendelse vi muligens er interessert i, plukkes denne meldingen 
opp sendes med et HTTP-kall til behandlings-appen.
