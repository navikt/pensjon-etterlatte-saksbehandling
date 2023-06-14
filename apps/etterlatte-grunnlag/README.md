# Grunnlag

Grunnlag er både en kafka-app og en REST-app.

Tjenesten har ansvar for henting og lagring av opplysninger, og sammenstillning av opplysningene til ett "grunnlag" for videre bruk.
REST-api'et tilbyr tjenester for å hente ut enkeltopplysninger fra et grunnlag i en sak.

## Kom i gang

### Hvordan kjøre lokalt mot dev-gcp

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
GRUNNLAG_API_URL=http://host.docker.internal:8092
```

## Konsepter

### Opplysning
Per nå er all informasjon som inngår i en behandling en opplysning. En opplysning er av en type og den har en kilde.

En opplysning kan være `Konstant` eller `Periodisert`.
En `Periodisert` opplysning har en fom- og en tom-dato den gjelder for mens en konstant opplysning er alltid gjeldende.
En opplysning som har fnr er en opplysning på tilhørende person, mens en opplysning som har fnr lik null, er en opplysning på saksnivå.

Eksempel på en konstant opplysning: Fødselsdato\
Eksempel på en periodisert opplysning: Adresse

### Grunnlag
Grunnlag er summen av alle opplysninger i en sak. Målet med grunnlag er å samle, og enkelt strukturere alle opplysninger sånn at
det blir enklere å bruke gjeldende informasjon i andre apper.

## Flyt

### Opplysningsbehov
Grunnlag lytter på når nye behandlinger blir opprettet. Da skal grunnlagsappen sende ut opplysningsbehov om personene i familien fra søknaden.

### Lagring av opplysninger
Når nye opplysninger kommer inn, f.eks som ett svar på opplysningsbehoven, så skal grunnlagsappen lagre de nye opplysningene og
og sende en event om at nye opplysninger finnes på saken.

### Legg på grunnlag på meldinger
Når grunnlag leser en melding uten grunnlag så skal den hente det gjeldende grunnlaget for saken og legge på det på meldingene for videre bruk i andre apper.

## Enhetstester
Noen av testene bruker https://www.testcontainers.org/ for å dra opp en docker-container.
Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir "borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 

