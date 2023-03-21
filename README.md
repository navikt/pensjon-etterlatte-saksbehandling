# pensjon-etterlatte-saksbehandling

Monorepoet bruker `husky` for pre-commit-hooks. Denne kjører `Prettier` for frontend og `ktlint` for backend.
Før man starter å kode er det derfor viktig å kjøre `bash get-started.sh` fra Root. Da vil alle de tre overnevnte bli 
installert.

Monorepo for ny saksbehandlingsløsning for ytelser til etterlatte

### AzureAD secrets på lokal maskin

Tokens/secrets som gjør det mulig å gå mot dev-gcp fra lokal maskin.\
Kan hentes ved å kjøre følgende kommando:
```
./get-secret.sh apps/<app_du_vil_lage_secrets_for>
```

Hvis du får feilmelding `Not logged in or lacking rights. Ensure you're connected to naisdevice and gcp.`
og du er sikker på du er koblet både naisdevice og gcp så kan det være at du må sette default namespace på context:

```
kubectl config set-context --current --namespace=etterlatte
```

Ved endring i filen kan denne kommandoen kjøres (OBS: dette innebærer at alle må hente tokens på nytt). Eksempel for etterlatte-saksbehandling-ui:
```
kubectl apply -f apps/etterlatte-saksbehandling-ui/.nais/azuread-etterlatte-saksbehandling-ui-lokal.yaml
```

**NB:** Ved kjøring av en eller flere backend(s) lokalt kan det være lurt å installere
[EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) i IntelliJ. Uten denne må du manuelt kopiere
fra .env-filen til appen sine miljøvariabler.

## Apper

[etterlatte-behandling](apps/etterlatte-behandling) \
Tjeneste som holder styr på behandlinger.

[etterlatte-beregning](apps/etterlatte-beregning) \
// TODO

[etterlatte-brev-api](apps/etterlatte-brev-api) \
Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.

[etterlatte-fordeler](apps/etterlatte-fordeler) \
Fordeler aktuelle saker inn til behandling i ny applikasjon.

[etterlatte-grunnlag](apps/etterlatte-grunnlag) \
Tjeneste som holder styr på behandlinger.

[etterlatte-gyldig-soeknad](apps/etterlatte-gyldig-soeknad) \
// TODO

[etterlatte-hendelser-pdl](apps/etterlatte-hendelser-pdl) \
Lytter på hendelser fra Livet er en strøm av hendelser

[etterlatte-oppdater-behandling](apps/etterlatte-oppdater-behandling) \
Oppdater behandling er en Rapid-app, som brukes til å lese meldinger relevant for behandlings-appen og sende HTTP-kall til behandlingsappen for å gjøre forskjellige handlinger.

[etterlatte-opplysninger-fra-pdl](apps/etterlatte-opplysninger-fra-pdl) \
// TODO

[etterlatte-opplysninger-fra-soeknad](apps/etterlatte-opplysninger-fra-soeknad) \
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
Tjeneste som tilbyr forslag til vedtak som kan fattes og attesteres

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

subgraph registere["Registere"]
    pdl["PDL-tjenester"]
    inntektskomponenten
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
beregning -.- t11[/"Beregnet behandling"/]:::msg -.-> vedtaksvurdering

vedtaksvurdering <--> db2[(Database)]:::db
```

# Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Lokal bygg/test - docker

Docker må være installert og kjørende for at lokal bygg/test skal fungere. Hvis man bruker docker desktop så klarer testene
å finne docker socket automatisk. Hvis man har colima kjørende så må man sette opp noen miljøvariabler.

```
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

* DOCKER_HOST peker docker app osv mot sock filen fra colima
* TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE setter sock adressen på innsiden av kontainere

## Arbeidsflyter og bygging
Foruten lokal Gradle-bruk skjer bygging via Github Actions. Vi har definert flere arbeidsflyter (workflows) som spiller sammen.
Hver app har sin app-etterlatte-[appnavn].yaml i .github/workflows. Denne fila må ha samme navn som appen har under apps/

Denne spesifiserer noen parametre og kaller igjen videre. I en pull request blir test-bygget kjørt, ved merge til main blir bygg- og deploy-bygga kjørt
Vi har én .test.yaml, én .build.yaml og én .deploy.yaml som alle apps bruker. build tagger imaget med sha samt hvilken git-grein det blir bygga fra (oftest main).

Fra GitHub er det også mulig å kjøre et bygg for en enkelt app på valgfri git-grein. Denne bruker samme byggejobb som nevnt over.

For å sette alle applikasjonene i dev tilbake til main, har vi en egen byggejobb _etterlatte-tilbakestill-alle.yaml_. Denne deployer for kvar applikasjon nyaste image som er tagga med _main_.

For frontend-appen _saksbehandling-ui_ gjør vi det litt annerledes, så den har sine egne byggejobber deklarert i si yaml-fil.

# Varsling
Det legges ut varsler om feilmeldinger i appene på Slack i kanalen `#team-etterlatte-alerts-dev` og `#team-etterlatte-alerts-prod`

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.

