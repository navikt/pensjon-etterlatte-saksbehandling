# etterlatte-utbetaling

Tjeneste som håndterer oversettelse av vedtak til utbetalinger mot økonomi, samt sjekker at det er samsvar mellom våre
utbetalinger og utbetalingene iverksatt av økonomi.

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

### Lokal testing
Det er laget flere integrasjonstester som kjører hele flyten. Dette kan i hovedsak erstatte lokal testing.

### Integrasjonstester
Integrasjonstester er satt opp med testcontainers og bruker egene images for Postgres og IBM MQ for å simulere 
henholdsvis database og mottak hos økonomi.

Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir 
"borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende 
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock