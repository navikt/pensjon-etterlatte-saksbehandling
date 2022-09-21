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

### Lokal testing
En rigg for lokal testing er ikke satt opp pr nå.

### Integrasjonstester
Integrasjonstester er satt opp med testcontainers og bruker egene images for Postgres og IBM MQ for å simulere
henholdsvis database og mottak hos økonomi.

Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir
"borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 


## Varsling
Ikke satt opp noe pr nå.