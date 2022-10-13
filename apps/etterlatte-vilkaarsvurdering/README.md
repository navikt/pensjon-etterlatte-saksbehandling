# etterlatte-vilkaarsvurdering

Tjeneste som tilbyr endepunkter for å hente ut vilkår for en behandling og sette dem til oppfylt / ikke oppfylt.

Det som støttes så langt er
- Hovedvilkår
- Unntaksvilkår

Utestående funksjonalitet er
- Grunnlag på vilkår
- Automatiserte vilkårsvurdering

## Kjøre lokalt

1. Start Kafka lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
   `KAFKA_RAPID_TOPIC=etterlatte;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;NAIS_APP_NAME=etterlatte-vilkaarsvurdering`
3. Sett følgende i VM options: `-Dio.ktor.development=true`

### Teste mot rapid
Kjør følgende for å poste innholdet i en fil til kafka:
`jq -c . src/test/resources/grunnlagEndret.json | docker exec -i etterlatte-vilkaarsvurdering-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte 'jq'`

### Teste mot REST-endepunkter
- Gjør requester mot endepunkter under `http://localhost:8080/api/vilkaarsvurdering`
- Følgende header må settes for å få inn saksbehandler riktig i forespørselene: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY`