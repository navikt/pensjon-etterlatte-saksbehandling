# BEHANDLING

Tjeneste som holder styr på behandlinger

## Konsepter

### Sak
En sak er et overheng som representerer alt nav veit eller gjør i forbindelse med en spesifikk ytelse til en spesifikk person.

### Behandling
En behandling representerer at noe skal gjøres knyttet til en sak. En behandling gjelder kun en sak, men det kan gjøres mange behandlinger i samme sak.
Et eksempel på behandling er førstegangsbehandling av søknad om barnepensjon.
En Behandling starter når det er identifisert et behandlingsbehov. En behandling innebærer

1. Motta behandlingsbehov
2. Innhente opplysninger
3. Vilkårsprøve
4. Beregne
5. Fatte vedtak
6. Attestere
7. Iverksette

#### Forbedringspotensiale
* Må ha mye mer integritetssjekker
* Har ingen funksjonalitet eller modell for vilkårsprøve, beregne, fatte vedtak, attestere eller iverskette
* Har ikke funksjonalitet for å innhente noen opplysninger automatisk

### Behandlingsbehov
Et behandlingsbehov representerer at det har skjedd noe som gjør at det må gjøres en behandling i en sak. Det kan for eksempel være at Nav har mottat en søknad.
#### Forbedringspotensiale
* Pt bare en referanse til saken det skal opprettes behandling på

### Opplysning
Pr nå er all informasjon som inngår i en behandling en opplysning. En opplysning er av en type og den har en kilde.
#### Forbedringspotensiale
 * Opplysninger må kanskje kunne periodiseres avhengig av hvordan vi tenker på tidslinjer (eks: Skal et ekteskap være en opplysning med fom og potensiell tom-dato, eller er det en opplysning om at ekteskap er innkått og en annen opplysning om at ekteskap er opphørt)

## Enhetstester
Noen av testene bruker https://www.testcontainers.org/ for å dra opp en docker-container.
Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock blir "borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 

