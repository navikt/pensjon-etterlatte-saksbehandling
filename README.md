# pensjon-etterlatte-saksbehandling

### Ny saksbehandlingsløsning for ytelser til etterlatte

Monorepoet bruker `husky` for pre-commit-hooks. Denne kjører `Prettier` for frontend og `ktlint` for backend.
Før man starter å kode er det derfor viktig å kjøre `bash get-started.sh` fra Root. Da vil alle de tre overnevnte bli
installert.

# Lokal utvikling

## Hvordan kjøre en eller flere apper

Det enkleste er å kjøre opp frontend og valgfri backend med docker compose som starter alt man trenger for at APIet skal
fungere.

1. **Hent secrets**

   Kjør scriptet `get-secret.sh` med appnavn.
    ```shell
    ./get-secret.sh <app_name>
    ```

   Eksempel for henting av secrets til frontend:
    ```shell
    ./get-secret.sh etterlatte-saksbehandling-ui
    ```

2. **Kjør docker compose**
    ```shell
    docker compose up -d
    ```
   Legg til `--build` hvis du trenger å bygge imaget på nytt. Mer info
   her: [docker compose up](https://docs.docker.com/engine/reference/commandline/compose_up/) \
   NB: Hvis du vil kjøre frontend mot lokal backend må du overskrive variabel for `_API`-url (se oversikt i `config.ts`)

3. **Kjør run config** \
   Config for å kjøre appen (i IntelliJ) ligger i `.run`. Denne skal dukke opp automatisk under `Run configurations` i
   IntelliJ.

### Feilsøking

- Hvis du får feilmelding `Not logged in or lacking rights. Ensure you're connected to naisdevice and gcp.`
  og du er sikker på du er koblet både naisdevice og gcp, kan det være at du må sette default namespace på context:\
  `kubectl config set-context --current --namespace=etterlatte`
- Run config for backend bruker [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) i IntelliJ, så pass på at
  denne pluginen er installert før kjøring.

### Frontend mot lokal backend

Om du skal kjøre med frontend og wonderwall mot lokal backend må du overskrive backend-appen sin 
`*_API_URL` i `./apps/etterlatte-saksbehandling-ui/.env.dev-gcp`. 

Se i filen `config.ts` for å se hvilke `_API_URL` verdier som kan overskrives. 

Eksempel på overskrevet verdi: \
`BEHANDLING_API_URL=http://host.docker.internal:8090`

### Postgres (DB)

Apper som har en database vil få dette opprettet ved å kjøre `docker-compose up -d`. Alternativt er det mulig å
kjøre en proxy mot gcp-dev ved å kjøre `nais postgres proxy <appnavn>`. Merk at for at dette skal fungere
kan det ikke sendes passord ved opprettelse av database-kobling.

**OBS:** Ved bruk av proxy til lokal kjøring må du passe på å ikke endre på Flyway-scripts i prosjektet.

# Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Maven

Vi bruker personal access token til autentisering for å hente intern pakker til prosjektet.
Du må derfor sette `export GITHUB_TOKEN='DITT_TOKEN'` som miljøvariabel.
Dette tokenet må autoriseres mot navikt.

## Lokal bygg/test - docker

Docker må være installert og kjørende for at lokal bygg/test skal fungere. Hvis man bruker docker desktop så klarer
testene å finne docker socket automatisk. Hvis man har colima kjørende så må man sette opp noen miljøvariabler.

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

