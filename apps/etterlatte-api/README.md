# Etterlatte-api

Tjenste som kobler frontend og backend sammen. Kallene gjøres i hovedsak via REST, med unntak av posting av data til
grunnlag som lager en kafka-melding.

## Koblinger

Tjenesten har koblinger mot databasene i behandling, grunnlag, og vedtaksvurdering.
Den har også en kobling mot pdl-tjenester for oppslag av en person.
Hovedfunksjonalitetene til tjenesten er:

### Uthenting av behandling med vedtak

Tjenesten henter ut en behandling og legger til informasjon fra vedtak. Dette brukes for visningen av hele
behandlingsløpet.

### Lagring av grunnlag

Saksbehandler kan legge til opplysninger i en behandling, og disse blir postet til grunnlagsdatabasen som trigger en ny
vurdering av behandlingen.

### Oppgavebenken

Oppgavelisten blir hentet fra behandlingsdatabasen og sendes så til oppgavebenken i frontend. Mer funksjonalitet skal
inn her senere, så mulig dette blir flyttet til en egen tjeneste på sikt.

### Brukeroversikt

I frontend skal saksbehandler kunne slå opp en person på fødselsnummer, og så informasjon om hvilke saker som ligger
inne. 

