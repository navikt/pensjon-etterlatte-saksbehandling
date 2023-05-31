# etterlatte-brev-api

Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.


## Lokal utvikling

### Krav for å kjøre lokalt
- **Kafka** må kjøres lokalt. Docker er anbefalt.
- **Postgres**: Kjør f. eks opp PostgreSQL med Docker eller bruk [Postgres.app](https://postgresapp.com/)
- **ey-pdfgen**: Repo [pensjon-etterlatte-felles](https://github.com/navikt/pensjon-etterlatte-felles) må klones. Deretter kan pdfgen kjøres via Docker.  

### Hvordan kjøre brev-api

Det enkleste er å kjøre opp frontend og docker compose som starter alt man trenger for at APIet skal fungere.  

Kommandoen under må kjøres _før_ docker compose og trenger kun kjøres én gang.

OBS: Hvis du bruker bash i stedet for zsh må du bytte ut `~/.zshrc` med `~/.bashrc`  

```shell
echo "export PDFGEN_HOME=$(find ~/ -type d -name ey-pdfgen -print -quit 2>/dev/null)" >> ~/.zshrc \
  && source ~/.zshrc
```


1. Kjør docker compose
    ```shell
    docker compose up -d
    ```

2. Opprett/oppdater app config i IntelliJ
   
    Kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
    ```shell
    ../../get-secret.sh etterlatte-brev-api
    ```

    Om du skal kjøre med frontend og wonderwall må du også kjøre:
    ```shell
    ../../get-secret.sh etterlatte-saksbehandling-ui
    ```
    **OBS:** Siden vi går mot Norg2 APIet i dev (fra lokal maskin) må du koble til Naisdevice.

3. Kjør lagret run config \
    Config for å kjøre appen (i IntelliJ) ligger i `.run`. Denne skal dukke opp automatisk under IntelliJ sin liste
    over `Run configurations` med navnet `etterlatte-brev-api.dev-gcp`
