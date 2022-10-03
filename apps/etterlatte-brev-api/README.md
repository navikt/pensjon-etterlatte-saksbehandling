# etterlatte-brev-api

Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.


## Lokal utvikling

### Krav for å kjøre lokalt
- **Kafka** må kjøres lokalt. Docker er anbefalt.
- **Postgres**: Kjør f. eks opp PostgreSQL med Docker eller bruk [Postgres.app](https://postgresapp.com/)
- **ey-pdfgen**: Repo [pensjon-etterlatte-felles](https://github.com/navikt/pensjon-etterlatte-felles) må klones. Deretter kan pdfgen kjøres via Docker.  

### Hvordan kjøre brev-api

Det enkleste er å kjøre docker compose som starter alt man trenger for at APIet skal fungere.  

Kommandoen under må kjøres _før_ docker compose og trenger kun kjøres én gang.

OBS: Hvis du bruker bash i stedet for zsh må du bytte ut `~/.zshrc` med `~/.bashrc`  

```shell
echo "export PDFGEN_HOME=$(find ~/ -type d -name ey-pdfgen -print -quit 2>/dev/null)" >> ~/.zshrc \
  && source ~/.zshrc
```


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
NAIS_APP_NAME=etterlatte-brev-api;
HTTP_PORT=8085;
ETTERLATTE_PDFGEN_URL=http://localhost:8081/api/v1/genpdf/brev;
NORG2_URL=https://norg2.dev-fss-pub.nais.io/norg2/api/v1;
HENT_DOKUMENT_URL=https://saf-q1.dev.intern.nav.no/rest/hentdokument;
SAF_GRAPHQL_URL=https://saf-q1.dev.intern.nav.no/graphql;
ETTERLATTE_PROXY_URL="";
ETTERLATTE_PROXY_OUTBOUND_SCOPE="";
NORG2_URL="";
ETTERLATTE_BRREG_URL="";
```

##### 3. Kjør din nye run config (ApplicationKt)
