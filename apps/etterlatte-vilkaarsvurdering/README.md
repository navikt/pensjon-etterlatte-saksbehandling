# etterlatte-vilkaarsvurdering

Tjeneste som tilbyr endepunkter for å hente ut vilkår for en behandling og sette dem til oppfylt / ikke oppfylt.

## Kjøre lokalt

1. Start Kafka lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
   `KAFKA_RAPID_TOPIC=etterlatte;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;KAFKA_CONSUMER_GROUP_ID=0;NAIS_APP_NAME=etterlatte-vilkaarsvurdering;DEV_MODE=TRUE`

### Teste rapid
Kjør følgende for å poste innholdet i en fil `test.json` til kafka: `cat test.json | docker exec -i etterlatte-vilkaarsvurdering-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte 'cat'`.
Merk at innholdet må være på en enkelt linje.

### Teste REST-endepunkter
Gjør requester mot endepunkter under `http://localhost:8080/api/vilkaarsvurdering`