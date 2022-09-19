# Grunnlag

Grunnlag er både en kafka-app og en REST-app.

Tjenesten har som ansvar for henting og lagring av opplysninger, og sammenstellning av opplysningene til ett "grunnlag" for videre bruk.
REST-api:et finnes for å kunne få innsikt i grunnlaget for en sak.

## Konsepter

### Opplysning
Per nå er all informasjon som inngår i en behandling en opplysning. En opplysning er av en type og den har en kilde.

### Grunnlag
Grunnlag er summen av alle opplysninger. Målet med grunnlag er å samle å enkelt strukturere alle opplysninger sån at
det blir enklere å bruke gjeldende informasjon i andre apper.

## Flyt

### Opplysningsbehov
Grunnlag lytter på når nye behandlinger blir opprettet. Då skal grunnlagsappen sende ut opplysningsbehov om personene i familien fra søknaden.

### Lagring av opplysninger
Når nye opplysninger kommer in av noen grunn, f.eks som ett svar på opplysningsbehoven, så skal grunnlagsappen lagre de nye opplysningene og
og sende ett event om at nye opplysninger finnes på saken.

### Legg på grunnlag på meldinger
Når grunnlag leser en melding uten grunnlag så skal den hente det gjeldende grunnlaget for saken og legge på det på meldingene for videre bruk i andre apper.

## Enhetstester
Noen av testene bruker https://www.testcontainers.org/ for å dra opp en docker-container.
Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir "borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 

