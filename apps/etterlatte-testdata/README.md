# etterlatte-testdata

En app med et enkelt gui som poster meldinger til Kafka for å gjøre det enklere å teste de ulike appene til team etterlatte. Den har også kobling mot dolly for å opprette testpersoner.  

### Teknologi
Appen er bygget med kotlin/ktor og gui'et er laget ved hjelp av handlebars.

## Lokal utvikling


Kjør opp kafka i docker med: \
`docker-compose up -d` \
og hent secret med \
`get-secret etterlatte-testdata`

Deretter kan du kjøre `ApplicationKt` med følgende environment variables:

```
DEV=true;KAFKA_BROKERS=0.0.0.0:9092;KAFKA_TARGET_TOPIC=etterlatte.dodsmelding
```

## Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`


## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.


## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
