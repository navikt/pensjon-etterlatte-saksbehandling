# etterlatte-utbetaling

Tjeneste som håndterer oversettelse av vedtak til utbetalinger mot økonomi, samt sjekker at det er samsvar mellom våre
utbetalinger og utbetalingene iverksatt av økonomi.

## Oppsett mot oppdrag
En beskrivelse for dette finnes på confluence: https://confluence.adeo.no/display/TE/Koble+opp+mot+Oppdrag+med+MQ

## Konsepter

### Iverksetting
Iverksetting foregår ved at det lyttes etter vedtak som så gjøres om til en utbetaling med utbetalingslinjer basert på 
periodene i vedtaket. Utbetalingen vil så sendes til økonomi over MQ. 

Det lyttes så på kvitteringer fra økonomi for å se om utbetalingen var vellykket eller om det oppsto noen problemer.  

### Grensesnittavstemming
grensesnittavstemming er implementert som en jobb som kjører en gang i døgnet og overfører til økonomi hvilke 
utbetalinger som er opprettet siden forrige gang avstemming kjørte og status på disse. Innholdet i en slik avstemming 
er fordelingen av utbetalinger på de ulike typene statuser og hvilke totalbeløper som er registrert for hver av disse. 
Hensikten med dette er å påse at fagsystemet og okonomi er i synk.

Ved registrert avvik hos økonomi, vil teamet kontaktes for å finne ut av hva dette skyldes. Det er altså ingen måte å 
kommunisere dette med feks tjenestekall.

### Konsistensavstemming
Arbeidet med dette er ikke påbegynt da de ikke ble sett på som hensiktsmessig før vi har kommet lenger med utarbeidelse
av vedtak.

### Simulering
Så langt er det ikke definert om simulering er noe vi skal støtte.


## Testing

### Kjøre lokalt

1. Start Kafka lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
   `KAFKA_RAPID_TOPIC=etterlatte;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;NAIS_APP_NAME=etterlatte-utbetaling;DB_DATABASE=postgres;DB_HOST=localhost;DB_PORT=5432;DB_USERNAME=postgres;DB_PASSWORD=postgres;OPPDRAG_MQ_HOSTNAME=localhost;OPPDRAG_MQ_PORT=1414;OPPDRAG_MQ_MANAGER=QM1;OPPDRAG_MQ_CHANNEL=DEV.ADMIN.SVRCONN;OPPDRAG_SEND_MQ_NAME=DEV.QUEUE.1;OPPDRAG_KVITTERING_MQ_NAME=DEV.QUEUE.2;OPPDRAG_AVSTEMMING_MQ_NAME=DEV.QUEUE.1;srvuser=admin;srvpwd=passw0rd;ELECTOR_PATH=localhost:1080`
3. Dersom man ønsker å sjekke avstemmingsjobb må `hostname` settes under feltet "name" i `api-mock/elector-response.json`

#### Teste mot rapid
Kjør følgende for å poste innholdet i en fil til kafka:
`jq -c . src/test/resources/vedtak.json | docker exec -i etterlatte-utbetaling-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte 'jq'`

Merk at det også er laget flere integrasjonstester som kjører hele flyten. Dette kan i stor grad erstatte lokal testing.


### Integrasjonstester
Integrasjonstester er satt opp med testcontainers og bruker egene images for Postgres og IBM MQ for å simulere 
henholdsvis database og mottak hos økonomi.

Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir 
"borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende 
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock

### Integrasjonstester, MQ og M1
Fram til bugen i https://stackoverflow.com/questions/71096568/ibm-mq-for-apple-silicon er fiksa, vil IBM MQ feile ved køyring på Apple M1-arkitekturen. 
Derfor har vi ein mekanisme (annotasjon) som gjer at desse testane vil bli hoppa over for M1-chipsets. 
Ved bygging på GitHub Actions brukar dei ein annan arkitektur, så der vil uansett desse testane køyre (kan jo sjølvsagt endre seg, men verkar lite plausibelt).