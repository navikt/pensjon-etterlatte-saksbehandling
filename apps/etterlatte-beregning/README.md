# etterlatte-beregning

Tjeneste som tilbyr endepunkter for å opprette/oppdatere og hente ut en beregning tilknyttet en behandling

Det som støttes
- Opprette / Oppdatere en beregning
- Hente en beregning
- Slette beregninger tilknyttet en sak ved kafka meldinger fra vedlikeholdsriveren


## Kom i gang
Applikasjonen tilbyr et REST-api. I bakkant ligger en PostgreSQL-database som lager, oppdaterer, henter og sletter 
beregninger tilknyttet en behandling.

### Hvordan kjøre lokalt mot dev-gcp 

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
BEREGNING_API_URL=http://host.docker.internal:8089
```

### Kjøre lokalt med mock auth

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
