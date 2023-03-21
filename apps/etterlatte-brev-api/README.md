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


#### 1. Kjør docker compose

```shell
docker compose up -d
```

#### 2. Opprett/oppdater app config i IntelliJ

**Med auth mot dev-gcp:** \
Kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..). 

`$ ./get-secret.sh apps/etterlatte-brev-api`

Om du skal kjøre med frontend og wonderwall må du også kjøre (fra [rotmappe](../..)): 

`$ ./get-secret.sh apps/etterlatte-saksbehandling-ui`

Copy-paste dette inn i "environment variables".

```
BRREG_URL=https://data.brreg.no
ETTERLATTE_GRUNNLAG_CLIENT_ID=ce96a301-13db-4409-b277-5b27f464d08b;
ETTERLATTE_GRUNNLAG_URL=https://etterlatte-grunnlag.dev.intern.nav.no;
ETTERLATTE_PDFGEN_URL=http://host.docker.internal:8081/api/v1/genpdf/brev;
ETTERLATTE_PROXY_OUTBOUND_SCOPE="";
ETTERLATTE_PROXY_URL=host.docker.internal:9091;
ETTERLATTE_VEDTAK_CLIENT_ID=069b1b2c-0a06-4cc9-8418-f100b8ff71be;
ETTERLATTE_VEDTAK_URL=https://etterlatte-vedtaksvurdering.dev.intern.nav.no
HTTP_PORT=8084;
KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092;
KAFKA_CONSUMER_GROUP_ID=0;
KAFKA_RAPID_TOPIC=brev;
NAIS_APP_NAME=etterlatte-brev-api;
NORG2_URL=https://norg2.dev-fss-pub.nais.io/norg2/api/v1;
SAF_BASE_URL=host.docker.internal:9091;
SAF_SCOPE=dev-fss:saf:saf;
```

**OBS:** Siden vi går mot Norg2 APIet i dev (fra lokal maskin) må du koble til Naisdevice.

##### 3. Kjør din nye run config (ApplicationKt)
