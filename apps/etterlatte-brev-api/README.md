# etterlatte-brev-api

Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.


## Lokal utvikling
1. Kafka må kjøres lokalt. Anbefale å benytte [Red Panda](https://redpanda.com/), som ikke trenger zookeeper.
2. Krever kobling mot brev-databasen. Kjør f. eks opp PostgreSQL med Docker.
3. Krever at ey-pdfgen kjører lokalt (ey-pdfgen finnes i [pensjon-etterlatte-felles](https://github.com/navikt/pensjon-etterlatte-felles)). 

Legg f. eks til følgende environment variabler i IntelliJ:

`DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;DB_USERNAME=postgres;DB_PASSWORD=postgres;BREV_LOCAL_DEV=true;KAFKA_RAPID_TOPIC=brev;KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:51336;KAFKA_CONSUMER_GROUP_ID=0;NAIS_APP_NAME=etterlatte-brev-distribusjon;HTTP_PORT=8085;ETTERLATTE_PDFGEN_URL=http://localhost:8081/api/v1/genpdf/brev;HENT_DOKUMENT_URL=https://saf-q1.dev.intern.nav.no/rest/hentdokument;SAF_GRAPHQL_URL=https://saf-q1.dev.intern.nav.no/graphql;`

#### Hvordan kjøre brev-api

1. Kjør `init_services.sh` for å starte kafka, sette opp brev-databasen og starte ey-pdfgen.
2. Kjør deretter `Application.kt` for å starte Brev API