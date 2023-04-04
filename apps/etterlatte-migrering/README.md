# etterlatte-migrering

Migrering fra Pesys til Doffen

## Kjøre lokalt (auth og database)

1. For å kjøre lokalt, start postgres ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
```
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
```

## Kjøre lokalt (auth fra dev-gcp + db lokalt eller proxy gcp)
1. For å sette opp riktig konfigurasjon for applikasjonen, kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
```
./get-secret.sh apps/etterlatte-migrering
```
2. Sett følgende environment-variabler under oppstart av applikasjonen.
```
DB_JDBC_URL=jdbc:postgresql://localhost:5432/migrering?user=FORNAVN.ETTERNAVN@nav.no;
DB_PASSWORD=postgres;
DB_USERNAME=postgres;
```
Legg også til `.env.dev-gcp` som `Env-file` under `Run configurations` i Intellij.

3. Kjør opp en lokal postgres-database med `docker-compose up -d`. Alternativt er det mulig å kjøre en proxy mot 
gcp-dev ved å kjøre `nais postgres proxy etterlatte-migrering`. Merk at for at dette skal fungere kan det ikke sendes
passord ved opprettelse av database-kobling.