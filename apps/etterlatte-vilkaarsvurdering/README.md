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
KAFKA_RAPID_TOPIC=etterlatte;
KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;
NAIS_APP_NAME=etterlatte-vilkaarsvurdering;
AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;
AZURE_APP_CLIENT_ID=clientId;
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
HTTP_PORT=8087;
```

### Teste mot rapid
Kjør følgende for å poste innholdet i en fil til kafka:
`jq -c . src/test/resources/grunnlagEndret.json | docker exec -i etterlatte-vilkaarsvurdering-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte 'jq'`

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
