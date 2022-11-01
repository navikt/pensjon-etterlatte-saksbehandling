# etterlatte-vedtaksvurdering

Tjeneste som tilbyr forslag til vedtak som kan fattes og attesteres

## Kjøre lokalt

1. Start Kafka og prostgres lokalt ved å kjøre `docker compose up -d` eller `docker-compose up -d`
2. Start app lokalt ved enten å:
   1. Kjøre den vanlige main-metoden:
      1. Sett følgende miljøvariabler ved oppstart av applikasjon: 
         `LOCAL_DEV=true;KAFKA_RAPID_TOPIC=etterlatte.dodsmelding;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;NAIS_APP_NAME=etterlatte-vedtakvurdering;DB_JDBC_URL=jdbc:postgresql://localhost:5433/postgres;DB_PASSWORD=postgres;DB_USERNAME=postgres;HTTP_PORT=8086`
      2. start no.nav.etterlatte.ApplicationKt.main
   2. Starte no.nav.etterlatte.app.LocalDevApplicationKt.main

### Teste REST-endepunkter
Gjør requester mot endepunkter under `http://localhost:8085/api/`