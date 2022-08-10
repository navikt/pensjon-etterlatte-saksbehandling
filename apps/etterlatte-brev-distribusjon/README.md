# etterlatte-brev-distribusjon

Rapid app for å distribuere brev. Håndterer journalføring og distribusjon. 

## Lokal utvikling

### Krav for å kjøre lokalt
- **Kafka** må kjøres lokalt. Docker er anbefalt.

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
