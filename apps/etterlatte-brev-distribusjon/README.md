# etterlatte-brev-distribusjon

Rapid app for å distribuere brev. Håndterer journalføring og distribusjon.

Applikasjonen lytter på events av typen `BREV:FERDIGSTILT` som blir utstedt av `etterlatte-brev-api` når saksbehandler
velger å sende et brev til distribusjon. Applikasjonen svarer tilbake i to steg med status og response/id når den har 
gjennomført hhv. journalføring (`BREV:JOURNALFOERT`) og distribusjon (`BREV:DISTRIBUERT`).

Applikasjonen av avhengig av rest-endepunkt i `etterlatte-brev-api` for å hente ut PDF som skal journalføres, ettersom
den kan være større en konfiguert max-størrelse på kafka-meldinger. `etterlatte-brev-api` lagrer svarene fra disse 
meldingene ned i egen postgres-database for å kunne vise rett status i frontend.

## Lokal utvikling

### Krav for å kjøre lokalt
- **Kafka** må kjøres lokalt. Docker er anbefalt.
- Hvis `BREV_LOCAL_DEV=true` er satt som environment property blir kallene til journalføring/distribusjon mocket ut, og
authentisering blir skrudd av så man kan teste flyten lokalt. Se forøvrig README-filen til `etterlatte-brev-api`.

### Hvordan kjøre brev-distribusjon

Det enkleste er å kjøre docker compose som starter kafka.

**OBS:** Dersom du skal kjøre brev-api i tillegg er bedre å kjøre `docker-compose.yml` 
som ligger i [../etterlatte-brev-api](../etterlatte-brev-api).


##### 1. Kjør docker compose

```shell
docker compose up -d
```

##### 2. Opprett/oppdater app config i IntelliJ

Copy-paste dette inn i "environment variables".

```
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
BREV_LOCAL_DEV=true;
KAFKA_RAPID_TOPIC=brev;
KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;
KAFKA_CONSUMER_GROUP_ID=0;
NAIS_APP_NAME=etterlatte-brev-distribusjon;
HTTP_PORT=8090;
```

##### 3. Kjør din nye run config (ApplicationKt)
