# etterlatte-opplysninger-fra-inntektskomponenten

Enkel Rapids&River-app som svarer på behov om opplysning `AVDOED_INNTEKT_V1`. 

Appen gjør kall til A-inntekt for å hente inntekter, samt AA-registeret for å hente arbeidsforhold. Følgende blir 
responsene fra disse tjenestene transformert til to separate grunnlagsopplysninger, som postes tilbake til kafka for 
å besvare behovet.

Disse opplysningene blir senere benyttet for å kunne avklare avdødes forutgående medlemskap, og si noe om utbetaling
av ytelser (uføretrygd, alderspensjon, foreldrepermisjon, sykepenger osv.)

Bygget med Kotlin, Rapids & Rivers og [Ktor](https://ktor.io/).


# Kom i gang

## Kjøre lokalt

Appen er ikke satt opp for kunne kjøres lokalt p.t.

# Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-opplysninger-fra-inntektskomponenten/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.


## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
