# etterlatte-vedtaksvurdering

Tjeneste som tilbyr forslag til vedtak som kan fattes og attesteres

## Kjøre lokalt

1. Start Kafka, lokal Postgres og Mock-OAuth2-Server lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
```
KAFKA_RAPID_TOPIC=etterlatte;
KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;
NAIS_APP_NAME=etterlatte-vedtaksvurdering;
AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;
AZURE_APP_CLIENT_ID=clientId;
DB_JDBC_URL=jdbc:postgresql://localhost:5433/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
HTTP_PORT=8085;
```
### Teste REST-endepunkter
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
- Url: `http://localhost:8085` 
- Header: `Authorization: Bearer $token`
