# etterlatte-beregning

Tjeneste som tilbyr endepunkter for å opprette/oppdatere og hente ut en beregning tilknyttet en behandling

Det som støttes
- Opprette / Oppdatere en beregning
- Hente en beregning
- Slette beregninger tilknyttet en sak ved kafka meldinger fra vedlikeholdsriveren


## Oppsett

Applikasjonen tilbyr et REST-api. I bakkant ligger en PostgreSQL-database som lager, oppdaterer, henter og sletter 
beregninger tilknyttet en behandling.

## Kjøre lokalt (auth fra dev-gcp + db lokalt eller proxy gcp)
1. For å sette opp riktig konfigurasjon for applikasjonen, kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
```
./get-secret.sh apps/etterlatte-beregning
```
2. Sett følgende environment-variabler under oppstart av applikasjonen.
```
DB_JDBC_URL=jdbc:postgresql://localhost:5433/postgres;
DB_PASSWORD=postgres;
DB_USERNAME=postgres;
ETTERLATTE_BEHANDLING_CLIENT_ID=59967ac8-009c-492e-a618-e5a0f6b3e4e4;
ETTERLATTE_BEHANDLING_URL=https://etterlatte-behandling.intern.dev.nav.no;
ETTERLATTE_GRUNNLAG_CLIENT_ID=ce96a301-13db-4409-b277-5b27f464d08b;
ETTERLATTE_GRUNNLAG_URL=https://etterlatte-grunnlag.intern.dev.nav.no;
ETTERLATTE_TRYGDETID_CLIENT_ID=8385435e-f3d7-45ec-be59-ebd1c71df735;
ETTERLATTE_TRYGDETID_URL=https://etterlatte-trygdetid.intern.dev.nav.no;
ETTERLATTE_VILKAARSVURDERING_CLIENT_ID=dev-gcp.etterlatte.etterlatte-vilkaarsvurdering;
ETTERLATTE_VILKAARSVURDERING_URL=http://etterlatte-vilkaarsvurdering;
GRUNNLAG_AZURE_SCOPE=api://dev-gcp.etterlatte.etterlatte-grunnlag/.default;
NAIS_APP_NAME=etterlatte-beregning;
NAIS_CLUSTER_NAME=dev-gcp;
ELECTOR_PATH=localhost;
HTTP_PORT=8089;
```
Legg også til `.env.dev-gcp` som `Env-file` under `Run configurations` i Intellij.

3. Kjør opp en lokal postgres-database med `docker-compose up -d`. Alternativt er det mulig å kjøre en proxy mot
   gcp-dev ved å kjøre `nais postgres proxy etterlatte-beregning`. Merk at for at dette skal fungere kan det ikke sendes
   passord ved opprettelse av database-kobling.

5. Om du skal kjøre med frontend og wonderwall må du også kjøre (fra rotmappe):
   `./get-secret.sh apps/etterlatte-saksbehandling-ui`
   og legge til følgende linjer nederst i `.env.dev-gcp` fila til saksbehandling-ui.
```
BEREGNING_API_URL=http://host.docker.internal:8089
BEREGNING_API_SCOPE=api://<BEREGNING_CLIENT_ID>/.default // Se .env.dev-gcp fila du opprettet i steg 1.
```

## Kjøre lokalt

1. Start Mock-OAuth2-Server og Postgres lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
```
AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;
AZURE_APP_CLIENT_ID=clientId;
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
HTTP_PORT=8087;
```

### Teste mot REST-endepunkter

#### Hente token
```
curl --location --request POST 'http://localhost:8082/azure/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_id=clientId' \
--data-urlencode 'client_secret=not_so_secret' \
--data-urlencode 'scope=clientId'
```

#### Kjøre request
- Url: `http://localhost:8080/api/beregning`
- Header: `Authorization: Bearer $token`
