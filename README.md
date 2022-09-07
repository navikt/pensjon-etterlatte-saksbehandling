# pensjon-etterlatte-saksbehandling

Monorepoet bruker `husky` for pre-commit-hooks. Denne kjører `Prettier` for frontend og `ktlint` for backend.
Før man starter å kode er det derfor viktig å kjøre `bash get-started.sh` fra Root. Da vil alle de tre overnevnte bli 
installert.

Monorepo for ny saksbehandlingsløsning for ytelser til etterlatte

## Apper

[etterlatte-api](apps/etterlatte-api) \
Tjeneste som kobler frontend og backend sammen. Kallene gjøres i hovedsak via REST, med unntak av posting av data til grunnlag som lager en kafka-melding.

[etterlatte-avkorting](apps/etterlatte-avkorting) \
// TODO

[etterlatte-behandling](apps/etterlatte-behandling) \
Tjeneste som holder styr på behandlinger.

[etterlatte-beregning](apps/etterlatte-beregning) \
// TODO

[etterlatte-brev-api](apps/etterlatte-brev-api) \
Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.

[etterlatte-brev-distribusjon](apps/etterlatte-brev-distribusjon) \
Rapid app for å distribuere brev. Håndterer journalføring og distribusjon.

[etterlatte-brreg](apps/etterlatte-brreg) \
App med API for å hente data fra enhetsregisteret.

[etterlatte-fordeler](apps/etterlatte-fordeler) \
Fordeler aktuelle saker inn til behandling i ny applikasjon.

[etterlatte-grunnlag](apps/etterlatte-grunnlag) \
Tjeneste som holder styr på behandlinger.

[etterlatte-gyldig-soeknad](apps/etterlatte-gyldig-soeknad) \
// TODO

[etterlatte-hendelser-pdl](apps/etterlatte-hendelser-pdl) \
Lytter på hendelser fra Livet er en strøm av hendelser

etterlatte-medl-proxy](apps/etterlatte-medl-proxy) \
Oppslagstjeneste mot medlemskapsregistret (folketrygden).

[etterlatte-oppdater-behandling](apps/etterlatte-oppdater-behandling) \
// TODO

[etterlatte-opplysninger-fra-inntektskomponenten](apps/etterlatte-opplysninger-fra-inntektskomponenten) \
// TODO

[etterlatte-opplysninger-fra-pdl](apps/etterlatte-opplysninger-fra-pdl) \
// TODO

[etterlatte-opplysninger-fra-soeknad](apps/etterlatte-opplysninger-fra-soeknad) \
// TODO

[etterlatte-overvaaking](apps/etterlatte-overvaaking) \
// TODO

[etterlatte-pdltjenester](apps/etterlatte-pdltjenester) \
// TODO

[etterlatte-saksbehandling-ui](apps/etterlatte-saksbehandling-ui) \
Appen består av en statisk frontend og en backend-for-frontends i NodeJS.

[etterlatte-testdata](apps/etterlatte-testdata) \
App for forenkling av manuell testing.

[etterlatte-tilbakekreving](apps/etterlatte-tilbakekreving) \
Mottar tilbakekrevingsvedtak fra Tilbakekrevingskomponenten til Oppdrag

[etterlatte-utbetaling](apps/etterlatte-utbetaling) \
Oversetter vedtak til et format som kan oversendes til oppdrag.

[etterlatte-vedtaksvurdering](apps/etterlatte-vedtaksvurdering) \
// TODO

[etterlatte-vilkaar-kafka](apps/etterlatte-vilkaar-kafka) \
// TODO

# Flytdiagram

### Hvordan appene samarbeider

```mermaid
flowchart TD

classDef front fill:#BA99D2FF,color:#000,stroke:#C488F0FF
classDef app fill:#88AACC,color:#000,stroke:#335577
classDef db fill:#D7AE69FF,color:#000,stroke:#F39D0CFF
classDef kafka fill:#79ab6d,color:#000,stroke:#39ba15
classDef msg fill:#555,color:#ddd,stroke:none

fordeler:::kafka -.-> gyldig-soeknad

gyldig-soeknad:::kafka -.- t1[/"S&oslash;knad med\n behandlingsId og saksId"/]:::msg -.-> opplysninger-fra-soeknad
gyldig-soeknad -.-> behandling:::app
gyldig-soeknad --> pdl

behandling -.- t2[/"Behandling opprettet\n inkludert persongalleri\n fra s&oslash;knad"/]:::msg -.-> grunnlag
behandling -.- t3[/"Behandling uten grunnlag"/]:::msg -.-> grunnlag
behandling --> etterlatte-api:::app
etterlatte-api --> frontend:::front

subgraph registere["Registere"]
    pdl["PDL-tjenester"]
    inntektskomponenten
    MEDL
    BRREG
end

opplysninger-fra-soeknad:::kafka -.- t5[/"Ny opplysning"/]:::msg -.-> grunnlag:::app
opplysninger-fra-pdl:::kafka --> pdl
opplysninger-fra-pdl -- "(kommer)" --> inntektskomponenten
opplysninger-fra-pdl -.->  t5 -.-> grunnlag

oppdater-behandling -.-> behandling

grunnlag -.- t6[/"Opplysningsbehov"/]:::msg -.-> opplysninger-fra-pdl
grunnlag -.- t7[/"Grunnlagsendring p&aring; sak"/]:::msg -.-> oppdater-behandling:::kafka
grunnlag -.- t8[/"Behandling med grunnlag"/]:::msg -.-> vilkaar:::kafka
grunnlag --> database[(Database)]:::db

vilkaar -.- t9[/"Vilkaarsvurdert behandling"/]:::msg -.-> vedtaksvurdering:::kafka
t9 -.-> beregning:::kafka
beregning -.- t11[/"Beregnet behandling"/]:::msg -.-> avkortning:::kafka
t11 -.-> vedtaksvurdering
avkortning -.- t12[/"Avkortet behandling"/]:::msg -.-> vedtaksvurdering

vedtaksvurdering <--> db2[(Database)]:::db
vedtaksvurdering -.-> etterlatte-api
```

# Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
