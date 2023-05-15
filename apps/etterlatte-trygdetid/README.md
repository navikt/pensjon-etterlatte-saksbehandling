# etterlatte-trygdetid

Tjeneste for å opprette trygdetidsgrunnlag og beregne total trygdetid for avdød.

## Kjøre lokalt (auth fra dev-gcp + db lokalt)
1. For å sette opp riktig konfigurasjon for applikasjonen, kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
```
./get-secret.sh etterlatte-trygdetid
```
2. Sett følgende environment-variabler under oppstart av applikasjonen:
```
HTTP_PORT=8088
DB_JDBC_URL=jdbc:postgresql://localhost:5434/postgres;
DB_PASSWORD=postgres;
DB_USERNAME=postgres;
ETTERLATTE_BEHANDLING_CLIENT_ID=59967ac8-009c-492e-a618-e5a0f6b3e4e4;
ETTERLATTE_BEHANDLING_URL=https://etterlatte-behandling.intern.dev.nav.no;
ETTERLATTE_GRUNNLAG_CLIENT_ID=ce96a301-13db-4409-b277-5b27f464d08b;
ETTERLATTE_GRUNNLAG_URL=https://etterlatte-grunnlag.intern.dev.nav.no;
```
3. Legg til `.env.dev-gcp` som `Env-file` under `Run configurations` i Intellij.
4. Kjør opp en lokal postgres-database:
```
docker-compose up -d
```
5. Om du skal kjøre med frontend og wonderwall må du også kjøre (fra rotmappe):
```
./get-secret.sh etterlatte-saksbehandling-ui
```
og legge til følgende linjer nederst i `.env.dev-gcp` fila til saksbehandling-ui.
```
TRYGDETID_API_URL=http://host.docker.internal:8088
TRYGDETID_API_SCOPE=api://8385435e-f3d7-45ec-be59-ebd1c71df735/.default
```