# etterlatte-brev-distribusjon

Rapid app for å distribuere brev. Håndterer journalføring og distribusjon. 


## Lokal utvikling
1. Kafka må kjøres lokalt. Anbefale å benytte [Red Panda](https://redpanda.com/), som ikke trenger zookeeper.
2. Krever kobling mot brev-databasen. Kjør f. eks opp PostgreSQL med Docker.

Legg f. eks til følgende environment variabler i IntelliJ: 

`DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;DB_USERNAME=postgres;DB_PASSWORD=postgres;BREV_LOCAL_DEV=true;KAFKA_RAPID_TOPIC=brev;KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:51336;KAFKA_CONSUMER_GROUP_ID=0;NAIS_APP_NAME=etterlatte-brev-distribusjon;HTTP_PORT=8090;`