# etterlatte-tilbakekreving

Tjeneste som håndterer tilbakekrevingsvedtak fra økonomi og oppretter tilbakekrevinger i fagsystemet.

Denne tjenesten er kun påbegynt men har blitt prioritert bort inntil videre på grunn av andre og viktigere oppgaver.

## Konsepter

### Mottak av vedtak
Tilbakekrevingsvedtak mottas fra en MQ kø. Disse vil så lagres ned i databasen og trigge en hendelse om at det har
kommet en tilbakekreving.

### Tjeneste for å hente tilbakekrevingsvedtak
Det vil være mulig å hente tilbakekrevingsvedtak via et REST-grensesnitt, men dette er bare påbegynt.

### Sending av vedtak
Ikke påbegynt


## Testing

### Kjøre lokalt

1. Start Kafka lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
   `KAFKA_RAPID_TOPIC=etterlatte;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;NAIS_APP_NAME=etterlatte-tilbakekreving;DB_DATABASE=postgres;DB_HOST=localhost;DB_PORT=5432;DB_USERNAME=postgres;DB_PASSWORD=postgres;OPPDRAG_MQ_HOSTNAME=localhost;OPPDRAG_MQ_PORT=1414;OPPDRAG_MQ_MANAGER=QM1;OPPDRAG_MQ_CHANNEL=DEV.ADMIN.SVRCONN;OPPDRAG_KRAVGRUNNLAG_MQ_NAME=DEV.QUEUE.1;srvuser=admin;srvpwd=passw0rd`
3. Sett følgende i VM options: `-Dio.ktor.development=true`

### Teste mot rapid
Ikke satt opp ennå

### Teste mot REST-endepunkter
- Gjør requester mot endepunkter under `http://localhost:8080/api/tilbakekreving`
- Følgende header må settes for å få inn saksbehandler riktig i forespørselene: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY`

### Integrasjonstester
Integrasjonstester er satt opp med testcontainers og bruker egene images for Postgres og IBM MQ for å simulere
henholdsvis database og mottak hos økonomi.

Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir
"borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock
