# Fordeler
Fordeler er en kafka-app.
Tjenesten har ansvar for å avgjøre om søknaden skal rutes til Pesys eller ny vedtaksløsning.
Fordelingskriteriene bestemmes basert på hvilke saker ny vedtaksløsning har støtte for.
Fordeleren benytter PDL for å hente opplysninger.

## Konsepter

### Fordelerkriterier

Fordelingskriterier er formulert som negative utsagn og om ett eller fler av de slår til anses søknaden som ikke egnet for fordeling til ny vedtaksløsning.

Eksempel på et fordelingskriterie:

`GJENLEVENDE_HAR_IKKE_FORELDREANSVAR` : Fra PDL hentes listen over foreldre med foreldreansvar fra barnet sitt familierelasjonobjekt.
Hvis gjenlevendes fødselsnummer ikke er en del av denne listen anser vi at utsagnet har slått til og det blir lagt til på en liste over hvorfor saken ikke blir fordelt til ny løsning 


## Flyt

Fordeler leser meldinger på Kafka med samme kriterier som journalfoer-soeknad og sjekker søknadsinfo mot fordelingskriterier. Fordeler produserer en ny melding på kafka om søknaden anses som gyldig for fordeling til ny vedtaksløsning.

Flyten må oppdateres på et senere tidspunkt for å også sørge for at søknader som fordeles til ny vedtaksløsning ikke journalføres i Pesys

## TODO
* Bør vi ha en wrapper rundt systemklokka? Gir mulighet til å parameterstyre fast dato eller offset for testing.
* Bør legges på en spesifik sjekk mot adressebeskyttelse