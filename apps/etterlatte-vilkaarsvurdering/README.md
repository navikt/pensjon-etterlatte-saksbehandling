# etterlatte-vilkaarsvurdering

Tjeneste som tilbyr endepunkter for å hente ut vilkår for en behandling og sette dem til oppfylt / ikke oppfylt.

Det som støttes så langt er
- Hovedvilkår
- Unntaksvilkår

Utestående funksjonalitet er
- Grunnlag på vilkår
- Automatiserte vilkårsvurdering

## Kjøre lokalt

1. Start Kafka og Mock-OAuth2-Server lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
```
AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;
AZURE_APP_CLIENT_ID=clientId;
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
HTTP_PORT=8088;
```

## Kjøre med auth mot dev-gcp
1. Kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
```
./get-secret.sh apps/etterlatte-vilkaarsvurdering
```
2. Kjør opp en proxy mot postgres i dev: `nais postgres proxy etterlatte-vilkaarsvurdering`, eller kjør opp en lokal 
   instans med `docker-compose up -d`.
3. Kopier inn følgende environment variabler i IntelliJ (merk at passord må kommenteres bort mot databasen dersom man bruker proxy):
```
DB_JDBC_URL=jdbc:postgresql://localhost:5432/vilkaarsvurdering?user=FORNAVN.ETTERNAVN@nav.no;
DB_PASSWORD=postgres;
DB_USERNAME=postgres;
ETTERLATTE_BEHANDLING_CLIENT_ID=59967ac8-009c-492e-a618-e5a0f6b3e4e4;
ETTERLATTE_BEHANDLING_URL=https://etterlatte-behandling.intern.dev.nav.no;
ETTERLATTE_GRUNNLAG_CLIENT_ID=ce96a301-13db-4409-b277-5b27f464d08b;
ETTERLATTE_GRUNNLAG_URL=https://etterlatte-grunnlag.intern.dev.nav.no/api;
HTTP_PORT=8087
```
4. Om du skal kjøre med frontend og wonderwall må du også kjøre (fra rotmappe):
`./get-secret.sh apps/etterlatte-saksbehandling-ui`
og legge til følgende linjer nederst i `.env.dev-gcp` fila til saksbehandling-ui.
```
VILKAARSVURDERING_API_URL=http://host.docker.internal:8087
VILKAARSVURDERING_API_SCOPE=app://<VILKAARSVURDERING_CLIENT_ID>/.default // Se .env.dev-gcp fila du opprettet i steg 1.
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
- Url: `http://localhost:8080/api/vilkaarsvurdering`
- Header: `Authorization: Bearer $token`
